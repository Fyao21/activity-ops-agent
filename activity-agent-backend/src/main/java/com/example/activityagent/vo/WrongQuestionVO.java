package com.example.activityagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WrongQuestionVO {

    private Long answerRecordId;
    private Long questionId;
    private Long courseId;
    private String knowledgePoint;
    private String questionContent;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String userAnswer;
    private String correctAnswer;
    private String analysis;
    private LocalDateTime answerTime;
}
