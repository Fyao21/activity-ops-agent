# 知识库后端接口测试文档

本文用于测试 SpringBoot 后端知识库模块，以及后端调用 Python RAG 服务的完整链路。

## 1. 前置条件

如果还没有创建知识库表，先执行数据库脚本：

```powershell
cd E:\Project\activity-agent
mysql -uroot -p activity_agent < .\sql\schema.sql
```

如果当前模型代理不支持 `/embeddings`，例如 DeepSeek，请确认 `agent-service/.env` 中使用本地向量模式：

```env
EMBEDDING_PROVIDER=local
VECTOR_STORE_PATH=./vector_store
```

测试用样例文档：

```text
E:\Project\activity-agent\docs\rag-sample-618.md
```

## 2. 启动服务

先启动 Python RAG 服务：

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

再打开一个新的 PowerShell，启动 SpringBoot 后端：

```powershell
cd E:\Project\activity-agent\activity-agent-backend
mvn spring-boot:run
```

本地知识库测试不依赖 RocketMQ，默认配置为：

```yaml
activity:
  mq:
    rocketmq:
      enabled: false
```

后端地址：

```text
http://localhost:8080
```

## 3. Apifox 调用方式

推荐直接把下面这个 OpenAPI 文件导入 Apifox：

```text
E:\Project\activity-agent\docs\apifox-rag-openapi.json
```

导入后选择分组：

```text
Java 知识库
```

包含以下接口：

- `POST /knowledge/upload`
- `GET /knowledge/list`
- `POST /knowledge/query`

如果你不导入 OpenAPI，也可以手动创建请求。

上传文档：

```text
Method: POST
URL: http://localhost:8080/knowledge/upload
Body: form-data
字段名: file
字段类型: File
文件: E:\Project\activity-agent\docs\rag-sample-618.md
```

查询文档列表：

```text
Method: GET
URL: http://localhost:8080/knowledge/list?pageNum=1&pageSize=10
```

知识库问答：

```text
Method: POST
URL: http://localhost:8080/knowledge/query
Headers:
  Content-Type: application/json
Body:
  raw / JSON
```

Body 示例：

```json
{
  "question": "618 加码活动的新用户奖励规则是什么？",
  "topK": 4
}
```

## 4. 上传知识文档

请求：

```powershell
curl.exe -X POST "http://localhost:8080/knowledge/upload" `
  -F "file=@E:\Project\activity-agent\docs\rag-sample-618.md"
```

预期返回示例：

```json
{
  "code": 1,
  "message": "success",
  "data": {
    "documentId": 1,
    "fileName": "rag-sample-618.md",
    "fileType": "md",
    "status": 1,
    "chunkCount": 2,
    "message": "Document indexed successfully"
  }
}
```

说明：

- `status=1` 表示 Python RAG 索引成功。
- `status=2` 表示 Python RAG 索引失败，查看 `data.message`。
- 上传文件会保存到 `activity-agent-backend/uploads/knowledge`。
- FAISS 向量库会保存到 `agent-service/vector_store`。

## 5. 查询知识库文档列表

请求：

```powershell
curl.exe "http://localhost:8080/knowledge/list?pageNum=1&pageSize=10"
```

预期返回字段：

```json
{
  "code": 1,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "fileName": "rag-sample-618.md",
        "fileType": "md",
        "status": 1,
        "chunkCount": 2,
        "createTime": "2026-06-18T..."
      }
    ],
    "total": 1,
    "current": 1,
    "size": 10
  }
}
```

## 6. 知识库问答

请求：

```powershell
$body = @{
  question = "618 加码活动的新用户奖励规则是什么？"
  topK = 4
} | ConvertTo-Json -Compress

