# Codex 分步骤开发提示词

## 使用说明

本项目已经有 `docs/product.md` 产品文档。

请不要一次性让 Codex 改完整项目。正确做法是：

1. 先让 Codex 阅读 `docs/product.md`；
2. 让 Codex 输出当前项目分析和改造计划；
3. 每次只让 Codex 实现一个模块；
4. 每一步完成后先运行项目，确认没问题后再继续下一步；
5. 每完成一步就提交一次 Git，方便回退。

---

# 第 0 步：让 Codex 先阅读产品文档，不改代码

请先阅读 `docs/product.md`，理解当前项目要改造成的目标。

当前项目名称：

智能课程学习助手 Agent 平台

请注意：

1. 当前项目是在原有 activity-agent 项目基础上增量改造。
2. 不要重新创建新项目。
3. 不要删除现有能运行的基础结构。
4. 保留 SpringBoot 后端 + Python FastAPI Agent 服务的双服务架构。
5. 保留 Java 通过 HTTP 调用 Python Agent 服务的方式。
6. 保留 MySQL、Redis、RAG、Text-to-SQL、SQL Guard 等已有设计。
7. 消息队列已从 Redis Stream 调整为 RocketMQ。
8. Redis 只用于缓存，不再作为消息队列。
9. RocketMQ 用于学习行为记录、文档索引、答题统计等异步任务。

请先只输出以下内容，不要修改代码：

1. 当前项目中哪些模块可以保留；
2. 哪些模块需要新增；
3. 哪些模块需要从活动运营场景改造成教育学习场景；
4. RocketMQ 需要替换或新增哪些类；
5. 推荐的开发顺序；
6. 可能的风险点；
7. 哪些文件可能需要修改。

---

# 第 1 步：只改数据库 SQL

请基于 `docs/product.md`，先只修改数据库 SQL。

修改范围：

* sql/schema.sql
* sql/init.sql

不要修改 Java 代码。
不要修改 Python 代码。
不要修改 README。

要求：

1. 新增或完善以下表：

    * sys_user
    * course
    * knowledge_document
    * knowledge_chunk
    * agent_qa_record
    * learning_event
    * question
    * answer_record

2. 所有表使用 MySQL 语法。

3. 所有表使用 utf8mb4。

4. 添加必要索引。

5. learning_event 表需要增加 message_key 字段，用于 RocketMQ 消息幂等。

6. sql/init.sql 中插入测试数据：

    * 3 个用户：学生、教师、管理员；
    * 3 门课程：Java 后端开发、MySQL 数据库、Redis 实战；
    * 每门课程插入若干题目；
    * 插入学习行为记录；
    * 插入答题记录，包含正确和错误数据。

测试数据要能支持以下自然语言查询：

1. 统计最近 7 天每门课程提问次数；
2. 查询错题最多的知识点；
3. 统计某门课程正确率；
4. 对比不同课程学习活跃度；
5. 查询最近一周答题错误最多的学生。

完成后请说明修改了哪些 SQL 文件。

---

# 第 2 步：只实现 Python RAG 服务

请只修改 `agent-service`，不要修改 SpringBoot 后端。

目标：

实现课程资料 RAG 能力，包括文档读取、文本切分、Embedding 向量化、FAISS 持久化存储、RAG 查询。

需要新增或修改：

* main.py
* schemas.py
* rag_service.py
* vector_store.py
* document_loader.py
* requirements.txt
* .env.example

功能要求：

1. 新增 `POST /rag/index` 接口。

请求示例：

```json
{
  "document_id": 1,
  "course_id": 1,
  "file_path": "uploads/knowledge/redis.md",
  "file_name": "Redis复习资料.md"
}
```

处理流程：

* 支持读取 txt、md 文件；
* 使用 LangChain 文本切分器；
* chunk_size=500；
* chunk_overlap=80；
* 支持本地 Embedding；
* 向量存入 FAISS；
* 按 course_id 存入 metadata；
* FAISS 持久化到 VECTOR_STORE_PATH；
* 返回 document_id、course_id、chunk_count、success、message。

2. 新增 `POST /rag/query` 接口。

请求示例：

