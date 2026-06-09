package com.example.activityagent.mq;

import com.example.activityagent.service.ActivityStatisticsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

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
     * Recalculation makes the consumer safe to retry because it rewrites the final number.
     */
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            Long activityId = Long.valueOf(message.getValue().get("activityId"));
            LocalDateTime eventTime = LocalDateTime.parse(message.getValue().get("eventTime"), DATE_TIME_FORMATTER);

            activityStatisticsSyncService.syncParticipantStatistics(activityId, eventTime);
            stringRedisTemplate.opsForStream().acknowledge(
                RedisStreamKeys.ACTIVITY_EVENT_STREAM,
                RedisStreamKeys.ACTIVITY_STAT_GROUP,
                message.getId()
            );
            log.info("Consumed activity event successfully, messageId={}, payload={}", message.getId(), message.getValue());
        } catch (Exception ex) {
            log.error("Consume activity event failed, messageId={}, payload={}", message.getId(), message.getValue(), ex);
            throw ex;
        }
    }
}
