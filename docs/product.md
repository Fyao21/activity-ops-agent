# 智能课程学习助手 Agent 平台产品文档

## 1. 项目定位

本项目是一个面向高校课程学习场景的智能课程学习助手平台，主要解决学生课程资料分散、知识点检索效率低、课后答疑依赖人工、教师难以及时掌握学生学习情况等问题。

系统基于 SpringBoot 搭建后端业务服务，使用 Python + FastAPI + LangChain 构建 Agent 服务，结合 RAG 知识库问答、Text-to-SQL 学习数据分析、Hybrid Agent 混合分析、Redis 缓存、RocketMQ 异步任务等能力，实现课程管理、课程资料上传、知识库问答、学习数据统计、错题分析和个性化学习建议等功能。

项目适合作为 Java 后端实习 / AI 应用开发方向的工程化项目展示，重点体现后端业务开发、数据库设计、缓存、消息队列、RAG、Agent、LLM 应用工程化能力。

---

## 2. 应用场景

### 2.1 学生学习场景

学生可以查看课程资料、上传个人学习资料，并通过自然语言进行提问，例如：

* Redis 缓存穿透是什么？
* MySQL 索引为什么能提高查询效率？
* JVM 垃圾回收机制有哪些？
* 帮我总结一下 Redis Stream 和 RocketMQ 的区别。
* 根据我的错题记录，帮我生成复习建议。

系统会从课程知识库中检索相关资料片段，并调用大模型生成回答，减少学生查找资料和整理笔记的时间。

### 2.2 教师教学辅助场景

教师可以上传课程资料、维护课程知识库，并通过自然语言查询学习数据，例如：

* 统计最近 7 天每门课程的提问次数。
* 查询 Java 后端开发课程错题最多的知识点。
* 统计 Redis 课程的平均正确率。
* 对比不同课程的学习活跃度。
* 查询最近一周答题错误最多的学生。

系统会通过 Text-to-SQL 将自然语言问题转换为 SQL 查询，再由大模型总结查询结果，帮助教师掌握学生学习情况。

### 2.3 教学运营分析场景

管理员可以查看整体平台使用情况，例如：

* 哪些课程访问量最高；
* 哪些知识点被提问最多；
* 哪些章节错题率最高；
* 哪些学生学习活跃度下降；
* 哪些课程资料需要补充。

---

## 3. 技术栈

### 3.1 Java 后端

* Java 17
* SpringBoot
* SpringMVC
* MyBatis / MyBatis-Plus
* MySQL
* Redis
* RocketMQ
* Maven
* Lombok
* Apifox

### 3.2 Python Agent 服务

* Python 3.11+
* FastAPI
* LangChain
* FAISS
* sentence-transformers / OpenAI Embedding
* OpenAI 兼容大模型 API
* SQLAlchemy
* PyMySQL

### 3.3 存储与中间件

* MySQL：业务数据存储
* Redis：登录态、热点课程信息、热点问答、Agent 会话上下文缓存
* RocketMQ：学习行为、答题记录、文档索引等异步任务
* FAISS：课程资料向量库，用于 RAG 检索

---

## 4. 系统架构

整体架构如下：

```text
前端 / Apifox
   ↓
SpringBoot 后端服务
   ↓
MySQL / Redis / RocketMQ
   ↓ HTTP 调用
Python FastAPI Agent 服务
   ↓
LangChain / RAG / Text-to-SQL / Hybrid Agent
   ↓
FAISS / MySQL / 大模型 API
```

### 4.1 SpringBoot 后端职责

SpringBoot 后端负责：

* 用户登录与权限控制；
* 课程管理；
* 课程资料上传；
* 题目管理；
* 答题与错题记录；
* 学习行为记录；
* Agent 问答记录保存；
* Redis 缓存；
* RocketMQ 消息发送与消费；
* 调用 Python Agent 服务。

### 4.2 Python Agent 服务职责

Python Agent 服务负责：

* 文档解析；
* 文本切分；
* Embedding 向量化；
* FAISS 向量存储；
* RAG 知识库问答；
* Text-to-SQL 学习数据分析；
* Hybrid 混合分析；
* 大模型结果总结。

