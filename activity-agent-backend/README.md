# Activity Agent Backend

## 1. 项目说明

`activity-agent-backend` 是活动运营数据分析系统的 Spring Boot 后端服务，负责：

- 用户登录
- 活动管理
- 用户参与活动
- 奖励发放
- 活动统计查询
- 调用 Python Agent 服务
- 保存问答记录
- 通过 RocketMQ 异步处理参与事件和奖励事件
- 保留 Redis Stream 旧实现，便于对照和回滚验证

## 2. 技术栈

- Java 17
- Spring Boot 3.3.2
- MyBatis-Plus
- MySQL
- Redis
- RocketMQ
- Redis Stream
- RestTemplate
- Maven

## 3. 目录结构

```text
activity-agent-backend/
├── src/main/java/com/example/activityagent
│   ├── controller
│   ├── service
│   ├── service/impl
│   ├── mapper
│   ├── entity
│   ├── dto
│   ├── vo
│   ├── config
│   ├── common
│   ├── client
│   └── mq
├── src/main/resources
│   └── application.yml
├── pom.xml
└── README.md
```

## 4. 当前配置

配置文件：

- [application.yml](E:\Project\activity-agent\activity-agent-backend\src\main\resources\application.yml)

当前默认值：

```yaml
server:
  port: 8080

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/activity_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: wt292292
  data:
    redis:
      host: 192.168.100.128
      port: 6379
      password: 1234
      database: 0

agent:
  python-url: http://localhost:8000/agent/query

rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: agent-task-producer-group

activity:
  mq:
    redis-stream:
      enabled: false
```

## 5. 已实现接口

- `GET /`
- `GET /health`
- `POST /auth/login`
- `POST /activity/create`
- `GET /activity/list`
- `GET /activity/{id}`
- `PUT /activity/update`
- `POST /activity/participate`
- `POST /reward/send`
- `GET /statistics/activity`
- `POST /agent/query`

## 6. 启动说明

### 6.1 初始化 MySQL

在项目根目录执行：

```powershell
mysql -uroot -pwt292292 < sql/schema.sql
mysql -uroot -pwt292292 < sql/init.sql
```

### 6.2 启动 Redis

确保 Redis 可以通过当前配置访问：

```text
host: 192.168.100.128
port: 6379
password: 1234
database: 0
```

### 6.3 启动 RocketMQ

启动 NameServer：

```powershell
mqnamesrv.cmd
```

启动 Broker：

```powershell
mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true
```

可选启动 Dashboard：

```powershell
java -jar rocketmq-dashboard-2.0.0.jar --spring.config.location=file:E:/Project/activity-agent/rocketmq-dashboard.yml
```

### 6.4 启动 Python Agent 服务

```powershell
cd E:\Project\activity-agent\agent-service
.\.venv\Scripts\Activate.ps1
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

健康检查：

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8000/health"
```

### 6.5 启动后端

```powershell
cd E:\Project\activity-agent\activity-agent-backend
mvn clean compile
mvn spring-boot:run
```

健康检查：

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/health"
```

## 7. 接口自测示例

### 7.1 登录

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\"}"
```

### 7.2 创建活动

```bash
curl -X POST "http://localhost:8080/activity/create" \
  -H "Content-Type: application/json" \
  -d "{\"activityName\":\"618加码活动\",\"activityType\":\"NEW_USER\",\"startTime\":\"2026-06-01T00:00:00\",\"endTime\":\"2026-06-30T23:59:59\",\"status\":1,\"ruleDesc\":\"新用户参与后发放优惠券\"}"
```

### 7.3 用户参与活动

```bash
curl -X POST "http://localhost:8080/activity/participate" \
  -H "Content-Type: application/json" \
  -d "{\"activityId\":1,\"userId\":91001,\"channel\":\"APP\"}"
```

### 7.4 发放奖励

```bash
curl -X POST "http://localhost:8080/reward/send" \
  -H "Content-Type: application/json" \
  -d "{\"activityId\":1,\"userId\":91001,\"rewardType\":\"COUPON\",\"rewardAmount\":10}"
```

### 7.5 查询活动统计

```bash
curl "http://localhost:8080/statistics/activity?activityId=1&startDate=2026-06-01&endDate=2026-06-30"
```

### 7.6 调用 Agent 查询

```bash
curl -X POST "http://localhost:8080/agent/query" \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"统计最近7天各活动的参与人数\",\"user_id\":1}"
```

## 8. RocketMQ 异步流程

当前默认消息队列实现为 RocketMQ。

### 8.1 Topic、Tag 与消费组

| 配置项 | 值 |
| --- | --- |
| Topic | `agent-task-topic` |
| 参与事件 Tag | `PARTICIPATE` |
| 奖励事件 Tag | `REWARD` |
| Producer Group | `agent-task-producer-group` |
| Consumer Group | `agent-task-consumer-group` |
| Tag 过滤表达式 | `PARTICIPATE || REWARD` |

### 8.2 代码结构

- `mq.constant.RocketMqConstant`：Topic、Tag 和消费组常量
- `mq.dto.AgentTaskMessage`：统一任务消息
- `mq.producer.AgentTaskProducer`：同步发送消息
- `mq.consumer.AgentTaskConsumer`：消费并分发参与、奖励任务

### 8.3 参与事件消费逻辑

- 参与记录写库后发送 `agent-task-topic:PARTICIPATE`
- 消费者按“活动 + 日期”重算 `participant_count`
- 源表重算具备幂等性，重复投递不会重复累加

### 8.4 奖励事件消费逻辑

- 奖励记录先以 `send_status = 0` 写库
- 发送 `agent-task-topic:REWARD`
- 消费者将状态更新为成功并记录发送时间
- 重算 `reward_count` 和 `reward_success_count`
- 重复投递时根据 `send_status` 避免重复处理

