package com.example.activityagent.controller;

import com.example.activityagent.common.RequireRole;
import com.example.activityagent.common.Result;
import com.example.activityagent.dto.ParticipateRequest;
import com.example.activityagent.entity.ActivityUserRecord;
import com.example.activityagent.service.ParticipateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
public class ParticipateController {

    private final ParticipateService participateService;

    @PostMapping("/participate")
    @RequireRole({"ADMIN", "OPERATOR"})
    public Result<ActivityUserRecord> participate(@Valid @RequestBody ParticipateRequest request) {
        return Result.success(participateService.participate(request));
    }
}
