# 智能课程学习助手 Agent 平台

## 1. 项目介绍

本项目是一个面向课程学习场景的智能学习助手平台，由 Spring Boot 后端服务和 Python FastAPI Agent 服务组成。

系统支持课程管理、课程资料上传、RAG 知识库问答、Text-to-SQL 学习数据分析、Hybrid Agent 混合分析、题目管理、答题判分、错题记录、学习行为记录和 RocketMQ 异步任务处理。

核心目标是把传统课程学习系统和大模型 Agent 能力结合起来，让学生可以基于课程资料提问，让教师可以用自然语言查询学习数据，让系统通过异步任务提升上传、索引、统计等流程的稳定性。

## 2. 应用场景

### 学生学习

- 查询课程列表和课程资料。
- 基于课程资料提问，例如“JVM 垃圾回收机制有哪些重点”。
- 提交题目答案，查看正确答案和解析。
- 查询错题记录，辅助复习。

### 教师教学

- 创建和维护课程。
- 上传课程资料，构建课程知识库。
- 创建课程题目。
- 通过自然语言查询课程学习数据，例如“统计最近 7 天每门课程的提问次数”。

### 教学运营分析

- 查询课程活跃度、提问次数、答题正确率等学习数据。
- 使用 Text-to-SQL 将自然语言转为安全 SQL。
- 使用 Hybrid Agent 同时结合课程资料和数据库统计结果生成分析结论。

## 3. 技术栈

### Java 后端

- Java 17
- Spring Boot 3.3.2
- Spring MVC
- MyBatis-Plus
- MySQL
- Redis
- RocketMQ
- Maven
- Lombok

### Python Agent 服务

- Python 3.11+
- FastAPI
- LangChain
- FAISS
- sentence-transformers
- OpenAI 兼容大模型接口
- SQLAlchemy
- PyMySQL

### 前端与测试

- Vue 3
- Vite
- Element Plus
- Apifox
- curl

## 4. 系统架构

```text
Apifox / 前端
    |
    v
Spring Boot 后端
    |
    |-- MySQL：业务数据、课程、资料、题目、答题、学习行为、问答记录
    |-- Redis：登录态、课程缓存等
    |-- RocketMQ：文档索引、学习行为、答题统计异步任务
    |
    v
Python FastAPI Agent 服务
    |
    |-- RAG：课程资料检索问答
    |-- Text-to-SQL：学习数据自然语言查询
    |-- Hybrid Agent：RAG + SQL 混合分析
    |-- FAISS：向量库
    |-- LLM API：答案生成和结果总结
```

Spring Boot 负责业务接口、数据库读写、文件上传、消息发送和调用 Python 服务。Python Agent 负责文档解析、向量检索、大模型问答、SQL 生成和 SQL 安全校验。

## 5. 项目目录结构

```text
activity-agent/
├── activity-agent-backend/
│   ├── src/main/java/com/example/activityagent/
│   │   ├── client/          # Java 调 Python Agent 客户端
│   │   ├── common/          # 统一返回、异常处理
│   │   ├── config/          # Redis、RestTemplate、MyBatis-Plus 配置
│   │   ├── controller/      # 后端接口
│   │   ├── dto/             # 请求 DTO
│   │   ├── entity/          # 数据库实体
│   │   ├── mapper/          # MyBatis-Plus Mapper
│   │   ├── mq/              # RocketMQ 消息、生产者、消费者
│   │   ├── service/         # 业务服务
│   │   └── vo/              # 响应 VO
│   ├── src/main/resources/application.yml
│   ├── SELF_TEST.md
│   └── pom.xml
├── agent-service/
│   ├── main.py              # FastAPI 入口
│   ├── agent.py             # Text-to-SQL Agent
│   ├── rag_service.py       # RAG 服务
│   ├── hybrid_service.py    # Hybrid Agent
│   ├── question_router.py   # SQL/RAG/Hybrid 路由
│   ├── schemas.py           # Pydantic 请求响应模型
│   ├── sql_guard.py         # SQL 安全校验
│   ├── vector_store.py      # FAISS 向量库
│   ├── .env.example
│   └── requirements.txt
├── activity-agent-front/    # Vue 演示前端
├── docs/
│   ├── product.md
│   ├── apifox-openapi.yaml
│   ├── knowledge-backend-test-guide.md
│   └── rag-samples/
├── sql/
│   ├── schema.sql           # 建表脚本
│   ├── init.sql             # 初始化数据
│   └── migration_add_question_options.sql
└── README.md
```

