# 活动运营数据分析 Agent 系统产品文档 / Codex 开发说明

## 1. 项目目标

请开发一个“活动运营数据分析 Agent 系统”。

本项目用于模拟活动运营平台中的数据查询和智能分析场景。运营人员可以通过自然语言提问，例如：

* 统计双十一活动的参与人数
* 查询最近 7 天奖励发放数量
* 分析某个活动的用户转化率
* 对比两个活动的参与效果
* 查询某个渠道带来的活动参与人数

系统需要自动将用户问题转换为 SQL 查询，执行查询后返回结构化数据，并调用大模型生成自然语言分析结论。

该项目用于 Java 后端实习简历展示，要求重点体现：

* SpringBoot 后端开发能力
* MySQL 表设计和 SQL 查询能力
* Redis 缓存能力
* 消息队列异步处理能力
* Python + LangChain Agent 能力
* Text-to-SQL 能力
* SQL 安全控制能力

---

## 2. 技术栈要求

### 2.1 Java 后端服务

使用：

* Java 17
* SpringBoot 3.x
* SpringMVC
* MyBatis / MyBatis-Plus
* MySQL
* Redis
* Redis Stream 作为消息队列
* Maven
* Lombok

Java 后端负责：

* 用户登录
* 活动管理
* 用户参与记录
* 奖励发放记录
* 活动统计数据
* 问答记录保存
* Redis 缓存
* Redis Stream 异步任务
* 调用 Python Agent 服务

### 2.2 Python Agent 服务

使用：

* Python 3.11+
* FastAPI
* LangChain
* langchain-openai
* langchain-community
* SQLAlchemy
* PyMySQL
* Uvicorn

Python 服务负责：

* 接收 Java 后端传来的自然语言问题
* 连接 MySQL 只读数据库
* 获取数据库表结构
* 使用 LangChain 构建 Text-to-SQL Agent
* 生成 SQL
* 校验 SQL
* 执行 SQL 查询
* 调用大模型总结查询结果
* 返回 generated_sql、query_result、answer 给 Java 后端

### 2.3 大模型

使用 OpenAI 兼容接口即可，要求通过环境变量配置：

```env
OPENAI_API_KEY=你的API_KEY
OPENAI_BASE_URL=你的模型服务地址
MODEL_NAME=deepseek-chat 或 qwen-plus 或 gpt-4o-mini
```

如果暂时没有真实大模型，也要保留接口代码，允许后续替换。

---

## 3. 系统整体架构

系统分为两个服务：

```text
前端 / Apifox
   ↓
SpringBoot 后端服务
   ↓ HTTP 调用
Python FastAPI Agent 服务
   ↓
LangChain Text-to-SQL Agent
   ↓
MySQL 数据库
```

### 3.1 Java 后端职责

Java 后端提供统一业务接口，前端只访问 Java 后端，不直接访问 Python 服务。

Java 后端接口示例：

```text
POST /agent/query
```

Java 后端收到用户问题后，调用 Python Agent 服务：

```text
POST http://localhost:8000/agent/query
```

然后将 Python 返回结果保存到问答记录表，并返回给前端。

### 3.2 Python Agent 职责

Python Agent 服务只负责智能查询与分析，不负责用户登录、权限、活动业务写入。

Python 服务接口示例：

```text
POST /agent/query
```

请求参数：

```json
{
  "question": "统计最近7天各活动的参与人数",
  "user_id": 1
}
```

返回参数：

```json
{
  "generated_sql": "SELECT ...",
  "query_result": [...],
  "answer": "最近7天参与人数最高的活动是..."
}
```

---

## 4. 核心业务模块

## 4.1 用户登录模块

### 功能说明

实现简单的用户登录功能，用于区分运营人员和管理员。

### 功能要求

* 用户通过 username 和 password 登录
* 登录成功后生成 token
* token 存入 Redis
* 后续请求通过 token 获取用户信息
* 用户角色分为 ADMIN 和 OPERATOR

### Redis Key 设计

```text
login:token:{token}
```

Value 保存：

```json
{
  "userId": 1,
  "username": "admin",
  "role": "ADMIN"
}
```

---

## 4.2 活动管理模块

### 功能说明

用于维护活动基础信息。

