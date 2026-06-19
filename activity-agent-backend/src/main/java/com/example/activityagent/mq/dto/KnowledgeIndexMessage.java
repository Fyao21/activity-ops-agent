package com.example.activityagent.mq.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeIndexMessage {
    private Long documentId;
    private Long courseId;
    private String filePath;
    private String fileName;
    private LocalDateTime eventTime;
}
