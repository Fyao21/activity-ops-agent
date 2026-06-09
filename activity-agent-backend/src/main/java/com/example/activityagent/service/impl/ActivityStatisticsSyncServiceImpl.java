package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.activityagent.entity.ActivityStatistics;
import com.example.activityagent.entity.ActivityUserRecord;
import com.example.activityagent.entity.RewardRecord;
import com.example.activityagent.mapper.ActivityStatisticsMapper;
import com.example.activityagent.mapper.ActivityUserRecordMapper;
import com.example.activityagent.mapper.RewardRecordMapper;
import com.example.activityagent.service.ActivityStatisticsSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ActivityStatisticsSyncServiceImpl implements ActivityStatisticsSyncService {

    private final ActivityStatisticsMapper activityStatisticsMapper;
    private final ActivityUserRecordMapper activityUserRecordMapper;
    private final RewardRecordMapper rewardRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncParticipantStatistics(Long activityId, LocalDateTime eventTime) {
        LocalDate statDate = eventTime.toLocalDate();
        LocalDateTime start = statDate.atStartOfDay();
        LocalDateTime end = statDate.atTime(LocalTime.MAX);

        long participantCount = activityUserRecordMapper.selectCount(new LambdaQueryWrapper<ActivityUserRecord>()
            .eq(ActivityUserRecord::getActivityId, activityId)
            .eq(ActivityUserRecord::getParticipateStatus, 1)
            .ge(ActivityUserRecord::getParticipateTime, start)
            .le(ActivityUserRecord::getParticipateTime, end));

        ActivityStatistics statistics = getOrCreateStatistics(activityId, statDate);
        statistics.setParticipantCount((int) participantCount);
        saveOrUpdateStatistics(statistics);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncRewardStatistics(Long activityId, LocalDateTime eventTime) {
        LocalDate statDate = eventTime.toLocalDate();
        LocalDateTime start = statDate.atStartOfDay();
        LocalDateTime end = statDate.atTime(LocalTime.MAX);

        long rewardCount = rewardRecordMapper.selectCount(new LambdaQueryWrapper<RewardRecord>()
            .eq(RewardRecord::getActivityId, activityId)
            .ge(RewardRecord::getSendTime, start)
            .le(RewardRecord::getSendTime, end)
            .in(RewardRecord::getSendStatus, 1, 2));

        long rewardSuccessCount = rewardRecordMapper.selectCount(new LambdaQueryWrapper<RewardRecord>()
            .eq(RewardRecord::getActivityId, activityId)
            .ge(RewardRecord::getSendTime, start)
            .le(RewardRecord::getSendTime, end)
            .eq(RewardRecord::getSendStatus, 1));

        ActivityStatistics statistics = getOrCreateStatistics(activityId, statDate);
        statistics.setRewardCount((int) rewardCount);
        statistics.setRewardSuccessCount((int) rewardSuccessCount);

        if (statistics.getParticipantCount() != null && statistics.getParticipantCount() > 0) {
            statistics.setConversionRate(
                BigDecimal.valueOf(rewardSuccessCount)
                    .divide(BigDecimal.valueOf(statistics.getParticipantCount()), 4, RoundingMode.HALF_UP)
            );
        } else {
            statistics.setConversionRate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        }

        saveOrUpdateStatistics(statistics);
    }

    private ActivityStatistics getOrCreateStatistics(Long activityId, LocalDate statDate) {
        ActivityStatistics statistics = activityStatisticsMapper.selectOne(new LambdaQueryWrapper<ActivityStatistics>()
            .eq(ActivityStatistics::getActivityId, activityId)
            .eq(ActivityStatistics::getStatDate, statDate)
            .last("LIMIT 1"));
        if (statistics != null) {
            return statistics;
        }

        ActivityStatistics created = new ActivityStatistics();
        created.setActivityId(activityId);
        created.setStatDate(statDate);
        created.setParticipantCount(0);
        created.setRewardCount(0);
        created.setRewardSuccessCount(0);
        created.setConversionRate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        created.setRetentionRate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        return created;
    }

    private void saveOrUpdateStatistics(ActivityStatistics statistics) {
        if (statistics.getId() == null) {
            activityStatisticsMapper.insert(statistics);
        } else {
            activityStatisticsMapper.updateById(statistics);
        }
    }
}