### 功能要求

提供以下接口：

```text
POST /activity/create
GET /activity/list
GET /activity/{id}
PUT /activity/update
```

活动字段包括：

* 活动名称
* 活动类型
* 开始时间
* 结束时间
* 活动状态
* 活动规则描述

### 缓存要求

查询活动详情时，优先查 Redis。

Redis Key：

```text
activity:info:{activityId}
```

缓存未命中时查 MySQL，并写入 Redis。

---

## 4.3 用户参与记录模块

### 功能说明

记录用户参与活动的行为数据。

### 功能要求

提供接口：

```text
POST /activity/participate
```

请求示例：

```json
{
  "activityId": 1,
  "userId": 1001,
  "channel": "APP"
}
```

业务流程：

1. 校验活动是否存在
2. 校验活动是否正在进行
3. 写入用户参与记录
4. 发送 Redis Stream 消息，用于异步更新统计数据

Redis Stream Key：

```text
stream:activity:event
```

消息内容：

```json
{
  "eventType": "PARTICIPATE",
  "activityId": 1,
  "userId": 1001,
  "channel": "APP",
  "eventTime": "2026-06-08 12:00:00"
}
```

---

## 4.4 奖励发放模块

### 功能说明

记录活动奖励发放情况。

### 功能要求

提供接口：

```text
POST /reward/send
```

请求示例：

```json
{
  "activityId": 1,
  "userId": 1001,
  "rewardType": "COUPON",
  "rewardAmount": 10
}
```

业务流程：

1. 校验活动和用户参与记录
2. 创建奖励发放记录，状态为 INIT
3. 发送 Redis Stream 消息
4. 消费者异步处理奖励发放
5. 发放成功后更新状态为 SUCCESS
6. 发放失败后更新状态为 FAIL，并记录失败原因

Redis Stream Key：

```text
stream:reward:event
```

---

## 4.5 活动统计模块

### 功能说明

用于保存和查询活动统计指标。

### 核心指标

* 活动参与人数
* 奖励发放数量
* 奖励发放成功率
* 用户转化率
* 渠道参与人数
* 活动留存率

### 功能要求

提供接口：

```text
GET /statistics/activity
```

请求参数：

```text
activityId
startDate
endDate
```

### Redis 缓存设计

统计数据 Redis Key：

```text
activity:stat:{activityId}:{date}
```

对于高频查询的活动统计数据，优先查 Redis，缓存不存在再查 MySQL。

---

## 4.6 智能问答 Agent 模块

### 功能说明

运营人员输入自然语言问题，系统自动查询数据库并生成分析结论。

### Java 后端接口

```text
POST /agent/query
```

请求参数：

```json
{
  "question": "统计最近7天各活动的参与人数和奖励发放数量"
}
```

返回参数：

```json
{
  "question": "统计最近7天各活动的参与人数和奖励发放数量",
  "generatedSql": "SELECT ...",
  "queryResult": "...",
  "answer": "最近7天..."
}
```

### Java 后端处理流程

1. 校验用户 token
2. 接收自然语言问题
3. 调用 Python FastAPI Agent 服务
4. 获取 generated_sql、query_result、answer
5. 保存问答记录到 agent_qa_record 表
6. 返回结果给前端

### Python Agent 处理流程

1. 接收 question
2. 获取数据库表列表
3. 获取相关表结构
4. 识别用户意图
5. 生成 SQL
6. 校验 SQL
7. 执行 SQL 查询
8. 生成自然语言总结
9. 返回结果给 Java

---

## 5. 数据库设计

请使用 MySQL，数据库名：

```sql
CREATE DATABASE activity_agent DEFAULT CHARACTER SET utf8mb4;
```

---

## 5.1 用户表 sys_user

