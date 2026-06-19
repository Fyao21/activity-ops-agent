# Edu Agent Service

`agent-service` 是“智能课程学习助手 Agent 平台”的 Python FastAPI 服务，负责课程资料 RAG、FAISS 向量检索、Text-to-SQL、Hybrid Agent 和大模型调用。

Spring Boot 后端通过 HTTP 调用本服务。

自测流程已单独移到 [SELF_TEST.md](./SELF_TEST.md)。

## 技术栈

- Python 3.11+
- FastAPI
- LangChain
- FAISS
- sentence-transformers
- langchain-openai
- SQLAlchemy
- PyMySQL
- Uvicorn

## 目录结构

```text
agent-service/
├─ main.py
├─ agent.py
├─ hybrid_service.py
├─ question_router.py
├─ rag_service.py
├─ vector_store.py
├─ document_loader.py
├─ text_splitter.py
├─ db.py
├─ schemas.py
├─ sql_guard.py
├─ requirements.txt
├─ .env.example
├─ SELF_TEST.md
└─ README.md
```

## 环境配置

复制 `.env.example` 为 `.env`，不要把真实 API Key 提交到代码仓库。

```env
OPENAI_API_KEY=your_api_key
OPENAI_BASE_URL=https://api.openai-proxy.org/v1
MODEL_NAME=deepseek-v4-flash

EMBEDDING_PROVIDER=local
EMBEDDING_MODEL=BAAI/bge-small-zh-v1.5
VECTOR_STORE_PATH=./vector_store

MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=agent_readonly
MYSQL_PASSWORD=123456
MYSQL_DATABASE=edu_agent
```

`EMBEDDING_PROVIDER=local` 时使用本地 sentence-transformers 模型。首次运行可能会下载 `BAAI/bge-small-zh-v1.5`。

`EMBEDDING_PROVIDER=openai` 预留 OpenAI 兼容 embedding 实现。

## 主要接口

- `GET /health`
- `POST /rag/index`
- `POST /rag/query`
- `POST /rag/delete`
- `POST /agent/query`
- `GET /docs`

## RAG 能力

`/rag/index` 支持 `txt` 和 `md` 文件：

- 使用 LangChain 文本切分器
- `chunk_size=500`
- `chunk_overlap=80`
- 支持本地 Embedding
- 向量持久化到 FAISS
- metadata 保存 `document_id`、`course_id`、`file_name`、`file_path`、`chunk_index`

`/rag/query` 会按 `course_id` 检索同一课程下的资料片段，并基于课程资料上下文生成回答。

`/rag/delete` 会按 `document_id` 删除 FAISS 中对应文档片段。

## Text-to-SQL

`/agent/query` 保留 Text-to-SQL 能力，并根据问题路由到 SQL、RAG 或 Hybrid 流程。

SQL 执行前会经过 `sql_guard.py` 校验：

- 只允许单条 `SELECT`
- 禁止 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE`、`CREATE`
- 禁止查询 `password` 字段
- 没有 `LIMIT` 时默认追加限制

## 启动

```powershell
cd E:\Project\activity-agent\agent-service
.\.venv\Scripts\Activate.ps1
uvicorn main:app --host 0.0.0.0 --port 8000
```

详细自测步骤见 [SELF_TEST.md](./SELF_TEST.md)。
