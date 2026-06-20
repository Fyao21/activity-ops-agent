# SpringBoot 复习资料

## SpringBoot 自动配置

SpringBoot 的核心目标是减少 Spring 项目的重复配置，让开发者更快搭建可运行的业务系统。自动配置主要依赖 `spring-boot-autoconfigure` 中的大量 `AutoConfiguration` 类。这些配置类会根据当前 classpath 中是否存在某些依赖、配置文件中是否开启某些属性、容器中是否已经存在某些 Bean 来决定是否生效。

常见条件注解包括 `@ConditionalOnClass`、`@ConditionalOnMissingBean`、`@ConditionalOnProperty`、`@ConditionalOnBean` 等。例如项目引入 Redis 依赖后，SpringBoot 会尝试自动创建 Redis 相关连接工厂和模板 Bean；如果用户自己定义了同类型 Bean，自动配置通常会让用户配置优先。理解自动配置可以帮助排查“为什么某个 Bean 被创建了”“为什么配置没有生效”“为什么引入依赖后行为发生变化”等问题。

排查自动配置问题时，可以开启 `debug=true` 查看 Condition Evaluation Report，或在日志中查看哪些自动配置类匹配成功、哪些没有生效。实际项目里不建议盲目排除自动配置，而是先确认依赖、配置项、Bean 覆盖关系和 Profile 是否正确。

## Starter 机制

Starter 是 SpringBoot 提供的一种依赖聚合方式。比如 `spring-boot-starter-web` 会把 Spring MVC、Tomcat、Jackson、Validation 等 Web 开发常用依赖组合起来，开发者不需要手动维护大量版本。Starter 本身通常不写业务逻辑，主要负责依赖管理和触发自动配置。

自定义 Starter 通常包含两部分：一个 autoconfigure 模块和一个 starter 模块。autoconfigure 模块提供配置属性类、自动配置类和默认 Bean；starter 模块负责引入 autoconfigure 和第三方依赖。这样业务项目只要引入 starter，就能获得一组默认能力。常见场景包括统一日志、统一鉴权、统一限流、统一 MQ 客户端、统一对象存储客户端等。

设计 Starter 时要注意默认值要保守，不能强行覆盖业务系统已有 Bean。自动配置类应尽量使用 `@ConditionalOnMissingBean`，让业务方可以自定义替换。配置项也应有清晰前缀，例如 `edu.agent.xxx`，避免污染全局配置。

## Spring MVC 请求流程

Spring MVC 的核心请求链路是：请求进入 DispatcherServlet，DispatcherServlet 根据 HandlerMapping 找到对应 Controller 方法，再由 HandlerAdapter 调用方法，方法返回结果后经过 MessageConverter 序列化为 JSON，最终写回响应。

在这个过程中，常见扩展点包括 Filter、Interceptor、ControllerAdvice、ArgumentResolver 和 HttpMessageConverter。Filter 属于 Servlet 规范，执行位置更靠前，适合处理跨域、编码、日志 TraceId 等通用逻辑。Interceptor 属于 Spring MVC，适合做登录态校验、权限校验、接口耗时统计。ControllerAdvice 常用于全局异常处理和统一响应包装。ArgumentResolver 可以把请求中的 token、用户信息等转换为方法参数。

排查接口 404 时，要确认请求路径、Controller 注解、应用 context-path 和请求方法是否一致。排查 400 时，通常要看参数校验、JSON 字段名和数据类型。排查 500 时，要结合全局异常日志、数据库操作和下游服务调用。

## 全局异常处理

业务系统不应该把底层异常栈直接返回给前端。常见做法是定义统一返回体，例如 `code`、`message`、`data`，再通过 `@RestControllerAdvice` 捕获业务异常、参数校验异常和未知异常。业务异常用于表达可预期问题，例如课程不存在、题目不存在、上传文件为空、SQL 校验失败等。

全局异常处理的价值有三点。第一，前端可以统一解析响应结构。第二，用户看到的是友好的错误信息，而不是数据库或框架异常。第三，后端日志仍然可以保留完整异常栈，方便排查。对于未知异常，返回给前端的信息应尽量简短，例如“系统异常，请稍后重试”，详细原因只写入日志。

在 Agent 项目中，全局异常尤其重要。因为 Python Agent、模型 API、向量库、数据库查询都可能失败，如果直接暴露异常，既不安全也不友好。更好的方式是保存问答失败记录，并给前端返回明确但不过度暴露内部细节的信息。

## 配置文件与环境隔离

SpringBoot 常用 `application.yml` 管理配置。实际项目中，本地、测试、生产环境的数据库、Redis、RocketMQ、模型服务地址通常不同，因此需要通过 Profile 或环境变量隔离。例如 `application-dev.yml` 用于本地开发，`application-prod.yml` 用于生产配置。敏感信息如数据库密码、API Key 不应提交到 Git。

配置项可以通过 `@ConfigurationProperties` 绑定到 Java 对象，适合管理一组有前缀的配置，例如 Python Agent 地址、超时时间、对象存储配置等。相比直接使用 `@Value`，`@ConfigurationProperties` 更适合复杂配置，也方便做默认值和类型校验。

排查配置不生效时，要确认激活的 Profile、配置文件路径、属性前缀、字段命名和环境变量覆盖关系。SpringBoot 的 relaxed binding 支持中划线、下划线、驼峰之间转换，但前缀和层级仍要保持一致。

## 事务管理

Spring 事务通常通过 `@Transactional` 实现。它基于 AOP 代理，在方法执行前开启事务，方法正常结束时提交，遇到运行时异常时回滚。常见失效原因包括同类内部方法调用、方法不是 public、异常被捕获没有继续抛出、数据库引擎不支持事务、事务方法没有被 Spring 容器管理等。

在课程学习助手项目中，保存答题记录、发送消息、保存文档元数据等操作都涉及事务边界。需要注意的是，本地数据库事务和 MQ 消息发送不是天然原子。如果数据库写成功但消息发送失败，或者消息发送成功但事务回滚，就会出现一致性边界问题。生产级方案可以考虑本地消息表、事务消息或 Outbox Pattern。

事务不是越大越好。长事务会占用连接、持有锁、增加 undo 版本压力，影响并发。查询接口一般不需要开启写事务；批量处理任务应控制事务粒度，避免一次事务处理过多数据。

## 常见面试问题

1. SpringBoot 自动配置如何生效？
2. `@ConditionalOnMissingBean` 的作用是什么？
3. Filter 和 Interceptor 有什么区别？
4. 为什么要做全局异常处理？
5. `@Transactional` 在哪些情况下会失效？
6. 配置文件中的敏感信息为什么不能提交到 Git？
7. SpringBoot 项目如何做多环境配置？
