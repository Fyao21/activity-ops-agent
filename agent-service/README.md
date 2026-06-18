# Edu Agent Service

`agent-service` 是智能课程学习助手 Agent 平台的 Python FastAPI 服务，负责提供课程资料 RAG 问答、Text-to-SQL 学习数据分析和 Hybrid Agent 混合分析能力。

SpringBoot 后端通过 HTTP 调用本服务。当前服务启动命令：

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

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
├── main.py
├── agent.py
├── hybrid_service.py
├── question_router.py
├── rag_service.py
├── vector_store.py
├── document_loader.py
├── text_splitter.py
├── db.py
├── schemas.py
├── sql_guard.py
├── requirements.txt
├── .env.example
└── README.md
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

`EMBEDDING_PROVIDER=local` 使用本地 sentence-transformers 模型。首次运行可能会下载 `BAAI/bge-small-zh-v1.5`。

`EMBEDDING_PROVIDER=openai` 预留 OpenAI 兼容 embedding 实现，可配置：

```env
EMBEDDING_PROVIDER=openai
EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_BASE_URL=https://api.openai.com/v1
EMBEDDING_API_KEY=your_embedding_api_key
```

## 安装依赖

```bash
cd agent-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## 启动服务

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

开发时可加 `--reload`：

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

可访问：

- `GET /health`
- `POST /rag/index`
- `POST /rag/query`
- `POST /agent/query`
- `GET /docs`

## RAG 文档索引

接口：`POST /rag/index`

请求示例：

```json
{
  "document_id": 1,
  "course_id": 1,
  "file_path": "uploads/knowledge/redis-cache.md",
  "file_name": "Redis复习资料.md"
}
```

处理流程：

- 读取 `txt` 或 `md` 文件；
- 使用 LangChain `RecursiveCharacterTextSplitter` 切分文本；
- `chunk_size=500`，`chunk_overlap=80`；
- 使用本地或 OpenAI 兼容 Embedding；
- 向量写入 FAISS；
- metadata 保存 `document_id`、`course_id`、`file_name`、`file_path`、`chunk_index`；
- FAISS 持久化到 `VECTOR_STORE_PATH`。

响应示例：

```json
{
  "success": true,
  "document_id": 1,
  "course_id": 1,
  "chunk_count": 3,
  "message": "文档索引成功"
}
```

## RAG 查询

接口：`POST /rag/query`

请求示例：

```json
{
  "course_id": 1,
  "question": "Redis 缓存穿透是什么？",
  "top_k": 4
}
```

处理流程：

- 将问题向量化；
- 从 FAISS 检索同一 `course_id` 下的相关片段；
- 将检索片段拼接进 Prompt；
- 调用大模型生成回答；
- 如果没有相关片段，返回 `知识库中未找到相关信息`。

Prompt 约束：

```text
你是一个课程学习知识库助手。
你只能基于给定的课程资料上下文回答问题。
如果上下文中没有答案，请回答“知识库中未找到相关信息”。
不要编造课程资料中不存在的内容。
回答要适合学生理解。
如果涉及多个知识点，请分条说明。
```

## Agent 查询

接口：`POST /agent/query`

该接口保留原有 Text-to-SQL 能力，并根据问题路由到 SQL、RAG 或 Hybrid 流程。

请求示例：

```json
{
  "user_id": 1,
  "course_id": 1,
  "question": "统计最近 7 天每门课程提问次数"
}
```

Text-to-SQL 会经过 SQL Guard 校验后再执行。

## SQL Guard

规则实现见 `sql_guard.py`。

限制：

- 只允许单条 `SELECT`；
- 禁止 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE`、`CREATE` 等危险语句；
- 禁止多语句执行；
- 禁止查询 `sys_user.password` 或任何 `password` 字段；
- 如果没有 `LIMIT`，默认补 `LIMIT 100`。

## 快速自测

健康检查：

```bash
curl http://localhost:8000/health
```

索引文档：

```bash
curl -X POST "http://localhost:8000/rag/index" \
  -H "Content-Type: application/json" \
  -d "{\"document_id\":1,\"course_id\":1,\"file_path\":\"uploads/knowledge/redis-cache.md\",\"file_name\":\"Redis复习资料.md\"}"
```

查询知识库：

```bash
curl -X POST "http://localhost:8000/rag/query" \
  -H "Content-Type: application/json" \
  -d "{\"course_id\":1,\"question\":\"Redis 缓存穿透是什么？\",\"top_k\":4}"
```

Agent 查询：

```bash
curl -X POST "http://localhost:8000/agent/query" \
  -H "Content-Type: application/json" \
  -d "{\"user_id\":1,\"course_id\":1,\"question\":\"统计最近 7 天每门课程提问次数\"}"
```

## 常见问题

### 本地 Embedding 首次启动慢

首次加载 `BAAI/bge-small-zh-v1.5` 可能需要下载模型。确认网络可访问 HuggingFace，或提前将模型缓存到本机。

### 找不到上传文档

`document_loader.py` 会尝试从以下位置解析相对路径：

- 当前启动目录；
- `agent-service` 目录；
- 项目根目录；
- `activity-agent-backend` 目录。

如果 SpringBoot 后端传入 `uploads/knowledge/xxx.md`，文件通常应位于 `activity-agent-backend/uploads/knowledge/xxx.md`。

### RAG 查询没有结果

优先检查：

- 是否已经调用 `/rag/index`；
- `course_id` 是否一致；
- `VECTOR_STORE_PATH` 是否指向同一个目录；
- 文档是否为非空 `txt` 或 `md` 文件。
