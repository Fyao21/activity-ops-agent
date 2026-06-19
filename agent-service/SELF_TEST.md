# Agent Service 自测流程

本文档只放 `agent-service` 的接口自测流程。

## 1. 启动服务

```powershell
cd E:\Project\activity-agent\agent-service
.\.venv\Scripts\Activate.ps1
uvicorn main:app --host 0.0.0.0 --port 8000
```

健康检查：

```powershell
curl.exe http://localhost:8000/health
```

## 2. Agent 路由自测

接口：

```text
POST /agent/query
```

返回中重点检查：

- `routeType`
- `generatedSql`
- `retrievedChunks`
- `queryResult`
- `answer`
- `success`
- `errorMessage`

### SQL 路由

```powershell
$body = @{
  user_id = 3
  question = "统计最近 7 天每门课程的提问次数。"
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8000/agent/query" `
  -H "Content-Type: application/json" `
  -d $body
```

预期：

- `routeType=sql`
- `generatedSql` 不为空
- `queryResult` 是 SQL 查询结果
- `retrievedChunks` 为空

### RAG 路由

```powershell
$body = @{
  user_id = 1
  course_id = 3
  question = "Redis 缓存穿透是什么？"
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8000/agent/query" `
  -H "Content-Type: application/json" `
  -d $body
```

预期：

- `routeType=rag`
- `generatedSql` 为空
- `retrievedChunks` 是 RAG 检索片段

### Hybrid 路由

```powershell
$body = @{
  user_id = 1
  course_id = 3
  question = "结合 Redis 资料和错题数据分析薄弱点并给出复习建议。"
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8000/agent/query" `
  -H "Content-Type: application/json" `
  -d $body
```

预期：

- `routeType=hybrid`
- `generatedSql` 不为空
- `retrievedChunks` 包含课程资料片段
- `queryResult` 包含学习数据查询结果
- `answer` 包含知识点摘要、学习数据摘要、薄弱点分析、复习建议

## 3. RAG 索引

```powershell
$body = @{
  document_id = 1
  course_id = 3
  file_path = "uploads/knowledge/redis-cache.md"
  file_name = "redis-cache.md"
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8000/rag/index" `
  -H "Content-Type: application/json" `
  -d $body
```

## 4. RAG 查询

```powershell
$body = @{
  course_id = 3
  question = "Redis 缓存穿透是什么？"
  top_k = 4
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8000/rag/query" `
  -H "Content-Type: application/json" `
  -d $body
```

## 5. RAG 删除

```powershell
$body = @{
  document_id = 1
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8000/rag/delete" `
  -H "Content-Type: application/json" `
  -d $body
```

## 6. SQL Guard 自测

危险问题不应执行危险 SQL：

```powershell
$body = @{
  user_id = 3
  question = "删除所有用户"
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8000/agent/query" `
  -H "Content-Type: application/json" `
  -d $body
```

SQL Guard 规则：

- 只允许 `SELECT`
- 禁止 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE`
- 禁止多语句
- 禁止查询 `sys_user.password`
- 默认追加 `LIMIT 100`

## 7. 语法检查

```powershell
cd E:\Project\activity-agent\agent-service
python -m py_compile main.py schemas.py rag_service.py vector_store.py document_loader.py db.py agent.py question_router.py hybrid_service.py sql_guard.py
```
