import logging
from functools import lru_cache

from fastapi import FastAPI, HTTPException

from agent import EduSQLAgent
from hybrid_service import HybridAgent
from question_router import route_question
from rag_service import RagService
from schemas import (
    AgentQueryRequest,
    AgentQueryResponse,
    RagDeleteRequest,
    RagDeleteResponse,
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
    title="Edu Agent Service",
    version="1.0.0",
    description="FastAPI + LangChain service for course RAG, Text-to-SQL, and Hybrid Agent.",
)


@lru_cache
def get_agent() -> EduSQLAgent:
    return EduSQLAgent()


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
            result = _query_rag_route(request)
        elif route_type == "hybrid":
            result = get_hybrid_agent().query(
                request.question,
                user_id=request.user_id,
                course_id=request.course_id,
            )
        else:
            result = get_agent().query(request.question, request.user_id)
            result["routeType"] = "sql"
            result["retrievedChunks"] = []
        return AgentQueryResponse(**_normalize_agent_response(result))
    except SQLGuardError as exc:
        logging.exception("SQL guard rejected query")
        return AgentQueryResponse(**_failed_agent_response("SQL 校验未通过，已拒绝执行。", str(exc)))
    except Exception as exc:
        logging.exception("Agent query failed")
        return AgentQueryResponse(**_failed_agent_response("查询失败，请稍后重试。", str(exc)))


def _query_rag_route(request: AgentQueryRequest) -> dict:
    rag_result = get_rag_service().query(
        course_id=request.course_id,
        question=request.question,
        top_k=4,
    )
    return {
        "routeType": "rag",
        "generatedSql": "",
        "generated_sql": "",
        "queryResult": [],
        "query_result": [],
        "retrievedChunks": rag_result.get("retrieved_chunks", []),
        "answer": rag_result.get("answer", ""),
        "success": True,
        "errorMessage": None,
        "error_message": None,
    }


def _normalize_agent_response(result: dict) -> dict:
    generated_sql = result.get("generatedSql", result.get("generated_sql", ""))
    query_result = result.get("queryResult", result.get("query_result", []))
    error_message = result.get("errorMessage", result.get("error_message"))
    return {
        "routeType": result.get("routeType"),
        "generatedSql": generated_sql,
        "generated_sql": generated_sql,
        "queryResult": query_result,
        "query_result": query_result,
        "retrievedChunks": result.get("retrievedChunks", []),
        "answer": result.get("answer", ""),
        "success": bool(result.get("success", False)),
        "errorMessage": error_message,
        "error_message": error_message,
    }


def _failed_agent_response(answer: str, error_message: str) -> dict:
    return {
        "routeType": None,
        "generatedSql": "",
        "generated_sql": "",
        "queryResult": [],
        "query_result": [],
        "retrievedChunks": [],
        "answer": answer,
        "success": False,
        "errorMessage": error_message,
        "error_message": error_message,
    }


@app.post("/rag/index", response_model=RagIndexResponse)
def index_rag_document(request: RagIndexRequest) -> RagIndexResponse:
    try:
        result = get_rag_service().index_document(
            document_id=request.document_id,
            course_id=request.course_id,
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
            course_id=request.course_id,
            question=request.question,
            top_k=request.top_k,
        )
        return RagQueryResponse(**result)
    except Exception as exc:
        logging.exception("RAG query failed")
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/rag/delete", response_model=RagDeleteResponse)
def delete_rag_document(request: RagDeleteRequest) -> RagDeleteResponse:
    try:
        result = get_rag_service().delete_document(request.document_id)
        return RagDeleteResponse(**result)
    except Exception as exc:
        logging.exception("RAG delete failed")
        raise HTTPException(status_code=500, detail=str(exc)) from exc
