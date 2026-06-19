package com.example.activityagent.service;

import com.example.activityagent.dto.LearningEventRequest;
import com.example.activityagent.dto.LearningEventResponse;

public interface LearningEventService {

    LearningEventResponse record(LearningEventRequest request);

    Boolean delete(Long id);
}
