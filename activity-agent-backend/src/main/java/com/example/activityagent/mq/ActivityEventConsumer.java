package com.example.activityagent.mq;

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
public class ActivityEventConsumer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringRedisTemplate stringRedisTemplate;
    private final ActivityStatisticsSyncService activityStatisticsSyncService;

    /**
     * Recalculate the daily participant count after a participate event is consumed.
     * On failure: increment retry counter; after MAX_RETRY_COUNT exceeds,
     * move the message to DLQ and ack the original.
     */
    public void onMessage(MapRecord<String, String, String> message) {
        String messageId = message.getId().getValue();
        String retryKey = RedisStreamKeys.RETRY_KEY_PREFIX_ACTIVITY + messageId;
        try {
            Long activityId = Long.valueOf(message.getValue().get("activityId"));
            LocalDateTime eventTime = LocalDateTime.parse(message.getValue().get("eventTime"), DATE_TIME_FORMATTER);

            activityStatisticsSyncService.syncParticipantStatistics(activityId, eventTime);
            stringRedisTemplate.opsForStream().acknowledge(
                RedisStreamKeys.ACTIVITY_EVENT_STREAM,
                RedisStreamKeys.ACTIVITY_STAT_GROUP,
                message.getId()
            );
            stringRedisTemplate.delete(retryKey);
            log.info("Consumed activity event successfully, messageId={}, payload={}", messageId, message.getValue());
        } catch (Exception ex) {
            log.error("Consume activity event failed, messageId={}, payload={}", messageId, message.getValue(), ex);
            handleRetryOrDlq(message, retryKey, ex);
        }
    }

    private void handleRetryOrDlq(MapRecord<String, String, String> message, String retryKey, Exception ex) {
        String messageId = message.getId().getValue();
        Long retryCount = stringRedisTemplate.opsForValue().increment(retryKey);
        if (retryCount == null) {
            retryCount = 1L;
        }
        // Set retry key TTL to avoid orphaned keys
        stringRedisTemplate.expire(retryKey, Duration.ofHours(1));

        if (retryCount > RedisStreamKeys.MAX_RETRY_COUNT) {
            log.warn("Activity event exceeded max retries ({}), moving to DLQ, messageId={}", RedisStreamKeys.MAX_RETRY_COUNT, messageId);
            // Copy to DLQ
            stringRedisTemplate.opsForStream().add(
                StreamRecords.mapBacked(message.getValue())
                    .withStreamKey(RedisStreamKeys.ACTIVITY_EVENT_DLQ)
            );
            // Ack original to stop retrying
            stringRedisTemplate.opsForStream().acknowledge(
                RedisStreamKeys.ACTIVITY_EVENT_STREAM,
                RedisStreamKeys.ACTIVITY_STAT_GROUP,
                message.getId()
            );
            stringRedisTemplate.delete(retryKey);
            log.info("Activity event moved to DLQ, messageId={}", messageId);
        }
        // If retry count <= max, re-throw to keep message pending for retry
        throw new RuntimeException("Activity event consumption failed (retry " + retryCount + "/" + RedisStreamKeys.MAX_RETRY_COUNT + "): " + ex.getMessage(), ex);
    }
}
