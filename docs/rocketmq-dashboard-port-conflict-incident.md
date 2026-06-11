# RocketMQ Dashboard 请求误发到 Spring Boot 端口问题复盘

## 1. 问题背景

项目将 Redis Stream 消息队列迁移为 RocketMQ，相关服务端口如下：

| 服务 | 端口 |
| --- | --- |
| Spring Boot | `8080` |
| RocketMQ NameServer | `9876` |
| RocketMQ Broker | `10911` |
| RocketMQ Dashboard | `8088` |

Spring Boot、RocketMQ Producer 和 Consumer 都能正常启动，但打开 RocketMQ Dashboard 的消费者页面后，Spring Boot 控制台开始出现 HTTP 请求解析异常。

## 2. 异常现象

Spring Boot 日志：

```text
Invalid character found in method name
[0x00...{"code":203,"extFields":{"consumerGroup":"agent-task-consumer-group"}}]
HTTP method names must be tokens
```

Dashboard 日志：

```text
examineConsumeStats exception to consumerGroup agent-task-consumer-group,
response [send request to <127.0.0.1:8080> failed]
```

这里的 `code=203`、`consumerGroup` 等内容属于 RocketMQ Remoting 二进制协议，并不是 HTTP 请求。

## 3. 问题影响

- Spring Boot 的 `8080` 端口不断收到 RocketMQ 二进制请求。
- Tomcat 将二进制数据误当成 HTTP 请求行解析，因此持续输出异常。
- Dashboard 无法正确展示消费者状态和消费统计。
- Spring Boot 不会因此立即崩溃，但日志被污染，Dashboard 功能异常。

## 4. 排查过程

### 4.1 检查项目配置

首先确认 Spring Boot 配置：

```yaml
server:
  port: 8080

rocketmq:
  name-server: 127.0.0.1:9876
```

项目中的 NameServer 地址正确，没有把 RocketMQ 配置成 `8080`。

### 4.2 检查实际监听端口

确认各进程实际监听：

```text
8080  -> Spring Boot
9876  -> NameServer
10911 -> Broker
8088  -> Dashboard
```

因此不是 Broker 与 Spring Boot 监听了相同端口。

### 4.3 检查 NameServer 路由

通过 `mqadmin topicRoute` 检查 Topic 路由：

```text
brokerAddr = 172.22.55.173:10911
```

NameServer 返回的 Broker 地址也是正确的，排除了 Broker 错误注册为 `127.0.0.1:8080` 的可能。

### 4.4 检查 Dashboard 配置

解压 Dashboard JAR 后发现其内置 `application.yml` 包含：

```yaml
rocketmq:
  config:
    proxyAddr: 127.0.0.1:8080
    proxyAddrs:
      - 127.0.0.1:8080
```

Dashboard 2.0.0 支持 RocketMQ 5.x Proxy，默认把 `127.0.0.1:8080` 当作 Proxy 地址。但本机 `8080` 实际是 Spring Boot，而不是 RocketMQ Proxy。

### 4.5 检查 Dashboard 前端源码

即使使用外部配置清除了默认 Proxy 地址，访问消费者页面时问题仍然存在。继续检查 Dashboard 前端源码，发现：

```javascript
address: localStorage.getItem('isV5')
    ? localStorage.getItem('proxyAddr')
    : null
```

浏览器之前访问过 Proxy 页面，已经在 `localStorage` 中保存：

```text
isV5=true
proxyAddr=127.0.0.1:8080
```

这些数据不会因为 Dashboard 重启而消失。消费者页面会继续把旧 Proxy 地址作为 `address` 参数传给 Dashboard 后端，Dashboard 后端再向 `127.0.0.1:8080` 发送 RocketMQ 请求。

## 5. 根本原因

这是一个由两层配置共同导致的问题：

1. RocketMQ Dashboard 2.0.0 默认配置了 `127.0.0.1:8080` 作为 RocketMQ Proxy。
2. 浏览器 `localStorage` 持久化了 `isV5` 和旧的 `proxyAddr`。

由于 Spring Boot 恰好也监听 `8080`，Dashboard 将 RocketMQ Remoting 协议发送给了 Tomcat，最终产生 HTTP 请求解析异常。

这个问题不是 Spring Boot Controller、RocketMQ Producer、Consumer 或 NameServer 配置错误。

## 6. 最终解决方案

### 6.1 使用独立 Dashboard 配置

创建 `rocketmq-dashboard.yml`：

```yaml
server:
  port: 8088

spring:
  application:
    name: rocketmq-dashboard

rocketmq:
  config:
    namesrvAddrs:
      - 127.0.0.1:9876
    isVIPChannel: false
    # Dashboard 2.0.0 的历史图表采集与 RocketMQ 5.3.4 部分统计项不兼容
    enableDashBoardCollect: false
    dataPath: C:/Users/w2922/logs/dashboard-data
    loginRequired: false
    useTLS: false
```

启动命令：

```cmd
java -jar rocketmq-dashboard-2.0.0.jar --spring.config.location=file:E:/Project/activity-agent/rocketmq-dashboard.yml
```

