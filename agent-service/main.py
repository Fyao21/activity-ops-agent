import logging
from functools import lru_cache

from fastapi import FastAPI, HTTPException

from agent import ActivitySQLAgent
from hybrid_service import HybridAgent
from question_router import route_question
from rag_service import RagService
from schemas import (
    AgentQueryRequest,
    AgentQueryResponse,
    RagIndexRequest,
    RagIndexResponse,
    RagQueryRequest,
    RagQueryResponse,
)
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


@lru_cache
def get_rag_service() -> RagService:
    return RagService()


@lru_cache
def get_hybrid_agent() -> HybridAgent:
    return HybridAgent(get_agent(), get_rag_service())


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/agent/query", response_model=AgentQueryResponse)
def query_agent(request: AgentQueryRequest) -> AgentQueryResponse:
    try:
        route_type = route_question(request.question)
        if route_type == "rag":
            rag_result = get_rag_service().query(request.question, top_k=4)
            result = {
                "routeType": "rag",
                "generatedSql": "",
                "generated_sql": "",
                "query_result": [],
                "retrievedChunks": rag_result.get("retrieved_chunks", []),
                "answer": rag_result.get("answer", ""),
                "success": True,
                "error_message": None,
            }
        elif route_type == "hybrid":
            result = get_hybrid_agent().query(request.question, request.user_id)
        else:
            result = get_agent().query(request.question, request.user_id)
            result["routeType"] = "sql"
            result["generatedSql"] = result.get("generated_sql", "")
            result["retrievedChunks"] = []
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

@app.post("/rag/index", response_model=RagIndexResponse)
def index_rag_document(request: RagIndexRequest) -> RagIndexResponse:
    try:
        result = get_rag_service().index_document(
            document_id=request.document_id,
            file_path=request.file_path,
            file_name=request.file_name,
        )
        return RagIndexResponse(**result)
    except Exception as exc:
        logging.exception("RAG index failed")
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/rag/query", response_model=RagQueryResponse)
def query_rag(request: RagQueryRequest) -> RagQueryResponse:
    try:
        result = get_rag_service().query(
            question=request.question,
            top_k=request.top_k,
        )
        return RagQueryResponse(**result)
    except Exception as exc:
        logging.exception("RAG query failed")
        raise HTTPException(status_code=500, detail=str(exc)) from exc
