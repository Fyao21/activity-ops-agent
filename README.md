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
- Redis Stream 异步处理参与事件和奖励事件

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

### 6.3 启动 Python Agent 服务

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

### 6.4 启动后端服务

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
6. 查看 Redis Stream 消息
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

## 9. Redis Stream 使用说明

### 9.1 Stream

- `stream:activity:event`
- `stream:reward:event`

### 9.2 消费者组

- `group:activity:stat`
- `group:reward:send`

### 9.3 消费逻辑

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

### 9.4 Redis CLI 命令

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

## 10. 常用验证 SQL

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

## 11. 相关文档

- 产品文档：[docs/product.md](E:\Project\activity-agent\docs\product.md)
- 后端文档：[activity-agent-backend/README.md](E:\Project\activity-agent\activity-agent-backend\README.md)
- Agent 文档：[agent-service/README.md](E:\Project\activity-agent\agent-service\README.md)

## 12. 说明

- 当前后端配置是写死的，不是环境变量占位符方式
- 奖励发放是通过 Redis Stream 异步完成的
- 统计更新是按“活动 + 日期”重算，避免消息重试导致重复累计
- 当前仓库不包含前端页面