```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码',
    role VARCHAR(32) NOT NULL COMMENT '角色 ADMIN/OPERATOR',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## 5.2 活动表 activity

```sql
CREATE TABLE activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '活动ID',
    activity_name VARCHAR(128) NOT NULL COMMENT '活动名称',
    activity_type VARCHAR(64) NOT NULL COMMENT '活动类型',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status TINYINT NOT NULL COMMENT '状态 0未开始 1进行中 2已结束',
    rule_desc TEXT COMMENT '活动规则描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_time(status, start_time, end_time)
);
```

---

## 5.3 用户参与记录表 activity_user_record

```sql
CREATE TABLE activity_user_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    channel VARCHAR(64) NOT NULL COMMENT '参与渠道 APP/H5/WEB',
    participate_status TINYINT NOT NULL COMMENT '参与状态 1成功 0失败',
    participate_time DATETIME NOT NULL COMMENT '参与时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activity_time(activity_id, participate_time),
    INDEX idx_user_activity(user_id, activity_id),
    INDEX idx_channel(channel)
);
```

---

## 5.4 奖励发放记录表 reward_record

```sql
CREATE TABLE reward_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '奖励记录ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    reward_type VARCHAR(64) NOT NULL COMMENT '奖励类型 COUPON/POINT/CASH',
    reward_amount DECIMAL(10,2) NOT NULL COMMENT '奖励数量',
    send_status TINYINT NOT NULL COMMENT '发放状态 0初始化 1成功 2失败',
    fail_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    send_time DATETIME DEFAULT NULL COMMENT '发放时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_activity_status(activity_id, send_status),
    INDEX idx_user_activity(user_id, activity_id),
    INDEX idx_send_time(send_time)
);
```

---

## 5.5 活动统计表 activity_statistics

```sql
CREATE TABLE activity_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    participant_count INT DEFAULT 0 COMMENT '参与人数',
    reward_count INT DEFAULT 0 COMMENT '奖励发放数量',
    reward_success_count INT DEFAULT 0 COMMENT '奖励发放成功数量',
    conversion_rate DECIMAL(6,4) DEFAULT 0 COMMENT '转化率',
    retention_rate DECIMAL(6,4) DEFAULT 0 COMMENT '留存率',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_date(activity_id, stat_date)
);
```

---

## 5.6 Agent 问答记录表 agent_qa_record

```sql
CREATE TABLE agent_qa_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question TEXT NOT NULL COMMENT '用户问题',
    generated_sql TEXT COMMENT '生成的SQL',
    query_result TEXT COMMENT 'SQL查询结果',
    answer TEXT COMMENT '最终回答',
    success TINYINT DEFAULT 1 COMMENT '是否成功 1成功 0失败',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time(user_id, create_time)
);
```

---

## 6. Python Agent 服务要求

### 6.1 项目结构

请创建 Python 服务目录：

```text
agent-service/
├── main.py
├── agent.py
├── db.py
├── schemas.py
├── sql_guard.py
├── requirements.txt
├── .env.example
└── README.md
```

### 6.2 FastAPI 接口

接口路径：

```text
POST /agent/query
```

请求模型：

```python
class AgentQueryRequest(BaseModel):
    question: str
    user_id: Optional[int] = None
```

返回模型：

```python
class AgentQueryResponse(BaseModel):
    generated_sql: str
    query_result: Any
    answer: str
    success: bool
    error_message: Optional[str] = None
```

### 6.3 SQL 安全控制

必须实现 sql_guard.py，对模型生成的 SQL 做安全校验。

要求：

* 只允许 SELECT
* 禁止 INSERT
* 禁止 UPDATE
* 禁止 DELETE
* 禁止 DROP
* 禁止 ALTER
* 禁止 TRUNCATE
* 禁止 CREATE
* 禁止多语句执行
* 禁止查询 sys_user.password
* 限制最大返回行数，例如自动追加 LIMIT 100

示例函数：

```python
def validate_sql(sql: str) -> bool:
    pass
```

### 6.4 Agent Prompt 要求

System Prompt 需要包含以下约束：

```text
你是一个活动运营数据分析 Agent。
你只能生成 SELECT 查询语句。
你不能生成 INSERT、UPDATE、DELETE、DROP、ALTER、TRUNCATE 等 SQL。
回答必须基于 SQL 查询结果，不能编造不存在的数据。
执行 SQL 前必须检查表结构。
如果 SQL 执行失败，需要根据错误信息修正 SQL。
如果用户问题和活动数据无关，需要拒绝回答。
返回结果需要包含：生成的 SQL、查询结果摘要、自然语言分析结论。
```

### 6.5 Python 环境变量

`.env.example`：

```env
OPENAI_API_KEY=your_api_key
OPENAI_BASE_URL=https://api.openai.com/v1
MODEL_NAME=gpt-4o-mini

MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=agent_readonly
MYSQL_PASSWORD=123456
MYSQL_DATABASE=activity_agent
```

---

## 7. Java 后端项目要求

### 7.1 项目结构

请创建 SpringBoot 项目：

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
│   ├── application.yml
│   └── mapper
└── pom.xml
```

### 7.2 必须实现的 Controller

```text
AuthController
ActivityController
ParticipateController
RewardController
StatisticsController
AgentController
```

---

## 7.3 Java 调 Python 服务

请创建：

```text
client/PythonAgentClient.java
```

功能：

* 使用 RestTemplate 或 WebClient 调用 Python FastAPI 服务
* 请求地址从 application.yml 读取
* 设置超时时间
* 捕获异常并返回友好错误

application.yml 示例：

```yaml
agent:
  python-url: http://localhost:8000/agent/query
```

---

## 7.4 AgentController

接口：

```text
POST /agent/query
```

功能：

1. 接收用户 question
2. 调用 PythonAgentClient
3. 保存问答记录
4. 返回 answer、generatedSql、queryResult

---

## 8. Redis Stream 异步任务要求

### 8.1 活动参与事件

Stream Key：

```text
stream:activity:event
```

消费者组：

```text
group:activity:stat
```

消费者逻辑：

* 消费用户参与事件
* 更新 activity_statistics.participant_count
* 更新 Redis 统计缓存
* 消费成功后 ack
* 消费失败记录日志，不 ack，允许后续重试

### 8.2 奖励发放事件

Stream Key：

```text
stream:reward:event
```

消费者组：

```text
group:reward:send
```

消费者逻辑：

* 消费奖励发放事件
* 模拟奖励发放
* 更新 reward_record.send_status
* 更新 reward_record.send_time
* 更新 activity_statistics.reward_count 和 reward_success_count
* 消费成功后 ack
* 消费失败记录日志，不 ack，允许后续重试

---

## 9. 接口清单

### 9.1 登录接口

```text
POST /auth/login
```

请求：

```json
{
  "username": "admin",
  "password": "123456"
}
```

返回：

```json
{
  "token": "xxx"
}
```

---

### 9.2 创建活动

```text
POST /activity/create
```

请求：

```json
{
  "activityName": "双十一拉新活动",
  "activityType": "NEW_USER",
  "startTime": "2026-06-01 00:00:00",
  "endTime": "2026-06-30 23:59:59",
  "status": 1,
  "ruleDesc": "新用户参与后发放优惠券"
}
```

---

### 9.3 活动列表

```text
GET /activity/list?page=1&pageSize=10
```

---

### 9.4 活动详情

```text
GET /activity/{id}
```

---

### 9.5 用户参与活动

```text
POST /activity/participate
```

请求：

```json
{
  "activityId": 1,
  "userId": 1001,
  "channel": "APP"
}
```

---

### 9.6 奖励发放

```text
POST /reward/send
```

请求：

```json
{
  "activityId": 1,
  "userId": 1001,
  "rewardType": "COUPON",
  "rewardAmount": 10
}
```

---

### 9.7 活动统计查询

```text
GET /statistics/activity?activityId=1&startDate=2026-06-01&endDate=2026-06-30
```

---

### 9.8 Agent 自然语言查询

```text
POST /agent/query
```

请求：

```json
{
  "question": "统计最近7天各活动的参与人数和奖励发放数量"
}
```

返回：

```json
{
  "question": "统计最近7天各活动的参与人数和奖励发放数量",
  "generatedSql": "SELECT ...",
  "queryResult": "...",
  "answer": "最近7天共有3个活动产生参与数据，其中双十一拉新活动参与人数最高..."
}
```

---

## 10. 初始化数据要求

请提供 `init.sql`，插入以下测试数据：

* 2 个用户：admin、operator
* 5 个活动
* 每个活动 20 条用户参与记录
* 每个活动 10 条奖励发放记录
* 每个活动 3 条统计数据
* 若干问答记录

要求数据能支持以下问题查询：

```text
统计最近7天各活动的参与人数
查询双十一活动奖励发放成功率
对比 APP 和 H5 渠道的参与人数
查询奖励发放失败最多的活动
分析某活动的转化率
```

