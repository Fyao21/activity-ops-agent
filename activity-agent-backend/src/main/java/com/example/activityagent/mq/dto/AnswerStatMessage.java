package com.example.activityagent.mq.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnswerStatMessage {

    private Long answerRecordId;
    private Long userId;
    private Long courseId;
    private Long questionId;
    private Boolean correct;
    private LocalDateTime answerTime;
}
