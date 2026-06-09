package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.activityagent.common.BusinessException;
import com.example.activityagent.common.ErrorCode;
import com.example.activityagent.dto.ParticipateRequest;
import com.example.activityagent.entity.Activity;
import com.example.activityagent.entity.ActivityUserRecord;
import com.example.activityagent.mapper.ActivityMapper;
import com.example.activityagent.mapper.ActivityUserRecordMapper;
import com.example.activityagent.mq.ActivityEventMessage;
import com.example.activityagent.mq.RedisStreamPublisher;
import com.example.activityagent.service.ParticipateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipateServiceImpl implements ParticipateService {

    private final ActivityMapper activityMapper;
    private final ActivityUserRecordMapper activityUserRecordMapper;
    private final RedisStreamPublisher redisStreamPublisher;

    /**
     * Register a user's participation in an activity.
     *
     * Uses database UNIQUE constraint (activity_id, user_id) as the final
     * concurrency guard. The code-level check-and-insert is a fast-path;
     * DuplicateKeyException caught at the constraint layer provides the
     * definitive safety net under concurrent requests.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityUserRecord participate(ParticipateRequest request) {
        Activity activity = activityMapper.selectById(request.getActivityId());
        if (activity == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        if (activity.getStatus() == null || activity.getStatus() != 1
            || now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_ACTIVE);
        }

        // Fast-path dedup: skip insert if already present (handles 99.9% of cases)
        ActivityUserRecord existed = activityUserRecordMapper.selectOne(new LambdaQueryWrapper<ActivityUserRecord>()
            .eq(ActivityUserRecord::getActivityId, request.getActivityId())
            .eq(ActivityUserRecord::getUserId, request.getUserId())
            .last("LIMIT 1"));
        if (existed != null) {
            throw new BusinessException(ErrorCode.ACTIVITY_ALREADY_JOINED);
        }

        ActivityUserRecord record = new ActivityUserRecord();
        record.setActivityId(request.getActivityId());
        record.setUserId(request.getUserId());
        record.setChannel(request.getChannel());
        record.setParticipateStatus(1);
        record.setParticipateTime(now);
        try {
            activityUserRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            log.warn("Concurrent duplicate participation blocked by UNIQUE constraint: activityId={}, userId={}",
                request.getActivityId(), request.getUserId());
            throw new BusinessException(ErrorCode.ACTIVITY_ALREADY_JOINED);
        }

        // Publish async event for statistics recalculation
        ActivityEventMessage eventMessage = new ActivityEventMessage();
        eventMessage.setEventType("PARTICIPATE");
        eventMessage.setActivityId(record.getActivityId());
        eventMessage.setUserId(record.getUserId());
        eventMessage.setChannel(record.getChannel());
        eventMessage.setEventTime(record.getParticipateTime());
        redisStreamPublisher.publishActivityEvent(eventMessage);

        return record;
    }
}
