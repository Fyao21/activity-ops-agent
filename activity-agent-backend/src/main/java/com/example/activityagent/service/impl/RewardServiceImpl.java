package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.activityagent.common.BusinessException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

    private final ActivityMapper activityMapper;
    private final ActivityUserRecordMapper activityUserRecordMapper;
    private final RewardRecordMapper rewardRecordMapper;
    private final RedisStreamPublisher redisStreamPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RewardRecord sendReward(RewardSendRequest request) {
        Activity activity = activityMapper.selectById(request.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        ActivityUserRecord participation = activityUserRecordMapper.selectOne(new LambdaQueryWrapper<ActivityUserRecord>()
            .eq(ActivityUserRecord::getActivityId, request.getActivityId())
            .eq(ActivityUserRecord::getUserId, request.getUserId())
            .eq(ActivityUserRecord::getParticipateStatus, 1)
            .last("LIMIT 1"));
        if (participation == null) {
            throw new BusinessException("用户未参与该活动，不能发放奖励");
        }

        RewardRecord rewardRecord = new RewardRecord();
        rewardRecord.setActivityId(request.getActivityId());
        rewardRecord.setUserId(request.getUserId());
        rewardRecord.setRewardType(request.getRewardType());
        rewardRecord.setRewardAmount(request.getRewardAmount());
        rewardRecord.setSendStatus(0);
        rewardRecord.setSendTime(null);
        rewardRecordMapper.insert(rewardRecord);

        // Create an async reward event. The consumer finalizes send status and statistics.
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
