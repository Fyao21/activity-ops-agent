# Activity Agent Service

## 1. 项目说明

`agent-service` 是 Python FastAPI 服务，负责把自然语言转换成安全 SQL，查询 MySQL，并返回中文分析结果。

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
├── .env
└── README.md
```

## 4. 功能说明

- 提供 `POST /agent/query`
- 请求字段：`question`、`user_id`
- 返回字段：`generated_sql`、`query_result`、`answer`、`success`、`error_message`
- 使用 LangChain `SQLDatabase` 连接 MySQL
- 使用 `ChatOpenAI` 调用 OpenAI 兼容模型接口
- 执行前做 SQL 安全校验
- 如果没有 `LIMIT`，自动补 `LIMIT 100`
- SQL 执行失败时，自动尝试修复一次

## 5. SQL 安全规则

规则实现见：

- [sql_guard.py](E:\Project\activity-agent\agent-service\sql_guard.py)

限制如下：

- 只允许单条 `SELECT`
- 禁止 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE`、`CREATE`
- 禁止多语句执行
- 禁止查询 `sys_user.password`
- 默认自动补 `LIMIT 100`

为了降低敏感数据暴露，模型只会看到以下表：

- `activity`
- `activity_user_record`
- `reward_record`
- `activity_statistics`
- `agent_qa_record`

## 6. 环境配置

模板文件：

- [.env.example](E:\Project\activity-agent\agent-service\.env.example)

本地文件：

- [.env](E:\Project\activity-agent\agent-service\.env)

建议本地配置：

```env
OPENAI_API_KEY=your_api_key_here
OPENAI_BASE_URL=https://api.openai.com/v1
MODEL_NAME=gpt-4o-mini

MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=wt292292
MYSQL_DATABASE=activity_agent
```

## 7. 安装依赖

```bash
pip install -r requirements.txt
```

## 8. 启动方式

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

可访问接口：

- `GET /health`
- `POST /agent/query`
- `GET /docs`

## 9. 本地自测

### 9.1 初始化数据库

在 `agent-service` 目录下执行：

```bash
mysql -uroot -pwt292292 < ../sql/schema.sql
mysql -uroot -pwt292292 < ../sql/init.sql
```

### 9.2 创建虚拟环境

```bash
python -m venv .venv
```

PowerShell：

```powershell
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

### 9.3 配置 `.env`

至少确认以下三项正确：

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `MODEL_NAME`

### 9.4 启动服务

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### 9.5 健康检查

```bash
curl http://localhost:8000/health
```

预期返回：

```json
{"status":"ok"}
```

### 9.6 查询测试

```bash
curl -X POST "http://localhost:8000/agent/query" \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"统计最近7天各活动的参与人数\",\"user_id\":1}"
```

## 10. 建议验证问题

```text
统计最近7天各活动的参与人数
查询双十一拉新活动的奖励发放成功率
对比 APP 和 H5 渠道参与人数
查询奖励发放失败最多的活动
```

## 11. 常见问题

### 11.1 SQL 被拒绝

通常是以下原因：

- 不是 `SELECT`
- 命中危险关键字
- 查询了 `password`
- 多语句执行

### 11.2 查询结果为空

优先检查：

1. 是否执行了 `sql/init.sql`
2. `.env` 中数据库名是否是 `activity_agent`
3. MySQL 账号和密码是否正确
