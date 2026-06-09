from typing import Any, Optional

from pydantic import BaseModel, Field


class AgentQueryRequest(BaseModel):
    question: str = Field(..., min_length=1, description="Natural language question")
    user_id: Optional[int] = Field(default=None, description="Operator user id")


class AgentQueryResponse(BaseModel):
    generated_sql: str = ""
    query_result: Any = None
    answer: str = ""
    success: bool
    error_message: Optional[str] = None
    risk_level: int = 0
    risk_reason: str = ""
