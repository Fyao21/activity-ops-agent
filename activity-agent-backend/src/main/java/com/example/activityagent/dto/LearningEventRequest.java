package com.example.activityagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LearningEventRequest {

    @NotNull(message = "userId must not be null")
    private Long userId;

    @NotNull(message = "courseId must not be null")
    private Long courseId;

    @NotBlank(message = "eventType must not be blank")
    private String eventType;
}
