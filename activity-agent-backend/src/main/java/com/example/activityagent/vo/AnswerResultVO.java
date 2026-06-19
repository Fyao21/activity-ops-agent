package com.example.activityagent.vo;

import lombok.Data;

@Data
public class AnswerResultVO {

    private Long answerRecordId;
    private Long questionId;
    private String questionContent;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String userAnswer;
    private String correctAnswer;
    private Boolean correct;
    private String analysis;
}
