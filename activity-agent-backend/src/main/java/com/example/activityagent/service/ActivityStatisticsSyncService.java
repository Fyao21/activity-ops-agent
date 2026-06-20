package com.example.activityagent.service;

import java.time.LocalDateTime;

/**
 * @deprecated Legacy activity statistics sync service retained only for old demos.
 */
@Deprecated(forRemoval = false)
public interface ActivityStatisticsSyncService {

    void syncParticipantStatistics(Long activityId, LocalDateTime eventTime);

    void syncRewardStatistics(Long activityId, LocalDateTime eventTime);
}
