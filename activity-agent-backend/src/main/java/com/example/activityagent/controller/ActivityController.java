package com.example.activityagent.controller;

import com.example.activityagent.common.PageResult;
import com.example.activityagent.common.RequireRole;
import com.example.activityagent.common.Result;
import com.example.activityagent.dto.ActivityCreateRequest;
import com.example.activityagent.dto.ActivityUpdateRequest;
import com.example.activityagent.entity.Activity;
import com.example.activityagent.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping("/create")
    @RequireRole({"ADMIN"})
    public Result<Activity> create(@Valid @RequestBody ActivityCreateRequest request) {
        return Result.success(activityService.create(request));
    }

    @GetMapping("/list")
    public Result<PageResult<Activity>> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        return Result.success(activityService.list(page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Activity> detail(@PathVariable Long id) {
        return Result.success(activityService.getById(id));
    }

    @PutMapping("/update")
    @RequireRole({"ADMIN"})
    public Result<Activity> update(@Valid @RequestBody ActivityUpdateRequest request) {
        return Result.success(activityService.update(request));
    }
}
