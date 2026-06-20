package com.example.activityagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_user_record")
/**
 * @deprecated Legacy activity participation entity retained only for old demos.
 */
@Deprecated(forRemoval = false)
public class ActivityUserRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private String channel;
    private Integer participateStatus;
    private LocalDateTime participateTime;
    private LocalDateTime createTime;
}