[System.IO.File]::WriteAllText("$PWD\request.json", $body, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "http://localhost:8080/knowledge/query" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```

预期返回字段：

```json
{
  "code": 1,
  "message": "success",
  "data": {
    "answer": "根据知识库文档，...",
    "retrievedChunks": [
      {
        "documentId": 1,
        "chunkIndex": 0,
        "content": "...",
        "score": 0.8,
        "vectorId": "doc-1-chunk-0",
        "fileName": "rag-sample-618.md"
      }
    ],
    "sources": [
      "rag-sample-618.md"
    ]
  }
}
```

字段说明：

| 字段 | 含义 |
| --- | --- |
| `code` | 后端统一响应码，`1` 表示接口调用成功，`0` 表示失败。 |
| `message` | 后端统一响应消息，成功时通常是 `success`。 |
| `data` | 业务数据主体。 |
| `data.answer` | RAG 最终回答，由 Python Agent 基于检索到的知识库片段生成。 |
| `data.retrievedChunks` | 本次问答检索命中的知识库片段列表，用于解释答案依据。 |
| `data.retrievedChunks[].documentId` | 片段所属的知识库文档 ID，对应 `knowledge_document.id`。 |
| `data.retrievedChunks[].chunkIndex` | 片段在该文档中的切片序号，从 `0` 开始。 |
| `data.retrievedChunks[].content` | 检索命中的原始片段内容，答案应基于这些内容生成。 |
| `data.retrievedChunks[].score` | 相似度分数，越高表示该片段与问题越相关。当前分数由 FAISS 检索距离换算得到，主要用于排序参考。 |
| `data.retrievedChunks[].vectorId` | 片段在 FAISS 向量库中的 ID，例如 `doc-1-chunk-0`。 |
| `data.retrievedChunks[].fileName` | 片段来源文件名。 |
| `data.sources` | 本次回答引用到的文档来源列表，去重后的文件名集合。 |

可以继续测试这些问题：

```powershell
$body = @{
  question = "优惠券使用限制是什么？"
  topK = 4
} | ConvertTo-Json -Compress

[System.IO.File]::WriteAllText("$PWD\request.json", $body, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "http://localhost:8080/knowledge/query" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```

```powershell
$body = @{
  question = "奖励发放失败后如何处理？"
  topK = 4
} | ConvertTo-Json -Compress

[System.IO.File]::WriteAllText("$PWD\request.json", $body, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "http://localhost:8080/knowledge/query" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```

## 7. 直接测试 Python RAG 服务

如果 Java 上传或查询失败，可以先绕过 Java，直接测 Python，判断问题在哪一层。

直接索引文档：

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

直接问答：

```powershell
$body = @{
  question = "618 加码活动的新用户奖励规则是什么？"
  top_k = 4
} | ConvertTo-Json -Compress

[System.IO.File]::WriteAllText("$PWD\request.json", $body, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "http://localhost:8000/rag/query" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```

## 8. 常见问题

### 7.1 上传返回 `status=2`

优先查看 Python 服务日志。常见原因：

- Python 服务没有启动，或没有监听 `8000` 端口。
- `activity-agent-backend/src/main/resources/application.yml` 中的 `agent.rag-index-url` 配错。
- Python 服务无法读取 Java 传过去的文件路径。
- Embedding 配置错误。

### 7.2 Python 日志出现 `/embeddings` 404

说明当前模型代理不支持 embedding 接口。把 `agent-service/.env` 配成：

```env
EMBEDDING_PROVIDER=local
```

然后重启 Python 服务。

### 7.3 `/knowledge/list` 返回空

检查：

- `knowledge_document` 表是否已经创建。
- `/knowledge/upload` 是否返回 `code=1`。
- SpringBoot 连接的 MySQL 是否就是执行过 `schema.sql` 的数据库。

### 7.4 PowerShell 下 curl 参数异常

PowerShell 里的 `curl` 可能是 `Invoke-WebRequest` 别名，建议使用：

```powershell
curl.exe
```

另外，PowerShell 中不要使用 Bash 风格的 `{\"question\":\"...\"}`。推荐先用 `ConvertTo-Json` 生成请求体，再传给 `curl.exe --data-binary "@request.json"`。

### 7.5 启动时报 RocketMQ 连接失败

如果看到 `RemotingConnectException: connect to null failed`，说明本地 RocketMQ 没有启动或 topic 路由不存在。

知识库测试不需要 RocketMQ，请确认 `activity-agent-backend/src/main/resources/application.yml` 中：

```yaml
activity:
  mq:
    rocketmq:
      enabled: false
```

