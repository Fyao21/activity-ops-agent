package com.example.activityagent.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamPublisher {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringRedisTemplate stringRedisTemplate;

    public RecordId publishActivityEvent(ActivityEventMessage message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("eventType", message.getEventType());
        payload.put("activityId", String.valueOf(message.getActivityId()));
        payload.put("userId", String.valueOf(message.getUserId()));
        payload.put("channel", message.getChannel());
        payload.put("eventTime", DATE_TIME_FORMATTER.format(message.getEventTime()));

        RecordId recordId = stringRedisTemplate.opsForStream().add(
            StreamRecords.mapBacked(payload).withStreamKey(RedisStreamKeys.ACTIVITY_EVENT_STREAM)
        );
        log.info("Published activity event, recordId={}, payload={}", recordId, payload);
        return recordId;
    }

    public RecordId publishRewardEvent(RewardEventMessage message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("eventType", message.getEventType());
        payload.put("rewardRecordId", String.valueOf(message.getRewardRecordId()));
        payload.put("activityId", String.valueOf(message.getActivityId()));
        payload.put("userId", String.valueOf(message.getUserId()));
        payload.put("rewardType", message.getRewardType());
        payload.put("rewardAmount", message.getRewardAmount().toPlainString());
        payload.put("eventTime", DATE_TIME_FORMATTER.format(message.getEventTime()));

        RecordId recordId = stringRedisTemplate.opsForStream().add(
            StreamRecords.mapBacked(payload).withStreamKey(RedisStreamKeys.REWARD_EVENT_STREAM)
        );
        log.info("Published reward event, recordId={}, payload={}", recordId, payload);
        return recordId;
    }
}
