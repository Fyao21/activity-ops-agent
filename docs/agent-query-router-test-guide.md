# Agent Query 路由测试文档

本文用于测试 `agent-service` 中 `/agent/query` 的 SQL / RAG / Hybrid 自动路由能力。

## 1. 功能说明

`POST /agent/query` 会根据用户问题自动选择处理方式：

| 路由类型 | 说明 | 典型问题 |
| --- | --- | --- |
| `sql` | 走原有 Text-to-SQL 流程，生成 SQL 查询 MySQL，再总结结果。 | `统计最近 7 天各活动参与人数` |
| `rag` | 走知识库检索问答流程，从 FAISS 检索文档片段，再基于上下文回答。 | `618 活动规则是什么` |
| `hybrid` | 先检索知识库规则，再执行 SQL 查询数据，最后结合规则和数据分析。 | `根据 618 活动规则，分析最近 7 天奖励发放是否正常` |

路由方法：

```python
route_question(question: str) -> str
```

返回值：

```text
sql
rag
hybrid
```

## 2. 前置条件

先启动 Python Agent 服务：

```powershell
cd E:\Project\activity-agent\agent-service
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

如果要测试 RAG 或 Hybrid，请先确保知识库已经索引过文档。

可以直接索引样例文档：

```powershell
$body = @{
  document_id = 1
  file_path = "../docs/rag-sample-618.md"
  file_name = "rag-sample-618.md"
} | ConvertTo-Json -Compress