---

## 11. 统一返回格式

Java 后端统一返回：

```json
{
  "code": 1,
  "message": "success",
  "data": {}
}
```

失败返回：

```json
{
  "code": 0,
  "message": "错误信息",
  "data": null
}
```

---

## 12. 非功能要求

### 12.1 可运行

项目必须能本地运行。

启动顺序：

1. 启动 MySQL
2. 启动 Redis
3. 执行 init.sql
4. 启动 Python FastAPI Agent 服务
5. 启动 SpringBoot 后端服务
6. 使用 Apifox 调用接口测试

### 12.2 可演示

必须能演示以下流程：

1. 登录获取 token
2. 创建活动
3. 用户参与活动
4. 发送奖励
5. Redis Stream 异步更新统计数据
6. 调用自然语言查询接口
7. 返回 SQL 和分析结论
8. 查看问答记录

### 12.3 安全要求

* Agent 查询数据库账号建议使用只读账号
* Python Agent 必须做 SQL 关键字校验
* Java 后端也要保存 generated_sql，方便排查问题
* 禁止 Agent 查询用户密码字段
* Agent 回答必须基于查询结果，不允许编造数据

### 12.4 日志要求

需要打印关键日志：

* 用户登录日志
* 活动参与日志
* Redis Stream 发送消息日志
* Redis Stream 消费消息日志
* Python Agent 生成 SQL 日志
* SQL 校验失败日志
* Agent 查询失败日志

---

## 13. README 要求

请生成完整 README，包含：

1. 项目介绍
2. 技术栈
3. 系统架构
4. 数据库表说明
5. 启动方式
6. 环境变量配置
7. 接口列表
8. Agent 查询示例
9. Redis Stream 异步流程说明
10. 常见问题

---

## 14. Codex 开发优先级

请按照以下优先级开发。

### P0：必须完成

* MySQL 表结构和初始化数据
* SpringBoot 基础项目
* 活动管理
* 用户参与记录
* 奖励发放记录
* 活动统计查询
* Python FastAPI Agent 服务
* Java 调 Python 接口
* Agent 查询记录保存
* SQL 安全校验

### P1：尽量完成

* Redis 缓存活动详情
* Redis 缓存统计数据
* Redis Stream 异步处理参与事件和奖励事件
* 消费失败重试
* 问答历史查询

### P2：可选完成

* LangSmith 追踪配置
* 简单前端页面
* Docker Compose
* RocketMQ 替代 Redis Stream
* 多轮上下文记忆

---

## 15. 验收标准

项目完成后，必须满足以下条件：

1. SpringBoot 服务可以正常启动。
2. Python FastAPI 服务可以正常启动。
3. Java 后端可以通过 HTTP 调用 Python Agent 服务。
4. MySQL 初始化数据完整。
5. Redis 缓存功能可用。
6. Redis Stream 可以发送和消费消息。
7. 调用 `/agent/query` 可以输入自然语言问题。
8. Agent 可以生成 SQL。
9. Agent 可以执行 SQL 并返回查询结果。
10. Agent 可以返回自然语言分析结论。
11. 禁止执行 DELETE、UPDATE、INSERT、DROP 等危险 SQL。
12. 问答记录可以保存到 MySQL。
13. README 中有完整启动说明。

---

## 16. 示例演示问题

请确保系统支持以下自然语言问题：

```text
统计最近7天各活动的参与人数
查询双十一拉新活动的奖励发放成功率
统计 APP 渠道和 H5 渠道分别带来了多少参与人数
查询奖励发放失败次数最多的活动
分析最近一周活动参与人数最高的活动
查询每个活动的奖励发放总金额
对比不同活动的转化率
```

---

## 17. 最终交付内容

请最终生成以下内容：

```text
activity-agent/
├── activity-agent-backend/
│   └── SpringBoot 后端完整代码
├── agent-service/
│   └── Python FastAPI + LangChain Agent 完整代码
├── sql/
│   ├── schema.sql
│   └── init.sql
├── docs/
│   └── api.md
└── README.md
```

要求代码结构清晰，注释适量，能够本地运行，适合作为 Java 后端实习项目展示。
