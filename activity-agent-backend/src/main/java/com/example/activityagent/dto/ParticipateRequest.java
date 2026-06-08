package com.example.activityagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ParticipateRequest {
    @NotNull
    private Long activityId;
    @NotNull
    private Long userId;
    @NotBlank
    private String channel;
}
