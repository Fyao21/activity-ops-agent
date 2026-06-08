package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activityagent.common.BusinessException;
import com.example.activityagent.dto.ActivityCreateRequest;
import com.example.activityagent.dto.ActivityUpdateRequest;
import com.example.activityagent.entity.Activity;
import com.example.activityagent.mapper.ActivityMapper;
import com.example.activityagent.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Activity create(ActivityCreateRequest request) {
        Activity activity = new Activity();
        activity.setActivityName(request.getActivityName());
        activity.setActivityType(request.getActivityType());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setStatus(request.getStatus());
        activity.setRuleDesc(request.getRuleDesc());
        activityMapper.insert(activity);
        return activity;
    }

    @Override
    public List<Activity> list(int page, int pageSize) {
        Page<Activity> pageResult = activityMapper.selectPage(
            new Page<>(page, pageSize),
            new LambdaQueryWrapper<Activity>().orderByDesc(Activity::getId)
        );
        return pageResult.getRecords();
    }

    @Override
    public Activity getById(Long id) {
        String cacheKey = "activity:info:" + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof Activity activity) {
            return activity;
        }
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        redisTemplate.opsForValue().set(cacheKey, activity);
        return activity;
    }

    @Override
    public Activity update(ActivityUpdateRequest request) {
        Activity existing = activityMapper.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException("活动不存在");
        }
        if (request.getActivityName() != null) {
            existing.setActivityName(request.getActivityName());
        }
        if (request.getActivityType() != null) {
            existing.setActivityType(request.getActivityType());
        }
        if (request.getStartTime() != null) {
            existing.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            existing.setEndTime(request.getEndTime());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (request.getRuleDesc() != null) {
            existing.setRuleDesc(request.getRuleDesc());
        }
        activityMapper.updateById(existing);
        redisTemplate.delete("activity:info:" + existing.getId());
        return existing;
    }
}