### 6.2 清理浏览器缓存的 Proxy 地址

打开 `http://localhost:8088`，按 `F12`，在 Console 中执行：

```javascript
localStorage.removeItem('isV5');
localStorage.removeItem('proxyAddr');
location.reload();
```

注意键名和引号必须完整，JavaScript 区分大小写，正确名称是 `localStorage`。

不能使用下面的写法：

```javascript
localStorage.setItem('isV5', false);
```

因为 `localStorage` 保存的是字符串，读取出的 `"false"` 仍然是非空字符串，在 JavaScript 条件判断中依然为真。

## 7. 验证结果

修复后进行以下验证：

1. Dashboard 正常监听 `8088`。
2. NameServer 地址为 `127.0.0.1:9876`。
3. Broker 路由地址为 `172.22.55.173:10911`。
4. Dashboard 客户端显示 `vipChannelEnabled=false`。
5. Dashboard 日志不再出现新的：

```text
send request to <127.0.0.1:8080> failed
```

6. Spring Boot 不再出现 RocketMQ 二进制数据导致的 HTTP method 解析异常。

历史日志不会自动删除，验证时必须根据最新时间戳判断。

Dashboard 仍可能在查看消费者页面时输出以下警告：

```text
No topic route info in name server for the topic: %RETRY%...
the consumer group[...] not online
```

前者通常来自 Broker 中预置或历史遗留的系统消费组，但对应重试 Topic
从未创建；后者表示历史测试消费组当前没有在线客户端。它们与请求误发到
`8080` 的问题无关。

Dashboard 2.0.0 使用 RocketMQ 5.1.0 客户端，而当前 Broker 为 5.3.4。
旧 Dashboard 的历史图表采集任务可能无法读取新版 Broker 的部分
`TOPIC_PUT_NUMS` 和 `GROUP_GET_NUMS` 统计项，因此关闭了
`enableDashBoardCollect`。这不会影响集群、Topic 和消费者管理。

## 8. 排查中的一个干扰项

重复启动 Dashboard 时出现过：

```text
Port 8088 is already in use
```

这是另一个独立问题，只表示已经有 Dashboard 进程监听 `8088`。它与 RocketMQ 请求误发到 Spring Boot 的问题没有直接关系。

## 9. 面试回答模板

在 RocketMQ 迁移过程中，我遇到过一个比较隐蔽的协议串口问题。Spring Boot 的 Tomcat 日志一直报 HTTP method 包含非法字符，但日志内容中出现了 RocketMQ 的 `consumerGroup` 和请求码。

我先根据协议特征判断，这不是普通的非法 HTTP 请求，而是 RocketMQ 二进制协议被发送到了 Spring Boot 的 `8080` 端口。随后依次检查了应用配置、进程监听端口、NameServer 和 Broker 路由，确认它们都是正确的。

继续排查 Dashboard 后，我发现 Dashboard 2.0.0 默认把 `127.0.0.1:8080` 配置成 RocketMQ Proxy。同时，Dashboard 前端还会把 Proxy 地址保存在浏览器 `localStorage`。即使修改服务端配置并重启 Dashboard，浏览器仍会把旧地址传给后端，因此问题持续存在。

最终我使用独立配置文件移除了默认 Proxy 配置，并清除了浏览器中的 `isV5` 和 `proxyAddr`。修复后通过端口监听、Broker 路由和最新日志进行了验证，Dashboard 不再请求 `8080`，Tomcat 的解析异常也消失了。

这个问题让我认识到，排查分布式组件问题不能只看服务端 YAML，还要结合协议内容、网络端口、注册中心路由、第三方组件默认配置以及浏览器持久化状态一起分析。

## 10. 面试追问要点

### 为什么 Tomcat 报的是 HTTP method 非法？

Tomcat 收到 TCP 数据后会按照 HTTP 请求格式解析第一行。RocketMQ Remoting 使用二进制协议，其字节不符合 HTTP method 的 token 规则，所以在 Controller 之前就被 Tomcat 拒绝。

### 为什么修改 Spring Boot 配置没有用？

请求来源是 Dashboard，并不是 Spring Boot 主动访问了错误地址。Spring Boot 的 `8080` 配置本身正确。

### 为什么重启 Dashboard 后问题仍然存在？

浏览器 `localStorage` 独立于 Dashboard 进程生命周期。服务重启不会清除浏览器保存的 `isV5` 和 `proxyAddr`。

### 如何快速判断这是协议串口问题？

重点查看异常请求中的可读字段。如果 HTTP 解析错误中出现 `consumerGroup`、RocketMQ 请求码或序列化字段，说明收到的很可能不是 HTTP，而是其他协议流量。

### 如何避免类似问题？

- 为 HTTP、NameServer、Broker、Proxy 和 Dashboard 明确规划端口。
- 不使用的 Proxy 功能不要保留默认地址。
- 升级第三方组件时检查默认配置变化。
- 排查时同时检查服务端配置、运行参数和浏览器缓存。
- 根据最新时间戳验证，避免把历史日志误认为新错误。