## 6. MySQL 初始化方式

数据库名为 `edu_agent`，建库和建表逻辑在 `sql/schema.sql` 中。

启动 MySQL 后，在项目根目录执行：

```powershell
mysql -u your_user -p < .\sql\schema.sql
mysql -u your_user -p < .\sql\init.sql
```

不要在命令行或 README 中写真实数据库密码。执行后会初始化用户、课程、知识库文档、题目、答题记录、学习行为和 Agent 问答记录等测试数据。

后端数据库连接配置位于：

```text
activity-agent-backend/src/main/resources/application.yml
```

建议本地使用自己的配置覆盖，不要提交真实密码。

## 7. Redis 配置

Redis 用于登录态和缓存。后端配置示例：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
      database: 0
      timeout: 5s
```

如果本地 Redis 没有密码，可以按本机配置调整。不要把真实 Redis 密码提交到仓库。

## 8. RocketMQ 配置

后端 RocketMQ 基础配置示例：

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: edu-agent-producer-group
```

当前课程学习助手核心链路使用以下 RocketMQ Topic。

### Topic 列表

| Topic | Tag | 用途 |
| --- | --- | --- |
| `edu_knowledge_index_topic` | `DOC_INDEX` | 课程资料上传后异步索引 |
| `edu_learning_event_topic` | `QUESTION`、`ANSWER`、`VIEW_COURSE`、`UPLOAD_DOC` | 学习行为异步记录 |
| `edu_answer_stat_topic` | `ANSWER_STAT` | 答题统计异步处理 |

### Producer 列表

| Producer | 发送 Topic | 触发场景 |
| --- | --- | --- |
| `KnowledgeIndexProducer` | `edu_knowledge_index_topic` | `/knowledge/upload` 上传资料后发送索引任务 |
| `LearningEventProducer` | `edu_learning_event_topic` | `/learning/event` 记录学习行为 |
| `AnswerStatProducer` | `edu_answer_stat_topic` | `/answer/submit` 提交答案后发送答题统计任务 |

### Consumer 列表

| Consumer | 监听 Topic | 处理逻辑 |
| --- | --- | --- |
| `KnowledgeIndexConsumer` | `edu_knowledge_index_topic` | 调用 Python `/rag/index`，写入知识库切片并更新文档状态 |
| `LearningEventConsumer` | `edu_learning_event_topic` | 将学习行为消息写入 `learning_event` 表 |
| `AnswerStatConsumer` | `edu_answer_stat_topic` | 消费答题统计消息，当前为统计扩展预留点 |

### ConsumerGroup 列表

| ConsumerGroup | 对应 Consumer |
| --- | --- |
| `edu_knowledge_index_consumer_group` | `KnowledgeIndexConsumer` |
| `edu_learning_event_consumer_group` | `LearningEventConsumer` |
| `edu_answer_stat_consumer_group` | `AnswerStatConsumer` |

### 消费失败重试说明

- RocketMQ 消费者处理消息时抛出异常，Broker 会按 RocketMQ 策略进行重试。
- 文档索引失败时，`KnowledgeIndexConsumer` 会把 `knowledge_document.status` 更新为失败状态，便于排查。
- Python Agent 不可用、文件不存在、模型调用失败、数据库连接失败都可能导致消费失败或文档状态失败。
- 开发环境如果数据库已重置但 RocketMQ 仍有旧消息，可能出现旧 `documentId` 查不到的日志。此时需要清理旧消息、重置消费位点，或让消费者对已删除文档做忽略处理。

### 幂等处理说明

- `KnowledgeIndexConsumer`：如果文档已是索引成功状态，会直接跳过；重新保存切片前会按 `document_id` 删除旧切片，避免重复切片。
- `LearningEventConsumer`：通过 `messageKey` 判断是否已消费，数据库唯一键可防止重复写入。
- `AnswerStatConsumer`：当前使用内存集合按 `answerRecordId` 做重复消费保护，后续可扩展为数据库幂等表或统计表状态字段。

## 9. Python Agent 配置

进入 `agent-service` 后复制环境变量模板：

```powershell
cd .\agent-service
Copy-Item .env.example .env
```

`.env` 示例：

