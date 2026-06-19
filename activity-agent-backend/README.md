# Edu Agent Backend

`activity-agent-backend` 是智能课程学习助手 Agent 平台的 SpringBoot 后端服务，负责用户、课程资料、知识库文档、Agent 问答记录、MySQL/Redis/RocketMQ 集成，以及通过 HTTP 调用 Python Agent 服务。

当前后端保留 Java + Python 双服务架构：

- SpringBoot：业务数据、文件上传、RocketMQ 消息、MySQL 记录、统一接口。
- Python FastAPI：RAG 文档索引、FAISS 检索、Text-to-SQL、Hybrid Agent、大模型调用。

## 技术栈

- Java 17
- Spring Boot 3.3.2
- Spring MVC
- MyBatis-Plus
- MySQL
- Redis
- RocketMQ
- RestTemplate
- Maven

## 目录结构

```text
activity-agent-backend/
├── src/main/java/com/example/activityagent
│   ├── client
│   ├── common
│   ├── config
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── mapper
│   ├── mq
│   │   ├── constant
│   │   ├── consumer
│   │   ├── dto
│   │   └── producer
│   ├── service
│   └── vo
├── src/main/resources/application.yml
├── uploads/knowledge
└── pom.xml
```

## 当前配置

关键配置在 `src/main/resources/application.yml`。

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/edu_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: wt292292
  data:
    redis:
      host: 192.168.100.128
      port: 6379
      password: 1234
      database: 0

agent:
  python-url: http://localhost:8000/agent/query
  rag-index-url: http://localhost:8000/rag/index
  rag-query-url: http://localhost:8000/rag/query

rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: edu-agent-producer-group
```

Redis 当前只作为缓存使用，不再作为消息队列。文档索引等异步任务使用 RocketMQ。

## 知识库模块

## 课程管理模块

### 创建课程

接口：

```text
POST /course/create
```

请求示例：

```json
{
  "courseName": "Redis 实战",
  "teacherId": 2,
  "description": "覆盖 Redis 缓存、分布式锁、缓存穿透和缓存一致性。",
  "status": 1
}
```

curl 示例：

```powershell
$body = @{
  courseName = "Redis 实战"
  teacherId = 2
  description = "覆盖 Redis 缓存、分布式锁、缓存穿透和缓存一致性。"
  status = 1
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8080/course/create" `
  -H "Content-Type: application/json" `
  -d $body
```

### 查询课程列表

接口：

```text
GET /course/list
```

参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `pageNum` | Long | 否 | 页码，默认 1 |
| `pageSize` | Long | 否 | 每页数量，默认 10 |

示例：

```powershell
curl.exe "http://localhost:8080/course/list?pageNum=1&pageSize=10"
```

### 查询课程详情

接口：

```text
GET /course/{id}
```

示例：

```powershell
curl.exe "http://localhost:8080/course/3"
```

缓存规则：

- 查询课程详情时优先读取 Redis。
- Redis Key：`course:info:{courseId}`。
- 缓存过期时间：30 分钟。
- Redis 未命中时查询 MySQL，并写入 Redis。

### 更新课程

接口：

```text
PUT /course/update
```

请求示例：

```json
{
  "id": 3,
  "courseName": "Redis 实战",
  "teacherId": 2,
  "description": "覆盖 Redis 缓存、分布式锁、缓存穿透、缓存击穿和缓存一致性。",
  "status": 1
}
```

更新规则：

- 先更新 MySQL。
- 更新成功后删除 Redis 缓存：`course:info:{courseId}`。
- 后续再次查询详情时重新写入缓存。

curl 示例：

```powershell
$body = @{
  id = 3
  courseName = "Redis 实战"
  teacherId = 2
  description = "覆盖 Redis 缓存、分布式锁、缓存穿透、缓存击穿和缓存一致性。"
  status = 1
} | ConvertTo-Json -Compress

curl.exe -X PUT "http://localhost:8080/course/update" `
  -H "Content-Type: application/json" `
  -d $body
```

### Redis 缓存自测

第一次查询课程详情：

```powershell
curl.exe "http://localhost:8080/course/3"
```

查看 Redis Key：

```powershell
redis-cli -h 192.168.100.128 -p 6379 -a 1234 EXISTS course:info:3
redis-cli -h 192.168.100.128 -p 6379 -a 1234 TTL course:info:3
```

预期：

- `EXISTS` 返回 `1`
- `TTL` 接近 `1800` 秒

更新课程后再次查看：

```powershell
redis-cli -h 192.168.100.128 -p 6379 -a 1234 EXISTS course:info:3
```

预期：缓存被删除，返回 `0`。再次查询 `/course/3` 后会重新写入缓存。

### 上传课程资料

接口：

```text
POST /knowledge/upload
```

请求类型：

```text
multipart/form-data
```

参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `courseId` | Long | 是 | 课程 ID |
| `file` | File | 是 | `txt` 或 `md` 文件 |

处理流程：

1. 校验文件类型，仅支持 `txt`、`md`。
2. 自动创建 `uploads/knowledge` 目录。
3. 保存上传文件。
4. 写入 `knowledge_document`，初始 `status=0`。
5. 发送 RocketMQ 消息到 `edu_knowledge_index_topic:DOC_INDEX`。
6. 立即返回文档 ID、文件名和处理状态。

注意：上传接口不会同步调用 Python `/rag/index`，文档索引由 RocketMQ 消费者异步处理。

curl 示例：

```powershell
curl.exe -X POST "http://localhost:8080/knowledge/upload" `
  -F "courseId=3" `
  -F "file=@E:\Project\activity-agent\activity-agent-backend\uploads\knowledge\redis-cache.md"
```

响应示例：

```json
{
  "code": 1,
  "message": "success",
  "data": {
    "documentId": 10,
    "courseId": 3,
    "fileName": "redis-cache.md",
    "fileType": "md",
    "status": 0,
    "chunkCount": 0,
    "message": "Document uploaded, waiting for async indexing"
  }
}
```

### 文档异步索引

RocketMQ 配置：

| 项 | 值 |
| --- | --- |
| Topic | `edu_knowledge_index_topic` |
| Tag | `DOC_INDEX` |
| Consumer Group | `edu_knowledge_index_consumer_group` |

相关类：

- `mq.constant.RocketMqConstant`
- `mq.dto.KnowledgeIndexMessage`
- `mq.producer.KnowledgeIndexProducer`
- `mq.consumer.KnowledgeIndexConsumer`

消费逻辑：

1. 根据 `documentId` 查询 `knowledge_document`。
2. 如果 `status=1`，说明已索引成功，直接忽略，保证重复消费幂等。
3. 如果未成功，调用 Python `/rag/index`。
4. Python 成功后更新 `status=1`、`chunk_count`，并保存 `knowledge_chunk`。
5. Python 失败后更新 `status=2`、`error_message`。
6. 消费异常时记录日志并抛出异常，让 RocketMQ 后续重试。

### 查询课程资料列表

接口：

```text
GET /knowledge/list
```

参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `courseId` | Long | 是 | 课程 ID |
| `pageNum` | Long | 否 | 页码，默认 1 |
| `pageSize` | Long | 否 | 每页数量，默认 10 |

示例：

```powershell
curl.exe "http://localhost:8080/knowledge/list?courseId=3&pageNum=1&pageSize=10"
```

返回字段：

- `id`
- `courseId`
- `fileName`
- `fileType`
- `status`
- `chunkCount`
- `createTime`

### 课程知识库问答

接口：

```text
POST /knowledge/query
```

请求示例：

```json
{
  "courseId": 3,
  "userId": 1,
  "question": "Redis 缓存穿透是什么？",
  "topK": 4
}
```

处理流程：

1. Java 通过 `RestTemplate` 调用 Python `/rag/query`。
2. Python 根据 `course_id` 从 FAISS 检索同课程资料片段。
3. Java 返回 `answer`、`retrievedChunks`、`sources`。
4. Java 保存 `agent_qa_record`，`route_type=RAG`。

curl 示例：

```powershell
$body = @{
  courseId = 3
  userId = 1
  question = "Redis 缓存穿透是什么？"
  topK = 4
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8080/knowledge/query" `
  -H "Content-Type: application/json" `
  -d $body
```

## 启动顺序

### 1. 初始化 MySQL

在项目根目录执行：

```powershell
mysql -uroot -pwt292292 < sql/schema.sql
mysql -uroot -pwt292292 < sql/init.sql
```

当前默认数据库名是 `edu_agent`。

### 2. 启动 RocketMQ

```powershell
mqnamesrv.cmd
```

```powershell
mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true
```

可选启动 Dashboard：

```powershell
java -jar rocketmq-dashboard-2.0.0.jar --spring.config.location=file:E:/Project/activity-agent/rocketmq-dashboard.yml
```

### 3. 启动 Python Agent

```powershell
cd E:\Project\activity-agent\agent-service
.\.venv\Scripts\Activate.ps1
uvicorn main:app --host 0.0.0.0 --port 8000
```

健康检查：

```powershell
curl.exe http://localhost:8000/health
```

### 4. 启动 SpringBoot 后端

```powershell
cd E:\Project\activity-agent\activity-agent-backend
mvn spring-boot:run
```

健康检查：

```powershell
curl.exe http://localhost:8080/health
```

## RocketMQ 排查命令

查看 Topic：

```powershell
mqadmin.cmd topicList -n 127.0.0.1:9876
```

查看文档索引 Topic 路由：

```powershell
mqadmin.cmd topicRoute -n 127.0.0.1:9876 -t edu_knowledge_index_topic
```

查看消费进度：

```powershell
mqadmin.cmd consumerProgress -n 127.0.0.1:9876 -g edu_knowledge_index_consumer_group
```

## 常见问题

### 上传后文档一直是 status=0

优先检查：

1. RocketMQ NameServer 和 Broker 是否启动。
2. 后端启动日志里是否注册了 `edu_knowledge_index_consumer_group`。
3. Python Agent 是否运行在 `http://localhost:8000`。
4. `application.yml` 的 `agent.rag-index-url` 是否正确。
5. RocketMQ Dashboard 中是否有 `edu_knowledge_index_topic` 消息堆积。

### status=2

表示异步索引失败。查看 `knowledge_document.error_message`，常见原因：

- Python Agent 未启动；
- 文件路径不存在；
- Python 本地 Embedding 模型未下载完成；
- FAISS 向量库写入失败；
- Python `/rag/index` 返回失败。

### `/knowledge/query` 没有检索结果

优先检查：

1. 文档是否已经索引成功，`knowledge_document.status=1`。
2. 查询时的 `courseId` 是否和上传时一致。
3. Python `VECTOR_STORE_PATH` 是否和索引时使用的是同一个目录。
4. 文档内容是否确实包含问题相关知识点。

## 验证命令

编译：

```powershell
mvn -q -DskipTests compile
```

查看文档状态：

```sql
SELECT id, course_id, file_name, status, chunk_count, error_message
FROM knowledge_document
ORDER BY id DESC;
```

查看问答记录：

```sql
SELECT id, user_id, course_id, route_type, question, success, create_time
FROM agent_qa_record
ORDER BY id DESC;
```

## 完整自测流程

下面流程用于验证 Java 后端课程资料上传、RocketMQ 异步索引、Python RAG 调用和问答记录保存。

### 1. 确认基础服务

确认 MySQL 已初始化：

```powershell
mysql -uroot -pwt292292 < E:\Project\activity-agent\sql\schema.sql
mysql -uroot -pwt292292 < E:\Project\activity-agent\sql\init.sql
```

确认 RocketMQ 已启动：

```powershell
mqadmin.cmd topicList -n 127.0.0.1:9876
```

确认 Python Agent 已启动：

```powershell
curl.exe http://localhost:8000/health
```

确认 SpringBoot 后端已启动：

```powershell
curl.exe http://localhost:8080/health
```

### 2. 准备测试文档

项目中已有测试文档：

```text
activity-agent-backend/uploads/knowledge/java-backend.md
activity-agent-backend/uploads/knowledge/mysql-index-transaction.md
activity-agent-backend/uploads/knowledge/redis-cache.md
```

如果本地没有这些文件，可以手动创建一个 `redis-cache.md`：

```md
# Redis 缓存实战

Redis 缓存穿透是指查询一个缓存和数据库中都不存在的数据，导致请求绕过缓存直接访问数据库。

常见解决方案包括缓存空值、布隆过滤器和参数校验。
```

### 3. 上传课程资料

上传 Redis 课程资料：

```powershell
curl.exe -X POST "http://localhost:8080/knowledge/upload" `
  -F "courseId=3" `
  -F "file=@E:\Project\activity-agent\activity-agent-backend\uploads\knowledge\redis-cache.md"
```

预期返回：

```json
{
  "code": 1,
  "message": "success",
  "data": {
    "documentId": 1,
    "courseId": 3,
    "fileName": "redis-cache.md",
    "fileType": "md",
    "status": 0,
    "chunkCount": 0
  }
}
```

这里 `status=0` 是正常的，表示文档已上传，等待 RocketMQ 异步索引。

### 4. 查看 RocketMQ 消费

查看文档索引 Topic：

```powershell
mqadmin.cmd topicRoute -n 127.0.0.1:9876 -t edu_knowledge_index_topic
```

查看消费者进度：

```powershell
mqadmin.cmd consumerProgress -n 127.0.0.1:9876 -g edu_knowledge_index_consumer_group
```

后端日志中应能看到类似信息：

```text
Sent knowledge index message
Received knowledge index message
Knowledge document indexed successfully
```

### 5. 检查数据库索引状态

在 MySQL 中执行：

```sql
SELECT id, course_id, file_name, status, chunk_count, error_message
FROM knowledge_document
ORDER BY id DESC;
```

预期：

- `status=1`
- `chunk_count > 0`
- `error_message` 为空

检查切片记录：

```sql
SELECT document_id, course_id, chunk_index, LEFT(content, 80) AS content_preview, vector_id
FROM knowledge_chunk
ORDER BY document_id DESC, chunk_index ASC;
```

### 6. 查询文档列表

```powershell
curl.exe "http://localhost:8080/knowledge/list?courseId=3&pageNum=1&pageSize=10"
```

预期返回中能看到刚上传的文档，且 `status=1`、`chunkCount` 大于 0。

### 7. 查询课程知识库

```powershell
$body = @{
  courseId = 3
  userId = 1
  question = "Redis 缓存穿透是什么？"
  topK = 4
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8080/knowledge/query" `
  -H "Content-Type: application/json" `
  -d $body
```

预期返回：

```json
{
  "code": 1,
  "message": "success",
  "data": {
    "answer": "...",
    "retrievedChunks": [],
    "sources": []
  }
}
```

如果索引正常，`answer` 应围绕 Redis 缓存穿透解释，并且 `retrievedChunks` 通常不为空。

### 8. 检查问答记录

```sql
SELECT id, user_id, course_id, route_type, question, answer, success, error_message, create_time
FROM agent_qa_record
ORDER BY id DESC;
```

预期：

- `route_type=RAG`
- `course_id=3`
- `success=1`
- `question` 为刚才的问题

### 9. 常见自测失败原因

上传后一直 `status=0`：

- RocketMQ 未启动；
- `edu_knowledge_index_topic` 没有自动创建；
- `KnowledgeIndexConsumer` 没有注册；
- 后端没有连接到正确的 NameServer。

上传后变成 `status=2`：

- Python Agent 未启动；
- Python 本地 Embedding 模型未下载完成；
- 上传文件路径不存在；
- Python `/rag/index` 报错；
- FAISS 向量库目录无写入权限。

`/knowledge/query` 返回“知识库中未找到相关信息”：

- 文档还没有索引成功；
- 查询的 `courseId` 和上传的 `courseId` 不一致；
- 问题和文档内容无关；
- Python `VECTOR_STORE_PATH` 指向了另一个目录。
