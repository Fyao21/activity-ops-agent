package com.example.activityagent.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RewardSendRequest {
    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Pattern(regexp = "^(COUPON|POINT|CASH)$", message = "奖励类型必须是 COUPON/POINT/CASH")
    private String rewardType;

    @NotNull(message = "奖励金额不能为空")
    @DecimalMin(value = "0.01", message = "奖励金额必须大于0")
    private BigDecimal rewardAmount;
}
