package com.example.activityagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeQueryRequest {
    @NotBlank(message = "question must not be blank")
    private String question;
    private Integer topK = 4;
}
