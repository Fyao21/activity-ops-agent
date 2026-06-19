package com.example.activityagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_qa_record")
public class AgentQaRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long courseId;
    private String question;
    private String routeType;
    private String generatedSql;
    private String retrievedContext;
    @TableField(exist = false)
    private String queryResult;
    private String answer;
    private Integer success;
    private String errorMessage;
    private LocalDateTime createTime;
}
