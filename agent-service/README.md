# Activity Agent Service

## 1. 项目介绍

`agent-service` 是活动运营数据分析系统中的 Python Agent 服务，负责把自然语言问题转换成安全 SQL，查询 MySQL，并输出中文分析结论。

当前实现只生成 Python FastAPI + LangChain 服务，不包含 Java 后端代码。

## 2. 技术栈

- Python 3.11+
- FastAPI
- LangChain
- langchain-openai
- langchain-community
- SQLAlchemy
- PyMySQL
- Uvicorn

## 3. 目录结构

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

## 4. 功能说明

- 暴露 `POST /agent/query` 接口
- 请求体包含 `question` 和 `user_id`
- 返回 `generated_sql`、`query_result`、`answer`、`success`、`error_message`
- 使用 LangChain `SQLDatabase` 连接 MySQL
- 使用 `langchain-openai` 的 `ChatOpenAI` 连接 OpenAI 兼容模型接口
- 执行前进行 SQL 安全校验
- 自动给未带 `LIMIT` 的查询追加 `LIMIT 100`
- 如果首轮 SQL 执行失败，自动基于错误信息修正一次

## 5. SQL 安全策略

`sql_guard.py` 实现以下限制：

- 仅允许单条 `SELECT`
- 禁止 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE`、`CREATE`
- 禁止多语句执行
- 禁止查询 `sys_user.password`
- 默认自动追加 `LIMIT 100`

为了降低敏感字段暴露风险，LangChain 侧只向模型暴露以下表：

- `activity`
- `activity_user_record`
- `reward_record`
- `activity_statistics`
- `agent_qa_record`

## 6. 环境变量

复制 `.env.example` 为 `.env`，按实际环境修改：

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

仓库内已补一个最小本地模板 [`.env`](E:\Project\activity-agent\agent-service\.env)：

```env
OPENAI_API_KEY=your_api_key_here
OPENAI_BASE_URL=https://api.openai.com/v1
MODEL_NAME=gpt-4o-mini

MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=123456
MYSQL_DATABASE=activity_agent
```

说明：

- `OPENAI_API_KEY` 必须替换成你可用的模型服务密钥
- `OPENAI_BASE_URL` 保持 OpenAI 官方地址，或替换成你的兼容网关
- `MYSQL_*` 默认按本地 MySQL 直连配置填写

## 7. 安装依赖

```bash
pip install -r requirements.txt
```

## 8. 启动方式

在 `agent-service` 目录执行：

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

启动后访问：

- `http://localhost:8000/health`
- `http://localhost:8000/docs`

## 9. 本地启动自测步骤

### 9.1 准备数据库

先确保本地 MySQL 已启动，然后执行：

```bash
mysql -uroot -p123456 < ../sql/schema.sql
mysql -uroot -p123456 < ../sql/init.sql
```

如果你的 MySQL 账号或密码不是 `root/123456`，把命令改成你自己的。

### 9.2 安装依赖

建议在 `agent-service` 目录创建虚拟环境：

```bash
python -m venv .venv
```

Windows PowerShell:

```powershell
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

macOS/Linux:

```bash
source .venv/bin/activate
pip install -r requirements.txt
```

### 9.3 检查 `.env`

确认以下三项已经改成真实可用值：

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `MODEL_NAME`

如果你使用兼容网关，例如 DeepSeek、通义或其他 OpenAI-compatible 服务，`base_url` 和 `model_name` 要与服务端实际配置一致。

### 9.4 启动服务

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### 9.5 健康检查

浏览器访问：

- `http://localhost:8000/health`
- `http://localhost:8000/docs`

或者命令行执行：

```bash
curl http://localhost:8000/health
```

期望返回：

```json
{"status":"ok"}
```

### 9.6 接口自测

使用最近 7 天活动参与人数问题做一次最小验证：

```bash
curl -X POST "http://localhost:8000/agent/query" \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"统计最近7天各活动的参与人数\",\"user_id\":1}"
```

Windows PowerShell 也可以用：

```powershell
$body = @{
  question = "统计最近7天各活动的参与人数"
  user_id = 1
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8000/agent/query" `
  -ContentType "application/json" `
  -Body $body
```

### 9.7 建议验证的查询

以下问题都应该能返回 `generated_sql`、`query_result` 和 `answer`：

```text
统计最近7天各活动的参与人数
查询双十一拉新活动的奖励发放成功率
对比 APP 和 H5 渠道参与人数
查询奖励发放失败最多的活动
```
## 10. 接口说明

### POST /agent/query

请求示例：

```json
{
  "question": "统计最近7天各活动的参与人数",
  "user_id": 1
}
```

响应示例：

```json
{
  "generated_sql": "SELECT a.activity_name, COUNT(*) AS participant_count FROM activity_user_record aur JOIN activity a ON aur.activity_id = a.id WHERE aur.participate_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) GROUP BY a.activity_name LIMIT 100;",
  "query_result": [
    {
      "activity_name": "双十一拉新活动",
      "participant_count": 20
    }
  ],
  "answer": "最近7天内双十一拉新活动参与人数为 20。",
  "success": true,
  "error_message": null
}
```

## 11. 常见问题

### 1. 为什么没有查询 `sys_user`？

当前服务目标是活动运营分析，主查询范围集中在活动、参与、奖励、统计、问答记录。为避免敏感信息暴露，默认不把 `sys_user` 暴露给模型。

### 2. 为什么 SQL 会被拒绝？

以下情况会被拦截：

- 不是 `SELECT`
- 包含危险关键字
- 查询 `password`
- 多语句执行

### 3. 为什么自动加 `LIMIT 100`？

这是为了限制一次查询的最大返回量，避免大结果集拖慢模型总结和接口响应。
