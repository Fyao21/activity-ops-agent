package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.activityagent.dto.StatisticsQueryRequest;
import com.example.activityagent.entity.ActivityStatistics;
import com.example.activityagent.mapper.ActivityStatisticsMapper;
import com.example.activityagent.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final ActivityStatisticsMapper activityStatisticsMapper;

    @Override
    public List<ActivityStatistics> query(StatisticsQueryRequest request) {
        LambdaQueryWrapper<ActivityStatistics> wrapper = new LambdaQueryWrapper<ActivityStatistics>()
            .eq(request.getActivityId() != null, ActivityStatistics::getActivityId, request.getActivityId())
            .ge(request.getStartDate() != null, ActivityStatistics::getStatDate, request.getStartDate())
            .le(request.getEndDate() != null, ActivityStatistics::getStatDate, request.getEndDate())
            .orderByDesc(ActivityStatistics::getStatDate)
            .orderByAsc(ActivityStatistics::getActivityId);
        return activityStatisticsMapper.selectList(wrapper);
    }
}