---

## 5. 核心功能模块

## 5.1 用户模块

系统支持三类角色：

### 学生

* 查看课程；
* 上传个人学习资料；
* 基于课程资料提问；
* 查看问答记录；
* 提交答案；
* 查询错题；
* 获取复习建议。

### 教师

* 创建课程；
* 上传课程资料；
* 创建题目；
* 查看学生提问数据；
* 查询学习统计；
* 分析章节薄弱点。

### 管理员

* 管理用户；
* 管理课程；
* 查看平台整体数据；
* 管理知识库文档。

### 登录态设计

登录成功后生成 Token，并将用户信息存入 Redis。

Redis Key：

```text
login:token:{token}
```

Value 示例：

```json
{
  "userId": 1,
  "username": "student001",
  "role": "STUDENT"
}
```

---

## 5.2 课程管理模块

### 功能说明

用于维护课程基础信息，例如课程名称、授课教师、课程描述、课程状态等。

### 核心接口

```text
POST /course/create
GET  /course/list
GET  /course/{id}
PUT  /course/update
```

### Redis 缓存设计

课程详情属于读多写少数据，查询课程详情时优先从 Redis 获取。

Redis Key：

```text
course:info:{courseId}
```

缓存策略：

* 查询课程详情时先查 Redis；
* Redis 未命中时查询 MySQL；
* 查询成功后写入 Redis；
* 更新课程信息后删除对应缓存；
* 缓存过期时间设置为 30 分钟。

---

## 5.3 课程资料知识库模块

### 功能说明

教师或学生可以上传课程资料，系统将资料切分后进行向量化，存入 FAISS，用于后续 RAG 检索。

第一版支持：

* txt
* md

后续可扩展：

* pdf
* docx
* pptx

### 核心接口

```text
POST /knowledge/upload
GET  /knowledge/list
POST /knowledge/query
```

### 文档上传流程

```text
用户上传课程资料
   ↓
SpringBoot 保存文件到 uploads/knowledge
   ↓
MySQL 保存 knowledge_document 记录，状态为待处理
   ↓
SpringBoot 发送 RocketMQ 文档索引消息
   ↓
RocketMQ 消费者调用 Python /rag/index
   ↓
Python 读取文档并切分
   ↓
Embedding 模型生成向量
   ↓
FAISS 保存向量
   ↓
Python 返回 chunk_count
   ↓
SpringBoot 更新文档状态为处理成功
```

### 文档状态

knowledge_document.status：

```text
0：待处理
1：处理成功
2：处理失败
```

---

## 5.4 RAG 知识库问答模块

### 功能说明

学生可以基于课程资料进行知识库问答。

示例问题：

```text
Redis 缓存穿透是什么？
MySQL 最左前缀原则是什么？
JVM 内存结构包括哪些部分？
帮我总结一下 Redis 复习资料中的重点。
```

### RAG 流程

```text
用户输入问题
   ↓
问题向量化
   ↓
FAISS 检索相关课程资料片段
   ↓
构造 Prompt
   ↓
调用大模型
   ↓
返回答案和引用片段
```

### RAG Prompt 约束

```text
你是一个课程学习知识库助手。
你只能基于给定的课程资料上下文回答问题。
如果上下文中没有答案，请回答“知识库中未找到相关信息”。
不要编造课程资料中不存在的内容。
回答要适合学生理解。
如果涉及多个知识点，请分条说明。
```

---

## 5.5 Text-to-SQL 学习数据分析模块

### 功能说明

教师或管理员可以通过自然语言查询学习数据，系统自动生成 SQL 并返回分析结论。

示例问题：

```text
统计最近 7 天每门课程的提问次数。
查询 Java 后端开发课程错题最多的知识点。
统计 MySQL 数据库课程的平均正确率。
对比不同课程的学习活跃度。
查询最近一周答题错误最多的学生。
```

### 执行流程

```text
用户输入数据分析问题
   ↓
Agent 判断为 SQL 查询类问题
   ↓
读取数据库表结构
   ↓
生成 SQL
   ↓
SQL Guard 安全校验
   ↓
执行 SQL 查询
   ↓
大模型总结查询结果
```

