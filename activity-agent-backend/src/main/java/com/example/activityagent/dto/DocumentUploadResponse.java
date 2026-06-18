package com.example.activityagent.dto;

import lombok.Data;

@Data
public class DocumentUploadResponse {
    private Long documentId;
    private String fileName;
    private String fileType;
    private Integer status;
    private Integer chunkCount;
    private String message;
}
