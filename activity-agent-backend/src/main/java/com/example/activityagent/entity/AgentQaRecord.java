package com.example.activityagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
    private String question;
    private String generatedSql;
    private String queryResult;
    private String answer;
    private Integer success;
    private String errorMessage;
    private Integer riskLevel;
    private String riskReason;
    private LocalDateTime createTime;
}
