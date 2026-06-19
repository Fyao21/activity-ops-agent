package com.example.activityagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeQueryRequest {
    @NotNull(message = "courseId must not be null")
    private Long courseId;
    private Long userId = 1L;
    @NotBlank(message = "question must not be blank")
    private String question;
    private Integer topK = 4;
}
