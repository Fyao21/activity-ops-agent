package com.example.activityagent.controller;

import com.example.activityagent.common.Result;
import com.example.activityagent.dto.RewardSendRequest;
import com.example.activityagent.entity.RewardRecord;
import com.example.activityagent.service.RewardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reward")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @PostMapping("/send")
    public Result<RewardRecord> send(@Valid @RequestBody RewardSendRequest request) {
        return Result.success(rewardService.sendReward(request));
    }
}
