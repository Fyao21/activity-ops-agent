# 废弃文件清单

本文记录当前仓库中已标注为废弃的旧活动分析链路文件。这些文件保留用于历史演示、方案对比或回滚参考，不属于“智能课程学习助手 Agent 平台”的核心链路。

核心链路仍以课程管理、课程资料知识库、RAG 问答、Text-to-SQL 学习数据分析、Hybrid Agent、题目答题、错题和学习行为记录为准。

## 后端旧活动分析模块

- `activity-agent-backend/src/main/java/com/example/activityagent/controller/ActivityController.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/controller/ParticipateController.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/controller/RewardController.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/controller/StatisticsController.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/dto/ActivityCreateRequest.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/dto/ActivityUpdateRequest.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/dto/ParticipateRequest.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/dto/RewardSendRequest.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/dto/StatisticsQueryRequest.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/entity/Activity.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/entity/ActivityStatistics.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/entity/ActivityUserRecord.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/entity/RewardRecord.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/mapper/ActivityMapper.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/mapper/ActivityStatisticsMapper.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/mapper/ActivityUserRecordMapper.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/mapper/RewardRecordMapper.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/service/ActivityService.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/service/ActivityStatisticsSyncService.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/service/ParticipateService.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/service/RewardService.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/service/StatisticsService.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/service/impl/ActivityServiceImpl.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/service/impl/ActivityStatisticsSyncServiceImpl.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/service/impl/ParticipateServiceImpl.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/service/impl/RewardServiceImpl.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/service/impl/StatisticsServiceImpl.java`

## 后端旧活动消息链路

- `activity-agent-backend/src/main/java/com/example/activityagent/mq/ActivityEventConsumer.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/mq/ActivityEventMessage.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/mq/RewardEventConsumer.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/mq/RewardEventMessage.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/mq/RedisStreamKeys.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/mq/RedisStreamPublisher.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/config/RedisStreamConfig.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/mq/consumer/AgentTaskConsumer.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/mq/dto/AgentTaskMessage.java`
- `activity-agent-backend/src/main/java/com/example/activityagent/mq/producer/AgentTaskProducer.java`

`RocketMqConstant.java` 没有整体废弃，因为它仍包含课程学习助手核心链路使用的知识库索引、学习行为和答题统计常量；仅其中旧活动任务相关常量已单独标注为废弃。

## 前端旧活动分析演示

- `activity-agent-front/src/App.vue`
- `activity-agent-front/src/router/index.js`
- `activity-agent-front/src/api/activity.js`
- `activity-agent-front/src/views/ActivityManage.vue`
- `activity-agent-front/src/views/Dashboard.vue`
- `activity-agent-front/src/views/Participate.vue`
- `activity-agent-front/src/views/RewardSend.vue`
- `activity-agent-front/src/views/Statistics.vue`

## 旧活动分析文档和样例

- `README.md`
- `activity-agent-backend/docs/rocketmq-migration-guide.md`
- `docs/interview-3min.md`
- `docs/interview-deepdive.md`
- `docs/interview-qa.md`
- `docs/rag-sample-618.md`
- `docs/rag-test-guide.md`
- `docs/agent-query-router-test-guide.md`

## API 文档标注

`docs/apifox-openapi.yaml` 中活动、奖励、活动统计相关接口已标记 `deprecated: true`。课程、题目、答题、知识库、Agent 问答和学习行为接口未标记废弃。
