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

## 14. RAG 测试流程

本节用于验证知识库文档上传、文档索引、知识库问答，以及 `/agent/query` 的 Hybrid 混合问答能力。

测试样例文档位于：

```text
docs/rag-samples/618活动规则.md
docs/rag-samples/优惠券使用FAQ.md
```

如果当前大模型代理不支持 `/embeddings`，例如 DeepSeek，请确认 `agent-service/.env` 中使用本地向量模式：

```env
EMBEDDING_PROVIDER=local
VECTOR_STORE_PATH=./vector_store
```

### 14.1 启动 Python Agent 服务

```powershell
cd E:\Project\activity-agent\agent-service
.\.venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

健康检查：

```powershell
curl.exe http://localhost:8000/health
```

### 14.2 启动 SpringBoot 后端

```powershell
cd E:\Project\activity-agent\activity-agent-backend
mvn spring-boot:run
```

健康检查：

```powershell
curl.exe http://localhost:8080/health
```

本地知识库测试不依赖 RocketMQ，默认配置：

```yaml
activity:
  mq:
    rocketmq:
      enabled: false
```

### 14.3 上传知识文档

上传 618 活动规则：

```powershell
curl.exe -X POST "http://localhost:8080/knowledge/upload" `
  -F "file=@E:\Project\activity-agent\docs\rag-samples\618活动规则.md"
```

上传优惠券 FAQ：

```powershell
curl.exe -X POST "http://localhost:8080/knowledge/upload" `
  -F "file=@E:\Project\activity-agent\docs\rag-samples\优惠券使用FAQ.md"
```

成功后返回中的 `status` 应为 `1`，`chunkCount` 应大于 `0`。

### 14.4 查询知识库文档列表

```powershell
curl.exe "http://localhost:8080/knowledge/list?pageNum=1&pageSize=10"
```

### 14.5 查询知识库

查询新用户奖励规则：

```powershell
$body = @{
  question = "618 活动的新用户奖励规则是什么？"
  topK = 4
} | ConvertTo-Json -Compress

