package com.example.activityagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("reward_record")
public class RewardRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private String rewardType;
    private BigDecimal rewardAmount;
    private Integer sendStatus;
    private String failReason;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
