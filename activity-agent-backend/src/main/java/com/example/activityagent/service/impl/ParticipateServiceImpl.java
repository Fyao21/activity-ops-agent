package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.activityagent.common.BusinessException;
import com.example.activityagent.dto.ParticipateRequest;
import com.example.activityagent.entity.Activity;
import com.example.activityagent.entity.ActivityUserRecord;
import com.example.activityagent.mapper.ActivityMapper;
import com.example.activityagent.mapper.ActivityUserRecordMapper;
import com.example.activityagent.mq.constant.RocketMqConstant;
import com.example.activityagent.mq.dto.AgentTaskMessage;
import com.example.activityagent.mq.producer.AgentTaskProducer;
import com.example.activityagent.service.ParticipateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ParticipateServiceImpl implements ParticipateService {

    private final ActivityMapper activityMapper;
    private final ActivityUserRecordMapper activityUserRecordMapper;
    private final AgentTaskProducer agentTaskProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityUserRecord participate(ParticipateRequest request) {
        Activity activity = activityMapper.selectById(request.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (activity.getStatus() == null || activity.getStatus() != 1
            || now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new BusinessException("活动当前不可参与");
        }

        ActivityUserRecord existed = activityUserRecordMapper.selectOne(new LambdaQueryWrapper<ActivityUserRecord>()
            .eq(ActivityUserRecord::getActivityId, request.getActivityId())
            .eq(ActivityUserRecord::getUserId, request.getUserId())
            .last("LIMIT 1"));
        if (existed != null) {
            throw new BusinessException("用户已参与该活动");
        }

        ActivityUserRecord record = new ActivityUserRecord();
        record.setActivityId(request.getActivityId());
        record.setUserId(request.getUserId());
        record.setChannel(request.getChannel());
        record.setParticipateStatus(1);
        record.setParticipateTime(now);
        activityUserRecordMapper.insert(record);

        // Publish an async RocketMQ task after the participate record is persisted.
        AgentTaskMessage message = new AgentTaskMessage();
        message.setTaskId(record.getId());
        message.setEventType(RocketMqConstant.AGENT_TASK_TAG_PARTICIPATE);
        message.setActivityId(record.getActivityId());
        message.setUserId(record.getUserId());
        message.setChannel(record.getChannel());
        message.setEventTime(record.getParticipateTime());
        agentTaskProducer.sendAgentTaskMessage(message);

        return record;
    }
}
