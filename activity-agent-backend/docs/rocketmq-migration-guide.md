# RocketMQ 迁移与验证说明

## 1. 背景

`activity-agent-backend` 原来使用 Redis Stream 作为消息队列，主要承担两类异步任务：

- 用户参与活动后，异步刷新 `activity_statistics.participant_count`
- 奖励发放后，异步更新 `reward_record.send_status`，并刷新 `activity_statistics.reward_count`、`reward_success_count`

当前默认实现已经切换为 RocketMQ。

Redis 仍然保留在项目里，继续用于：

- 缓存
- 登录态
- 其他后续能力

Redis Stream 相关类没有立即删除，只是标记为“已被 RocketMQ 替代”，方便回滚和对照。

## 2. 当前 RocketMQ 代码结构

主代码：

- `com.example.activityagent.mq.constant.RocketMqConstant`
- `com.example.activityagent.mq.dto.AgentTaskMessage`
- `com.example.activityagent.mq.producer.AgentTaskProducer`
- `com.example.activityagent.mq.consumer.AgentTaskConsumer`

测试代码：

- `src/test/java/com/example/activityagent/mq/RocketMqSendTest.java`
- `src/test/resources/application-test.yml`

## 3. RocketMQ 配置

主配置：

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: agent-task-producer-group
```

测试配置：

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: agent-test-producer-group
```

Redis Stream 默认已关闭：

```yaml
activity:
  mq:
    redis-stream:
      enabled: false
```

如果需要临时回滚验证旧 Redis Stream 逻辑，可以显式改为：

```yaml
activity:
  mq:
    redis-stream:
      enabled: true
```

## 4. 消费成功、重试、死信队列

### 4.1 消费成功

RocketMQ 下不需要手动 ACK。  
`AgentTaskConsumer.onMessage(...)` 正常执行结束，就表示消息消费成功。

### 4.2 消费失败重试

如果消费过程中抛出异常：

- RocketMQ 会把这次消费视为失败
- Broker 会根据 RocketMQ 的重试策略再次投递消息

当前项目里：

- 参与事件消费失败，会抛异常，等待 RocketMQ 重试
- 奖励事件消费失败，也会抛异常，等待 RocketMQ 重试

### 4.3 死信队列

如果消息重试多次仍然失败：

- RocketMQ 会把消息投递到死信队列（DLQ）

这替代了 Redis Stream 里原先“pending list 长时间未确认”的处理思路。

当前项目还没有额外实现：

- DLQ 消息巡检
- DLQ 消息补偿消费
- 重试告警

所以当前状态是“最小改动可跑通”，不是最终生产级治理方案。

## 5. 幂等说明

### 5.1 参与事件

参与统计消费走的是“重算统计”逻辑：

- 根据 `activityId + eventTime` 重新统计当天参与人数
- 重复消费只会重写最终值

所以天然具备幂等性。

### 5.2 奖励事件

奖励发放消费依赖 `reward_record.send_status` 状态机：

- `0`：待处理
- 非 `0`：已经处理过

如果 RocketMQ 重复投递同一条奖励消息：

- 已完成的记录不会重复更新发奖状态
- 只会重新刷新统计

这就是当前版本的幂等保障。

## 6. 本地启动 RocketMQ

Windows 下常见命令：

### 6.1 启动 NameServer

```powershell
mqnamesrv.cmd
```

### 6.2 启动 Broker

```powershell
mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true

java -jar rocketmq-dashboard-2.0.0.jar --spring.config.location=file:E:/Project/activity-agent/rocketmq-dashboard.yml
```

Dashboard 使用独立配置文件，避免其内置的 `127.0.0.1:8080`
Proxy 地址与 Spring Boot 端口冲突。

## 7. 本地手动验证步骤

### 7.1 启动基础设施

先启动：

1. MySQL
2. Redis
3. RocketMQ NameServer
4. RocketMQ Broker

### 7.2 启动后端

```powershell
cd E:\Project\activity-agent\activity-agent-backend
mvn spring-boot:run
```

### 7.3 验证“参与活动”异步链路

先调用：

```text
POST /activity/participate
```

预期：

1. 业务接口同步返回成功
2. RocketMQ 收到参与消息
3. `AgentTaskConsumer` 处理参与消息
4. `activity_statistics.participant_count` 被刷新

### 7.4 验证“奖励发放”异步链路

调用：

```text
POST /reward/send
```

预期：

1. 接口同步写入一条 `reward_record`
2. 初始 `send_status = 0`
3. RocketMQ 收到奖励消息
4. `AgentTaskConsumer` 更新 `send_status = 1`
5. `activity_statistics.reward_count` 和 `reward_success_count` 被刷新

## 8. 测试运行方式

### 8.1 编译测试代码

```powershell
cd E:\Project\activity-agent\activity-agent-backend
mvn -q -DskipTests test-compile
```

### 8.2 运行 RocketMQ 测试

```powershell
mvn -Dtest=RocketMqSendTest test
```

当前测试会：

- 启动 Spring 测试容器
- 发送一条 RocketMQ 消息
- 打印 `SendResult`
- 等待几秒，方便本地观察输出

## 9. 旧 Redis Stream 类保留说明

这些类当前已被 RocketMQ 替代，但仍保留：

- `RedisStreamConfig`
- `RedisStreamKeys`
- `RedisStreamPublisher`
- `ActivityEventConsumer`
- `RewardEventConsumer`
- `ActivityEventMessage`
- `RewardEventMessage`

这些类暂不删除，用于：

1. 对照 Redis Stream 与 RocketMQ 的实现差异
2. 演示消息队列方案演进过程
3. 必要时进行回滚验证

Redis Stream 默认关闭，避免与 RocketMQ 同时消费同一业务事件。

## 10. 当前剩余风险

### 10.1 重试次数和死信治理未细化

当前只依赖 RocketMQ 默认重试行为，没有额外配置：

- 最大重试次数
- 失败告警
- DLQ 补偿机制

### 10.2 奖励消费幂等依赖业务状态字段

现在用的是 `reward_record.send_status`。

如果后续发奖链路更复杂，建议补充：

- 更明确的消息唯一键
- 消费去重记录
- 状态流转审计

### 10.3 旧 Redis Stream 代码仍在仓库中

默认不会启用，但源码还在。  
当前选择保留，用作历史方案、技术对比和回滚参考。
