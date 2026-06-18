package com.example.activityagent.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeQueryResponse {
    private String answer;
    @JsonAlias("retrieved_chunks")
    private List<RetrievedChunk> retrievedChunks = new ArrayList<>();
    private List<String> sources = new ArrayList<>();

    @Data
    public static class RetrievedChunk {
        @JsonAlias("document_id")
        private Long documentId;
        @JsonAlias("chunk_index")
        private Integer chunkIndex;
        private String content;
        private Double score;
        @JsonAlias("vector_id")
        private String vectorId;
        @JsonAlias("file_name")
        private String fileName;
    }
}
