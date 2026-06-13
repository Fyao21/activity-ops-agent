# Activity Ops Agent

## 项目结构

```
activity-ops-agent-main/
├── activity-agent-backend/   # Spring Boot 后端 (Java 17, 端口 8080)
│   ├── src/
│   └── Dockerfile
├── agent-service/            # FastAPI AI 代理 (Python, 端口 8000)
│   ├── main.py
│   ├── requirements.txt
│   └── Dockerfile
├── frontend/                 # React + Vite 前端 (端口 5173)
│   ├── src/
│   └── package.json
└── docker-compose.yml        # backend + agent-service（不含前端运行、不含数据库）
```

## 启动规则（重要）

**前端不进 Docker，永远本地启动**；MySQL / Redis 是本机 Docker 里**独立的常驻容器**（`mysql8`、`redis7`），不属于本项目的 compose，不要在 docker-compose.yml 里重复定义。

```bash
# 1. 基础设施（机器上已存在的独立容器，没在跑就 start）
docker start mysql8 redis7

# 2. 后端 + AI 代理（本项目 compose，连接宿主机的 mysql/redis）
docker compose up -d backend agent-service

# 3. 前端（单独开终端，本地直跑，自带热更新）
cd frontend
npm run dev
# 访问 http://localhost:5173，Vite proxy 把 /api 转发到 localhost:8080
```

改了后端 / agent-service 代码后需要重建镜像才生效：

```bash
docker compose build backend && docker compose up -d backend
```

⚠️ **不要随手用 `--no-cache`**：反复无缓存构建曾在本机堆出 3.3GB 构建缓存。怀疑缓存脏了再用，用完可以 `docker builder prune -f` 清理。

### 本地开发启动（完全不依赖 Docker 的备选）

```bash
# 后端（中文路径下必须用 java -jar，mvn spring-boot:run 会报错）
cd activity-agent-backend
mvn clean package -DskipTests
set MYSQL_USER=agent_user && set MYSQL_PASSWORD=Agent123456 && java -jar target/activity-agent-backend-1.0.0.jar

# AI 代理
cd agent-service
pip install -r requirements.txt
python main.py
```

## 端口汇总

| 服务 | 端口 | 运行方式 |
|------|------|----------|
| Vite 前端 | 5173 | 本地 `npm run dev` |
| Spring Boot 后端 | 8080 | Docker（ao-backend）或本地 jar |
| FastAPI 代理 | 8000 | Docker（ao-agent）或本地 python |
| MySQL | 3306 | 本机独立容器 `mysql8` |
| Redis | 6379 | 本机独立容器 `redis7` |

## 环境变量（与 docker-compose.yml 一致）

| 变量 | 值 / 默认 | 说明 |
|------|-----------|------|
| `MYSQL_HOST` | Docker 内 `host.docker.internal`，本地 `localhost` | MySQL 主机 |
| `MYSQL_DATABASE` | `activity_agent` | 数据库名 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `agent_user` / `Agent123456` | 数据库账号 |
| `REDIS_HOST` / `REDIS_PORT` | 同上主机逻辑 / `6379` | Redis |
| `OPENAI_API_KEY` | —（必填，.env） | LLM API 密钥（DeepSeek 兼容 OpenAI 协议） |
| `OPENAI_BASE_URL` | `https://api.deepseek.com/v1` | LLM API 地址 |
| `MODEL_NAME` | `deepseek-chat` | 模型名 |
| `AGENT_PYTHON_URL` | `http://agent-service:8000` | 后端调用 AI 代理的地址 |

## 关键设计

- 后端通过 `agent.python-base-url` 配置调用 AI 代理
- 前端 Vite proxy 将 `/api` 重写到后端 8080
- **Tailwind v4 只通过 `frontend/vite.config.js` 的 `css.postcss` 配置加载**（没有独立 postcss.config 文件）——删掉这段配置所有布局类会全部失效、页面塌掉
- 设计系统工具类：`yuu-input`、`yuu-select`、`yuu-label`、`yuu-result-block`、`yuu-card`、`yuu-btn-primary`、`yuu-btn-ghost`、`yuu-icon-chip`、`yuu-alert`、`yuu-seg-btn`、`yuu-list-row`
- 主题令牌（亮/暗双套，定义在 `frontend/src/index.css`）：`--yuu-bg`、`--yuu-panel`、`--yuu-border`、`--yuu-text`、`--yuu-muted`、`--yuu-accent`，玻璃配方 `--glass-highlight`、`--glass-cyan-glow`、`--glass-body-a/b`、`--glass-edge`、`--glass-blur`、`--glass-saturate`
- 页面内禁止写死颜色 rgba 内联样式，统一走 CSS 变量或上述工具类（否则暗色模式会出突兀亮块）
