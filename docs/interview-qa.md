# 智能课程学习助手 Agent 平台面试讲解文档

## 1. 项目一句话介绍

这是一个面向高校课程学习场景的智能课程学习助手 Agent 平台，我主要负责 Spring Boot 后端业务、Java 与 Python Agent 联调、RAG 知识库链路、Text-to-SQL 查询链路、RocketMQ 异步任务和问答记录落库。

用业务语言来说，这个项目就是把“课程资料、课后答疑、学习行为、答题错题、教学数据分析”串到一起，让学生能基于课程资料提问，让教师能用自然语言查询学习数据。

---

## 2. 项目背景

传统课程系统通常只能展示课程资料和题目，学生遇到问题还需要自己翻课件、查资料或等老师答疑；教师想看学生学习情况，也经常需要手写 SQL 或依赖后台报表。

所以这个项目做了三类 Agent 能力：

- RAG：学生基于课程资料提问。
- Text-to-SQL：教师用自然语言查询学习数据。
- Hybrid Agent：同时结合课程资料和学习数据，生成薄弱点分析和复习建议。

---

## 3. 技术架构

### 3.1 Java 后端服务

技术栈：

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL
- Redis
- RocketMQ

主要职责：

- 用户登录
- 课程管理
- 课程资料上传
- 题目管理
- 答题判分
- 错题记录
- 学习行为记录
- Agent 问答记录保存
- 调用 Python Agent 服务
- 发送和消费 RocketMQ 异步任务

### 3.2 Python Agent 服务

技术栈：

- Python 3.11+
- FastAPI
- LangChain
- FAISS
- sentence-transformers
- SQLAlchemy
- PyMySQL
- OpenAI 兼容模型接口

主要职责：

- 文档解析和文本切分
- Embedding 向量化
- FAISS 向量检索
- RAG 知识库问答
- Text-to-SQL 学习数据分析
- SQL Guard 安全校验
- Hybrid Agent 综合分析

---

## 4. 数据库设计思路

项目核心表包括：

### 4.1 `sys_user`

保存用户信息和角色，用于区分学生、教师、管理员。

### 4.2 `course`

保存课程基础信息，例如课程名称、教师、描述和状态。

### 4.3 `knowledge_document`

保存上传的课程资料元数据，包括文件路径、处理状态、切片数量和失败原因。

### 4.4 `knowledge_chunk`

保存文档切片元数据，包括 `document_id`、`course_id`、`chunk_index`、`content` 和向量库 ID。

### 4.5 `agent_qa_record`

保存 Agent 问答记录，包括问题、路由类型、生成 SQL、检索上下文、回答、成功状态和错误信息。

### 4.6 `learning_event`

保存学生学习行为，例如提问、答题、查看课程、上传资料。`message_key` 用于 MQ 消费幂等。

### 4.7 `question`

保存题目、知识点、选项、正确答案和解析。

### 4.8 `answer_record`

保存学生答题记录、是否正确，用于错题查询和后续统计。

---

## 5. 我在项目里做了什么

### 5.1 后端基础框架和接口

我搭建了 Spring Boot 分层结构，完成统一返回、全局异常处理、MyBatis-Plus 分页和基础配置，并实现课程、知识库、题目、答题、错题、学习行为、Agent 查询等核心接口。

### 5.2 RAG 知识库链路

我实现了课程资料上传和异步索引链路：后端保存文件和 `knowledge_document`，发送 RocketMQ 文档索引消息，消费者调用 Python `/rag/index`，成功后保存 `knowledge_chunk` 并更新文档状态。

### 5.3 Java 调 Python Agent 联调

Java 后端通过客户端调用 Python 的 RAG 查询、RAG 索引、Agent 查询接口，并把问答结果保存到 `agent_qa_record`，用于问答历史、问题排查和审计。

### 5.4 Text-to-SQL 安全控制

Python 侧 SQL Guard 会对模型生成 SQL 做校验：只允许单条 `SELECT`，禁止危险关键字、多语句、密码字段和非白名单表，没有 `LIMIT` 时自动补 `LIMIT 100`。

### 5.5 RocketMQ 异步任务

我把文档索引、学习行为、答题统计拆成异步任务：

- `edu_knowledge_index_topic`：文档索引。
- `edu_learning_event_topic`：学习行为记录。
- `edu_answer_stat_topic`：答题统计扩展。

这样耗时任务不会阻塞主接口，也便于后续扩展统计、画像和推荐。

---

## 6. 项目亮点

### 6.1 亮点一：RAG 知识库降低幻觉

