package com.example.activityagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("activity_statistics")
public class ActivityStatistics {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private LocalDate statDate;
    private Integer participantCount;
    private Integer rewardCount;
    private Integer rewardSuccessCount;
    private BigDecimal conversionRate;
    private BigDecimal retentionRate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