```json
{
  "course_id": 1,
  "question": "Redis 缓存穿透是什么？",
  "top_k": 4
}
```

处理流程：

* 将问题向量化；
* 从 FAISS 检索同一 course_id 下相关文档片段；
* 将检索片段拼接进 Prompt；
* 调用大模型生成回答；
* 如果没有相关片段，返回“知识库中未找到相关信息”。

3. RAG Prompt 要求：

```text
你是一个课程学习知识库助手。
你只能基于给定的课程资料上下文回答问题。
如果上下文中没有答案，请回答“知识库中未找到相关信息”。
不要编造课程资料中不存在的内容。
回答要适合学生理解。
如果涉及多个知识点，请分条说明。
```

4. `.env.example` 增加：

```env
OPENAI_API_KEY=your_api_key
OPENAI_BASE_URL=https://api.openai-proxy.org/v1
MODEL_NAME=deepseek-v4-flash
EMBEDDING_PROVIDER=local
EMBEDDING_MODEL=BAAI/bge-small-zh-v1.5
VECTOR_STORE_PATH=./vector_store
```

5. 如果 `EMBEDDING_PROVIDER=local`，使用本地 embedding 模型。
6. 如果 `EMBEDDING_PROVIDER=openai`，预留 OpenAI 兼容 embedding 实现。
7. 不要把 API Key 写死在代码里。
8. 不要删除现有 Text-to-SQL 功能。
9. 保证可以通过以下命令启动：

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

---

# 第 3 步：实现 Java 知识库模块，但文档索引用 RocketMQ 异步处理

请只修改 `activity-agent-backend`，不要修改 Python 代码。

目标：

让 SpringBoot 后端支持上传课程资料，并通过 RocketMQ 异步触发 Python RAG 文档索引。

需要新增：

entity：

* KnowledgeDocument
* KnowledgeChunk

mapper：

* KnowledgeDocumentMapper
* KnowledgeChunkMapper

service：

* KnowledgeService
* KnowledgeServiceImpl

controller：

* KnowledgeController

client：

* PythonRagClient

dto / vo：

* KnowledgeUploadResponse
* KnowledgeQueryRequest
* KnowledgeQueryResponse
* KnowledgeDocumentVO
* RagIndexRequest
* RagIndexResponse
* RagQueryRequest
* RagQueryResponse

RocketMQ 相关：

* KnowledgeIndexProducer
* KnowledgeIndexConsumer
* KnowledgeIndexMessage
* RocketMqConstant

application.yml 增加：

```yaml
agent:
  python-url: http://localhost:8000/agent/query
  rag-index-url: http://localhost:8000/rag/index
  rag-query-url: http://localhost:8000/rag/query

rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: edu-agent-producer-group
```

接口要求：

## 1. POST /knowledge/upload

功能：

1. 支持上传 txt、md 文件；
2. 请求参数包含 courseId；
3. 文件保存到 uploads/knowledge 目录；
4. 保存 knowledge_document 记录，status=0；
5. 发送 RocketMQ 文档索引消息到 `edu_knowledge_index_topic`，Tag 为 `DOC_INDEX`；
6. 立即返回文档 ID、文件名、处理状态。

注意：

上传接口不要同步调用 Python /rag/index，文档索引交给 RocketMQ 消费者异步处理。

## 2. KnowledgeIndexConsumer

功能：

1. 消费 `edu_knowledge_index_topic`；
2. 收到消息后查询 knowledge_document；
3. 如果文档状态已经是 1，说明已经处理成功，直接忽略，避免重复消费；
4. 如果文档状态不是 1，则调用 Python `/rag/index`；
5. Python 返回成功后，更新 knowledge_document.status=1，更新 chunk_count；
6. Python 返回失败后，更新 status=2，保存 error_message；
7. 消费成功后正常返回；
8. 消费失败时记录错误日志并抛出异常，让 RocketMQ 后续重试。

## 3. GET /knowledge/list

功能：

* 根据 courseId 分页查询文档列表；
* 返回 id、courseId、fileName、fileType、status、chunkCount、createTime。

## 4. POST /knowledge/query

请求：

