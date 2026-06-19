# Backend 自测流程

本文档只放 `activity-agent-backend` 的接口自测流程。

## 1. 前置服务

初始化数据库：

```powershell
mysql -uroot -p < E:\Project\activity-agent\sql\schema.sql
mysql -uroot -p < E:\Project\activity-agent\sql\init.sql
```

启动 RocketMQ：

```powershell
mqnamesrv.cmd
```

```powershell
mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true
```

启动 Python Agent：

```powershell
cd E:\Project\activity-agent\agent-service
.\.venv\Scripts\Activate.ps1
uvicorn main:app --host 0.0.0.0 --port 8000
```

启动后端：

```powershell
cd E:\Project\activity-agent\activity-agent-backend
mvn spring-boot:run
```

## 2. 课程管理自测

创建课程：

```powershell
$body = @{
  courseName = "Redis 实战"
  teacherId = 2
  description = "覆盖 Redis 缓存、缓存穿透、缓存击穿和缓存一致性。"
  status = 1
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8080/course/create" `
  -H "Content-Type: application/json" `
  -d $body
```

查询课程列表：

```powershell
curl.exe "http://localhost:8080/course/list?pageNum=1&pageSize=10"
```

查询课程详情：

```powershell
curl.exe "http://localhost:8080/course/3"
```

更新课程：

```powershell
$body = @{
  id = 3
  courseName = "Redis 实战"
  teacherId = 2
  description = "覆盖 Redis 缓存、缓存穿透、缓存击穿、缓存雪崩和缓存一致性。"
  status = 1
} | ConvertTo-Json -Compress

curl.exe -X PUT "http://localhost:8080/course/update" `
  -H "Content-Type: application/json" `
  -d $body
```

检查 Redis 缓存：

```powershell
redis-cli -h 192.168.100.128 -p 6379 -a 1234 EXISTS course:info:3
redis-cli -h 192.168.100.128 -p 6379 -a 1234 TTL course:info:3
```

## 3. 知识库上传和异步索引

先确认课程 ID 存在：

```powershell
curl.exe "http://localhost:8080/course/list?pageNum=1&pageSize=10"
```

上传资料：

```powershell
curl.exe -X POST "http://localhost:8080/knowledge/upload" `
  -F "courseId=3" `
  -F "file=@E:\Project\activity-agent\activity-agent-backend\uploads\knowledge\redis-cache.md"
```

注意：

- `courseId` 必须是 `course` 表中真实存在的课程 ID。
- 刚上传返回 `status=0`、`chunkCount=0` 是正常的，表示等待 RocketMQ 异步索引。

检查索引状态：

```sql
SELECT id, course_id, file_name, status, chunk_count, error_message
FROM knowledge_document
ORDER BY id DESC;
```

预期：

- `status=1`：索引成功
- `chunk_count > 0`
- `error_message` 为空

检查切片：

```sql
SELECT document_id, course_id, chunk_index, LEFT(content, 80) AS content_preview, vector_id
FROM knowledge_chunk
ORDER BY document_id DESC, chunk_index ASC;
```

## 4. 知识库查询

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

检查问答记录：

```sql
SELECT id, user_id, course_id, route_type, question, success, create_time
FROM agent_qa_record
ORDER BY id DESC;
```

## 5. 知识库删除

先查资料 ID：

```powershell
curl.exe "http://localhost:8080/knowledge/list?courseId=3&pageNum=1&pageSize=10"
```

删除资料，把 `1` 换成真实 `documentId`：

```powershell
curl.exe -X DELETE "http://localhost:8080/knowledge/1"
```

再次查询确认删除：

```powershell
curl.exe "http://localhost:8080/knowledge/list?courseId=3&pageNum=1&pageSize=10"
```

如果资料已索引成功，删除时需要 Python Agent 正在运行，因为后端会调用 Python `/rag/delete` 清理 FAISS 向量。

## 6. 学习行为记录

发送学习行为：

```powershell
$body = @{
  userId = 1
  courseId = 1
  eventType = "QUESTION"
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8080/learning/event" `
  -H "Content-Type: application/json" `
  -d $body
```

支持的 `eventType`：

- `QUESTION`
- `ANSWER`
- `VIEW_COURSE`
- `UPLOAD_DOC`

检查落库：

