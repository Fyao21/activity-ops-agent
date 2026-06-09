# 活动运营数据分析平台

基于 Spring Boot + Redis Stream + FastAPI + LangChain 实现自然语言转 SQL 查询、异步统计更新与问答记录审计。

## 1. 项目简介

本项目包含两个服务：

- `activity-agent-backend`：Spring Boot 后端服务
- `agent-service`：Python FastAPI + LangChain Text-to-SQL 服务

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
- Redis / Redis Stream

### 3.2 Agent 服务

- Python 3.11+
- FastAPI
- LangChain
- OpenAI 兼容接口
- SQLAlchemy + PyMySQL

## 4. 核心数据表

- `sys_user`
- `activity`
- `activity_user_record`
- `reward_record`
- `activity_statistics`
- `agent_qa_record`

## 5. 环境配置

所有敏感配置（数据库密码、Redis 密码等）必须通过环境变量注入，禁止硬编码到配置文件中。

### 5.1 后端环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `MYSQL_HOST` | MySQL 地址 | localhost |
| `MYSQL_PORT` | MySQL 端口 | 3306 |
| `MYSQL_DATABASE` | 数据库名 | activity_agent |
| `MYSQL_USER` | MySQL 用户名 | 无默认值，必须设置 |
| `MYSQL_PASSWORD` | MySQL 密码 | 无默认值，必须设置 |
| `REDIS_HOST` | Redis 地址 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `REDIS_PASSWORD` | Redis 密码 | 无密码 |
| `REDIS_DATABASE` | Redis 库 | 0 |
| `SERVER_PORT` | 后端端口 | 8080 |
| `AGENT_PYTHON_URL` | Agent 服务地址 | http://localhost:8000 |

### 5.2 Agent 服务环境变量

参见 `agent-service/.env.example`，核心变量：

- `OPENAI_API_KEY`：LLM API Key
- `OPENAI_BASE_URL`：LLM API 地址
- `MODEL_NAME`：模型名称
- `MYSQL_HOST/PORT/USER/PASSWORD/DATABASE`：数据库连接

## 6. 快速启动

### 6.1 初始化数据库

确保已设置 MySQL 环境变量，然后执行：

```bash
mysql -h $MYSQL_HOST -u $MYSQL_USER -p$MYSQL_PASSWORD < sql/schema.sql
mysql -h $MYSQL_HOST -u $MYSQL_USER -p$MYSQL_PASSWORD < sql/init.sql
```

### 6.2 启动 Redis

确保 Redis 可通过配置的环境变量访问。

### 6.3 启动 Python Agent 服务

```bash
cd agent-service
cp .env.example .env
# 编辑 .env 填写真实配置
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

健康检查：

```bash
curl http://localhost:8000/health
```

### 6.4 启动后端服务

```bash
cd activity-agent-backend
# 设置环境变量后启动
mvn clean compile
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/health
```

## 7. 后端接口示例

### 7.1 登录

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<your_password>"}'
```

响应返回 token，后续请求需在 Header 中携带：
```
Authorization: Bearer <token>
```

### 7.2 用户参与活动

```bash
curl -X POST "http://localhost:8080/activity/participate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"activityId":1,"userId":92001,"channel":"APP"}'
```

### 7.3 发放奖励

```bash
curl -X POST "http://localhost:8080/reward/send" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"activityId":1,"userId":92001,"rewardType":"COUPON","rewardAmount":10}'
```

### 7.4 调用 Agent 查询

```bash
curl -X POST "http://localhost:8080/agent/query" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"question":"统计最近7天各活动的参与人数","user_id":1}'
```

## 8. Redis Stream 使用说明

### 8.1 Stream

- `stream:activity:event`
- `stream:reward:event`

### 8.2 消费者组

- `group:activity:stat`
- `group:reward:send`

### 8.3 消费逻辑

参与事件消费者：读取事件流 → 重算当天 participant_count → ack，失败保留 pending
奖励事件消费者：读取事件流 → 更新 send_status → 重算 reward 统计 → ack，失败保留 pending

### 8.4 Redis CLI 命令

```bash
# 查看 Stream
redis-cli XRANGE stream:activity:event - +
redis-cli XRANGE stream:reward:event - +

# 查看消费者组
redis-cli XINFO GROUPS stream:activity:event
redis-cli XINFO GROUPS stream:reward:event

# 查看 pending 消息
redis-cli XPENDING stream:activity:event group:activity:stat
redis-cli XPENDING stream:reward:event group:reward:send
```

## 9. 相关文档

- 产品文档：[docs/product.md](docs/product.md)
- 后端文档：[activity-agent-backend/README.md](activity-agent-backend/README.md)
- Agent 文档：[agent-service/README.md](agent-service/README.md)

## 10. 说明

- 后端配置使用环境变量注入，不包含任何硬编码密码
- 奖励发放通过 Redis Stream 异步完成
- 统计更新按"活动 + 日期"重算，避免消息重试导致重复累计
- 登录使用 BCrypt 密码哈希，token 通过拦截器统一校验