```env
OPENAI_API_KEY=your_api_key
OPENAI_BASE_URL=https://your-llm-endpoint/v1
MODEL_NAME=your_model_name

EMBEDDING_PROVIDER=local
EMBEDDING_MODEL=BAAI/bge-small-zh-v1.5
EMBEDDING_BASE_URL=
EMBEDDING_API_KEY=
VECTOR_STORE_PATH=./vector_store

MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=your_readonly_user
MYSQL_PASSWORD=your_readonly_password
MYSQL_DATABASE=edu_agent
```

注意：不要提交真实 API Key、真实数据库密码或生产环境连接信息。

## 10. RAG 功能说明

RAG 用于基于课程资料回答问题。

主要接口：

```text
POST /knowledge/upload
GET  /knowledge/list
POST /knowledge/query
DELETE /knowledge/{id}
```

处理流程：

1. 用户上传课程资料。
2. Java 后端保存文件和 `knowledge_document` 记录，初始状态为待索引。
3. 后端发送 `edu_knowledge_index_topic:DOC_INDEX` 消息。
4. `KnowledgeIndexConsumer` 调用 Python `/rag/index`。
5. Python 解析文档、切分文本、生成向量并写入 FAISS。
6. Java 后端保存切片元数据到 `knowledge_chunk`，并更新文档状态。
7. 用户调用 `/knowledge/query` 基于课程资料提问。

当前资料类型主要支持 `txt` 和 `md`。

## 11. Text-to-SQL 功能说明

Text-to-SQL 用于把学习数据分析问题转成 SQL 查询。

典型问题：

```text
统计最近 7 天每门课程的提问次数
查询 Java 课程错题最多的知识点
统计 Redis 课程的平均正确率
```

Python Agent 会生成 SQL，并通过 `sql_guard.py` 做安全校验：

- 只允许单条 `SELECT`。
- 禁止 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE`、`CREATE` 等危险语句。
- 禁止查询密码字段。
- 没有 `LIMIT` 时自动补限制，避免大结果集拖垮服务。

## 12. Hybrid Agent 功能说明

Hybrid Agent 用于同时结合课程资料和数据库统计数据回答问题。

典型问题：

```text
结合 Redis 课程资料和最近答题情况，分析学生薄弱点
根据 MySQL 索引资料和错题记录，给出复习建议
```

处理流程：

1. 问题路由器判断问题属于 `hybrid`。
2. RAG 检索课程资料片段。
3. Text-to-SQL 查询学习数据。
4. 大模型综合资料上下文和 SQL 查询结果，生成分析结论。

统一入口：

```text
POST /agent/query
```

## 13. RocketMQ 异步任务说明

当前核心异步任务有三类：

1. 文档索引任务：上传资料后异步调用 Python RAG 建索引。
2. 学习行为任务：学习行为接口只发送消息，消费者异步落库。
3. 答题统计任务：提交答案后发送统计消息，为后续课程统计、知识点统计预留扩展点。

异步化的目的：

- 避免文档索引阻塞上传接口。
- 避免高频学习行为写入影响主流程响应。
- 为统计、推荐、画像等后续功能预留扩展点。
- 利用 RocketMQ 重试能力提升任务可靠性。

## 14. 启动步骤

以下命令均在 Windows PowerShell 下执行。

### 14.1 启动 MySQL

按本机安装方式启动 MySQL 服务，确认 3306 端口可用。

```powershell
mysql -u your_user -p -e "SELECT VERSION();"
```

### 14.2 执行 `sql/schema.sql`

在项目根目录执行：

```powershell
mysql -u your_user -p < .\sql\schema.sql
```

### 14.3 执行 `sql/init.sql`

```powershell
mysql -u your_user -p < .\sql\init.sql
```

### 14.4 启动 Redis

按本机安装方式启动 Redis，确认端口可用：

```powershell
redis-cli -h localhost -p 6379 PING
```

返回 `PONG` 表示 Redis 可用。

### 14.5 启动 RocketMQ NameServer

```powershell
mqnamesrv.cmd
```

### 14.6 启动 RocketMQ Broker

另开一个 PowerShell 窗口：

```powershell
mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true
```

如果本机 RocketMQ 需要指定配置文件，请按你的 RocketMQ 安装路径调整命令。

### 14.7 启动 Python Agent

```powershell
cd E:\Project\activity-agent\agent-service
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

健康检查：

```powershell
curl http://localhost:8000/health
```

### 14.8 启动 Spring Boot 后端

另开 PowerShell 窗口：

```powershell
cd E:\Project\activity-agent\activity-agent-backend
mvn spring-boot:run
```

健康检查：

```powershell
curl http://localhost:8080/health
```

### 14.9 使用 Apifox / curl 测试接口

