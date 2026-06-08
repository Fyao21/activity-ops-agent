package com.example.activityagent.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityUpdateRequest {
    @NotNull
    private Long id;
    private String activityName;
    private String activityType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String ruleDesc;
}
