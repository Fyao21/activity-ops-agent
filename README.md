# 活动运营数据分析 Agent 项目

## 1. 项目简介

本项目是一个“活动运营数据分析 Agent 系统”，由两个服务组成：

- `activity-agent-backend`：Spring Boot 后端服务
- `agent-service`：Python FastAPI + LangChain Text-to-SQL 服务

系统主要功能：

- 用户登录
- 活动管理
- 用户参与活动
- 奖励发放记录
- 活动统计查询
- 自然语言查询活动数据
- RocketMQ 异步处理参与事件和奖励事件
- 保留 Redis Stream 旧实现，便于对照和回滚验证

## 2. 项目结构

```text
activity-agent/
├── activity-agent-backend/
├── agent-service/
├── sql/
│   ├── schema.sql
│   └── init.sql
├── docs/
│   └── product.md
└── README.md
```

## 3. 技术栈

### 3.1 后端

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL
- Redis
- RocketMQ
- Redis Stream

### 3.2 Agent 服务

- Python 3.11+
- FastAPI
- LangChain
- OpenAI 兼容接口
- SQLAlchemy
- PyMySQL

## 4. 核心数据表

- `sys_user`
- `activity`
- `activity_user_record`
- `reward_record`
- `activity_statistics`
- `agent_qa_record`

建表脚本：

- [sql/schema.sql](E:\Project\activity-agent\sql\schema.sql)

初始化数据：

- [sql/init.sql](E:\Project\activity-agent\sql\init.sql)

## 5. 当前本地配置

### 5.1 MySQL

```text
host: localhost
port: 3306
database: activity_agent
username: root
password: wt292292
```

### 5.2 Redis

```text
host: 192.168.100.128
port: 6379
password: 1234
database: 0
```

### 5.3 Python Agent 地址

```text
http://localhost:8000/agent/query
```

### 5.4 RocketMQ

```text
NameServer: 127.0.0.1:9876
Broker: 10911
Dashboard: 8088
Topic: agent-task-topic
Producer Group: agent-task-producer-group
Consumer Group: agent-task-consumer-group
```

## 6. 快速启动

### 6.1 初始化数据库

在项目根目录执行：

```powershell
mysql -uroot -pwt292292 < sql/schema.sql
mysql -uroot -pwt292292 < sql/init.sql
```

### 6.2 启动 Redis

确保 Redis 可以用当前配置访问：

```text
192.168.100.128:6379
password: 1234
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

Dashboard 地址：

```text
http://localhost:8088
```

### 6.4 启动 Python Agent 服务

```powershell
cd E:\Project\activity-agent\agent-service
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

健康检查：

```bash
curl http://localhost:8000/health
```

### 6.5 启动后端服务

```powershell
cd E:\Project\activity-agent\activity-agent-backend
mvn clean compile
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/health
```

## 7. 建议演示流程

1. 登录
2. 创建活动
3. 查询活动列表和详情
4. 用户参与活动
5. 发放奖励
6. 在 RocketMQ Dashboard 中查看 `agent-task-topic`
7. 查看 `activity_statistics` 是否更新
8. 调用 `/agent/query`
9. 查看 `agent_qa_record`

## 8. 后端接口示例

### 8.1 登录

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\"}"
```

### 8.2 用户参与活动

```bash
curl -X POST "http://localhost:8080/activity/participate" \
  -H "Content-Type: application/json" \
  -d "{\"activityId\":1,\"userId\":92001,\"channel\":\"APP\"}"
```

### 8.3 发放奖励

```bash
curl -X POST "http://localhost:8080/reward/send" \
  -H "Content-Type: application/json" \
  -d "{\"activityId\":1,\"userId\":92001,\"rewardType\":\"COUPON\",\"rewardAmount\":10}"