```json
{
  "courseId": 1,
  "question": "Redis 缓存穿透是什么？"
}
```

功能：

1. Java 调用 Python `/rag/query`；
2. 返回 answer、retrievedChunks、sources；
3. 保存问答记录到 agent_qa_record，route_type=RAG。

要求：

1. Java 调 Python 优先使用 RestTemplate。
2. 文件上传目录不存在时自动创建。
3. 代码有清晰注释。
4. 不要删除现有 AgentController 和 Text-to-SQL 功能。
5. Redis 不再作为消息队列使用。

---

# 第 4 步：实现课程管理模块

请在 `activity-agent-backend` 中新增课程管理模块。

需要新增：

entity：

* Course

mapper：

* CourseMapper

service：

* CourseService
* CourseServiceImpl

controller：

* CourseController

dto / vo：

* CourseCreateRequest
* CourseUpdateRequest
* CourseVO

接口：

1. `POST /course/create`
2. `GET /course/list`
3. `GET /course/{id}`
4. `PUT /course/update`

Redis 缓存要求：

1. 查询课程详情时优先查 Redis；
2. Redis Key：`course:info:{courseId}`；
3. 更新课程信息后删除缓存；
4. 缓存过期时间设置为 30 分钟。

要求：

1. 不要修改 Python 代码。
2. 不要删除已有知识库模块。
3. 代码有清晰注释。

---

# 第 5 步：实现学习行为记录和 RocketMQ 异步消费

请在 `activity-agent-backend` 中补充学习行为记录模块和 RocketMQ 异步处理。

目标：

用户产生学习行为时，先发送到 RocketMQ，再由消费者异步写入 learning_event 表。

需要新增：

entity：

* LearningEvent

mapper：

* LearningEventMapper

service：

* LearningEventService
* LearningEventServiceImpl

controller：

* LearningEventController

mq：

* LearningEventProducer
* LearningEventConsumer
* LearningEventMessage

constant：

* RocketMqConstant

接口：

`POST /learning/event`

请求示例：

```json
{
  "userId": 1,
  "courseId": 1,
  "eventType": "QUESTION"
}
```

RocketMQ：

```text
Topic：edu_learning_event_topic
ConsumerGroup：edu_learning_event_consumer_group
Tags：
- QUESTION
- ANSWER
- VIEW_COURSE
- UPLOAD_DOC
```

消费者要求：

1. 接收到学习行为后发送消息到 RocketMQ；
2. 消费者监听 `edu_learning_event_topic`；
3. 消费成功后写入 learning_event 表；
4. 消费成功后正常返回；
5. 消费失败时记录日志并抛出异常，让 RocketMQ 后续重试；
6. 使用 messageKey 做幂等，避免重复消费导致重复写入；
7. 代码要有清晰注释。

不要修改 RAG 功能。
不要使用 Redis Stream。
不要删除现有代码。

---

# 第 6 步：实现题目、答题、错题模块，并发送 RocketMQ 统计消息

请在 `activity-agent-backend` 中实现题目、答题和错题模块。

需要新增：

entity：

* Question
* AnswerRecord

mapper：

* QuestionMapper
* AnswerRecordMapper

service：

* QuestionService
* AnswerService

controller：

* QuestionController
* AnswerController

dto / vo：

* QuestionCreateRequest
* AnswerSubmitRequest
* QuestionVO
* AnswerResultVO
* WrongQuestionVO

RocketMQ：

* AnswerStatProducer
* AnswerStatConsumer
* AnswerStatMessage

接口：

## 1. POST /question/create

教师创建题目。

## 2. GET /question/list

根据 courseId 查询题目列表。

## 3. POST /answer/submit

请求示例：

```json
{
  "userId": 1,
  "courseId": 1,
  "questionId": 1,
  "userAnswer": "A"
}
```

处理逻辑：

1. 查询题目正确答案；
2. 判断是否正确；
3. 保存 answer_record；
4. 返回是否正确和题目解析；
5. 发送 RocketMQ 学习行为消息，Tag=ANSWER；
6. 发送 RocketMQ 答题统计消息到 `edu_answer_stat_topic`。

## 4. GET /answer/wrong/list

