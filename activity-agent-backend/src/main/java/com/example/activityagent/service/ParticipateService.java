package com.example.activityagent.service;

import com.example.activityagent.dto.ParticipateRequest;
import com.example.activityagent.entity.ActivityUserRecord;

public interface ParticipateService {
    ActivityUserRecord participate(ParticipateRequest request);
}
