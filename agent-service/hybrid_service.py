import json
from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage

from agent import EduSQLAgent
from rag_service import NO_CONTEXT_ANSWER, RagService


HYBRID_SYSTEM_PROMPT = (
    "你是智能课程学习分析 Agent。\n"
    "你会同时获得课程资料上下文和数据库查询结果。\n"
    "请结合两部分信息生成学习分析。\n"
    "不要编造数据库中不存在的数据。\n"
    "不要编造课程资料中不存在的内容。\n"
    "如果资料不足，请明确说明。\n"
    "输出包括：\n"
    "1. 相关知识点摘要；\n"
    "2. 学习数据摘要；\n"
    "3. 薄弱点分析；\n"
    "4. 复习建议。"
)


class HybridAgent:
    def __init__(self, sql_agent: EduSQLAgent, rag_service: RagService) -> None:
        self.sql_agent = sql_agent
        self.rag_service = rag_service

    def query(
        self,
        question: str,
        user_id: int | None = None,
        course_id: int | None = None,
    ) -> dict[str, Any]:
        rag_result = self.rag_service.retrieve(question, top_k=4, course_id=course_id)
        sql_result = self.sql_agent.query(question, user_id)
        answer = self._summarize(question, rag_result, sql_result)
        query_result = sql_result.get("queryResult", sql_result.get("query_result", []))
        generated_sql = sql_result.get("generatedSql", sql_result.get("generated_sql", ""))

        return {
            "routeType": "hybrid",
            "generatedSql": generated_sql,
            "generated_sql": generated_sql,
            "queryResult": query_result,
            "query_result": query_result,
            "retrievedChunks": rag_result.get("retrieved_chunks", []),
            "answer": answer,
            "success": bool(sql_result.get("success", True)),
            "errorMessage": sql_result.get("errorMessage", sql_result.get("error_message")),
            "error_message": sql_result.get("errorMessage", sql_result.get("error_message")),
        }

    def _summarize(
        self,
        question: str,
        rag_result: dict[str, Any],
        sql_result: dict[str, Any],
    ) -> str:
        context = rag_result.get("context") or NO_CONTEXT_ANSWER
        sql_payload = json.dumps(
            {
                "generatedSql": sql_result.get("generatedSql", ""),
                "queryResult": sql_result.get("queryResult", []),
                "sqlAnswer": sql_result.get("answer", ""),
                "success": sql_result.get("success", False),
                "errorMessage": sql_result.get("errorMessage"),
            },
            ensure_ascii=False,
            default=str,
        )
        messages = [
            SystemMessage(content=HYBRID_SYSTEM_PROMPT),
            HumanMessage(
                content=(
                    f"用户问题：{question}\n\n"
                    f"课程资料上下文：\n{context}\n\n"
                    f"数据库查询结果：\n{sql_payload}\n\n"
                    "请按要求输出中文学习分析。"
                )
            ),
        ]
        response = self.sql_agent.llm.invoke(messages)
        return str(response.content).strip()