查询某个学生某门课程的错题列表。

消费者要求：

1. AnswerStatConsumer 消费 `edu_answer_stat_topic`；
2. 消费成功后可先只记录日志，后续再扩展统计表；
3. 消费失败记录日志并抛出异常，让 RocketMQ 后续重试；
4. 使用 answerRecordId 做幂等预留。

要求：

1. 保证答题记录正常入库。
2. 错题列表只查询 correct=0 的记录。
3. 保留必要注释。
4. 不要修改 Python 代码。
5. 不要使用 Redis Stream。

---

# 第 7 步：升级 Text-to-SQL 为教育学习数据分析

请在 `agent-service` 中升级现有 Text-to-SQL Agent，使其适配教育场景数据库。

目标：

让教师或管理员可以用自然语言查询学习数据。

示例问题：

1. 统计最近 7 天每门课程的提问次数。
2. 查询 Java 后端开发课程错题最多的知识点。
3. 统计 MySQL 数据库课程的平均正确率。
4. 对比不同课程的学习活跃度。
5. 查询最近一周答题错误最多的学生。

要求：

1. 保留 SQL Guard。
2. 只允许 SELECT。
3. 禁止 INSERT、UPDATE、DELETE、DROP、ALTER、TRUNCATE。
4. 禁止查询 sys_user.password。
5. 禁止多语句执行。
6. 查询结果默认 LIMIT 100。
7. System Prompt 改成教育学习数据分析场景。
8. 允许查询以下表：

    * sys_user
    * course
    * knowledge_document
    * agent_qa_record
    * learning_event
    * question
    * answer_record

返回内容包括：

* generated_sql
* query_result
* answer
* success
* error_message

不要删除 RAG 功能。

---

# 第 8 步：实现 SQL / RAG / Hybrid 路由

请在 `agent-service` 中升级 `/agent/query` 接口，实现问题路由。

目标：

根据用户问题自动选择：

1. SQL：学习数据统计类问题；
2. RAG：课程资料问答类问题；
3. HYBRID：结合课程资料和学习数据的综合分析问题。

请实现 `route_question(question: str) -> str`。

返回值：

* sql
* rag
* hybrid

路由规则先用关键词实现。

SQL 类关键词：

```text
统计、数量、人数、正确率、错题数、排名、对比、最近、今天、昨天、本周、平均、活跃度
```

RAG 类关键词：

```text
什么是、解释、原理、规则、说明、知识点、总结、复习、资料、文档、为什么、如何
```

Hybrid 类关键词：

```text
结合、根据、分析、建议、薄弱点、是否正常、原因、复习计划、掌握情况
```

处理逻辑：

1. SQL 问题走 Text-to-SQL。
2. RAG 问题走 RAG 检索问答。
3. Hybrid 问题先 RAG 检索课程资料，再 SQL 查询学习数据，最后调用大模型综合分析。

Hybrid Prompt：

```text
你是智能课程学习分析 Agent。
你会同时获得课程资料上下文和数据库查询结果。
请结合两部分信息生成学习分析。
不要编造数据库中不存在的数据。
不要编造课程资料中不存在的内容。
如果资料不足，请明确说明。
输出包括：
1. 相关知识点摘要；
2. 学习数据摘要；
3. 薄弱点分析；
4. 复习建议。
```

`/agent/query` 返回字段：

* routeType
* generatedSql
* retrievedChunks
* queryResult
* answer
* success
* errorMessage

不要破坏 Java 后端原有调用方式。

---

# 第 9 步：Java 适配新的 /agent/query 返回

请在 `activity-agent-backend` 中适配 Python `/agent/query` 的新返回结构。

要求：

1. 修改 PythonAgentClient，使其可以接收：

    * routeType
    * generatedSql
    * retrievedChunks
    * queryResult
    * answer
    * success
    * errorMessage

2. 修改 AgentController：

接口：

`POST /agent/query`

请求：

```json
{
  "userId": 1,
  "courseId": 1,
  "question": "根据 Redis 复习资料，分析最近一周学生错题集中在哪些知识点。"
}
```

处理逻辑：

