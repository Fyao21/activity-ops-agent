package com.example.activityagent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class RagDeleteResponse {

    private Boolean success;

    @JsonAlias("document_id")
    private Long documentId;

    @JsonAlias("deleted_count")
    private Integer deletedCount;

    private String message;
}
