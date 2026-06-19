package com.example.activityagent.mq.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearningEventMessage {

    private Long userId;
    private Long courseId;
    private String eventType;
    private LocalDateTime eventTime;
    private String messageKey;
}
