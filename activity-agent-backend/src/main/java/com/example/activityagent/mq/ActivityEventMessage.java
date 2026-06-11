package com.example.activityagent.mq;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Legacy Redis Stream payload retained for rollback only.
 * The active RocketMQ payload is {@code AgentTaskMessage}.
 */
@Data
@Deprecated(forRemoval = false)
public class ActivityEventMessage {
    private String eventType;
    private Long activityId;
    private Long userId;
    private String channel;
    private LocalDateTime eventTime;
}
