package com.example.activityagent.service;

import com.example.activityagent.dto.ActivityCreateRequest;
import com.example.activityagent.dto.ActivityUpdateRequest;
import com.example.activityagent.entity.Activity;

import java.util.List;

/**
 * @deprecated Legacy activity-analysis service retained only for old demos.
 */
@Deprecated(forRemoval = false)
public interface ActivityService {
    Activity create(ActivityCreateRequest request);

    List<Activity> list(int page, int pageSize);

    Activity getById(Long id);

    Activity update(ActivityUpdateRequest request);
}
