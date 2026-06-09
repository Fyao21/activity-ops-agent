# Activity Agent Backend

## 1. 项目说明

`activity-agent-backend` 是活动运营数据分析平台的 Spring Boot 后端服务，负责：

- 用户登录（BCrypt 密码校验 + Token）
- 活动管理（创建/查询/更新）
- 用户参与活动（含去重、权限校验）
- 奖励发放（异步处理、失败重试）
- 活动统计查询（按活动+日期重算）
- 调用 Python Agent 服务（Text-to-SQL）
- 保存问答记录
- Redis Stream 异步处理参与/奖励事件

## 2. 技术栈

- Java 17
- Spring Boot 3.3.2
- MyBatis-Plus
- MySQL
- Redis / Redis Stream
- RestTemplate
- Maven

## 3. 目录结构

```text
activity-agent-backend/
├── src/main/java/com/example/activityagent
│   ├── controller      # REST API
│   ├── service         # 业务接口
│   ├── service/impl    # 业务实现
│   ├── mapper          # MyBatis-Plus Mapper
│   ├── entity          # 数据库实体
│   ├── dto             # 请求 DTO（含校验）
│   ├── vo              # 响应 VO
│   ├── config          # 配置（Redis、Auth、WebMvc）
│   ├── common          # 公共类（Result、ErrorCode、枚举、异常）
│   ├── client          # Python Agent HTTP 客户端
│   └── mq              # Redis Stream 消息队列
├── src/main/resources
│   └── application.yml
├── pom.xml
└── README.md
```

## 4. 环境配置

所有敏感信息通过环境变量注入，参见根目录 README.md 第 5 节。

## 5. 已实现接口

- `GET /health`
- `POST /auth/login`
- `POST /activity/create` — 需 ADMIN 角色
- `GET /activity/list` — 分页返回
- `GET /activity/{id}` — 带缓存
- `PUT /activity/update` — 需 ADMIN 角色
- `POST /activity/participate` — 需 ADMIN/OPERATOR 角色
- `POST /reward/send` — 需 ADMIN/OPERATOR 角色
- `GET /statistics/activity`
- `POST /agent/query` — 需 ADMIN/OPERATOR 角色

## 6. 启动说明

### 6.1 初始化 MySQL

在项目根目录执行（使用你的环境变量）：

```bash
mysql -h $MYSQL_HOST -u $MYSQL_USER -p$MYSQL_PASSWORD < sql/schema.sql
mysql -h $MYSQL_HOST -u $MYSQL_USER -p$MYSQL_PASSWORD < sql/init.sql
```

### 6.2 启动 Redis

确保 Redis 可通过环境变量 `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` 访问。

### 6.3 启动 Python Agent 服务

```bash
cd agent-service
cp .env.example .env
# 编辑 .env 填写真实配置
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### 6.4 启动后端

```bash
cd activity-agent-backend
# 设置 MYSQL_USER, MYSQL_PASSWORD, REDIS_PASSWORD 等环境变量
mvn clean compile
mvn spring-boot:run
```

## 7. 接口示例

### 7.1 登录

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<your_password>"}'
```

返回 token 后，后续请求需带 Header：
```
Authorization: Bearer <token>
```

### 7.2 创建活动

```bash
curl -X POST "http://localhost:8080/activity/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"activityName":"促销活动","activityType":"NEW_USER","startTime":"2026-06-01T00:00:00","endTime":"2026-06-30T23:59:59","status":1,"ruleDesc":"规则描述"}'
```

### 7.3 用户参与活动

```bash
curl -X POST "http://localhost:8080/activity/participate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"activityId":1,"userId":91001,"channel":"APP"}'
```

### 7.4 发放奖励

```bash
curl -X POST "http://localhost:8080/reward/send" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"activityId":1,"userId":91001,"rewardType":"COUPON","rewardAmount":10}'
```

### 7.5 查询活动统计

```bash
curl "http://localhost:8080/statistics/activity?activityId=1&startDate=2026-06-01&endDate=2026-06-30" \
  -H "Authorization: Bearer <token>"
```

### 7.6 Agent 查询

```bash
curl -X POST "http://localhost:8080/agent/query" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"question":"统计最近7天各活动的参与人数","user_id":1}'
```

## 8. Redis Stream 异步流程

### 8.1 Stream 与消费者组

- 参与事件：`stream:activity:event` → `group:activity:stat`
- 奖励事件：`stream:reward:event` → `group:reward:send`
- 死信队列：`stream:activity:event:dlq` / `stream:reward:event:dlq`

### 8.2 消费逻辑

参与事件消费者：监听事件 → 重算当天 participant_count → ack；失败时记录重试次数，超过 3 次移入 DLQ

奖励事件消费者：监听事件 → 模拟发放（~80% 成功率）→ 更新 send_status → 重算统计 → ack；失败处理同上

### 8.3 Redis CLI 排查

```bash
# 查看 Stream
redis-cli XRANGE stream:activity:event - +
redis-cli XRANGE stream:reward:event - +

# 查看消费者组
redis-cli XINFO GROUPS stream:activity:event
redis-cli XINFO GROUPS stream:reward:event

# 查看 pending
redis-cli XPENDING stream:activity:event group:activity:stat
redis-cli XPENDING stream:reward:event group:reward:send

# 查看 DLQ
redis-cli XLEN stream:activity:event:dlq
redis-cli XLEN stream:reward:event:dlq
```

## 9. 重要说明

- 密码使用 BCrypt 哈希存储，不保存明文
- 所有业务接口通过 AuthInterceptor 统一校验 token
- 统计更新按"活动 + 日期"重算，避免消息重试重复累计
- 参与记录有数据库唯一约束 `(activity_id, user_id)` 防并发重复
- 奖励记录有唯一约束 `(activity_id, user_id, reward_type)` 防重复发放
- 消费失败消息保留 pending，60 秒后自动 reclaim 重试
- 超过 3 次重试的消息自动移入 DLQ
