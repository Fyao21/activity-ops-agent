package com.example.activityagent.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ParticipateRequest {
    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Pattern(regexp = "^(APP|H5|WEB)$", message = "渠道必须是 APP/H5/WEB")
    private String channel;
}