### SQL Guard 安全要求

必须保留 SQL 安全校验：

* 只允许 SELECT；
* 禁止 INSERT；
* 禁止 UPDATE；
* 禁止 DELETE；
* 禁止 DROP；
* 禁止 ALTER；
* 禁止 TRUNCATE；
* 禁止多语句执行；
* 禁止查询 sys_user.password；
* 查询结果默认限制 LIMIT 100。

---

## 5.6 Hybrid Agent 混合分析模块

### 功能说明

有些问题既需要查询课程资料，又需要查询学习数据，例如：

```text
根据 Redis 复习资料，分析最近一周学生错题集中在哪些知识点。
结合 JVM 课程资料，分析学生目前掌握较差的部分。
根据 MySQL 索引章节内容，给错题较多的学生生成复习建议。
```

这类问题需要同时使用：

* RAG 检索课程资料；
* Text-to-SQL 查询学习数据；
* 大模型综合分析。

### Hybrid 流程

```text
用户输入问题
   ↓
问题路由判断为 HYBRID
   ↓
RAG 检索相关课程资料
   ↓
Text-to-SQL 查询学习数据
   ↓
构造综合分析 Prompt
   ↓
大模型生成分析结论和学习建议
```

### Hybrid 输出内容

回答内容包括：

1. 相关知识点摘要；
2. 学习数据摘要；
3. 薄弱点分析；
4. 复习建议。

---

## 5.7 学习行为记录模块

### 功能说明

系统记录学生的学习行为，用于后续数据分析。

学习行为包括：

* 查看课程；
* 提问；
* 查看答案；
* 提交题目；
* 收藏知识点；
* 上传资料。

### RocketMQ 异步处理流程

用户产生学习行为后，主流程只发送消息到 RocketMQ，不直接阻塞写统计数据。

```text
用户触发学习行为
   ↓
SpringBoot 发送 RocketMQ 消息
   ↓
RocketMQ Broker
   ↓
LearningEventConsumer 消费消息
   ↓
写入 learning_event 表
   ↓
更新课程学习统计
```

### Topic 设计

```text
Topic：edu_learning_event_topic
ConsumerGroup：edu_learning_event_consumer_group
```

### Tag 设计

```text
QUESTION：学生提问
ANSWER：学生答题
VIEW_COURSE：查看课程
UPLOAD_DOC：上传资料
```

---

## 5.8 题目、答题与错题模块

### 功能说明

教师可以创建题目，学生可以提交答案。系统判断正误后保存答题记录，并支持查询错题。

### 核心接口

```text
POST /question/create
GET  /question/list
POST /answer/submit
GET  /answer/wrong/list
```

### 答题流程

```text
学生提交答案
   ↓
SpringBoot 查询题目正确答案
   ↓
判断是否正确
   ↓
保存 answer_record
   ↓
发送 RocketMQ 学习行为消息，Tag=ANSWER
   ↓
返回答题结果和解析
```

---

## 5.9 RocketMQ 异步任务模块

本项目使用 RocketMQ 替代 Redis Stream 作为消息队列，主要用于业务解耦、流量削峰和异步处理。

### 主要异步场景

1. 学习行为异步记录；
2. 文档索引异步处理；
3. 答题后统计数据异步更新；
4. Agent 问答记录异步保存，后续可扩展。

### Topic 设计

#### 1. 学习行为 Topic

```text
Topic：edu_learning_event_topic
Producer：LearningEventProducer
Consumer：LearningEventConsumer
ConsumerGroup：edu_learning_event_consumer_group
```

消息体示例：

```json
{
  "userId": 1,
  "courseId": 1,
  "eventType": "QUESTION",
  "eventTime": "2026-06-18 12:00:00"
}
```

#### 2. 文档索引 Topic

```text
Topic：edu_knowledge_index_topic
Producer：KnowledgeIndexProducer
Consumer：KnowledgeIndexConsumer
ConsumerGroup：edu_knowledge_index_consumer_group
```

消息体示例：