Apifox 可导入：

```text
docs/apifox-openapi.yaml
```

导入后将环境地址设置为：

```text
http://localhost:8080
```

## 15. 接口测试示例

### 登录

```powershell
curl -X POST "http://localhost:8080/auth/login" `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"student01\",\"password\":\"123456\"}"
```

### 查询课程列表

```powershell
curl "http://localhost:8080/course/list?pageNum=1&pageSize=10"
```

### 创建课程

```powershell
curl -X POST "http://localhost:8080/course/create" `
  -H "Content-Type: application/json" `
  -d "{\"courseName\":\"操作系统\",\"teacherId\":2,\"description\":\"操作系统基础课程\",\"status\":1}"
```

### 上传课程资料

```powershell
curl -X POST "http://localhost:8080/knowledge/upload" `
  -F "courseId=1" `
  -F "file=@E:\Project\activity-agent\docs\rag-samples\JVM复习资料.md"
```

### 查询知识库文档

```powershell
curl "http://localhost:8080/knowledge/list?courseId=1&pageNum=1&pageSize=10"
```

### 知识库问答

```powershell
curl -X POST "http://localhost:8080/knowledge/query" `
  -H "Content-Type: application/json" `
  -d "{\"courseId\":1,\"userId\":1,\"question\":\"JVM 垃圾回收有哪些重点？\",\"topK\":4}"
```

### Agent 统一问答

```powershell
curl -X POST "http://localhost:8080/agent/query" `
  -H "Content-Type: application/json" `
  -d "{\"question\":\"统计最近 7 天每门课程的提问次数\",\"user_id\":1,\"course_id\":1}"
```

### 创建题目

```powershell
curl -X POST "http://localhost:8080/question/create" `
  -H "Content-Type: application/json" `
  -d "{\"courseId\":1,\"knowledgePoint\":\"JVM\",\"questionContent\":\"JVM 判断对象是否可回收常用什么方法？\",\"optionA\":\"可达性分析\",\"optionB\":\"随机判断\",\"optionC\":\"哈希取模\",\"optionD\":\"轮询检测\",\"answer\":\"A\",\"analysis\":\"从 GC Roots 出发不可达的对象通常可被回收。\"}"
```

### 提交答案

```powershell
curl -X POST "http://localhost:8080/answer/submit" `
  -H "Content-Type: application/json" `
  -d "{\"userId\":1,\"courseId\":1,\"questionId\":1,\"userAnswer\":\"A\"}"
```

### 查询错题

```powershell
curl "http://localhost:8080/answer/wrong/list?userId=1&courseId=1"
```

### 记录学习行为

```powershell
curl -X POST "http://localhost:8080/learning/event" `
  -H "Content-Type: application/json" `
  -d "{\"userId\":1,\"courseId\":1,\"eventType\":\"VIEW_COURSE\"}"
```

## 16. RocketMQ 测试说明

### 16.1 验证文档索引消息

1. 启动 MySQL、Redis、RocketMQ、Python Agent 和 Spring Boot 后端。
2. 调用上传接口：

```powershell
curl -X POST "http://localhost:8080/knowledge/upload" `
  -F "courseId=1" `
  -F "file=@E:\Project\activity-agent\docs\rag-samples\JVM复习资料.md"
```

3. 查看后端日志，应看到 `Sent knowledge index message` 和 `Received knowledge index message`。
4. 查询文档状态：

```sql
SELECT id, course_id, file_name, status, chunk_count, error_message
FROM knowledge_document
ORDER BY id DESC
LIMIT 5;
```

`status=1` 且 `chunk_count > 0` 表示索引成功。

### 16.2 验证学习行为消息

调用学习行为接口：

```powershell
curl -X POST "http://localhost:8080/learning/event" `
  -H "Content-Type: application/json" `
  -d "{\"userId\":1,\"courseId\":1,\"eventType\":\"VIEW_COURSE\"}"
```

查看后端日志，应看到 `Sent learning event message` 和 `Received learning event message`。

查询落库结果：

```sql
SELECT id, user_id, course_id, event_type, message_key, create_time
FROM learning_event
ORDER BY id DESC
LIMIT 5;
```

### 16.3 验证答题统计消息

调用答题接口：

```powershell
curl -X POST "http://localhost:8080/answer/submit" `
  -H "Content-Type: application/json" `
  -d "{\"userId\":1,\"courseId\":1,\"questionId\":1,\"userAnswer\":\"A\"}"
