# Activity Agent Front

## 1. 项目说明

`activity-agent-front` 是活动运营数据分析 Agent 系统的前端演示项目。

这个前端只负责页面展示和接口调用：
- 不直接调用 Python Agent
- 只通过 SpringBoot 后端完成业务操作和 Agent 查询
- 适合作为 Java 后端实习项目的演示界面

## 2. 技术栈

- Vue 3
- Vite
- Element Plus
- Axios
- Vue Router

## 3. 项目结构

```text
activity-agent-front/
├── package.json
├── vite.config.js
├── index.html
├── .gitignore
├── README.md
└── src/
    ├── main.js
    ├── App.vue
    ├── router/index.js
    ├── api/request.js
    ├── api/activity.js
    ├── api/agent.js
    └── views/
        ├── Dashboard.vue
        ├── ActivityManage.vue
        ├── Participate.vue
        ├── RewardSend.vue
        ├── Statistics.vue
        └── AgentQuery.vue
```

## 4. 页面说明

### 4.1 首页 Dashboard

展示内容：
- 活动总数
- 参与人数
- 奖励发放数量
- Agent 查询次数
- 最近活动列表

### 4.2 活动管理 ActivityManage

支持：
- 创建活动
- 分页查看活动列表
- 查看活动详情

调用接口：
- `POST /activity/create`
- `GET /activity/list`
- `GET /activity/{id}`

### 4.3 用户参与 Participate

支持输入：
- `activityId`
- `userId`
- `channel`

调用接口：
- `POST /activity/participate`

### 4.4 奖励发放 RewardSend

支持输入：
- `activityId`
- `userId`
- `rewardType`
- `rewardAmount`

调用接口：
- `POST /reward/send`

### 4.5 统计查询 Statistics

支持输入：
- `activityId`
- `startDate`
- `endDate`

调用接口：
- `GET /statistics/activity`

### 4.6 Agent 查询 AgentQuery

支持：
- 输入自然语言问题
- 调用后端 `/agent/query`
- 展示 `question`、`generatedSql`、`queryResult`、`answer`

示例问题：
- 统计最近7天各活动的参与人数
- 查询618活动奖励发放成功率
- 对比 APP 和 H5 渠道参与人数
- 查询奖励发放失败最多的活动

## 5. 接口代理说明

前端通过 Vite 代理访问后端：

```text
/api -> http://localhost:8080
```

配置文件：
- [vite.config.js](E:\Project\activity-agent\activity-agent-front\vite.config.js)

## 6. 启动前准备

### 6.1 启动 SpringBoot 后端

后端地址：

```text
http://localhost:8080
```

建议先确认：

```bash
curl http://localhost:8080/health
```

### 6.2 启动 Python Agent

前端虽然不直接调用 Python Agent，但后端的 `/agent/query` 会转调 Python 服务。  
如果要演示 Agent 查询页面，Python Agent 也要先启动。

## 7. 安装依赖

```powershell
cd E:\Project\activity-agent\activity-agent-front
npm install
```

## 8. 启动项目

```powershell
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

## 9. 演示建议顺序

1. 首页：看统计卡片和最近活动列表
2. 活动管理：创建新活动并查看详情
3. 用户参与：提交参与请求
4. 奖励发放：提交发奖请求
5. 统计查询：查看统计表变化
6. Agent 查询：输入自然语言并展示 SQL 与结论

## 10. 常见问题

### 10.1 页面打开但接口报错

优先检查：

1. SpringBoot 后端是否已启动
2. 后端地址是否为 `http://localhost:8080`
3. Vite 代理是否生效

### 10.2 Agent 查询失败

优先检查：

1. SpringBoot 后端是否正常
2. Python Agent 是否已启动
3. 后端 `application.yml` 中的 Python 服务地址是否正确

### 10.3 用户参与失败

优先检查：

1. 活动是否存在
2. 活动状态是否为 `1`
3. 当前时间是否在活动时间范围内
4. 用户是否已经参与过

### 10.4 奖励发放失败

优先检查：

1. 用户是否已经先参与活动
2. 活动 ID 和用户 ID 是否对应
3. RocketMQ 的 `agent-task-consumer-group` 是否正常消费
4. 如果正在回滚验证旧方案，再检查 Redis Stream 异步消费

## 11. 补充说明

- 当前前端是演示项目，不包含复杂权限系统
- 当前没有引入 Vuex / Pinia，逻辑尽量保持简单
- 页面风格偏简洁，适合 Java 后端实习项目演示
- Agent 查询次数当前使用浏览器 `localStorage` 做本地演示统计
