package com.example.activityagent.service;

import com.example.activityagent.dto.StatisticsQueryRequest;
import com.example.activityagent.entity.ActivityStatistics;

import java.util.List;

public interface StatisticsService {
    List<ActivityStatistics> query(StatisticsQueryRequest request);
}
