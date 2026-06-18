from typing import Any, Optional

from pydantic import BaseModel, Field


class AgentQueryRequest(BaseModel):
    question: str = Field(..., min_length=1, description="Natural language question")
    user_id: Optional[int] = Field(default=None, description="Operator user id")


class AgentQueryResponse(BaseModel):
    routeType: Optional[str] = None
    generatedSql: str = ""
    generated_sql: str = ""
    query_result: Any = None
    retrievedChunks: list[Any] = Field(default_factory=list)
    answer: str = ""
    success: bool
    error_message: Optional[str] = None


class RagIndexRequest(BaseModel):
    document_id: int = Field(..., ge=1, description="Knowledge document id")
    file_path: str = Field(..., min_length=1, description="Document file path")
    file_name: str = Field(..., min_length=1, description="Original file name")


class RagChunkInfo(BaseModel):
    document_id: int
    chunk_index: int
    content: str
    vector_id: Optional[str] = None
    score: Optional[float] = None
    file_name: Optional[str] = None


class RagIndexResponse(BaseModel):
    success: bool
    document_id: int
    chunk_count: int
    message: str
    chunks: list[RagChunkInfo] = Field(default_factory=list)


class RagQueryRequest(BaseModel):
    question: str = Field(..., min_length=1, description="Knowledge question")
    top_k: int = Field(default=4, ge=1, le=20, description="Retrieved chunk count")


class RagQueryResponse(BaseModel):
    answer: str
    retrieved_chunks: list[RagChunkInfo] = Field(default_factory=list)
    sources: list[str] = Field(default_factory=list)
