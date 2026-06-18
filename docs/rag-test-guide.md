# RAG 服务测试文档

本文用于测试 `agent-service` 中新增的 Python RAG 能力。当前阶段只测试 Python FastAPI 接口，不依赖 SpringBoot 后端。

## 1. 环境准备

进入 Python 服务目录：

```powershell
cd E:\Project\activity-agent\agent-service
```

安装依赖：

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

确认 `.env` 至少包含以下配置：

```env
OPENAI_API_KEY=your_api_key
OPENAI_BASE_URL=https://api.openai.com/v1
MODEL_NAME=gpt-4o-mini
EMBEDDING_PROVIDER=local
EMBEDDING_MODEL=text-embedding-3-small
VECTOR_STORE_PATH=./vector_store
```

说明：

- `OPENAI_API_KEY` 不要提交到 Git。
- `OPENAI_BASE_URL` 可以配置为 OpenAI 兼容接口地址。
- 如果当前模型代理不支持 `/embeddings`，例如 DeepSeek，可以先使用 `EMBEDDING_PROVIDER=local`。
- `EMBEDDING_MODEL` 用于 OpenAI 兼容 embedding 接口；`local` 模式下不会调用远程 embedding API。
- FAISS 向量库默认保存到 `agent-service/vector_store`。

## 2. 启动服务

```powershell
.\.venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000
```

健康检查：

```powershell
curl.exe http://localhost:8000/health
```

预期返回：

```json
{"status":"ok"}
```

## 3. 索引测试文档

本仓库提供了样例知识库文档：

- `docs/rag-sample-618.md`

调用 `/rag/index`：

```powershell
curl.exe -X POST "http://localhost:8000/rag/index" `
  -H "Content-Type: application/json" `
  -d "{\"document_id\":1,\"file_path\":\"../docs/rag-sample-618.md\",\"file_name\":\"rag-sample-618.md\"}"
```

预期返回示例：

```json
{
  "success": true,
  "document_id": 1,
  "chunk_count": 3,
  "message": "文档索引成功",
  "chunks": []
}
```

实际 `chunk_count` 可能因为文本切分结果略有不同。

索引成功后，目录中应出现 FAISS 文件：

```text
agent-service/vector_store/knowledge.faiss
agent-service/vector_store/knowledge.pkl
```

## 4. RAG 问答测试

查询新用户奖励规则：

```powershell
curl.exe -X POST "http://localhost:8000/rag/query" `
  -H "Content-Type: application/json" `
  -d "{\"question\":\"618 加码活动的新用户奖励规则是什么？\",\"top_k\":4}"
```

预期效果：

- `answer` 基于 `rag-sample-618.md` 中的内容回答。
- `retrieved_chunks` 返回命中的文档片段。
- `sources` 包含 `rag-sample-618.md`。

查询优惠券限制：

```powershell
curl.exe -X POST "http://localhost:8000/rag/query" `
  -H "Content-Type: application/json" `
  -d "{\"question\":\"优惠券使用限制是什么？\",\"top_k\":4}"
```

查询知识库不存在的问题：

```powershell
curl.exe -X POST "http://localhost:8000/rag/query" `
  -H "Content-Type: application/json" `
  -d "{\"question\":\"双十一活动的会员等级规则是什么？\",\"top_k\":4}"
```

如果检索上下文不足，回答应倾向于：

```text
知识库中未找到相关信息
```

## 5. Text-to-SQL 回归测试

确认现有 `/agent/query` 仍可用：

```powershell
curl.exe -X POST "http://localhost:8000/agent/query" `
  -H "Content-Type: application/json" `
  -d "{\"question\":\"统计最近 7 天各活动参与人数\",\"user_id\":1}"
```

该接口仍走现有 Text-to-SQL 流程，本阶段尚未接入 RAG/SQL 混合路由。

## 6. 常见问题

### 6.1 `/rag/index` 返回 embedding 相关错误

优先检查：

- `.env` 中是否配置 `OPENAI_API_KEY`
- `OPENAI_BASE_URL` 是否支持 embedding 接口
- `EMBEDDING_MODEL` 是否是当前模型代理支持的 embedding 模型

### 6.2 `/rag/index` 返回文件不存在

确认服务启动目录是：

```text
E:\Project\activity-agent\agent-service
```

并使用以下路径：

```text
../docs/rag-sample-618.md
```

### 6.3 `/rag/query` 返回知识库为空

先执行 `/rag/index`，并确认 `agent-service/vector_store` 下已经生成 FAISS 文件。

### 6.4 Windows PowerShell 下 curl 参数异常

PowerShell 中 `curl` 可能是 `Invoke-WebRequest` 别名，建议使用：

```powershell
curl.exe
```