学生提问时，系统先按课程从 FAISS 检索相关资料片段，再让大模型基于上下文回答。这个设计比直接调用大模型更适合课程场景，因为答案可控、可追溯，也更贴近教师上传的资料。

### 6.2 亮点二：Text-to-SQL 降低教师查数门槛

教师可以问“统计最近 7 天每门课程的提问次数”“查询 Java 课程错题最多的知识点”这类问题，系统自动生成 SQL 查询数据库，再把结果总结成自然语言。

### 6.3 亮点三：SQL Guard 做安全兜底

模型生成 SQL 不能直接信任，所以我加了 SQL Guard。它限制只读查询、禁止危险语句、禁止密码字段、限制表白名单，并自动补 `LIMIT`，避免误操作业务数据和大结果集拖慢服务。

### 6.4 亮点四：Hybrid Agent 综合分析

对于“结合课程资料和最近错题情况，分析学生薄弱点”这种问题，系统会同时走 RAG 和 SQL，再把知识点内容和学习数据交给大模型综合分析，输出复习建议。

### 6.5 亮点五：RocketMQ 异步解耦

文档索引、学习行为、答题统计都通过 RocketMQ 异步处理。上传资料后不用等待向量化完成，学习行为也不会阻塞主流程，系统更接近真实业务里的消息驱动架构。

### 6.6 亮点六：幂等和可排障设计

文档索引用文档状态判断是否已处理，重新索引前删除旧切片；学习行为用 `messageKey` 防重复写入；答题统计按 `answerRecordId` 去重；文档索引失败会记录 `error_message`，Agent 问答也会保存成功失败状态。

---

## 7. 项目中的困难、难点和解决方案

### 7.1 难点一：文档索引耗时不稳定

问题：

文档上传后如果同步做切分、Embedding 和 FAISS 入库，接口会变慢，而且 Python 服务或模型接口波动会影响上传成功率。

解决：

我把上传和索引拆开。上传接口只保存文件和文档元数据，然后发送 RocketMQ 消息。消费者异步调用 Python 建索引，并把处理状态写回数据库。

### 7.2 难点二：模型生成 SQL 有安全风险

问题：

大模型可能生成 `UPDATE`、`DELETE`、多语句、访问密码字段或扫描过大结果集。

解决：

执行前必须经过 SQL Guard。它从语句类型、危险关键字、多语句、敏感字段、白名单表和默认 `LIMIT` 几层做限制，把风险控制在只读查询范围内。

### 7.3 难点三：RAG、SQL、Hybrid 三条链路要统一入口

问题：

前端希望只调一个 `/agent/query`，但后端处理逻辑可能是资料问答、数据库查询或混合分析。

解决：

Python 侧增加问题路由器，根据“统计、数量、正确率”等 SQL 关键词，“什么是、解释、总结、资料”等 RAG 关键词，以及“结合、分析、建议、薄弱点”等 Hybrid 关键词做分流。

### 7.4 难点四：消息重复消费导致重复写入

问题：

RocketMQ 可能重复投递。如果消费者不做幂等，文档切片、学习行为或统计任务可能重复写入。

解决：

不同任务用不同业务键做幂等。文档索引看文档状态并删除旧切片；学习行为使用 `messageKey` 和数据库唯一键；答题统计使用答题记录 ID 做重复消费保护。

### 7.5 难点五：Agent 问题不好排查

问题：

Agent 出错时，可能是路由错、SQL 生成错、检索上下文不准、模型回答不稳定，不像普通 CRUD 接口那样容易定位。

解决：

我把问题、路由类型、生成 SQL、检索上下文、回答、成功状态和错误信息都落到 `agent_qa_record`，方便复盘和优化。

---

## 8. 面试 Q&A

### Q1：你这个项目主要解决什么问题？

A：

主要解决学生课程资料检索和答疑效率低、教师查询学习数据依赖 SQL、课程资料和学习数据割裂的问题。系统通过 RAG 做资料问答，通过 Text-to-SQL 做学习数据查询，通过 Hybrid Agent 做综合分析和复习建议。

---

### Q2：为什么要拆成 Java 后端和 Python Agent 两个服务？

A：

Java 更适合做业务系统、数据库事务、缓存、MQ 和权限；Python 在 LangChain、RAG、Embedding、FAISS 和大模型调用方面生态更成熟。拆开以后职责更清晰，也方便后续独立升级 Agent 能力。

---

### Q3：`/agent/query` 的完整链路是什么？

A：