### 8.5 重试与死信

- `onMessage` 正常结束表示消费成功，无需手动 ACK
- 消费过程中抛出异常时，RocketMQ 会重新投递
- 超过最大重试次数后，消息进入 `%DLQ%agent-task-consumer-group`

## 9. RocketMQ 联调自测

### 9.1 查看 Topic 和路由

```powershell
mqadmin.cmd topicList -n 127.0.0.1:9876
mqadmin.cmd topicRoute -n 127.0.0.1:9876 -t agent-task-topic
```

### 9.2 查看消费进度

```powershell
mqadmin.cmd consumerProgress -n 127.0.0.1:9876 -g agent-task-consumer-group
```

### 9.3 查看消息统计

```powershell
mqadmin.cmd statsAll -n 127.0.0.1:9876
```

### 9.4 完整验证步骤

1. 调用 `POST /activity/participate`
2. 确认生产者发送 `PARTICIPATE` 消息
3. 确认消费者刷新 `activity_statistics.participant_count`
4. 调用 `POST /reward/send`
5. 确认生产者发送 `REWARD` 消息
6. 确认 `reward_record.send_status` 由 `0` 更新为 `1`
7. 确认奖励统计被刷新

详细说明：

- [docs/rocketmq-migration-guide.md](E:\Project\activity-agent\activity-agent-backend\docs\rocketmq-migration-guide.md)

## 10. Redis Stream 异步流程（保留方案）

Redis Stream 是最初实现，源码仍保留，但默认通过以下开关关闭：

```yaml
activity:
  mq:
    redis-stream:
      enabled: false
```

需要回滚验证时可以临时设置为 `true`。不要与 RocketMQ 消费链路同时处理同一业务事件。

### 10.1 Stream

- 参与事件：`stream:activity:event`
- 奖励事件：`stream:reward:event`

### 10.2 消费者组

- 参与统计组：`group:activity:stat`
- 奖励发放组：`group:reward:send`

### 10.3 参与事件消费逻辑

- 监听 `stream:activity:event`
- 收到消息后，按“活动 + 日期”重算 `participant_count`
- 成功后 `ack`
- 失败只记日志，不 `ack`

### 10.4 奖励事件消费逻辑

- 监听 `stream:reward:event`
- 收到消息后，把 `reward_record.send_status` 从 `0` 更新为 `1`
- 更新 `reward_record.send_time`
- 按“活动 + 日期”重算 `reward_count`
- 按“活动 + 日期”重算 `reward_success_count`
- 成功后 `ack`
- 失败只记日志，不 `ack`

## 11. Redis Stream 联调自测

### 11.1 查看 stream 内容

```bash
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XRANGE stream:activity:event - +
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XRANGE stream:reward:event - +
```

### 11.2 查看消费者组

```bash
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XINFO GROUPS stream:activity:event
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XINFO GROUPS stream:reward:event
```

### 11.3 查看 pending 消息

```bash
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XPENDING stream:activity:event group:activity:stat
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XPENDING stream:reward:event group:reward:send
```

### 11.4 完整验证步骤

步骤 1：发送参与请求

```bash
curl -X POST "http://localhost:8080/activity/participate" \
  -H "Content-Type: application/json" \
  -d "{\"activityId\":1,\"userId\":91002,\"channel\":\"APP\"}"
```

步骤 2：查看参与 stream

```bash
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XRANGE stream:activity:event - +
```

步骤 3：查看统计表

```sql
SELECT *
FROM activity_statistics
WHERE activity_id = 1
ORDER BY stat_date DESC;
```

步骤 4：发送奖励请求

```bash
curl -X POST "http://localhost:8080/reward/send" \
  -H "Content-Type: application/json" \
  -d "{\"activityId\":1,\"userId\":91002,\"rewardType\":\"COUPON\",\"rewardAmount\":10}"
```

步骤 5：查看奖励记录状态

```sql
SELECT id, activity_id, user_id, send_status, send_time
FROM reward_record
WHERE user_id = 91002
ORDER BY id DESC;
```

步骤 6：查看奖励 stream 和 pending

```bash
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XRANGE stream:reward:event - +
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XPENDING stream:reward:event group:reward:send
```

## 12. 重要说明

- `POST /reward/send` 会先写一条 `send_status = 0` 的记录，再由 RocketMQ 消费者异步更新为成功
- Redis Stream 旧链路仍保留，但默认关闭
- 统计更新不是简单 `+1`，而是按天重算，避免消息重试导致重复累计
- RocketMQ 消费失败通过抛出异常触发 Broker 重试
- Redis Stream 方案中消费失败不 `ack`，消息会留在 pending list

## 13. 常见问题

### 13.1 `/agent/query` 调用失败

优先检查：

1. Python Agent 是否已启动
2. `application.yml` 中的 `agent.python-url` 是否可访问
3. `agent-service/.env` 的模型配置是否正确

### 13.2 RocketMQ 消费者没有工作

优先检查：

1. NameServer 是否监听 `9876`
2. Broker 是否监听 `10911`
3. `application.yml` 中 `rocketmq.name-server` 是否正确
4. 启动日志中是否注册 `agent-task-consumer-group`
5. `consumerProgress` 是否能查询到消费进度

### 13.3 Redis Stream 消费者没有工作

优先检查：

1. Redis 地址和密码是否正确
2. `XINFO GROUPS` 是否能看到消费者组
3. 启动日志里是否有 `Redis Stream consumers started`
4. `activity.mq.redis-stream.enabled` 是否为 `true`

### 13.4 参与活动失败

优先检查：

1. 活动是否存在
2. 活动 `status` 是否为 `1`
3. 当前时间是否在活动时间范围内
4. 用户是否已经参与过该活动
