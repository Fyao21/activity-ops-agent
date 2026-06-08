package com.example.activityagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RewardSendRequest {
    @NotNull
    private Long activityId;
    @NotNull
    private Long userId;
    @NotBlank
    private String rewardType;
    @NotNull
    private BigDecimal rewardAmount;
}
