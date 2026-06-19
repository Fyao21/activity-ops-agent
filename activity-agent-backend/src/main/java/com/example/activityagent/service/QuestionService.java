package com.example.activityagent.service;

import com.example.activityagent.dto.QuestionCreateRequest;
import com.example.activityagent.vo.QuestionVO;

import java.util.List;

public interface QuestionService {

    QuestionVO create(QuestionCreateRequest request);

    List<QuestionVO> list(Long courseId);

    Boolean delete(Long id);
}
