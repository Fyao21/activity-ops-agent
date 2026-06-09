import logging
from functools import lru_cache

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from agent import ActivitySQLAgent
from db import get_engine
from schemas import AgentQueryRequest, AgentQueryResponse
from sql_guard import SQLGuardError
from sqlalchemy import text


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)

app = FastAPI(
    title="Activity Agent Service",
    version="1.0.0",
    description="FastAPI + LangChain Text-to-SQL service for activity analytics.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@lru_cache
def get_agent() -> ActivitySQLAgent:
    return ActivitySQLAgent()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/health/ready")
def readiness() -> dict:
    """Readiness probe: checks DB connectivity.
    Does NOT leak internal error details — only returns ok/unreachable."""
    checks = {}
    try:
        with get_engine().connect() as conn:
            result = conn.execute(text("SELECT 1")).scalar()
            checks["mysql"] = "ok" if result == 1 else "unreachable"
    except Exception:
        logging.exception("MySQL health check failed")
        checks["mysql"] = "unreachable"
    checks["status"] = "ready" if checks.get("mysql") == "ok" else "degraded"
    return checks


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
