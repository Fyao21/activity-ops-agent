package com.example.activityagent.mq;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RewardEventMessage {
    private String eventType;
    private Long rewardRecordId;
    private Long activityId;
    private Long userId;
    private String rewardType;
    private BigDecimal rewardAmount;
    private LocalDateTime eventTime;
}