```

### 8.4 调用 Agent 查询

```bash
curl -X POST "http://localhost:8080/agent/query" \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"统计最近7天各活动的参与人数\",\"user_id\":1}"
```

## 9. RocketMQ 使用说明

当前默认使用 RocketMQ，参与事件和奖励事件共用一个 Topic，通过 Tag 区分业务类型。

### 9.1 Topic 与 Tag

| 类型 | 配置 |
| --- | --- |
| Topic | `agent-task-topic` |
| 参与事件 Tag | `PARTICIPATE` |
| 奖励事件 Tag | `REWARD` |
| Producer Group | `agent-task-producer-group` |
| Consumer Group | `agent-task-consumer-group` |

### 9.2 生产流程

参与活动：

1. 同步写入 `activity_user_record`
2. 向 `agent-task-topic:PARTICIPATE` 发送消息
3. 消费者按“活动 + 日期”重算参与人数

奖励发放：

1. 同步写入 `reward_record`，初始 `send_status = 0`
2. 向 `agent-task-topic:REWARD` 发送消息
3. 消费者更新奖励状态并重算奖励统计

### 9.3 重试与幂等

- 消费方法正常结束表示消费成功，无需手动 ACK
- 消费抛出异常时由 RocketMQ 重新投递
- 多次重试失败后进入死信队列
- 参与统计采用源表重算，重复消费不会重复累加
- 奖励任务使用 `reward_record.send_status` 判断是否已处理

### 9.4 常用验证命令

查看 Topic：

```powershell
mqadmin.cmd topicList -n 127.0.0.1:9876
```

查看 Topic 路由：

```powershell
mqadmin.cmd topicRoute -n 127.0.0.1:9876 -t agent-task-topic
```

查看消费进度：

```powershell
mqadmin.cmd consumerProgress -n 127.0.0.1:9876 -g agent-task-consumer-group
```

查看整体统计：

```powershell
mqadmin.cmd statsAll -n 127.0.0.1:9876
```

详细迁移与验证步骤见：

- [activity-agent-backend/docs/rocketmq-migration-guide.md](E:\Project\activity-agent\activity-agent-backend\docs\rocketmq-migration-guide.md)

## 10. Redis Stream 使用说明（保留方案）

Redis Stream 是项目最初的消息队列实现，相关代码仍然保留，但当前默认关闭：

```yaml
activity:
  mq:
    redis-stream:
      enabled: false
```

如需对照或回滚验证，可将 `enabled` 临时改为 `true`。不要在同一环境中同时启用两套消费者，否则同一业务事件可能被重复处理。

### 10.1 Stream

- `stream:activity:event`
- `stream:reward:event`

### 10.2 消费者组

- `group:activity:stat`
- `group:reward:send`

### 10.3 消费逻辑

参与事件消费者：

- 读取 `stream:activity:event`
- 重新计算当天的 `participant_count`
- 成功后 `ack`
- 失败只记录日志，不 `ack`

奖励事件消费者：

- 读取 `stream:reward:event`
- 更新 `reward_record.send_status`
- 重新计算 `reward_count` 和 `reward_success_count`
- 成功后 `ack`
- 失败只记录日志，不 `ack`

### 10.4 Redis CLI 命令

查看参与事件：

```bash
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XRANGE stream:activity:event - +
```

查看奖励事件：

```bash
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XRANGE stream:reward:event - +
```

查看参与事件消费者组：

```bash
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XINFO GROUPS stream:activity:event
```

查看奖励事件消费者组：

```bash
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XINFO GROUPS stream:reward:event
```

查看参与事件 pending：

```bash
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XPENDING stream:activity:event group:activity:stat
```

查看奖励事件 pending：

```bash
redis-cli -h 192.168.100.128 -p 6379 -a 1234 XPENDING stream:reward:event group:reward:send
```

## 11. 常用验证 SQL

查看最新统计：

```sql
SELECT *
FROM activity_statistics
ORDER BY stat_date DESC, activity_id ASC;
```

查看最新奖励记录：

```sql
SELECT id, activity_id, user_id, send_status, send_time
FROM reward_record
ORDER BY id DESC;
```

查看问答记录：

```sql
SELECT id, user_id, question, generated_sql, success, create_time
FROM agent_qa_record
ORDER BY id DESC;
```

## 12. 相关文档

- 产品文档：[docs/product.md](E:\Project\activity-agent\docs\product.md)
- 后端文档：[activity-agent-backend/README.md](E:\Project\activity-agent\activity-agent-backend\README.md)
- RocketMQ 迁移文档：[activity-agent-backend/docs/rocketmq-migration-guide.md](E:\Project\activity-agent\activity-agent-backend\docs\rocketmq-migration-guide.md)
- RocketMQ Dashboard 故障复盘：[docs/rocketmq-dashboard-port-conflict-incident.md](E:\Project\activity-agent\docs\rocketmq-dashboard-port-conflict-incident.md)
- Agent 文档：[agent-service/README.md](E:\Project\activity-agent\agent-service\README.md)

## 13. 说明

- 当前后端配置是写死的，不是环境变量占位符方式
- 当前默认通过 RocketMQ 异步处理参与事件和奖励事件
- Redis Stream 旧代码仍保留，但默认关闭
- 统计更新是按“活动 + 日期”重算，避免消息重试导致重复累计
- 当前仓库不包含前端页面
