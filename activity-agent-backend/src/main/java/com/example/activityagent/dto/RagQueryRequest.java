package com.example.activityagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RagQueryRequest {
    @JsonProperty("course_id")
    private Long courseId;
    private String question;
    @JsonProperty("top_k")
    private Integer topK;
}
