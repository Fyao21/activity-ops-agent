package com.example.activityagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentVO {
    private Long id;
    private String fileName;
    private String fileType;
    private Integer status;
    private Integer chunkCount;
    private LocalDateTime createTime;
}