```

查看后端日志，应看到 `Sent answer stat message` 和 `Received answer stat message`。

查询答题记录：

```sql
SELECT id, user_id, course_id, question_id, user_answer, correct, create_time
FROM answer_record
ORDER BY id DESC
LIMIT 5;
```

### 16.4 RocketMQ 排查命令

不同 RocketMQ 安装包的脚本路径可能不同，以下命令按本地环境调整：

```powershell
mqadmin topicList -n 127.0.0.1:9876
mqadmin consumerProgress -n 127.0.0.1:9876 -g edu_knowledge_index_consumer_group
mqadmin consumerProgress -n 127.0.0.1:9876 -g edu_learning_event_consumer_group
mqadmin consumerProgress -n 127.0.0.1:9876 -g edu_answer_stat_consumer_group
```

重点检查：

- Topic 是否存在。
- ConsumerGroup 是否在线。
- 消费堆积是否持续增长。
- 后端日志是否有消费异常。
- Python Agent 日志是否有 `/rag/index` 或 `/rag/query` 异常。

## 17. 常见问题

### 17.1 后端启动时报数据库连接失败

检查 MySQL 是否启动，`edu_agent` 是否已创建，`application.yml` 中的用户名和密码是否是本机配置。不要使用 README 中的占位值。

### 17.2 Redis 连接失败

检查 Redis host、port、password 是否和本地一致。如果本地 Redis 没有密码，需要调整 `application.yml`。

### 17.3 RocketMQ 连接失败

确认 NameServer 和 Broker 已启动：

```powershell
mqnamesrv.cmd
mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true
```

如果 Topic 不存在，开发环境可开启 `autoCreateTopicEnable=true`，或手动创建 Topic。

### 17.4 文档上传后一直是 `status=0`

说明索引消息可能没有被消费。检查：

- RocketMQ 是否启动。
- `KnowledgeIndexConsumer` 是否有消费日志。
- Python Agent 是否启动。
- `agent.rag-index-url` 是否指向 `http://localhost:8000/rag/index`。

### 17.5 文档变成 `status=2`

说明索引失败。检查后端日志和 Python Agent 日志，常见原因包括：

- 上传文件路径不存在。
- Python 依赖未安装。
- Embedding 模型不可用。
- 大模型 API 配置错误。
- FAISS 向量库路径不可写。

### 17.6 日志出现 `Knowledge document not found`

通常是 RocketMQ 中还有旧消息，但数据库已被重置或对应文档已删除。开发环境可以清理旧消息、重置消费位点，或让消费者对已删除文档直接跳过。

### 17.7 Text-to-SQL 没有返回结果

检查 Python Agent 的 MySQL 只读账号配置是否正确，并确认问题能够路由到 SQL 场景。SQL Guard 会拒绝非 `SELECT` 或危险 SQL。

### 17.8 PowerShell 下 curl JSON 转义失败

PowerShell 中建议使用反引号换行，并对 JSON 内部双引号做转义。也可以直接使用 Apifox 测试接口。

## 18. 面试亮点

- Java + Python 双服务架构：后端业务和 Agent 能力解耦，职责清晰。
- RAG 知识库：课程资料上传后异步索引，用 FAISS 检索降低大模型幻觉。
- Text-to-SQL：支持教师和管理员用自然语言查询学习数据。
- SQL Guard：限制只读查询、禁用危险语句、屏蔽敏感字段、自动补 `LIMIT`。
- Hybrid Agent：同时结合课程资料和数据库统计结果，输出更完整的学习分析。
- RocketMQ 异步解耦：文档索引、学习行为、答题统计从主流程拆出，提升接口响应速度。
- 幂等设计：文档索引状态判断、学习行为 `messageKey` 去重、答题统计按 `answerRecordId` 去重。
- Redis 缓存：登录态和课程热点数据缓存，降低数据库压力。
- 工程分层清晰：Controller、Service、Mapper、DTO、VO、Entity 分层明确，便于维护和扩展。
- 可测试性强：提供 Apifox OpenAPI 文档、curl 示例、自测文档和初始化数据。

## 19. 相关文档

- 产品文档：`docs/product.md`
- Apifox OpenAPI：`docs/apifox-openapi.yaml`
- 后端自测：`activity-agent-backend/SELF_TEST.md`
- 知识库测试：`docs/knowledge-backend-test-guide.md`
- Python Agent 说明：`agent-service/README.md`
- 废弃文件清单：`docs/deprecated-files.md`
