package com.example.activityagent.dto;

import lombok.Data;

@Data
public class LearningEventResponse {

    private Long userId;
    private Long courseId;
    private String eventType;
    private String messageKey;
    private String status;
}
