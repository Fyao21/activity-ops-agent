import logging
from functools import lru_cache

from fastapi import FastAPI

from agent import ActivitySQLAgent
from schemas import AgentQueryRequest, AgentQueryResponse
from sql_guard import SQLGuardError


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)

app = FastAPI(
    title="Activity Agent Service",
    version="1.0.0",
    description="FastAPI + LangChain Text-to-SQL service for activity analytics.",
)


@lru_cache
def get_agent() -> ActivitySQLAgent:
    return ActivitySQLAgent()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/agent/query", response_model=AgentQueryResponse)
def query_agent(request: AgentQueryRequest) -> AgentQueryResponse:
    try:
        result = get_agent().query(request.question, request.user_id)
        return AgentQueryResponse(**result)
    except SQLGuardError as exc:
        logging.exception("SQL guard rejected query")
        return AgentQueryResponse(
            generated_sql="",
            query_result=[],
            answer="SQL 校验未通过，已拒绝执行。",
            success=False,
            error_message=str(exc),
        )
    except Exception as exc:
        logging.exception("Agent query failed")
        return AgentQueryResponse(
            generated_sql="",
            query_result=[],
            answer="查询失败，请稍后重试。",
            success=False,
            error_message=str(exc),
        )