[System.IO.File]::WriteAllText("$PWD\request.json", $body, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "http://localhost:8080/knowledge/query" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```

查询优惠券使用限制：

```powershell
$body = @{
  question = "优惠券是否可以叠加使用？退款后优惠券如何处理？"
  topK = 4
} | ConvertTo-Json -Compress

[System.IO.File]::WriteAllText("$PWD\request.json", $body, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "http://localhost:8080/knowledge/query" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```

### 14.6 使用 `/agent/query` 测试 Hybrid 问题

Hybrid 问题会先检索知识库规则，再生成 SQL 查询数据，最后结合规则和 SQL 查询结果进行分析。

```powershell
$body = @{
  question = "根据 618 活动规则，分析最近 7 天奖励发放是否正常"
  user_id = 1
} | ConvertTo-Json -Compress

[System.IO.File]::WriteAllText("$PWD\request.json", $body, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "http://localhost:8000/agent/query" `
  -H "Content-Type: application/json" `
  --data-binary "@request.json"
```

预期返回中包含：

```json
{
  "routeType": "hybrid",
  "generatedSql": "SELECT ...",
  "retrievedChunks": [],
  "answer": "..."
}
```

如果通过 Java 后端代理调用，则地址为：

```text
http://localhost:8080/agent/query
```

Java 后端会额外包一层统一响应：

```json
{
  "code": 1,
  "message": "success",
  "data": {
    "routeType": "hybrid",
    "generatedSql": "SELECT ...",
    "retrievedChunks": [],
    "answer": "..."
  }
}
```

## 15. RAG 功能说明

### 15.1 功能介绍

系统已补充 RAG 知识库能力，支持上传活动规则、优惠券说明、运营 FAQ、数据指标说明等业务文档。

文档上传后会被切分成多个文本片段，并转换为向量写入 FAISS 向量库。用户后续提问时，Agent 会先从知识库中检索相关文档片段，再结合检索上下文调用大模型生成回答，从而减少规则类问题的幻觉，提高回答可追溯性。

适合放入知识库的文档示例：

- 活动规则说明
- 优惠券使用说明
- 运营 FAQ
- 奖励发放规则
- 指标口径说明
- 风控和异常处理规则

### 15.2 RAG 架构说明

整体流程如下：

```text
用户上传文档
  -> Java 后端保存文件和文档元数据
  -> Java 后端调用 Python /rag/index
  -> Python 读取 txt/md 文档
  -> LangChain 文本切分
  -> Embedding 向量化
  -> FAISS 本地持久化存储
  -> 用户提问
  -> Python 从 FAISS 检索相关片段
  -> 将检索片段拼接到 Prompt
  -> 大模型基于知识库上下文生成回答
```

各组件职责：

| 组件 | 职责 |
| --- | --- |
| SpringBoot 后端 | 接收上传文件、保存 MySQL 元数据、调用 Python RAG 服务、提供 Java 侧知识库接口。 |
| MySQL | 保存 `knowledge_document` 和 `knowledge_chunk` 元数据。 |
| Python FastAPI Agent | 文档读取、文本切分、向量化、FAISS 存储、RAG 问答、Hybrid 分析。 |
| FAISS | 本地向量库，默认保存到 `agent-service/vector_store`。 |
| 大模型 | 基于 SQL 查询结果或知识库上下文生成最终回答。 |

### 15.3 Text-to-SQL + RAG 混合 Agent

`POST /agent/query` 是统一 Agent 查询入口，会根据问题内容自动路由：

| 问题类型 | 路由 | 示例 |
| --- | --- | --- |
| 数据统计类问题 | SQL | `统计最近 7 天各活动参与人数` |
| 规则说明类问题 | RAG | `618 活动规则是什么` |
| 规则 + 数据分析类问题 | Hybrid | `根据 618 活动规则，分析最近 7 天奖励发放是否正常` |

路由规则目前使用关键词实现：

- SQL 类关键词：`统计`、`人数`、`数量`、`成功率`、`转化率`、`留存`、`对比`、`排名`、`最近`、`今天`、`昨天`、`本周`、`奖励发放量`
- RAG 类关键词：`规则`、`说明`、`限制`、`条件`、`如何`、`为什么`、`介绍`、`文档`、`FAQ`、`使用方式`
- Hybrid 类关键词：`根据`、`结合`、`分析`、`是否正常`、`原因`、`对照规则`

Hybrid 流程：

```text
用户问题
  -> RAG 检索活动规则上下文
  -> Text-to-SQL 查询业务数据
  -> 将规则上下文和 SQL 查询结果交给大模型
  -> 输出规则摘要、数据摘要、分析结论和建议
```

### 15.4 环境变量说明

Python Agent 服务使用 `agent-service/.env` 配置环境变量。

| 环境变量 | 说明 |
| --- | --- |
| `OPENAI_API_KEY` | OpenAI 兼容接口 API Key。不要提交到 Git。 |
| `OPENAI_BASE_URL` | OpenAI 兼容接口地址，例如 OpenAI、DeepSeek 或其他代理地址。 |
| `MODEL_NAME` | 对话模型名称，用于 SQL 生成、RAG 回答和 Hybrid 总结。 |
| `EMBEDDING_MODEL` | Embedding 模型名称，默认可使用 `text-embedding-3-small`。 |
| `VECTOR_STORE_PATH` | FAISS 向量库存储路径，默认 `./vector_store`。 |
| `MYSQL_HOST` | MySQL 主机地址。 |
| `MYSQL_PORT` | MySQL 端口。 |
| `MYSQL_USER` | MySQL 用户名，建议使用只读账号。 |
| `MYSQL_PASSWORD` | MySQL 密码。不要提交到 Git。 |
| `MYSQL_DATABASE` | MySQL 数据库名，默认 `activity_agent`。 |

如果当前模型代理不支持 `/embeddings`，可以使用本地向量模式：

```env
EMBEDDING_PROVIDER=local
```

### 15.5 接口说明

#### 15.5.1 上传知识文档

```text
POST /knowledge/upload
```

服务地址：

```text
http://localhost:8080/knowledge/upload
```

请求类型：

```text
multipart/form-data
```

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `file` | File | 上传的 txt 或 md 文件。 |

处理流程：

1. Java 后端保存文件到 `activity-agent-backend/uploads/knowledge`。
2. Java 后端写入 `knowledge_document`，初始状态为 `0`。
3. Java 后端调用 Python `/rag/index`。
4. Python 切分文档、向量化并写入 FAISS。
5. Java 后端更新文档状态和切片数量。

#### 15.5.2 查询知识库文档列表

```text
GET /knowledge/list
```

服务地址：

```text
http://localhost:8080/knowledge/list?pageNum=1&pageSize=10
```

返回内容包括：

- 文档 ID
- 文件名
- 文件类型
- 处理状态
- 切片数量
- 创建时间

#### 15.5.3 知识库问答

```text
POST /knowledge/query
```

服务地址：

```text
http://localhost:8080/knowledge/query
```

请求示例：

```json
{
  "question": "618 活动的新用户奖励规则是什么？",
  "topK": 4
}
```

返回内容包括：

- `answer`：最终回答
- `retrievedChunks`：检索命中的知识库片段
- `sources`：引用来源文件

#### 15.5.4 统一 Agent 查询

```text
POST /agent/query
```

Python 服务地址：

```text
http://localhost:8000/agent/query
```

Java 代理地址：

```text
http://localhost:8080/agent/query
```

请求示例：

```json
{
  "question": "根据 618 活动规则，分析最近 7 天奖励发放是否正常",
  "user_id": 1
}
```

返回内容包括：

- `routeType`：`sql`、`rag` 或 `hybrid`
- `generatedSql` / `generated_sql`：生成的 SQL
- `query_result`：SQL 查询结果
- `retrievedChunks`：RAG 检索片段
- `answer`：最终回答

### 15.6 注意事项

- `.env` 不允许提交到 Git，里面包含 API Key、数据库密码等敏感信息。
- `agent-service/vector_store` 是本地 FAISS 向量库目录，可以选择不提交到 Git。
- 数据库账号建议使用只读账号，降低 Text-to-SQL 执行风险。
- SQL Guard 仍然保留，并且只允许执行单条 `SELECT`。
- SQL Guard 会拦截 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE`、`CREATE` 等危险语句。
- 不允许查询 `password` 字段。
- DeepSeek 等部分模型代理可能不支持 embedding 接口，测试时可使用 `EMBEDDING_PROVIDER=local`。
- 修改 Python 代码后需要重启 `uvicorn`，或者使用 `--reload` 启动。
