package com.example.activityagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerSubmitRequest {

    @NotNull(message = "userId must not be null")
    private Long userId;

    @NotNull(message = "courseId must not be null")
    private Long courseId;

    @NotNull(message = "questionId must not be null")
    private Long questionId;

    @NotBlank(message = "userAnswer must not be blank")
    private String userAnswer;
}
