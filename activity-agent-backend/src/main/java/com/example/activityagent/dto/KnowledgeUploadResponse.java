package com.example.activityagent.dto;

import lombok.Data;

@Data
public class KnowledgeUploadResponse {
    private Long documentId;
    private Long courseId;
    private String fileName;
    private String fileType;
    private Integer status;
    private Integer chunkCount;
    private String message;
}
