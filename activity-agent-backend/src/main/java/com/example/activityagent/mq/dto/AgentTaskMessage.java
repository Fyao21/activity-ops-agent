package com.example.activityagent.mq.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AgentTaskMessage implements Serializable {

    private Long taskId;
    private String eventType;
    private Long rewardRecordId;
    private Long activityId;
    private Long userId;
    private String channel;
    private String rewardType;
    private BigDecimal rewardAmount;
    private LocalDateTime eventTime;
}
