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
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Configuration
@EnableScheduling
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
        // Build instance-specific consumer names for observability
        String instanceId = resolveInstanceId();
        RedisStreamKeys.ACTIVITY_STAT_CONSUMER = "consumer-activity-stat-" + instanceId;
        RedisStreamKeys.REWARD_SEND_CONSUMER = "consumer-reward-send-" + instanceId;
        log.info("Redis Stream consumer instance: {}", instanceId);

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

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

    /**
     * Periodically reclaim pending messages that have been idle for too long.
     * Runs every 60 seconds to avoid overwhelming the system.
     */
    @Scheduled(fixedDelay = 60_000)
    public void reclaimPendingMessages() {
        reclaimPending(RedisStreamKeys.ACTIVITY_EVENT_STREAM, RedisStreamKeys.ACTIVITY_STAT_GROUP);
        reclaimPending(RedisStreamKeys.REWARD_EVENT_STREAM, RedisStreamKeys.REWARD_SEND_GROUP);
    }

    private void reclaimPending(String streamKey, String groupName) {
        String consumerName = streamKey.equals(RedisStreamKeys.REWARD_EVENT_STREAM)
            ? RedisStreamKeys.REWARD_SEND_CONSUMER
            : RedisStreamKeys.ACTIVITY_STAT_CONSUMER;
        try {
            PendingMessagesSummary summary = stringRedisTemplate.opsForStream()
                .pending(streamKey, groupName);
            if (summary == null || summary.getTotalPendingMessages() == 0) {
                return;
            }
            long pendingCount = summary.getTotalPendingMessages();
            log.info("Pending messages in {}/{}: {}", streamKey, groupName, pendingCount);
            // Cap to avoid fetching millions of pending messages at once
            long fetchCount = Math.min(pendingCount, 100);
            PendingMessages pending = stringRedisTemplate.opsForStream()
                .pending(streamKey, Consumer.from(groupName, "*"), null, fetchCount);
            pending.forEach(msg -> {
                if (msg.getElapsedTimeSinceLastDelivery() != null
                    && msg.getElapsedTimeSinceLastDelivery().compareTo(Duration.ofMinutes(2)) > 0) {
                    stringRedisTemplate.opsForStream().claim(
                        streamKey, groupName,
                        consumerName,
                        Duration.ofMinutes(1),
                        RecordId.of(msg.getIdAsString())
                    );
                    log.info("Claimed idle pending message: {}/{} msg={}", streamKey, groupName, msg.getIdAsString());
                }
            });
        } catch (Exception ex) {
            log.warn("Failed to reclaim pending messages for {}/{}: {}", streamKey, groupName, ex.getMessage());
        }
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

    private String resolveInstanceId() {
        try {
            String host = InetAddress.getLocalHost().getHostName().replaceAll("[^a-zA-Z0-9]", "-");
            return host + "-" + System.getProperty("server.port", "8080");
        } catch (Exception e) {
            return "node-" + System.currentTimeMillis() % 10000;
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
