import os
from dataclasses import dataclass
from functools import lru_cache
from typing import Any, Dict, List
from urllib.parse import quote_plus

from dotenv import load_dotenv
from langchain_community.utilities import SQLDatabase
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine


load_dotenv()


ALLOWED_TABLES = [
    "activity",
    "activity_user_record",
    "reward_record",
    "activity_statistics",
    "agent_qa_record",
]


@dataclass(frozen=True)
class Settings:
    openai_api_key: str
    openai_base_url: str
    model_name: str
    mysql_host: str
    mysql_port: int
    mysql_user: str
    mysql_password: str
    mysql_database: str

    @property
    def mysql_uri(self) -> str:
        password = quote_plus(self.mysql_password)
        return (
            f"mysql+pymysql://{self.mysql_user}:{password}"
            f"@{self.mysql_host}:{self.mysql_port}/{self.mysql_database}?charset=utf8mb4"
        )


def _require_env(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


@lru_cache
def get_settings() -> Settings:
    return Settings(
        openai_api_key=_require_env("OPENAI_API_KEY"),
        openai_base_url=_require_env("OPENAI_BASE_URL"),
        model_name=_require_env("MODEL_NAME"),
        mysql_host=_require_env("MYSQL_HOST"),
        mysql_port=int(_require_env("MYSQL_PORT")),
        mysql_user=_require_env("MYSQL_USER"),
        mysql_password=_require_env("MYSQL_PASSWORD"),
        mysql_database=_require_env("MYSQL_DATABASE"),
    )


@lru_cache
def get_engine() -> Engine:
    settings = get_settings()
    return create_engine(
        settings.mysql_uri,
        pool_pre_ping=True,
        future=True,
    )


@lru_cache
def get_sql_database() -> SQLDatabase:
    return SQLDatabase.from_uri(
        get_settings().mysql_uri,
        include_tables=ALLOWED_TABLES,
        sample_rows_in_table_info=2,
        view_support=False,
    )


def execute_query(sql: str) -> List[Dict[str, Any]]:
    with get_engine().connect() as conn:
        result = conn.execute(text(sql))
        rows = result.mappings().all()
        return [dict(row) for row in rows]
