import json
import logging
import re
from typing import Any, Dict, List

from langchain.chains import create_sql_query_chain
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_core.prompts import PromptTemplate
from langchain_openai import ChatOpenAI

from db import ALLOWED_TABLES, execute_query, get_settings, get_sql_database
from sql_guard import guard_sql, strip_sql_fence


logger = logging.getLogger(__name__)


SQL_PROMPT = PromptTemplate.from_template(
    """You are an education learning analytics SQL generator for a course learning assistant platform.
You must follow these rules:
- Only generate one MySQL SELECT statement.
- Do not generate INSERT, UPDATE, DELETE, DROP, ALTER, TRUNCATE, CREATE, WITH, or multiple statements.
- Never query sys_user.password or any password field.
- Only use these tables: {table_info}
- If the user's question is unrelated to course learning, course documents, Q&A records, learning events, questions, or answer records, return exactly REFUSE.
- Prefer these tables for common tasks:
  - course: course names and teachers.
  - agent_qa_record: student questions and route types.
  - learning_event: learning activity such as QUESTION, ANSWER, VIEW_COURSE, UPLOAD_DOC.
  - question: question content and knowledge_point.
  - answer_record: student answers and correctness.
  - sys_user: usernames and roles. Never select password.
  - knowledge_document and knowledge_chunk: course knowledge documents.
- Use course.course_name when grouping by course.
- For question count analytics, use agent_qa_record unless the user explicitly asks for learning_event event_type='QUESTION'.
- For learning activity analytics, use learning_event.
- For accuracy analytics, use answer_record.correct where 1 means correct and 0 means wrong.
- For wrong knowledge point analytics, join answer_record to question and group by question.knowledge_point where answer_record.correct = 0.
- For student ranking, join answer_record.user_id to sys_user.id and select sys_user.username.
- Return raw SQL only. Do not wrap it in markdown. Do not explain anything.

Database dialect: {dialect}
Top K default rows: {top_k}

Question: {input}"""
)


SUMMARY_SYSTEM_PROMPT = (
    "你是一个课程学习数据分析 Agent。\n"
    "你必须基于真实 SQL 查询结果回答，不能编造数据。\n"
    "如果结果为空，明确说明没有查到数据。\n"
    "回答要直接、清晰，适合教师或管理员理解。\n"
    "涉及课程、知识点、学生或正确率时，请给出关键名称和数字。"
)


class EduSQLAgent:
    def __init__(self) -> None:
        settings = get_settings()
        self.db = get_sql_database()
        self.llm = ChatOpenAI(
            model=settings.model_name,
            api_key=settings.openai_api_key,
            base_url=settings.openai_base_url,
            temperature=0,
            timeout=60,
        )
        self.sql_chain = create_sql_query_chain(
            llm=self.llm,
            db=self.db,
            prompt=SQL_PROMPT,
            k=100,
        )

    def query(self, question: str, user_id: int | None = None) -> Dict[str, Any]:
        del user_id
        sql = self._generate_sql(question)
        if sql.upper() == "REFUSE":
            return {
                "generated_sql": "",
                "query_result": [],
                "answer": "该问题与课程学习数据分析无关，当前服务拒绝回答。",
                "success": False,
                "error_message": "Question is not related to the education learning database.",
            }

        try:
            guarded_sql = guard_sql(sql, default_limit=100)
            logger.info("Generated SQL: %s", guarded_sql)
            query_result = execute_query(guarded_sql)
        except Exception as exc:
            logger.warning("SQL execution failed, retrying once: %s", exc)
            guarded_sql, query_result = self._repair_and_retry(question, sql, exc)

        answer = self._summarize(question, guarded_sql, query_result)
        return {
            "generated_sql": guarded_sql,
            "query_result": query_result,
            "answer": answer,
            "success": True,
            "error_message": None,
        }

    def _generate_sql(self, question: str) -> str:
        raw_sql = self.sql_chain.invoke(
            {
                "question": question,
                "table_names_to_use": ALLOWED_TABLES,
            }
        )
        sql = strip_sql_fence(str(raw_sql)).strip()
        return re.sub(r"\s+", " ", sql)

    def _repair_and_retry(
        self,
        question: str,
        original_sql: str,
        error: Exception,
    ) -> tuple[str, List[Dict[str, Any]]]:
        repair_prompt = [
            SystemMessage(
                content=(
                    "You fix MySQL SELECT statements for an education learning analytics database. "
                    "Return exactly one corrected SELECT statement. "
                    "Use only these tables: "
                    f"{', '.join(ALLOWED_TABLES)}. "
                    "Do not use WITH. Do not access password fields. "
                    "Do not return markdown."
                )
            ),
            HumanMessage(
                content=(
                    f"Question: {question}\n"
                    f"Allowed tables: {', '.join(ALLOWED_TABLES)}\n"
                    f"Original SQL: {original_sql}\n"
                    f"Execution error: {error}\n"
                    "Return corrected SQL only."
                )
            ),
        ]
        repaired_sql = self.llm.invoke(repair_prompt).content
        guarded_sql = guard_sql(str(repaired_sql), default_limit=100)
        logger.info("Repaired SQL: %s", guarded_sql)
        return guarded_sql, execute_query(guarded_sql)

    def _summarize(
        self,
        question: str,
        sql: str,
        query_result: List[Dict[str, Any]],
    ) -> str:
        payload = json.dumps(query_result, ensure_ascii=False, default=str)
        messages = [
            SystemMessage(content=SUMMARY_SYSTEM_PROMPT),
            HumanMessage(
                content=(
                    f"用户问题：{question}\n"
                    f"执行 SQL：{sql}\n"
                    f"查询结果：{payload}\n"
                    "请输出中文分析结论。"
                )
            ),
        ]
        response = self.llm.invoke(messages)
        return str(response.content).strip()
