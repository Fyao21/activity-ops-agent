package com.example.activityagent.service;

import com.example.activityagent.dto.ParticipateRequest;
import com.example.activityagent.entity.ActivityUserRecord;

/**
 * @deprecated Legacy activity participation service retained only for old demos.
 */
@Deprecated(forRemoval = false)
public interface ParticipateService {
    ActivityUserRecord participate(ParticipateRequest request);
}
