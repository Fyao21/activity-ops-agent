package com.example.activityagent.service;

import com.example.activityagent.dto.RewardSendRequest;
import com.example.activityagent.entity.RewardRecord;

/**
 * @deprecated Legacy activity reward service retained only for old demos.
 */
@Deprecated(forRemoval = false)
public interface RewardService {
    RewardRecord sendReward(RewardSendRequest request);
}
