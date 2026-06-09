# 活动运营数据分析 Agent 项目 3 分钟面试口述版

## 1. 适合直接说的版本

我做过一个“活动运营数据分析 Agent”项目，整体是 Java 后端加 Python Agent 双服务架构。

Java 后端这边我用的是 Spring Boot 3、MyBatis-Plus、MySQL 和 Redis，主要负责用户登录、活动管理、用户参与记录、奖励发放记录、活动统计查询，以及调用 Python Agent 服务。

Python 这边用的是 FastAPI、LangChain 和 OpenAI 兼容模型接口，主要负责把自然语言问题转成 SQL，执行查询，再把结果总结成自然语言返回。

这个项目解决的核心问题是：运营人员不会写 SQL，但是经常要问类似“最近 7 天各活动参与人数是多少”“哪个活动奖励发放失败最多”这样的问题，所以我们把自然语言查询能力接到了业务系统里。

我在这个项目里主要做了几块事情。

第一块是后端基础架构和核心接口开发。我搭了 Spring Boot 项目结构，做了 controller、service、mapper、entity、dto、vo 这些分层，也实现了统一返回类 Result 和全局异常处理。

第二块是核心业务接口，包括登录、活动创建、活动列表、活动详情、活动更新、参与活动、发放奖励、统计查询，以及 `/agent/query` 这个自然语言查询接口。

第三块是 Java 调 Python Agent 的联调。我通过 RestTemplate 去调用 Python 的 `/agent/query`，并且把 Python 返回的 `question`、`generated_sql`、`query_result`、`answer`、`success` 这些信息保存到 `agent_qa_record` 表里，方便后续排查和审计。

第四块是 Redis Stream 异步处理。我把“用户参与活动”和“奖励发放”都做成了异步事件流。参与活动成功后会发消息到 `stream:activity:event`，奖励发放请求后会发消息到 `stream:reward:event`，消费者异步更新统计表和奖励状态。

这个项目里我觉得比较有亮点的地方有三个。

第一，做了自然语言转 SQL 的能力，不是普通 CRUD 项目。

第二，做了 SQL 安全控制，比如只允许 SELECT、禁止危险语句、禁止查 `sys_user.password`、自动补 `LIMIT 100`。

第三，统计更新不是简单加一，而是按“活动 + 日期”重算。因为 Redis Stream 消息有重试可能，如果直接做 `+1`，重复消费会把统计做错，按天重算可以保证结果最终一致。

如果要说这个项目还有什么可以继续优化，我会从三个方向讲：

一是补完整的 token 鉴权和权限控制；
二是给 Redis Stream 增加失败重试、死信和 pending 消息自动认领；
三是进一步完善 Agent 侧的问答历史、监控和测试。

整体上，这个项目让我比较完整地练到了 Spring Boot 后端开发、MySQL 表设计、Redis 异步处理、跨语言服务调用，以及 AI Agent 和业务系统结合的这几块能力。

---

## 2. 更短的 1 分钟压缩版

我做的是一个活动运营数据分析 Agent 项目，后端是 Spring Boot 3 + MyBatis-Plus + MySQL + Redis，Agent 服务是 Python FastAPI + LangChain。  
项目主要解决运营人员不会写 SQL，但又需要查活动数据的问题。  
我的主要工作是后端分层搭建、核心业务接口实现、Java 调 Python Agent 联调、问答记录落库，以及基于 Redis Stream 做参与事件和奖励事件的异步处理。  
项目亮点主要是自然语言转 SQL、SQL 安全控制，以及为了防止消息重试导致统计错误，我把统计更新做成了按活动和日期重算，而不是简单累加。  
这个项目比较能体现我在 Spring Boot、数据库设计、Redis 异步处理和 AI Agent 集成方面的实践能力。
