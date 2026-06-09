package com.example.activityagent.mq;

import com.example.activityagent.entity.RewardRecord;
import com.example.activityagent.mapper.RewardRecordMapper;
import com.example.activityagent.service.ActivityStatisticsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class RewardEventConsumer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringRedisTemplate stringRedisTemplate;
    private final RewardRecordMapper rewardRecordMapper;
    private final ActivityStatisticsSyncService activityStatisticsSyncService;

    /**
     * Finalize reward send asynchronously.
     * Success updates reward_record to SUCCESS and rewrites daily reward statistics.
     * Infrastructure failures are logged and left pending for later retry.
     */
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            Long rewardRecordId = Long.valueOf(message.getValue().get("rewardRecordId"));
            Long activityId = Long.valueOf(message.getValue().get("activityId"));
            LocalDateTime eventTime = LocalDateTime.parse(message.getValue().get("eventTime"), DATE_TIME_FORMATTER);

            RewardRecord rewardRecord = rewardRecordMapper.selectById(rewardRecordId);
            if (rewardRecord == null) {
                throw new IllegalStateException("Reward record not found: " + rewardRecordId);
            }

            // If the record was already finalized by a previous attempt, just ack and stop.
            if (rewardRecord.getSendStatus() != null && rewardRecord.getSendStatus() != 0) {
                activityStatisticsSyncService.syncRewardStatistics(
                    activityId,
                    rewardRecord.getSendTime() != null ? rewardRecord.getSendTime() : eventTime
                );
                stringRedisTemplate.opsForStream().acknowledge(
                    RedisStreamKeys.REWARD_EVENT_STREAM,
                    RedisStreamKeys.REWARD_SEND_GROUP,
                    message.getId()
                );
                log.info("Reward event already processed, messageId={}, rewardRecordId={}", message.getId(), rewardRecordId);
                return;
            }

            rewardRecord.setSendStatus(1);
            rewardRecord.setFailReason(null);
            rewardRecord.setSendTime(eventTime);
            rewardRecordMapper.updateById(rewardRecord);

            activityStatisticsSyncService.syncRewardStatistics(activityId, eventTime);
            stringRedisTemplate.opsForStream().acknowledge(
                RedisStreamKeys.REWARD_EVENT_STREAM,
                RedisStreamKeys.REWARD_SEND_GROUP,
                message.getId()
            );
            log.info("Consumed reward event successfully, messageId={}, payload={}", message.getId(), message.getValue());
        } catch (Exception ex) {
            log.error("Consume reward event failed, messageId={}, payload={}", message.getId(), message.getValue(), ex);
            throw ex;
        }
    }
}
