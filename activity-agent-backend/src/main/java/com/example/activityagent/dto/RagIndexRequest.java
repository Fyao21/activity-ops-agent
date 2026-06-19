package com.example.activityagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RagIndexRequest {
    @JsonProperty("document_id")
    private Long documentId;
    @JsonProperty("course_id")
    private Long courseId;
    @JsonProperty("file_path")
    private String filePath;
    @JsonProperty("file_name")
    private String fileName;
}