```json
{
  "documentId": 1,
  "courseId": 1,
  "filePath": "uploads/knowledge/redis.md",
  "fileName": "Redis复习资料.md"
}
```

#### 3. 答题统计 Topic

```text
Topic：edu_answer_stat_topic
Producer：AnswerStatProducer
Consumer：AnswerStatConsumer
ConsumerGroup：edu_answer_stat_consumer_group
```

消息体示例：

```json
{
  "userId": 1,
  "courseId": 1,
  "questionId": 1,
  "knowledgePoint": "Redis缓存穿透",
  "correct": false,
  "answerTime": "2026-06-18 12:00:00"
}
```

### RocketMQ 消费要求

1. 消费成功后正常返回，完成 ACK；
2. 消费失败时记录错误日志并抛出异常，让 RocketMQ 后续重试；
3. 消费者逻辑要考虑幂等，避免重复消费导致统计重复增加；
4. 对于文档索引任务，重复消费时需要判断 knowledge_document 状态；
5. 对于学习行为任务，可以通过业务唯一键或消息唯一 ID 控制重复写入；
6. 对于答题统计任务，可基于 answer_record.id 做幂等判断。

---

## 6. 数据库设计

## 6.1 用户表 sys_user

```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码',
    role VARCHAR(32) NOT NULL COMMENT '角色 STUDENT/TEACHER/ADMIN',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username(username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

## 6.2 课程表 course

```sql
CREATE TABLE course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
    course_name VARCHAR(128) NOT NULL COMMENT '课程名称',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    description TEXT COMMENT '课程描述',
    status TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_teacher(teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';
```

## 6.3 知识库文档表 knowledge_document

```sql
CREATE TABLE knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(32) NOT NULL COMMENT '文件类型',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    status TINYINT DEFAULT 0 COMMENT '处理状态 0待处理 1成功 2失败',
    chunk_count INT DEFAULT 0 COMMENT '切片数量',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course(course_id),
    INDEX idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';
```

## 6.4 知识库切片表 knowledge_chunk

```sql
CREATE TABLE knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '片段ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    chunk_index INT NOT NULL COMMENT '片段序号',
    content TEXT NOT NULL COMMENT '片段内容',
    vector_id VARCHAR(128) COMMENT '向量库ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_document(document_id),
    INDEX idx_course(course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库切片表';
```

## 6.5 Agent 问答记录表 agent_qa_record

```sql
CREATE TABLE agent_qa_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    course_id BIGINT DEFAULT NULL COMMENT '课程ID',
    question TEXT NOT NULL COMMENT '用户问题',
    route_type VARCHAR(32) COMMENT '路由类型 SQL/RAG/HYBRID',
    generated_sql TEXT COMMENT '生成SQL',
    retrieved_context TEXT COMMENT '检索上下文',
    answer TEXT COMMENT '最终回答',
    success TINYINT DEFAULT 1 COMMENT '是否成功',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time(user_id, create_time),
    INDEX idx_course(course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent问答记录表';
```

## 6.6 学习行为记录表 learning_event

```sql
CREATE TABLE learning_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '事件ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    event_type VARCHAR(64) NOT NULL COMMENT '行为类型',
    event_time DATETIME NOT NULL COMMENT '行为时间',
    message_key VARCHAR(128) DEFAULT NULL COMMENT 'RocketMQ消息唯一键，用于幂等',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_key(message_key),
    INDEX idx_user_course(user_id, course_id),
    INDEX idx_course_time(course_id, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习行为记录表';
```

## 6.7 题目表 question

```sql
CREATE TABLE question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    knowledge_point VARCHAR(128) COMMENT '知识点',
    question_content TEXT NOT NULL COMMENT '题目内容',
    answer VARCHAR(255) NOT NULL COMMENT '正确答案',
    analysis TEXT COMMENT '解析',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_course(course_id),
    INDEX idx_knowledge_point(knowledge_point)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';
```

## 6.8 学生答题记录表 answer_record

```sql
CREATE TABLE answer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '答题记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    user_answer VARCHAR(255) COMMENT '用户答案',
    correct TINYINT NOT NULL COMMENT '是否正确 1正确 0错误',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_course(user_id, course_id),
    INDEX idx_question(question_id),
    INDEX idx_correct(correct)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录表';
```

---

## 7. Redis 设计

本项目中 Redis 主要用于缓存，不再作为消息队列使用。

### 7.1 登录态缓存

```text
login:token:{token}
```

### 7.2 课程详情缓存

```text
course:info:{courseId}
```

### 7.3 Agent 会话上下文缓存

```text
agent:session:{userId}:{courseId}
```

### 7.4 热点问答缓存

```text
agent:qa:cache:{questionHash}
```

### 7.5 热点课程统计缓存

```text
course:stat:{courseId}
```

---

## 8. Python Agent 服务设计

Python 服务目录建议：

```text
agent-service/
├── main.py
├── agent.py
├── sql_agent.py
├── rag_service.py
├── router.py
├── vector_store.py
├── document_loader.py
├── sql_guard.py
├── schemas.py
├── requirements.txt
├── .env.example
└── vector_store/
```

### 环境变量

```env
OPENAI_API_KEY=your_api_key
OPENAI_BASE_URL=https://api.openai-proxy.org/v1
MODEL_NAME=deepseek-v4-flash

EMBEDDING_PROVIDER=local
EMBEDDING_MODEL=BAAI/bge-small-zh-v1.5
VECTOR_STORE_PATH=./vector_store

MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=your_password
MYSQL_DATABASE=edu_agent
```

### Python 接口

#### 文档索引接口

```text
POST /rag/index
```

请求示例：

```json
{
  "document_id": 1,
  "course_id": 1,
  "file_path": "uploads/knowledge/redis.md",
  "file_name": "Redis复习资料.md"
}
```

#### RAG 查询接口

```text
POST /rag/query
```

请求示例：

```json
{
  "course_id": 1,
  "question": "Redis 缓存穿透是什么？",
  "top_k": 4
}
```

#### 混合 Agent 查询接口

```text
POST /agent/query
```

请求示例：

```json
{
  "user_id": 1,
  "course_id": 1,
  "question": "根据 Redis 复习资料，分析最近一周学生错题集中在哪些知识点。"
}
```

---

## 9. 问题路由设计

Agent 接收到问题后，先判断问题类型。

### SQL 类问题

适合查询数据库统计数据。

关键词：

```text
统计、数量、人数、正确率、错题数、排名、对比、最近、今天、昨天、本周、平均、活跃度
```

返回：

```text
sql
```

### RAG 类问题

适合查询课程资料、知识点解释、FAQ。

关键词：

```text
什么是、解释、原理、规则、说明、知识点、总结、复习、资料、文档、为什么、如何
```

返回：

```text
rag
```

### Hybrid 类问题

适合结合资料和数据综合分析。

关键词：

```text
结合、根据、分析、建议、薄弱点、是否正常、原因、复习计划、掌握情况
```

返回：

```text
hybrid
```

---

## 10. 核心接口设计

### 用户登录

```text
POST /auth/login
```

### 创建课程

```text
POST /course/create
```

### 查询课程列表

```text
GET /course/list
```

### 查询课程详情

```text
GET /course/{id}
```

### 上传课程资料

```text
POST /knowledge/upload
```

### 查询课程资料列表

```text
GET /knowledge/list
```

### RAG 问答

```text
POST /knowledge/query
```

### Agent 混合问答

```text
POST /agent/query
```

### 创建题目

```text
POST /question/create
```

### 查询题目列表

```text
GET /question/list
```

### 提交答案

```text
POST /answer/submit
```

### 查询错题

```text
GET /answer/wrong/list
```

### 提交学习行为

```text
POST /learning/event
```

---

## 11. RocketMQ 配置设计

application.yml 示例：

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: edu-agent-producer-group
```

建议常量类：

```java
public class RocketMqConstant {
    public static final String LEARNING_EVENT_TOPIC = "edu_learning_event_topic";
    public static final String KNOWLEDGE_INDEX_TOPIC = "edu_knowledge_index_topic";
    public static final String ANSWER_STAT_TOPIC = "edu_answer_stat_topic";

    public static final String TAG_QUESTION = "QUESTION";
    public static final String TAG_ANSWER = "ANSWER";
    public static final String TAG_VIEW_COURSE = "VIEW_COURSE";
    public static final String TAG_UPLOAD_DOC = "UPLOAD_DOC";
    public static final String TAG_DOC_INDEX = "DOC_INDEX";
}
```

### 生产者设计

* LearningEventProducer：发送学习行为消息；
* KnowledgeIndexProducer：发送文档索引消息；
* AnswerStatProducer：发送答题统计消息。

### 消费者设计

* LearningEventConsumer：消费学习行为消息，写入 learning_event 表；
* KnowledgeIndexConsumer：消费文档索引消息，调用 Python /rag/index，更新文档状态；
* AnswerStatConsumer：消费答题统计消息，后续可扩展更新课程统计或知识点统计。

### 消费失败处理

1. 消费成功：正常返回，RocketMQ 认为消息消费成功；
2. 消费失败：记录错误日志并抛出异常，让 RocketMQ 自动重试；
3. 多次失败：后续可接入死信队列；
4. 幂等控制：通过 messageKey、documentId、answerRecordId 等业务唯一标识避免重复处理。

---

## 12. 工程化亮点

### 12.1 Java 与 Python 服务解耦

SpringBoot 负责业务系统，Python 负责 AI 能力，二者通过 HTTP 接口通信，便于独立开发、部署和扩展。

### 12.2 RAG 降低大模型幻觉

系统不会直接让大模型凭空回答，而是先从课程知识库中检索相关资料片段，再让模型基于上下文回答。

### 12.3 Text-to-SQL 提升教学数据查询效率

教师可以用自然语言查询学习数据，降低 SQL 使用门槛。

### 12.4 Hybrid Agent 支持综合分析

系统可以同时结合课程资料和学习数据，生成更有价值的学习建议。

### 12.5 Redis 提升访问性能

使用 Redis 缓存登录态、课程信息、热点问答和 Agent 会话上下文。

### 12.6 RocketMQ 实现异步解耦

学习行为记录、文档索引、答题统计等任务通过 RocketMQ 异步处理，降低主流程耗时，并提升系统在高并发场景下的稳定性。

### 12.7 SQL Guard 控制数据安全

模型生成的 SQL 必须经过安全校验，只允许 SELECT，避免误操作业务数据。

### 12.8 本地 Embedding 降低部署成本

开发阶段可以使用本地 Embedding 模型生成向量，避免依赖外部 embedding API。

---

## 13. 项目难点与解决方案

### 13.1 文档问答容易出现幻觉

解决方案：

使用 RAG，将检索到的课程资料片段放入 Prompt，并要求模型只能基于上下文回答。

### 13.2 模型生成 SQL 有安全风险

解决方案：

增加 SQL Guard，只允许 SELECT，禁止修改类 SQL，同时限制敏感字段。

### 13.3 文档索引耗时较长

解决方案：

上传文档后先保存元数据，再发送 RocketMQ 文档索引消息，由消费者异步调用 Python RAG 服务完成文档切分和向量化。

### 13.4 学习行为高频写入影响主流程

解决方案：

学习行为先发送 RocketMQ 消息，消费者异步写入数据库，避免主接口被统计写入阻塞。

### 13.5 消息重复消费导致数据重复

解决方案：

通过 messageKey、documentId、answerRecordId 等业务唯一标识做幂等判断。

---

## 14. 简历描述

项目名称：智能课程学习助手 Agent 平台

技术栈：

SpringBoot + MyBatis + MySQL + Redis + RocketMQ + Python + FastAPI + LangChain + FAISS + 大模型 API

项目描述：

基于 SpringBoot 与 Python FastAPI + LangChain 搭建智能课程学习助手 Agent 平台，面向高校课程学习与教学辅助场景，采用 Java 后端业务服务与 Python Agent 服务解耦的双服务架构。平台支持课程管理、课程资料上传、RAG 知识库问答、Text-to-SQL 学习数据分析、Hybrid Agent 综合分析、答题与错题记录、学习行为统计和个性化复习建议等功能。

系统中 SpringBoot 负责用户、课程、资料、题目、答题、学习行为和异步任务等业务流程，Python Agent 服务负责文档解析、文本切分、Embedding 向量化、FAISS 检索、大模型问答和 SQL 生成分析。通过 Redis 缓存热点数据与会话上下文，通过 RocketMQ 处理学习行为记录、文档索引和答题统计等异步任务，提升主流程响应速度和系统扩展性。

项目亮点：

* 基于 RAG 构建课程知识库问答流程，支持 txt、md 等课程资料上传，将文档切分后进行 Embedding 向量化并持久化到 FAISS，通过 course_id 做课程级检索隔离，提升回答准确性与可控性。
* 使用 LangChain 构建 Text-to-SQL Agent，将教师或管理员的自然语言问题转换为安全 SQL 查询，实现课程提问次数、错题数量、知识点错误分布、课程正确率、学习活跃度和学生错误排行等指标分析。
* 设计 Hybrid Agent 路由机制，根据问题类型自动选择 RAG、SQL 或混合分析流程，支持结合课程资料内容和学生学习数据，生成薄弱点分析与个性化复习建议。
* 保留 SpringBoot 后端与 Python Agent 服务解耦架构，Java 侧负责业务数据和权限流程，Python 侧负责 RAG、Text-to-SQL 和大模型调用，便于独立开发、部署和扩展 AI 能力。
* 使用 Redis 缓存登录态、热点课程信息、Agent 会话上下文和热点问答结果，降低数据库重复查询压力，提升高频访问场景下的响应速度。
* 引入 RocketMQ 处理学习行为记录、课程资料索引、答题统计等异步任务，将耗时任务从主业务链路中拆出，实现业务解耦、削峰填谷和失败重试。
* 针对模型生成 SQL 的安全风险，设计 SQL Guard，只允许单条 SELECT 查询，禁止危险关键字、多语句执行和敏感字段访问，并默认补充 LIMIT 100，降低误操作和数据泄露风险。
* 设计学习行为、答题记录、知识点和 Agent 问答等核心表结构，为后续学习画像、薄弱知识点识别和课程质量分析提供数据基础。

---

## 15. 面试介绍话术

这个项目是一个面向高校课程学习场景的智能课程学习助手平台。传统学习系统一般只能展示课程资料和题目，学生遇到问题时还需要自己翻课件或者等老师答疑，效率比较低。所以我在这个项目里引入了 RAG 和 Agent 能力。

系统分成 SpringBoot 后端和 Python Agent 服务两部分。SpringBoot 负责用户、课程、文档、题目、错题、学习记录等业务模块；Python 服务负责 RAG 检索、Text-to-SQL 和大模型调用。

在 RAG 部分，教师可以上传课程资料，系统会对文档进行切分、向量化，并存入 FAISS。学生提问时，系统先从向量库中检索相关课程片段，再把这些片段和问题一起交给大模型回答，从而减少模型幻觉。

在数据分析部分，我使用 Text-to-SQL Agent，让教师可以直接用自然语言查询学习数据，比如某门课程的错题数量、学生正确率、提问次数等。为了防止模型生成危险 SQL，我做了 SQL Guard，只允许 SELECT 查询，并限制敏感字段。

另外我还设计了 Hybrid 模式，对于“根据课程资料分析学生薄弱点”这类问题，系统会先通过 RAG 找到相关知识点，再通过 SQL 查询错题和学习数据，最后综合生成学习建议。

消息队列方面，我使用 RocketMQ 处理学习行为记录、文档索引、答题统计等异步任务。例如上传文档后，主流程只保存文件和文档元数据，然后发送文档索引消息，由消费者异步调用 Python RAG 服务完成向量化，避免文档处理阻塞上传接口。学习行为和答题统计也通过 RocketMQ 异步处理，从而实现业务解耦和流量削峰。

这个项目主要体现了 SpringBoot 后端开发、MySQL 表设计、Redis 缓存、RocketMQ 异步处理、RAG、Text-to-SQL 和 Agent 工程化落地能力。
