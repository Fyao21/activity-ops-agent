package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.activityagent.common.BusinessException;
import com.example.activityagent.common.ErrorCode;
import com.example.activityagent.dto.RewardSendRequest;
import com.example.activityagent.entity.Activity;
import com.example.activityagent.entity.ActivityUserRecord;
import com.example.activityagent.entity.RewardRecord;
import com.example.activityagent.mapper.ActivityMapper;
import com.example.activityagent.mapper.ActivityUserRecordMapper;
import com.example.activityagent.mapper.RewardRecordMapper;
import com.example.activityagent.mq.RedisStreamPublisher;
import com.example.activityagent.mq.RewardEventMessage;
import com.example.activityagent.service.RewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

    private final ActivityMapper activityMapper;
    private final ActivityUserRecordMapper activityUserRecordMapper;
    private final RewardRecordMapper rewardRecordMapper;
    private final RedisStreamPublisher redisStreamPublisher;

    /**
     * Initiate a reward send request.
     *
     * The reward record is created with sendStatus=0 (pending) and the actual
     * distribution happens asynchronously via Redis Stream.
     *
     * Idempotency: the database UNIQUE constraint on (activity_id, user_id, reward_type)
     * prevents duplicate reward records for the same type. A code-level check
     * provides a fast-path rejection.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RewardRecord sendReward(RewardSendRequest request) {
        // Validate activity
        Activity activity = activityMapper.selectById(request.getActivityId());
        if (activity == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        // Validate participation
        ActivityUserRecord participation = activityUserRecordMapper.selectOne(new LambdaQueryWrapper<ActivityUserRecord>()
            .eq(ActivityUserRecord::getActivityId, request.getActivityId())
            .eq(ActivityUserRecord::getUserId, request.getUserId())
            .eq(ActivityUserRecord::getParticipateStatus, 1)
            .last("LIMIT 1"));
        if (participation == null) {
            throw new BusinessException(ErrorCode.REWARD_NOT_ELIGIBLE);
        }

        // Validate reward amount
        if (request.getRewardAmount() == null
            || request.getRewardAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.REWARD_AMOUNT_INVALID);
        }

        // Fast-path idempotency check
        RewardRecord existing = rewardRecordMapper.selectOne(new LambdaQueryWrapper<RewardRecord>()
            .eq(RewardRecord::getActivityId, request.getActivityId())
            .eq(RewardRecord::getUserId, request.getUserId())
            .eq(RewardRecord::getRewardType, request.getRewardType())
            .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.REWARD_ALREADY_SENT,
                "该用户已发放过 " + request.getRewardType() + " 类型奖励");
        }

        RewardRecord rewardRecord = new RewardRecord();
        rewardRecord.setActivityId(request.getActivityId());
        rewardRecord.setUserId(request.getUserId());
        rewardRecord.setRewardType(request.getRewardType());
        rewardRecord.setRewardAmount(request.getRewardAmount());
        rewardRecord.setSendStatus(0);  // PENDING
        rewardRecord.setSendTime(null);
        try {
            rewardRecordMapper.insert(rewardRecord);
        } catch (DuplicateKeyException e) {
            log.warn("Duplicate reward blocked by UNIQUE constraint: activityId={}, userId={}, rewardType={}",
                request.getActivityId(), request.getUserId(), request.getRewardType());
            throw new BusinessException(ErrorCode.REWARD_ALREADY_SENT,
                "该类型奖励已发放（并发保护）");
        }

        // Publish async reward event — consumer finalizes status and updates statistics
        RewardEventMessage eventMessage = new RewardEventMessage();
        eventMessage.setEventType("SEND_REWARD");
        eventMessage.setRewardRecordId(rewardRecord.getId());
        eventMessage.setActivityId(rewardRecord.getActivityId());
        eventMessage.setUserId(rewardRecord.getUserId());
        eventMessage.setRewardType(rewardRecord.getRewardType());
        eventMessage.setRewardAmount(rewardRecord.getRewardAmount());
        eventMessage.setEventTime(LocalDateTime.now());
        redisStreamPublisher.publishRewardEvent(eventMessage);

        return rewardRecord;
    }
}
