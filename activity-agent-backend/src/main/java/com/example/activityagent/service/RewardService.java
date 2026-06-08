package com.example.activityagent.service;

import com.example.activityagent.dto.RewardSendRequest;
import com.example.activityagent.entity.RewardRecord;

public interface RewardService {
    RewardRecord sendReward(RewardSendRequest request);
}
