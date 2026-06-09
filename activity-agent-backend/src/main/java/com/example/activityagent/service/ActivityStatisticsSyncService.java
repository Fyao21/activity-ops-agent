package com.example.activityagent.service;

import java.time.LocalDateTime;

public interface ActivityStatisticsSyncService {

    void syncParticipantStatistics(Long activityId, LocalDateTime eventTime);

    void syncRewardStatistics(Long activityId, LocalDateTime eventTime);
}
