package com.example.activityagent.config;

import com.example.activityagent.mq.ActivityEventConsumer;
import com.example.activityagent.mq.RedisStreamKeys;
import com.example.activityagent.mq.RewardEventConsumer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisStreamConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final StringRedisTemplate stringRedisTemplate;
    private final ActivityEventConsumer activityEventConsumer;
    private final RewardEventConsumer rewardEventConsumer;

    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;

    @PostConstruct
    public void init() {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        // Create and manage the Redis Stream container inside this config class
        // to avoid bean initialization order issues and circular references.
        listenerContainer = StreamMessageListenerContainer.create(redisConnectionFactory, options);

        ensureGroup(RedisStreamKeys.ACTIVITY_EVENT_STREAM, RedisStreamKeys.ACTIVITY_STAT_GROUP);
        ensureGroup(RedisStreamKeys.REWARD_EVENT_STREAM, RedisStreamKeys.REWARD_SEND_GROUP);

        subscriptions.add(listenerContainer.receive(
            Consumer.from(RedisStreamKeys.ACTIVITY_STAT_GROUP, RedisStreamKeys.ACTIVITY_STAT_CONSUMER),
            StreamOffset.create(RedisStreamKeys.ACTIVITY_EVENT_STREAM, ReadOffset.lastConsumed()),
            activityEventConsumer::onMessage
        ));

        subscriptions.add(listenerContainer.receive(
            Consumer.from(RedisStreamKeys.REWARD_SEND_GROUP, RedisStreamKeys.REWARD_SEND_CONSUMER),
            StreamOffset.create(RedisStreamKeys.REWARD_EVENT_STREAM, ReadOffset.lastConsumed()),
            rewardEventConsumer::onMessage
        ));

        listenerContainer.start();
        log.info("Redis Stream consumers started");
    }

    @PreDestroy
    public void destroy() {
        subscriptions.forEach(Subscription::cancel);
        if (listenerContainer != null) {
            listenerContainer.stop();
        }
    }

    private void ensureGroup(String streamKey, String groupName) {
        try {
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(streamKey))) {
                stringRedisTemplate.opsForStream().add(
                    StreamRecords.mapBacked(Map.of("init", "true")).withStreamKey(streamKey)
                );
            }
            stringRedisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), groupName);
            log.info("Created Redis Stream group, streamKey={}, group={}", streamKey, groupName);
        } catch (Exception ex) {
            if (isBusyGroupException(ex)) {
                log.info("Redis Stream group already exists, streamKey={}, group={}", streamKey, groupName);
                return;
            }
            throw ex;
        }
    }

    private boolean isBusyGroupException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