[System.IO.File]::WriteAllText("$PWD\request.json", $body, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "http://localhost:8000/rag/index" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```

如果当前模型代理不支持 `/embeddings`，例如 DeepSeek，请确认 `agent-service/.env` 中有：

```env
EMBEDDING_PROVIDER=local
```

## 3. Apifox 调用方式

推荐直接把下面这个 OpenAPI 文件导入 Apifox：

```text
E:\Project\activity-agent\docs\apifox-rag-openapi.json
```

导入后选择分组：

```text
Python Agent 路由
```

然后分别运行：

```text
POST /agent/query
```

Apifox 中该接口已经内置 3 个请求示例：

- `SQL 路由`
- `RAG 路由`
- `Hybrid 路由`

如果你不导入 OpenAPI，也可以手动新建请求：

```text
Method: POST
URL: http://localhost:8000/agent/query
Headers:
  Content-Type: application/json
Body:
  raw / JSON
```

SQL 路由 Body：

```json
{
  "question": "统计最近 7 天各活动参与人数",
  "user_id": 1
}
```

RAG 路由 Body：

```json
{
  "question": "618 活动规则是什么",
  "user_id": 1
}
```

Hybrid 路由 Body：

```json
{
  "question": "根据 618 活动规则，分析最近 7 天奖励发放是否正常",
  "user_id": 1
}
```

## 4. 测试 SQL 路由

请求：

```powershell
$body = @{
  question = "统计最近 7 天各活动参与人数"
  user_id = 1
} | ConvertTo-Json -Compress

[System.IO.File]::WriteAllText("$PWD\request.json", $body, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "http://localhost:8000/agent/query" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```

预期：

- `routeType` 返回 `sql`
- `generatedSql` 和 `generated_sql` 有 SQL
- `query_result` 有 SQL 查询结果
- `retrievedChunks` 为空数组

响应示例：

```json
{
  "routeType": "sql",
  "generatedSql": "SELECT ... LIMIT 100",
  "generated_sql": "SELECT ... LIMIT 100",
  "query_result": [
    {
      "activity_name": "618加码活动",
      "participant_count": 120
    }
  ],
  "retrievedChunks": [],
  "answer": "根据查询结果，...",
  "success": true,
  "error_message": null
}
```

## 5. 测试 RAG 路由

请求：

```powershell
$body = @{
  question = "618 活动规则是什么"
  user_id = 1
} | ConvertTo-Json -Compress

[System.IO.File]::WriteAllText("$PWD\request.json", $body, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "http://localhost:8000/agent/query" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```

预期：

- `routeType` 返回 `rag`
- `generatedSql` 和 `generated_sql` 为空
- `query_result` 为空数组
- `retrievedChunks` 有知识库检索片段
- `answer` 基于知识库上下文回答

响应示例：

```json
{
  "routeType": "rag",
  "generatedSql": "",
  "generated_sql": "",
  "query_result": [],
  "retrievedChunks": [
    {
      "document_id": 1,
      "chunk_index": 0,
      "content": "618 加码活动规则说明...",
      "score": 0.82,
      "vector_id": "doc-1-chunk-0",
      "file_name": "rag-sample-618.md"
    }
  ],
  "answer": "根据知识库文档，618 活动规则是...",
  "success": true,
  "error_message": null
}
```

## 6. 测试 Hybrid 路由

请求：

```powershell
$body = @{
  question = "根据 618 活动规则，分析最近 7 天奖励发放是否正常"
  user_id = 1
} | ConvertTo-Json -Compress

[System.IO.File]::WriteAllText("$PWD\request.json", $body, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "http://localhost:8000/agent/query" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```

预期：

- `routeType` 返回 `hybrid`
- `generatedSql` 和 `generated_sql` 有 SQL
- `query_result` 有 SQL 查询结果
- `retrievedChunks` 有知识库规则片段
- `answer` 同时包含规则摘要、数据摘要、分析结论和建议

响应示例：

```json
{
  "routeType": "hybrid",
  "generatedSql": "SELECT ... LIMIT 100",
  "generated_sql": "SELECT ... LIMIT 100",
  "query_result": [
    {
      "stat_date": "2026-06-17",
      "reward_count": 100,
      "reward_success_count": 96
    }
  ],
  "retrievedChunks": [
    {
      "document_id": 1,
      "chunk_index": 1,
      "content": "奖励通常在用户满足条件后的 10 分钟内发放...",
      "score": 0.8,
      "vector_id": "doc-1-chunk-1",
      "file_name": "rag-sample-618.md"
    }
  ],
  "answer": "1. 相关规则摘要...\n2. 数据查询结果摘要...\n3. 分析结论...\n4. 建议...",
  "success": true,
  "error_message": null
}
```

## 7. 返回字段说明

| 字段 | 含义 |
| --- | --- |
| `routeType` | 本次问题的路由类型，取值为 `sql`、`rag`、`hybrid`。 |
| `generatedSql` | 驼峰格式 SQL 字段，给 Java 或前端新逻辑使用。 |
| `generated_sql` | 下划线格式 SQL 字段，保留给旧调用方兼容使用。 |
| `query_result` | SQL 查询结果。RAG 路由下通常为空数组。 |
| `retrievedChunks` | RAG 检索命中的知识库片段。SQL 路由下为空数组。 |
| `retrievedChunks[].document_id` | 文档 ID，对应知识库文档记录。 |
| `retrievedChunks[].chunk_index` | 文档切片序号。 |
| `retrievedChunks[].content` | 检索命中的片段原文。 |
| `retrievedChunks[].score` | 相似度分数，越高表示越相关。 |
| `retrievedChunks[].vector_id` | 片段在 FAISS 中的向量 ID。 |
| `retrievedChunks[].file_name` | 片段来源文件名。 |
| `answer` | 最终回答。 |
| `success` | 是否处理成功。 |
| `error_message` | 错误信息，成功时为 `null`。 |

## 8. 路由关键词规则

SQL 关键词：

```text
统计、人数、数量、成功率、转化率、留存、对比、排名、最近、今天、昨天、本周、奖励发放量
```

RAG 关键词：

```text
规则、说明、限制、条件、如何、为什么、介绍、文档、FAQ、使用方式
```

Hybrid 关键词：

```text
根据、结合、分析、是否正常、原因、对照规则
```

判断逻辑：

- 同时命中 SQL 和 RAG，返回 `hybrid`。
- 命中 Hybrid 且同时命中 SQL 或 RAG，返回 `hybrid`。
- 只命中 RAG，返回 `rag`。
- 其他情况默认返回 `sql`。

## 9. 常见问题

### 8.1 RAG 或 Hybrid 没有检索片段

先确认是否已经索引知识库文档：

```powershell
curl.exe -X POST "http://localhost:8000/rag/index" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```

并检查目录：

```text
agent-service/vector_store
```

### 8.2 DeepSeek 返回 `/embeddings` 404

说明当前模型代理不支持 embedding 接口。使用：

```env
EMBEDDING_PROVIDER=local
```

然后重启 Python 服务。

### 8.3 JSON 解析失败

PowerShell 下不要写 Bash 风格的 `{\"question\":\"...\"}`。

推荐写法：

```powershell
$body = @{
  question = "618 活动规则是什么"
  user_id = 1
} | ConvertTo-Json -Compress

[System.IO.File]::WriteAllText("$PWD\request.json", $body, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "http://localhost:8000/agent/query" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```


