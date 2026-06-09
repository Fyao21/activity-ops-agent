# 活动运营数据分析 Agent 项目面试讲解文档

## 1. 项目一句话介绍

这是一个“活动运营数据分析 Agent 系统”，我主要负责后端和服务联调部分。项目支持活动管理、用户参与、奖励发放、活动统计，以及通过自然语言查询活动数据，并结合 Python Agent 自动生成 SQL 和分析结论。

如果用更偏业务的话来说，这个项目就是模拟运营平台里“查数据、看统计、问问题”的完整链路。

---

## 2. 项目背景

很多运营系统里，运营同学并不会写 SQL，但他们经常会提类似这样的问题：

- 最近 7 天各活动参与人数是多少？
- 双十一活动奖励发放成功率是多少？
- APP 和 H5 渠道哪个带来的参与人数更多？
- 哪个活动奖励发放失败最多？

所以这个项目的目标，是让运营人员直接提自然语言问题，系统自动把问题转换成 SQL，查出结果后再返回结构化数据和自然语言分析结论。

---

## 3. 技术架构

整个项目分成两个服务：

### 3.1 Java 后端服务

技术栈：

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL
- Redis
- Redis Stream

主要职责：

- 登录
- 活动管理
- 用户参与记录
- 奖励发放记录
- 活动统计查询
- 调用 Python Agent 服务
- 保存问答记录

### 3.2 Python Agent 服务

技术栈：

- Python 3.11+
- FastAPI
- LangChain
- SQLAlchemy
- PyMySQL
- OpenAI 兼容模型接口

主要职责：

- 接收自然语言问题
- 获取数据库表结构
- 生成 SQL
- 校验 SQL 安全性
- 执行 SQL
- 生成自然语言分析结果

---

## 4. 数据库设计思路

项目里核心有 6 张表：

### 4.1 `sys_user`

用于登录用户信息，区分管理员和运营人员。

### 4.2 `activity`

用于存活动基本信息，比如：

- 活动名称
- 活动类型
- 开始时间
- 结束时间
- 状态
- 规则描述

### 4.3 `activity_user_record`

记录用户参与活动的行为数据。

典型字段：

- `activity_id`
- `user_id`
- `channel`
- `participate_status`
- `participate_time`

### 4.4 `reward_record`

记录奖励发放情况。

典型字段：

- `reward_type`
- `reward_amount`
- `send_status`
- `fail_reason`
- `send_time`

### 4.5 `activity_statistics`

保存活动每日统计数据。

典型字段：

- `participant_count`
- `reward_count`
- `reward_success_count`
- `conversion_rate`
- `retention_rate`

### 4.6 `agent_qa_record`

保存问答记录，便于后续排查和审计。

典型字段：

- `question`
- `generated_sql`
- `query_result`
- `answer`
- `success`
- `error_message`

---

## 5. 我在这个项目里做了什么

如果面试官问“你在这个项目里主要负责什么”，可以这样答：

### 5.1 我负责了后端基础框架搭建

包括：

- Spring Boot 项目初始化
- 分层结构设计
- 统一返回体 `Result`
- 全局异常处理
- MyBatis-Plus 集成
- Redis 集成

### 5.2 我实现了核心业务接口

包括：

- `POST /auth/login`
- `POST /activity/create`
- `GET /activity/list`
- `GET /activity/{id}`
- `PUT /activity/update`
- `POST /activity/participate`
- `POST /reward/send`
- `GET /statistics/activity`
- `POST /agent/query`

### 5.3 我完成了 Java 调 Python Agent 的联调

后端通过 `RestTemplate` 调用 Python 服务：

- 地址配置在 `application.yml`
- 支持超时设置
- 异常会包装成业务错误
- Python 返回后会落库到 `agent_qa_record`

### 5.4 我做了 Redis Stream 异步处理

包括两个 stream：

- `stream:activity:event`
- `stream:reward:event`

作用：

- 参与活动后异步刷新统计数据
- 发奖后异步更新奖励状态和统计数据

### 5.5 我配合完成了 Python Agent 侧的 SQL 安全控制

核心安全约束：

- 只允许 `SELECT`
- 禁止 `INSERT / UPDATE / DELETE / DROP / ALTER / TRUNCATE / CREATE`
- 禁止多语句执行
- 禁止查询 `sys_user.password`
- 自动补 `LIMIT 100`

---

## 6. 项目亮点

这部分很适合面试时主动讲。

### 6.1 亮点一：自然语言转 SQL

这不是普通 CRUD 项目，项目引入了 Python + LangChain + 大模型，把自然语言问题转成 SQL，再把结果转成自然语言分析。

