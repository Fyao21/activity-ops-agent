package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activityagent.common.BusinessException;
import com.example.activityagent.common.ErrorCode;
import com.example.activityagent.common.PageResult;
import com.example.activityagent.common.enums.ActivityStatus;
import com.example.activityagent.dto.ActivityCreateRequest;
import com.example.activityagent.dto.ActivityUpdateRequest;
import com.example.activityagent.entity.Activity;
import com.example.activityagent.mapper.ActivityMapper;
import com.example.activityagent.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "activity:info:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    // Short TTL for null markers to prevent cache penetration
    private static final Duration NULL_CACHE_TTL = Duration.ofMinutes(5);
    private static final String NULL_MARKER = "__NULL__";

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

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public PageResult<Activity> list(int page, int pageSize) {
        int safePageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 1);
        Page<Activity> pageResult = activityMapper.selectPage(
            new Page<>(safePage, safePageSize),
            new LambdaQueryWrapper<Activity>().orderByDesc(Activity::getId)
        );
        return PageResult.of(
            pageResult.getRecords(),
            pageResult.getTotal(),
            safePage,
            safePageSize
        );
    }

    @Override
    public Activity getById(Long id) {
        String cacheKey = CACHE_PREFIX + id;

        // 1. Try cache
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (NULL_MARKER.equals(cached)) {
                throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND.getMessage());
            }
            if (cached instanceof Activity activity) {
                return activity;
            }
        }

        // 2. Query DB
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            // Cache null marker to prevent cache penetration
            redisTemplate.opsForValue().set(cacheKey, NULL_MARKER, NULL_CACHE_TTL);
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND.getMessage());
        }

        // 3. Populate cache with TTL
        redisTemplate.opsForValue().set(cacheKey, activity, CACHE_TTL);
        return activity;
    }

    @Override
    public Activity update(ActivityUpdateRequest request) {
        Activity existing = activityMapper.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND.getMessage());
        }

        // Business rule: ended activities cannot be modified
        if (existing.getStatus() != null && existing.getStatus() == ActivityStatus.ENDED.getCode()) {
            throw new BusinessException(ErrorCode.ACTIVITY_CANNOT_MODIFY.getMessage());
        }

        // Prevent changing ended activity back to in-progress
        if (request.getStatus() != null
            && existing.getStatus() != null
            && existing.getStatus() == ActivityStatus.ENDED.getCode()
            && request.getStatus() != ActivityStatus.ENDED.getCode()) {
            throw new BusinessException(ErrorCode.ACTIVITY_CANNOT_MODIFY.getMessage());
        }

        // Validate time range if both times are provided
        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (!request.getEndTime().isAfter(request.getStartTime())) {
                throw new BusinessException(ErrorCode.ACTIVITY_TIME_INVALID.getMessage());
            }
        } else if (request.getStartTime() != null && existing.getEndTime() != null) {
            if (!existing.getEndTime().isAfter(request.getStartTime())) {
                throw new BusinessException(ErrorCode.ACTIVITY_TIME_INVALID.getMessage());
            }
        } else if (request.getEndTime() != null && existing.getStartTime() != null) {
            if (!request.getEndTime().isAfter(existing.getStartTime())) {
                throw new BusinessException(ErrorCode.ACTIVITY_TIME_INVALID.getMessage());
            }
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
        existing.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(existing);

        // Evict cache after update
        redisTemplate.delete(CACHE_PREFIX + existing.getId());
        return existing;
    }
}
