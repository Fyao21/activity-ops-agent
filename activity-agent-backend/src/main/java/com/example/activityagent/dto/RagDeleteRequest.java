package com.example.activityagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RagDeleteRequest {

    @JsonProperty("document_id")
    private Long documentId;
}