这个点能体现：

- 我理解 AI Agent 与业务系统集成的方式
- 我能做跨语言服务联调
- 我能考虑 SQL 安全问题

### 6.2 亮点二：Redis Stream 异步处理

我没有把统计更新直接写在主流程里，而是改成异步处理。

这样做的好处：

- 主流程响应更快
- 统计逻辑和业务写入逻辑解耦
- 更接近真实业务系统里的消息驱动架构

### 6.3 亮点三：统计数据按天重算，而不是简单累加

这是一个比较容易打动面试官的点。

因为 Redis Stream 消息可能重试，如果消费失败后再次消费，简单 `+1` 容易重复累计。

所以我这里采用“按活动 + 日期重算”的方式更新统计数据：

- 重新统计当天参与人数
- 重新统计当天奖励数
- 重新统计当天奖励成功数

这样消息即使重复投递，最终结果仍然是正确的。

### 6.4 亮点四：保留问答记录便于排障

`/agent/query` 返回之后，我会把以下内容保存到 `agent_qa_record`：

- 用户问题
- 生成的 SQL
- 查询结果
- 最终回答
- 是否成功
- 错误信息

这个设计的价值是：

- 方便复盘模型生成的 SQL 是否合理
- 方便排查用户反馈问题
- 为后续审计或优化 prompt 提供依据

---

## 7. 面试 Q&A

下面这部分你可以直接背，也可以按自己的表达方式说。

### Q1：你这个项目主要解决什么问题？

A：

这个项目主要解决的是运营人员不会写 SQL，但又经常要查活动数据的问题。  
我做的是一个活动运营数据分析系统，运营人员输入自然语言问题，系统自动生成 SQL 查询数据库，再把结果整理成自然语言分析返回。

---

### Q2：为什么项目要拆成 Java 后端和 Python Agent 两个服务？

A：

因为两边职责不同。

Java 后端更适合承接标准业务能力，比如：

- 登录
- 活动管理
- 参与记录
- 奖励记录
- 统计查询

Python 这边更适合做 Agent 和大模型相关能力，比如：

- Text-to-SQL
- LLM 调用
- SQL 校验
- 查询结果总结

拆开以后职责更清晰，也方便后续单独扩展 Python Agent 能力。

---

### Q3：`/agent/query` 的完整调用链路是什么？

A：

完整链路是这样的：

1. 前端调用 Java 后端的 `POST /agent/query`
2. Java 后端接收 `question` 和 `user_id`
3. Java 后端通过 `RestTemplate` 调用 Python FastAPI 的 `/agent/query`
4. Python Agent 基于表结构和问题生成 SQL
5. Python Agent 先做 SQL 安全校验
6. 校验通过后执行 SQL
7. Python Agent 再把查询结果总结成自然语言
8. 返回给 Java 后端
9. Java 后端把 `question / generated_sql / query_result / answer` 落库到 `agent_qa_record`
10. 最终返回给前端

---

### Q4：你做了哪些 SQL 安全控制？

A：

我重点做了这几层：

1. 只允许 `SELECT`
2. 禁止 `INSERT / UPDATE / DELETE / DROP / ALTER / TRUNCATE / CREATE`
3. 禁止多语句执行
4. 禁止查询 `sys_user.password`
5. 没有 `LIMIT` 就自动补 `LIMIT 100`
6. Python Agent 查询时只暴露必要表，不把敏感表全部开放给模型

这样可以把 Text-to-SQL 的风险控制在只读范围内。

---

### Q5：为什么用 Redis Stream？

A：

主要是为了把业务主流程和统计更新流程解耦。

比如用户参与活动时，核心是先把参与记录写成功。  
统计数据的刷新不是强实时交易逻辑，可以异步做。

我这里用 Redis Stream 的好处是：

- 实现简单
- 支持消费者组
- 支持 pending 消息
- 支持 ack 机制
- 比普通 list 更适合做异步事件流

---

### Q6：参与活动之后，Redis Stream 这块具体怎么处理？

A：

流程是：

1. 用户调用 `/activity/participate`
2. 后端校验活动存在、状态正确、时间合法、用户未重复参与
3. 写入 `activity_user_record`
4. 发送消息到 `stream:activity:event`
5. 消费者读取消息
6. 重新计算当天该活动的 `participant_count`
7. 更新 `activity_statistics`
8. 成功后 `ack`
9. 失败则只记录日志，不 `ack`

---

### Q7：奖励发放为什么也要走异步？

A：

因为奖励发放在真实业务里通常不是瞬时完成的。

它可能会涉及：

- 券服务
- 积分服务
- 风控服务
- 库存服务

