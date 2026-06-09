package com.example.activityagent.mq;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityEventMessage {
    private String eventType;
    private Long activityId;
    private Long userId;
    private String channel;
    private LocalDateTime eventTime;
}
