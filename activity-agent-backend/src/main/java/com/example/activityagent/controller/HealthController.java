package com.example.activityagent.controller;

import com.example.activityagent.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Result<Map<String, Object>> index() {
        return Result.success(Map.of(
            "service", "activity-agent-backend",
            "status", "ok"
        ));
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.success(Map.of("status", "ok"));
    }
}