1. Java 调用 Python `/agent/query`；
2. 保存问答记录到 agent_qa_record；
3. route_type 保存 routeType；
4. generated_sql 保存 generatedSql；
5. retrieved_context 保存 retrievedChunks；
6. answer 保存 answer；
7. success 和 error_message 正常保存；
8. 返回统一 Result。

要求：

1. 不要删除 `/knowledge/query`。
2. 不要删除原有业务接口。
3. 不要使用 Redis Stream。

---

# 第 10 步：补充示例课程资料

请在 `docs/rag-samples` 目录下新增示例课程资料，用于测试 RAG。

新增文件：

1. Redis复习资料.md
2. MySQL索引复习资料.md
3. JVM复习资料.md

每个文件至少 800 字，内容适合 RAG 检索。

Redis复习资料.md 内容包括：

* Redis 常用数据结构；
* 缓存穿透；
* 缓存击穿；
* 缓存雪崩；
* 分布式锁；
* RocketMQ 与 Redis Stream 的区别；
* RDB 和 AOF。

MySQL索引复习资料.md 内容包括：

* B+ 树索引；
* 聚簇索引；
* 非聚簇索引；
* 联合索引；
* 最左前缀原则；
* 索引失效；
* SQL 优化。

JVM复习资料.md 内容包括：

* JVM 内存结构；
* 类加载机制；
* 双亲委派；
* 垃圾回收算法；
* 常见 OOM 原因；
* Full GC 排查思路。

不要修改代码。

---

# 第 11 步：补充 README 和 RocketMQ 测试说明

请完善 README.md。

README 需要包含：

1. 项目介绍；
2. 应用场景；
3. 技术栈；
4. 系统架构；
5. 项目目录结构；
6. MySQL 初始化方式；
7. Redis 配置；
8. RocketMQ 配置；
9. Python Agent 配置；
10. RAG 功能说明；
11. Text-to-SQL 功能说明；
12. Hybrid Agent 功能说明；
13. RocketMQ 异步任务说明；
14. 启动步骤；
15. 接口测试示例；
16. 常见问题；
17. 面试亮点。

启动步骤必须包含：

1. 启动 MySQL；
2. 执行 sql/schema.sql；
3. 执行 sql/init.sql；
4. 启动 Redis；
5. 启动 RocketMQ NameServer；
6. 启动 RocketMQ Broker；
7. 启动 Python Agent；
8. 启动 SpringBoot 后端；
9. 使用 Apifox / curl 测试接口。

RocketMQ 说明必须包含：

* Topic 列表；
* Producer 列表；
* Consumer 列表；
* ConsumerGroup 列表；
* 消费失败重试说明；
* 幂等处理说明。

注意：

不要把真实 API Key、数据库密码写入 README。
只写 `.env.example` 示例。

---

# 第 12 步：最终自查和修复

请检查整个项目是否符合 `docs/product.md` 的目标，并修复必要问题。

重点检查：

1. SpringBoot 是否可以正常启动；
2. Python FastAPI 是否可以正常启动；
3. MySQL 表结构是否完整；
4. Redis 是否可以连接；
5. RocketMQ 是否可以连接；
6. RocketMQ Topic、Producer、Consumer 是否配置正确；
7. 课程管理接口是否可用；
8. 文档上传接口是否可用；
9. 上传文档后是否发送 RocketMQ 文档索引消息；
10. KnowledgeIndexConsumer 是否能消费消息并调用 Python `/rag/index`；
11. Python `/rag/index` 是否可用；
12. Python `/rag/query` 是否可用；
13. Java `/knowledge/query` 是否能调用 Python `/rag/query`；
14. Python `/agent/query` 是否支持 sql、rag、hybrid 三种路由；
15. Agent 问答记录是否能保存到 agent_qa_record；
16. 题目和答题模块是否能保存记录；
17. 答题后是否发送 RocketMQ 学习行为和答题统计消息；
18. 错题查询是否正常；
19. SQL Guard 是否仍然只允许 SELECT；
20. `.env`、`.venv`、`vector_store`、`target`、`.idea`、`*.iml` 是否不会提交到 Git。

请只修复必要问题，不要大规模重构项目。