1. 前端调用 Java 后端 `/agent/query`。
2. Java 封装请求调用 Python FastAPI。
3. Python 问题路由判断是 SQL、RAG 还是 Hybrid。
4. SQL 类问题生成 SQL 并通过 SQL Guard 后执行。
5. RAG 类问题检索课程资料片段后生成回答。
6. Hybrid 类问题同时执行 RAG 和 SQL，再综合分析。
7. Python 返回答案。
8. Java 保存问答记录到 `agent_qa_record` 并返回前端。

---

### Q4：你做了哪些 SQL 安全控制？

A：

只允许单条 `SELECT`；禁止危险关键字；禁止多语句；禁止查询 `password` 字段；限制只能访问白名单表；没有 `LIMIT` 自动补 `LIMIT 100`。

---

### Q5：为什么文档索引要用 RocketMQ？

A：

因为文档索引耗时不稳定，涉及文件读取、切分、Embedding 和向量入库。RocketMQ 可以把索引从上传主流程拆出去，让上传接口快速返回，同时利用重试能力提升任务可靠性。

---

### Q6：怎么保证 RocketMQ 重复消费不会出问题？

A：

文档索引用文档状态判断是否已经成功，重复索引前删除旧切片；学习行为用 `messageKey` 防重复写入；答题统计按 `answerRecordId` 做重复消费保护。

---

### Q7：RAG 如何减少幻觉？

A：

不直接让模型自由回答，而是先检索课程资料片段，把片段作为上下文传给模型，并在 Prompt 里约束只能基于上下文回答。如果资料里没有答案，就提示知识库未找到相关信息。

---

### Q8：Hybrid Agent 的价值是什么？

A：

它能回答单纯 RAG 或 SQL 解决不了的问题。例如分析薄弱点既需要课程资料里的知识点解释，也需要学生错题、答题正确率、学习行为等数据。Hybrid 会把两者结合后给出更有价值的建议。

---

### Q9：你这个项目有哪些索引设计思路？

A：

我会围绕查询场景建索引，比如：

- `course.teacher_id`：按教师查询课程。
- `knowledge_document(course_id, status)`：查某课程资料和处理状态。
- `knowledge_chunk(document_id)`、`knowledge_chunk(course_id)`：查文档切片和课程切片。
- `agent_qa_record(user_id, create_time)`：查用户问答历史。
- `learning_event(user_id, course_id)`、`learning_event(course_id, event_time)`：查学习行为。
- `answer_record(user_id, course_id)`、`answer_record(question_id)`、`answer_record(correct)`：查错题和正确率。

---

### Q10：这个项目还有什么可以优化？

A：

我会从五个方向回答：

1. 补完整 token 鉴权、角色权限和数据权限。
2. 扩展 PDF、DOCX、PPTX 等文档解析能力。
3. 把问题路由从关键词升级为规则加模型分类。
4. 完善 RocketMQ 告警、死信队列巡检和补偿消费。
5. 增加 RAG 命中率、SQL 成功率、模型调用耗时等监控和评测。

---

## 9. 面试时的简洁版自我表达模板

“我做过一个智能课程学习助手 Agent 平台，整体是 Spring Boot 后端加 Python FastAPI Agent 双服务架构。Spring Boot 负责课程、资料、题目、答题、错题、学习行为、Redis 缓存和 RocketMQ 异步任务；Python 负责 RAG、Text-to-SQL、Hybrid Agent、FAISS 检索和大模型调用。

项目里我重点做了课程资料上传后的异步索引、Java 调 Python 联调、Agent 问答记录落库、SQL Guard 安全校验，以及 RocketMQ 消费幂等。难点主要是文档索引耗时、模型 SQL 安全、问题路由和消息重复消费，我分别用异步索引、SQL Guard、统一路由和业务幂等解决。

这个项目比较能体现我在 Spring Boot 后端开发、MySQL 表设计、Redis 缓存、RocketMQ 异步处理、RAG、Text-to-SQL 和 Agent 工程化落地方面的能力。”

---

## 10. 面试讲述顺序建议

面试时建议按这个顺序讲：

1. 业务问题：学生答疑、教师查数、资料和数据割裂。
2. 架构拆分：Java 业务后端 + Python Agent 服务。
3. 核心链路：RAG、Text-to-SQL、Hybrid。
4. 你的职责：接口、联调、异步任务、记录落库、安全校验。
5. 项目亮点：RAG、SQL Guard、Hybrid、RocketMQ、幂等。
6. 遇到的困难：索引耗时、SQL 风险、路由、重复消费。
7. 后续优化：权限、文档类型、路由、告警、监控评测。
