package com.example.activityagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseVO {
    private Long id;
    private String courseName;
    private Long teacherId;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
