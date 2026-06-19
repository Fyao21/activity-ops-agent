package com.example.activityagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionCreateRequest {

    @NotNull(message = "courseId must not be null")
    private Long courseId;

    private String knowledgePoint;

    @NotBlank(message = "questionContent must not be blank")
    private String questionContent;

    @NotBlank(message = "optionA must not be blank")
    private String optionA;

    @NotBlank(message = "optionB must not be blank")
    private String optionB;

    @NotBlank(message = "optionC must not be blank")
    private String optionC;

    @NotBlank(message = "optionD must not be blank")
    private String optionD;

    @NotBlank(message = "answer must not be blank")
    private String answer;

    private String analysis;
}
