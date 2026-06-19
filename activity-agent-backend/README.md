# Edu Agent Backend

`activity-agent-backend` 是“智能课程学习助手 Agent 平台”的 Spring Boot 后端服务。

后端保留 Java + Python 双服务架构：Java 负责业务接口、MySQL 数据、Redis 缓存、RocketMQ 消息、文件上传；Python FastAPI 负责 RAG、FAISS 检索、Text-to-SQL 和大模型调用。Java 通过 HTTP 调用 Python Agent 服务。

自测流程已单独移到 [SELF_TEST.md](./SELF_TEST.md)。

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
├─ src/main/java/com/example/activityagent
│  ├─ client
│  ├─ common
│  ├─ config
│  ├─ controller
│  ├─ dto
│  ├─ entity
│  ├─ mapper
│  ├─ mq
│  │  ├─ constant
│  │  ├─ consumer
│  │  ├─ dto
│  │  └─ producer
│  ├─ service
│  └─ vo
├─ src/main/resources/application.yml
├─ uploads/knowledge
├─ SELF_TEST.md
└─ pom.xml
```

## 主要模块

### 课程管理

- `POST /course/create`
- `GET /course/list`
- `GET /course/{id}`
- `PUT /course/update`

课程详情使用 Redis 缓存：

- Key：`course:info:{courseId}`
- 过期时间：30 分钟
- 更新课程后删除缓存

### 知识库资料

- `POST /knowledge/upload`
- `GET /knowledge/list`
- `POST /knowledge/query`
- `DELETE /knowledge/{id}`

资料上传后不会同步调用 Python `/rag/index`，而是发送 RocketMQ 消息，由消费者异步触发 Python RAG 索引。

资料删除时，如果文档已索引成功，后端会调用 Python `/rag/delete` 删除 FAISS 中对应 `document_id` 的向量，再删除数据库记录和本地文件。

### 学习行为记录

- `POST /learning/event`

接口只负责发送 RocketMQ 消息，消费者异步写入 `learning_event` 表。

支持事件类型：

- `QUESTION`
- `ANSWER`
- `VIEW_COURSE`
- `UPLOAD_DOC`

消费者使用 `messageKey` 做幂等，避免 RocketMQ 重复投递导致重复落库。

## RocketMQ

知识库索引：

- Topic：`edu_knowledge_index_topic`
- Tag：`DOC_INDEX`
- Consumer Group：`edu_knowledge_index_consumer_group`

学习行为：

- Topic：`edu_learning_event_topic`
- Tags：`QUESTION`、`ANSWER`、`VIEW_COURSE`、`UPLOAD_DOC`
- Consumer Group：`edu_learning_event_consumer_group`

Redis 只用于缓存，不再作为消息队列使用。

## Python Agent 配置

关键配置位于 `src/main/resources/application.yml`：

```yaml
agent:
  python-url: http://localhost:8000/agent/query
  rag-index-url: http://localhost:8000/rag/index
  rag-query-url: http://localhost:8000/rag/query
  rag-delete-url: http://localhost:8000/rag/delete
```

## 本地启动

启动前请先准备 MySQL、Redis、RocketMQ、Python Agent 服务。

```powershell
cd E:\Project\activity-agent\activity-agent-backend
mvn spring-boot:run
```

详细自测步骤见 [SELF_TEST.md](./SELF_TEST.md)。