所以我这里模拟成：

1. 主流程先写 `reward_record`，状态是 `INIT`
2. 发消息到 `stream:reward:event`
3. 消费者异步把状态更新为 `SUCCESS`
4. 同步刷新 `reward_count` 和 `reward_success_count`

这样更贴近真实系统设计。

---

### Q8：为什么统计更新不用简单加一，而要重算？

A：

因为消息系统天然存在重复消费风险。

如果每次消费都直接 `participant_count + 1`，当消息重试时就会重复累计。

所以我这里按“活动 + 日期”重算：

- 再查一次当天真实参与人数
- 再查一次当天真实奖励数
- 再查一次当天真实奖励成功数

这样即使消息重复消费，统计结果也不会错。

---

### Q9：你这个项目里有哪些表索引设计思路？

A：

我会结合查询场景建索引。

例如：

- `activity_user_record(activity_id, participate_time)`：适合查某活动某时间段参与情况
- `activity_user_record(user_id, activity_id)`：适合判重
- `reward_record(activity_id, send_status)`：适合统计某活动奖励成功/失败情况
- `activity_statistics(activity_id, stat_date)`：适合查某活动某时间段统计
- `agent_qa_record(user_id, create_time)`：适合查用户问答历史

---

### Q10：如果面试官问“这个项目还有什么可以优化”？

A：

我会从这几个方向回答：

1. 增加统一登录鉴权拦截器
2. 给 Redis Stream 增加失败重试和死信处理
3. 增加只读数据库账号，进一步收紧 Agent 查询权限
4. 给 Agent 增加问答历史和上下文能力
5. 增加缓存穿透和缓存过期策略
6. 增加接口测试和集成测试
7. 增加监控，比如消息堆积、接口耗时、模型调用失败率

---

## 8. 可以主动讲的“设计思考”

这部分在面试里很加分。

### 8.1 为什么后端要保存 `generated_sql`

因为 Agent 项目里，很多问题不是“查不到数据”，而是“模型生成错 SQL”。

把 `generated_sql` 保存下来后：

- 可以复盘模型行为
- 可以定位 prompt 是否有问题
- 可以支持后续做 SQL 评估和调优

### 8.2 为什么把统计表单独设计出来

因为运营查询通常是高频的，而且很多查询都是聚合查询。

如果每次都实时扫明细表：

- 性能差
- SQL 更复杂
- 高并发时压力更大

所以用 `activity_statistics` 做每日聚合，是典型的数据汇总思路。

### 8.3 为什么要保留 Redis Stream 的 pending 消息

因为如果消费失败直接丢弃，会导致统计不一致。

不 `ack` 的好处是：

- 消息还在 pending list 里
- 后续可以人工处理或程序重试
- 能保证最终一致性

---

## 9. 项目中的不足与改进方向

这个部分面试时不要回避，反而可以体现你有工程意识。

可以这样说：

### 当前不足

1. 登录后还没有做完整 token 鉴权
2. Redis Stream 还没有做自动重试和死信队列
3. 奖励发放目前是模拟成功路径，没有接真实三方服务
4. Agent 目前主要是单轮问答
5. 测试覆盖还不够完整

### 后续优化

1. 增加 Spring 拦截器统一鉴权
2. 对 Redis Stream pending 消息做自动认领和重试
3. 增加只读数据库账号与数据库最小权限控制
4. 增加监控告警
5. 增加单元测试、接口测试、联调测试

---

## 10. 面试时的简洁版自我表达模板

如果面试官让你快速介绍项目，可以直接这样说：

“我做过一个活动运营数据分析 Agent 项目，整体是 Java 后端加 Python Agent 双服务架构。  
Java 后端用 Spring Boot、MyBatis-Plus、MySQL、Redis，负责登录、活动管理、参与记录、奖励记录、统计查询以及调用 Python Agent。  
Python 服务用 FastAPI 和 LangChain，把自然语言转成 SQL，并做 SQL 安全校验和结果总结。  
另外我还用 Redis Stream 做了参与事件和奖励事件的异步处理，消费成功后 ack，失败不 ack，保证后续可以重试。  
这个项目里我重点做了后端分层设计、Java 调 Python 联调、问答记录落库、以及 Redis Stream 异步统计更新。”  

---

## 11. 最后建议

你面试时不要只是背“我做了什么”，最好按照下面这个顺序讲：

1. 业务问题是什么
2. 架构怎么拆
3. 你负责哪部分
4. 有哪些亮点
5. 遇到什么问题，怎么解决
6. 后续还能怎么优化

这样比单纯讲接口列表更像真正参与过项目。
