package com.example.activityagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityCreateRequest {
    @NotBlank
    private String activityName;
    @NotBlank
    private String activityType;
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
    @NotNull
    private Integer status;
    private String ruleDesc;
}
