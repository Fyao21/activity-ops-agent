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
    """你是智能课程学习助手平台的教育学习数据分析 SQL Agent。

请把教师或管理员的自然语言问题转换为 MySQL 查询语句。

强制规则：
- 只能生成一条 MySQL SELECT 语句。
- 禁止 INSERT、UPDATE、DELETE、DROP、ALTER、TRUNCATE、CREATE、REPLACE、MERGE、CALL、GRANT、REVOKE。
- 禁止多语句。
- 禁止查询 sys_user.password，也禁止查询任何 password 字段。
- 只能使用下列数据库表，不允许使用其他表：
{table_info}
- 如果问题和课程、学生、学习行为、问答记录、题目、答题、知识库文档无关，返回 REFUSE。
- 只返回 SQL 本身，不要 markdown，不要解释。

业务表说明：
- sys_user：用户信息，字段 id、username、role。严禁查询 password。
- course：课程信息，字段 id、course_name、teacher_id、description、status。
- knowledge_document：课程资料文档，字段 id、course_id、file_name、file_type、status、chunk_count、create_time。
- agent_qa_record：Agent 问答记录，字段 user_id、course_id、question、route_type、success、create_time。
- learning_event：学习行为，字段 user_id、course_id、event_type、event_time、message_key。
- question：题目，字段 id、course_id、knowledge_point、question_content、option_a、option_b、option_c、option_d、answer、analysis。
- answer_record：答题记录，字段 user_id、course_id、question_id、user_answer、correct、create_time，其中 correct=1 表示正确，correct=0 表示错误。

常见分析口径：
- 统计提问次数：优先使用 agent_qa_record，并按 course_id 关联 course。
- 学习活跃度：使用 learning_event，按 event_type、course_id、user_id 聚合。
- 正确率：使用 AVG(answer_record.correct) 或 SUM(correct)/COUNT(*)。
- 错题最多的知识点：answer_record 关联 question，筛选 answer_record.correct=0，按 question.knowledge_point 分组。
- 答题错误最多的学生：answer_record 关联 sys_user，筛选 correct=0，按 user_id/username 分组。
- 涉及课程名称时，使用 course.course_name。
- 涉及“最近 7 天/最近一周”时，使用 NOW() 和 INTERVAL 7 DAY。
- 查询结果默认最多返回 {top_k} 行。

数据库方言：{dialect}

用户问题：{input}"""
)


SUMMARY_SYSTEM_PROMPT = (
    "你是课程学习数据分析助手。"
    "你只能基于 SQL 查询结果回答，不能编造数据。"
    "如果查询结果为空，请明确说明没有查到相关数据。"
    "回答要面向教师或管理员，直接、清晰，突出课程、知识点、学生、次数、正确率等关键指标。"
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
                "generatedSql": "",
                "generated_sql": "",
                "queryResult": [],
                "query_result": [],
                "answer": "该问题与教育学习数据分析无关，当前服务拒绝回答。",
                "success": False,
                "errorMessage": "Question is not related to the education learning database.",
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
            "generatedSql": guarded_sql,
            "generated_sql": guarded_sql,
            "queryResult": query_result,
            "query_result": query_result,
            "answer": answer,
            "success": True,
            "errorMessage": None,
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
                    "你负责修复教育学习数据分析场景下的 MySQL SELECT。"
                    "只返回一条修复后的 SELECT 语句。"
                    f"只能使用这些表：{', '.join(ALLOWED_TABLES)}。"
                    "禁止 WITH、禁止多语句、禁止 password 字段、禁止 markdown。"
                )
            ),
            HumanMessage(
                content=(
                    f"用户问题：{question}\n"
                    f"允许表：{', '.join(ALLOWED_TABLES)}\n"
                    f"原 SQL：{original_sql}\n"
                    f"执行错误：{error}\n"
                    "请只返回修复后的 SQL。"
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
