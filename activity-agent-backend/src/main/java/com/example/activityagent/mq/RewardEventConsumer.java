package com.example.activityagent.mq;

import com.example.activityagent.entity.RewardRecord;
import com.example.activityagent.mapper.RewardRecordMapper;
import com.example.activityagent.service.ActivityStatisticsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
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
     * Simulates external reward channel: ~80% success, ~20% failure.
     * On infrastructure failure: retry up to MAX_RETRY_COUNT, then move to DLQ.
     *
     * Note: @Transactional is not used here because the Redis Stream listener
     * invokes onMessage via method reference, which bypasses the Spring AOP proxy.
     * Transactional boundaries are handled inside the called service methods
     * (syncRewardStatistics, syncParticipantStatistics).
     */
    public void onMessage(MapRecord<String, String, String> message) {
        String messageId = message.getId().getValue();
        String retryKey = RedisStreamKeys.RETRY_KEY_PREFIX_REWARD + messageId;
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
                stringRedisTemplate.delete(retryKey);
                log.info("Reward event already processed, messageId={}, rewardRecordId={}", messageId, rewardRecordId);
                return;
            }

            // Simulate external reward channel (e.g., coupon service, payment gateway)
            // ~80% success rate to demonstrate real-world behavior
            boolean channelSuccess = Math.random() < 0.80;

            // Sync statistics BEFORE updating reward record.
            // If stats sync fails: record stays pending, message retries cleanly.
            // If stats succeed but updateById fails: stats are already correct, retry is a no-op.
            activityStatisticsSyncService.syncRewardStatistics(activityId, eventTime);

            if (channelSuccess) {
                rewardRecord.setSendStatus(1);  // SUCCESS
                rewardRecord.setFailReason(null);
            } else {
                rewardRecord.setSendStatus(2);  // FAIL
                rewardRecord.setFailReason("模拟发放通道异常：外部服务不可达");
            }
            rewardRecord.setSendTime(eventTime);
            rewardRecordMapper.updateById(rewardRecord);

            stringRedisTemplate.opsForStream().acknowledge(
                RedisStreamKeys.REWARD_EVENT_STREAM,
                RedisStreamKeys.REWARD_SEND_GROUP,
                message.getId()
            );
            stringRedisTemplate.delete(retryKey);
            log.info("Consumed reward event successfully, messageId={}, rewardRecordId={}, status={}",
                messageId, rewardRecordId, rewardRecord.getSendStatus());
        } catch (Exception ex) {
            log.error("Consume reward event failed, messageId={}, payload={}", messageId, message.getValue(), ex);
            handleRetryOrDlq(message, retryKey, ex);
        }
    }

    private void handleRetryOrDlq(MapRecord<String, String, String> message, String retryKey, Exception ex) {
        String messageId = message.getId().getValue();
        Long retryCount = stringRedisTemplate.opsForValue().increment(retryKey);
        if (retryCount == null) {
            retryCount = 1L;
        }
        stringRedisTemplate.expire(retryKey, Duration.ofHours(1));

        if (retryCount > RedisStreamKeys.MAX_RETRY_COUNT) {
            log.warn("Reward event exceeded max retries ({}), moving to DLQ, messageId={}", RedisStreamKeys.MAX_RETRY_COUNT, messageId);
            stringRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(message.getValue())
                    .withStreamKey(RedisStreamKeys.REWARD_EVENT_DLQ)
            );
            stringRedisTemplate.opsForStream().acknowledge(
                RedisStreamKeys.REWARD_EVENT_STREAM,
                RedisStreamKeys.REWARD_SEND_GROUP,
                message.getId()
            );
            stringRedisTemplate.delete(retryKey);
            log.info("Reward event moved to DLQ, messageId={}", messageId);
            return;
        }
        throw new RuntimeException("Reward event consumption failed (retry " + retryCount + "/" + RedisStreamKeys.MAX_RETRY_COUNT + "): " + ex.getMessage(), ex);
    }
}
