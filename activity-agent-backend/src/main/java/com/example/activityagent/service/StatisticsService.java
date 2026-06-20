package com.example.activityagent.service;

import com.example.activityagent.dto.StatisticsQueryRequest;
import com.example.activityagent.entity.ActivityStatistics;

import java.util.List;

/**
 * @deprecated Legacy activity statistics service retained only for old demos.
 */
@Deprecated(forRemoval = false)
public interface StatisticsService {
    List<ActivityStatistics> query(StatisticsQueryRequest request);
}
