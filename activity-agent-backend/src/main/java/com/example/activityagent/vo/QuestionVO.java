package com.example.activityagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionVO {

    private Long id;
    private Long courseId;
    private String knowledgePoint;
    private String questionContent;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String answer;
    private String analysis;
    private LocalDateTime createTime;
}
