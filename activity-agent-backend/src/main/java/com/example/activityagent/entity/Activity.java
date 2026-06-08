package com.example.activityagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String activityName;
    private String activityType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String ruleDesc;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