```sql
SELECT id, user_id, course_id, event_type, event_time, message_key, create_time
FROM learning_event
ORDER BY id DESC;
```

## 7. 题目、答题和错题

创建题目：

```powershell
$body = @{
  courseId = 1
  knowledgePoint = "Java 基础"
  questionContent = "Java 中 String 是否是不可变对象？"
  optionA = "是"
  optionB = "不是"
  optionC = "只有基本类型不可变"
  optionD = "运行时决定"
  answer = "A"
  analysis = "String 是不可变对象，字符串内容创建后不能被修改。"
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8080/question/create" `
  -H "Content-Type: application/json" `
  -d $body
```

查询题目列表：

```powershell
curl.exe "http://localhost:8080/question/list?courseId=1"
```

提交正确答案：

```powershell
$body = @{
  userId = 1
  courseId = 1
  questionId = 1
  userAnswer = "A"
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8080/answer/submit" `
  -H "Content-Type: application/json" `
  -d $body
```

提交错误答案：

```powershell
$body = @{
  userId = 1
  courseId = 1
  questionId = 1
  userAnswer = "B"
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8080/answer/submit" `
  -H "Content-Type: application/json" `
  -d $body
```

查询错题：

```powershell
curl.exe "http://localhost:8080/answer/wrong/list?userId=1&courseId=1"
```

检查答题记录：

```sql
SELECT id, user_id, course_id, question_id, user_answer, correct, create_time
FROM answer_record
ORDER BY id DESC;
```

答题提交后会同时发送：

- 学习行为消息：`edu_learning_event_topic:ANSWER`
- 答题统计消息：`edu_answer_stat_topic:ANSWER_STAT`

## 8. RocketMQ 排查

查看 Topic：

```powershell
mqadmin.cmd topicList -n 127.0.0.1:9876
```

查看知识库索引 Topic：

```powershell
mqadmin.cmd topicRoute -n 127.0.0.1:9876 -t edu_knowledge_index_topic
```

查看学习行为 Topic：

```powershell
mqadmin.cmd topicRoute -n 127.0.0.1:9876 -t edu_learning_event_topic
```

查看答题统计 Topic：

```powershell
mqadmin.cmd topicRoute -n 127.0.0.1:9876 -t edu_answer_stat_topic
```

查看消费者进度：

```powershell
mqadmin.cmd consumerProgress -n 127.0.0.1:9876 -g edu_knowledge_index_consumer_group
mqadmin.cmd consumerProgress -n 127.0.0.1:9876 -g edu_learning_event_consumer_group
mqadmin.cmd consumerProgress -n 127.0.0.1:9876 -g edu_answer_stat_consumer_group
```

## 9. 常见问题

### 上传时报外键错误

原因通常是 `courseId` 不存在。

先执行：

```powershell
curl.exe "http://localhost:8080/course/list?pageNum=1&pageSize=10"
```

然后使用真实存在的课程 ID 上传资料。

### 文档一直是 `status=0`

优先检查：

- RocketMQ NameServer 和 Broker 是否启动；
- 后端消费者是否启动；
- `edu_knowledge_index_topic` 是否存在；
- Python Agent 是否启动。

### 文档变成 `status=2`

查看：

```sql
SELECT id, status, error_message
FROM knowledge_document
ORDER BY id DESC;
```

常见原因：

- Python Agent 未启动；
- 本地 Embedding 模型未下载完成；
- 上传文件路径不存在；
- FAISS 向量库目录无写入权限。

## 10. 编译验证

```powershell
cd E:\Project\activity-agent\activity-agent-backend
mvn -q -DskipTests compile
```

## Agent 问答路由自测

请求：

```powershell
$body = @{
  userId = 1
  courseId = 3
  question = "根据 Redis 复习资料，分析最近一周学生错题集中在哪些知识点。"
} | ConvertTo-Json -Compress

curl.exe -X POST "http://localhost:8080/agent/query" `
  -H "Content-Type: application/json" `
  -d $body
```

返回中重点检查：

- `routeType`
- `generatedSql`
- `retrievedChunks`
- `queryResult`
- `answer`
- `success`
- `errorMessage`

检查问答记录是否保存：

```sql
SELECT id, user_id, course_id, route_type, generated_sql, retrieved_context, answer, success, error_message, create_time
FROM agent_qa_record
ORDER BY id DESC;
```
