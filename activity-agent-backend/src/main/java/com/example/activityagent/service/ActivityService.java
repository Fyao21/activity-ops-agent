package com.example.activityagent.service;

import com.example.activityagent.common.PageResult;
import com.example.activityagent.dto.ActivityCreateRequest;
import com.example.activityagent.dto.ActivityUpdateRequest;
import com.example.activityagent.entity.Activity;

public interface ActivityService {
    Activity create(ActivityCreateRequest request);

    PageResult<Activity> list(int page, int pageSize);

    Activity getById(Long id);

    Activity update(ActivityUpdateRequest request);
}
