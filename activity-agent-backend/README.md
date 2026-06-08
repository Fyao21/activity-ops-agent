# Activity Agent Backend

## 1. 项目说明

`activity-agent-backend` 是活动运营数据分析系统的 SpringBoot 后端服务，负责：

- 用户登录
- 活动管理
- 用户参与记录
- 奖励发放记录
- 活动统计查询
- 调用 Python Agent 服务
- 保存问答记录到 `agent_qa_record`

当前实现范围以 P0 为主，不包含前端页面和复杂异步消费链路。

## 2. 技术栈

- Java 17
- SpringBoot 3
- MyBatis-Plus
- MySQL
- Redis
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
│   ├── mq
│   └── client
├── src/main/resources
│   └── application.yml
├── pom.xml
└── README.md
```

## 4. 已实现接口

- `POST /auth/login`
- `POST /activity/create`
- `GET /activity/list`
- `GET /activity/{id}`
- `PUT /activity/update`
- `POST /activity/participate`
- `POST /reward/send`
- `GET /statistics/activity`
- `POST /agent/query`

## 5. 配置说明

配置文件位置：

- [application.yml](E:\Project\activity-agent\activity-agent-backend\src\main\resources\application.yml)

默认配置：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/activity_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: wt292292
  data:
    redis:
      host: localhost
      port: 6379

agent:
  python-url: http://localhost:8000/agent/query
```

## 6. 启动说明

### 6.1 准备 MySQL

确保本地 MySQL 已启动，然后在项目根目录执行：

```powershell
mysql -uroot -pwt292292 < sql/schema.sql
mysql -uroot -pwt292292 < sql/init.sql
```

如果你的 MySQL 账号密码不同，替换命令里的 `root` 和 `wt292292`。

### 6.2 准备 Redis

确保本地 Redis 已启动，默认地址：

```text
localhost:6379
```

### 6.3 启动 Python Agent 服务

后端依赖 Python Agent 接口 `http://localhost:8000/agent/query`。

先进入 `agent-service`：

```powershell
cd E:\Project\activity-agent\agent-service
```

确认 `.env` 已配置真实可用的模型密钥，然后启动：

```powershell
.\.venv\Scripts\Activate.ps1
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

健康检查：

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8000/health"
```

### 6.4 启动 SpringBoot 后端

进入后端目录：

```powershell
cd E:\Project\activity-agent\activity-agent-backend
```

编译：

```powershell
mvn clean compile
```

启动：

```powershell
mvn spring-boot:run
```

启动成功后访问：

- `http://localhost:8080`

## 7. 自测顺序

建议按下面顺序验证：

1. 登录
2. 创建活动
3. 查询活动列表
4. 查询活动详情
5. 更新活动
6. 用户参与活动
7. 发放奖励
8. 查询活动统计
9. 调用 Agent 查询

## 8. 接口自测示例

### 8.1 登录

`curl`：

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\"}"
```

PowerShell：

```powershell
$body = @{
  username = "admin"
  password = "123456"
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/auth/login" `
  -ContentType "application/json" `
  -Body $body
```

期望返回：

```json
{
  "code": 1,
  "message": "success",
  "data": {
    "token": "xxx"
  }
}
```

### 8.2 创建活动

```bash
curl -X POST "http://localhost:8080/activity/create" \
  -H "Content-Type: application/json" \
  -d "{\"activityName\":\"618加码活动\",\"activityType\":\"NEW_USER\",\"startTime\":\"2026-06-01T00:00:00\",\"endTime\":\"2026-06-30T23:59:59\",\"status\":1,\"ruleDesc\":\"新用户参与后发放优惠券\"}"
```

### 8.3 查询活动列表

```bash
curl "http://localhost:8080/activity/list?page=1&pageSize=10"
```

### 8.4 查询活动详情

示例查询活动 `1`：

```bash
curl "http://localhost:8080/activity/1"
```

### 8.5 更新活动

```bash
curl -X PUT "http://localhost:8080/activity/update" \
  -H "Content-Type: application/json" \
  -d "{\"id\":1,\"ruleDesc\":\"新用户参与后发放优惠券和积分\"}"
```

### 8.6 用户参与活动

为了避免和初始化数据冲突，建议使用一个新的 `userId`：

```bash
curl -X POST "http://localhost:8080/activity/participate" \
  -H "Content-Type: application/json" \
  -d "{\"activityId\":1,\"userId\":90001,\"channel\":\"APP\"}"
```

说明：

- 当前 P0 实现会校验活动状态和时间窗口
- 同一用户重复参与同一活动会被拒绝

### 8.7 发放奖励

先参与，再发奖：

```bash
curl -X POST "http://localhost:8080/reward/send" \
  -H "Content-Type: application/json" \
  -d "{\"activityId\":1,\"userId\":90001,\"rewardType\":\"COUPON\",\"rewardAmount\":10}"
```

### 8.8 查询活动统计

```bash
curl "http://localhost:8080/statistics/activity?activityId=1&startDate=2026-06-01&endDate=2026-06-30"
```

### 8.9 调用 Agent 查询

这个接口会调用 Python 服务，并把返回结果保存到 `agent_qa_record`。

`curl`：

```bash
curl -X POST "http://localhost:8080/agent/query" \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"统计最近7天各活动的参与人数\",\"user_id\":1}"
```

PowerShell：

```powershell
$body = @{
  question = "统计最近7天各活动的参与人数"
  user_id = 1
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/agent/query" `
  -ContentType "application/json" `
  -Body $body
```

如果你更希望后端请求字段统一使用驼峰，也可以这样发，当前后端已兼容：

```json
{
  "question": "统计最近7天各活动的参与人数",
  "userId": 1
}
```

期望返回结构：

```json
{
  "code": 1,
  "message": "success",
  "data": {
    "question": "统计最近7天各活动的参与人数",
    "generatedSql": "SELECT ...",
    "queryResult": [],
    "answer": "最近7天...",
    "success": true,
    "errorMessage": null
  }
}
```

## 9. 推荐验证的问题

完成初始化 SQL 后，建议优先验证以下问题：

```text
统计最近7天各活动的参与人数
查询双十一拉新活动的奖励发放成功率
对比 APP 和 H5 渠道参与人数
查询奖励发放失败最多的活动
```

## 10. 常见问题

### 10.1 `/agent/query` 调用失败

优先检查：

1. Python Agent 是否已启动
2. `application.yml` 中的 `agent.python-url` 是否可访问
3. `agent-service/.env` 中的模型配置是否正确

### 10.2 登录成功但后续接口未校验 token

当前是 P0 实现，只完成了登录和 Redis token 写入，还没有加统一鉴权拦截器。

### 10.3 参与活动失败

优先检查：

1. 活动是否存在
2. 活动 `status` 是否为 `1`
3. 当前时间是否在活动开始和结束时间之间
4. `userId` 是否已经参与过该活动

### 10.4 奖励发放失败

当前 P0 实现要求用户必须先存在成功的参与记录，否则会直接拒绝发奖。
