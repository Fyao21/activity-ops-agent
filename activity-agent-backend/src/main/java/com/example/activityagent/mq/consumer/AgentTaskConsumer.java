package com.example.activityagent.mq.consumer;

import com.example.activityagent.entity.RewardRecord;
import com.example.activityagent.mapper.RewardRecordMapper;
import com.example.activityagent.mq.constant.RocketMqConstant;
import com.example.activityagent.mq.dto.AgentTaskMessage;
import com.example.activityagent.service.ActivityStatisticsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = RocketMqConstant.AGENT_TASK_TOPIC,
    consumerGroup = RocketMqConstant.AGENT_TASK_CONSUMER_GROUP,
    selectorExpression = RocketMqConstant.AGENT_TASK_SELECTOR
)
public class AgentTaskConsumer implements RocketMQListener<AgentTaskMessage> {

    private final RewardRecordMapper rewardRecordMapper;
    private final ActivityStatisticsSyncService activityStatisticsSyncService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(AgentTaskMessage message) {
        log.info("Received RocketMQ agent task message: {}", message);
        if (RocketMqConstant.AGENT_TASK_TAG_REWARD.equalsIgnoreCase(message.getEventType())) {
            handleRewardTask(message);
            return;
        }
        handleParticipateTask(message);
    }

    /**
     * Participant statistics are recomputed from the source table, so duplicate
     * delivery rewrites the same final number and is naturally idempotent.
     */
    private void handleParticipateTask(AgentTaskMessage message) {
        activityStatisticsSyncService.syncParticipantStatistics(message.getActivityId(), message.getEventTime());
        log.info("Participate task processed successfully, taskId={}, activityId={}", message.getTaskId(), message.getActivityId());
    }

    /**
     * Reward messages use reward_record.send_status as the idempotent state
     * machine. Once a record leaves INIT(0), repeated delivery will only refresh
     * derived statistics and then return.
     */
    private void handleRewardTask(AgentTaskMessage message) {
        RewardRecord rewardRecord = rewardRecordMapper.selectById(message.getRewardRecordId());
        if (rewardRecord == null) {
            throw new IllegalStateException("Reward record not found: " + message.getRewardRecordId());
        }

        if (rewardRecord.getSendStatus() != null && rewardRecord.getSendStatus() != 0) {
            activityStatisticsSyncService.syncRewardStatistics(
                message.getActivityId(),
                rewardRecord.getSendTime() != null ? rewardRecord.getSendTime() : message.getEventTime()
            );
            log.info("Reward task already finalized, taskId={}, rewardRecordId={}", message.getTaskId(), message.getRewardRecordId());
            return;
        }

        rewardRecord.setSendStatus(1);
        rewardRecord.setFailReason(null);
        rewardRecord.setSendTime(message.getEventTime());
        rewardRecordMapper.updateById(rewardRecord);

        activityStatisticsSyncService.syncRewardStatistics(message.getActivityId(), message.getEventTime());
        log.info("Reward task processed successfully, taskId={}, rewardRecordId={}", message.getTaskId(), message.getRewardRecordId());
    }
}
