package com.example.activityagent.service;

import com.example.activityagent.dto.AnswerSubmitRequest;
import com.example.activityagent.vo.AnswerResultVO;
import com.example.activityagent.vo.WrongQuestionVO;

import java.util.List;

public interface AnswerService {

    AnswerResultVO submit(AnswerSubmitRequest request);

    List<WrongQuestionVO> listWrong(Long userId, Long courseId);

    Boolean delete(Long id);
}
