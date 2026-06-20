# 智能课程学习助手 Agent 平台 3 分钟面试口述版

## 1. 适合直接说的版本

我做过一个“智能课程学习助手 Agent 平台”，整体是 Spring Boot 后端加 Python FastAPI Agent 的双服务架构。这个项目面向课程学习和教学辅助场景，核心目标是把传统课程系统和大模型能力结合起来，让学生可以基于课程资料提问，让教师可以用自然语言查询学习数据，并且能结合课程资料和学习数据生成复习建议。

Java 后端这边我用的是 Spring Boot 3、MyBatis-Plus、MySQL、Redis 和 RocketMQ，主要负责用户登录、课程管理、课程资料上传、题目管理、答题判分、错题记录、学习行为记录、Agent 问答记录落库，以及调用 Python Agent 服务。

Python Agent 这边用的是 FastAPI、LangChain、FAISS、sentence-transformers、SQLAlchemy 和 OpenAI 兼容模型接口，主要负责 RAG 知识库问答、Text-to-SQL 学习数据分析、Hybrid Agent 混合分析、SQL 安全校验和大模型总结。

我在这个项目里主要做了几块事情。

第一块是后端基础架构和业务接口。我搭了 Controller、Service、Mapper、Entity、DTO、VO 这些分层，实现了统一返回、全局异常处理、MyBatis-Plus 分页查询，并完成课程、知识库、题目、答题、错题、学习行为和 Agent 查询等接口。

第二块是课程知识库 RAG 链路。用户上传 `txt` 或 `md` 资料后，后端先保存文件和 `knowledge_document` 记录，再发送 RocketMQ 文档索引消息。消费者异步调用 Python `/rag/index`，Python 完成文档解析、切分、向量化和 FAISS 入库，Java 再保存 `knowledge_chunk` 元数据并更新文档状态。

第三块是 Agent 查询链路。统一入口是 `/agent/query`，Python 侧会先根据问题关键词路由到 `sql`、`rag` 或 `hybrid`。如果是 SQL 类问题，就生成 SQL 并通过 SQL Guard 校验；如果是 RAG 类问题，就检索课程资料；如果是 Hybrid 类问题，就同时结合资料片段和学习数据生成分析结论。

第四块是 RocketMQ 异步处理。我把文档索引、学习行为记录、答题统计都拆成异步任务，分别使用 `edu_knowledge_index_topic`、`edu_learning_event_topic` 和 `edu_answer_stat_topic`。这样上传文档、记录行为、提交答案这些主流程不用等待耗时统计或索引任务完成。

这个项目里我觉得比较有亮点的地方有四个。

第一，RAG 知识库不是让大模型凭空回答，而是先检索课程资料片段，再要求模型基于上下文回答，减少幻觉。

第二，Text-to-SQL 做了安全控制。SQL Guard 只允许单条 `SELECT`，禁止 `INSERT / UPDATE / DELETE / DROP / ALTER / TRUNCATE / CREATE` 等危险语句，禁止查询密码字段，还会限制可访问表并自动补 `LIMIT 100`。

第三，Hybrid Agent 能把课程资料和学习数据结合起来。例如“结合 Redis 课程资料和最近错题情况分析学生薄弱点”，系统既会检索 Redis 资料，也会查询错题和答题数据，最后综合输出复习建议。

第四，异步任务做了幂等和可排障设计。文档索引会判断文档是否已成功索引，重复索引前会删除旧切片；学习行为用 `messageKey` 防重复写入；答题统计按 `answerRecordId` 做重复消费保护；文档索引失败会把失败原因写回 `knowledge_document.error_message`。

项目中遇到的困难主要有几个。

第一个困难是文档索引比较耗时，如果上传接口同步处理切分、Embedding 和向量入库，用户体验会很差。我用 RocketMQ 把索引任务异步化，上传接口只负责保存文件和发消息，真正的索引由消费者完成。

第二个困难是模型生成 SQL 不稳定，而且可能有安全风险。我没有直接执行模型生成的 SQL，而是做了 SQL Guard，从语句类型、危险关键字、多语句、敏感字段、表白名单和默认 `LIMIT` 几层做约束。

第三个困难是 RAG、SQL、Hybrid 三种问题类型需要统一入口，但处理链路完全不同。我用问题路由先判断问题类型，再把请求分发到不同能力，保证前端只调一个 `/agent/query`。

第四个困难是消息重复消费可能导致重复写库或重复切片。我在消费者里补了业务幂等：文档状态判断、旧切片删除、`messageKey` 唯一键和答题记录 ID 去重。

如果要说后续优化，我会从三个方向讲：一是补完整的登录鉴权、角色权限和数据权限；二是完善 RocketMQ 失败告警、死信队列巡检和自动补偿；三是增强 Agent 的多轮上下文、评测集和可观测性。

整体上，这个项目比较完整地体现了 Spring Boot 后端开发、MySQL 表设计、Redis 缓存、RocketMQ 异步处理、RAG、Text-to-SQL、Hybrid Agent 和大模型应用工程化落地能力。

---

## 2. 更短的 1 分钟压缩版

我做的是一个智能课程学习助手 Agent 平台，整体是 Spring Boot 后端加 Python FastAPI Agent 的双服务架构。后端用 Spring Boot、MyBatis-Plus、MySQL、Redis、RocketMQ，负责课程、资料、题目、答题、错题、学习行为和 Agent 问答记录；Python 服务用 FastAPI、LangChain、FAISS 和大模型接口，负责 RAG 问答、Text-to-SQL 和 Hybrid 分析。

项目主要解决三个问题：学生查资料和答疑效率低，教师查询学习数据需要写 SQL，以及课程资料和学习行为数据割裂。我的主要工作是后端分层和接口开发、Java 调 Python 联调、RAG 文档上传和异步索引、Agent 问答记录落库，以及 RocketMQ 异步任务和幂等处理。

项目亮点是 RAG 降低大模型幻觉、SQL Guard 控制 Text-to-SQL 安全、Hybrid Agent 同时结合课程资料和学习数据、RocketMQ 将文档索引和学习行为从主流程拆出。难点主要是文档索引耗时、模型 SQL 安全、问题路由和消息重复消费，我分别通过异步索引、SQL Guard、统一路由和业务幂等解决。
