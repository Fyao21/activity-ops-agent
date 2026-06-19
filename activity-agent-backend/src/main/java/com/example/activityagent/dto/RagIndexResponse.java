package com.example.activityagent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RagIndexResponse {
    private Boolean success;
    @JsonAlias("document_id")
    private Long documentId;
    @JsonAlias("course_id")
    private Long courseId;
    @JsonAlias("chunk_count")
    private Integer chunkCount;
    private String message;
    private List<RagChunkInfo> chunks = new ArrayList<>();

    @Data
    public static class RagChunkInfo {
        @JsonAlias("document_id")
        private Long documentId;
        @JsonAlias("course_id")
        private Long courseId;
        @JsonAlias("chunk_index")
        private Integer chunkIndex;
        private String content;
        @JsonAlias("vector_id")
        private String vectorId;
    }
}
