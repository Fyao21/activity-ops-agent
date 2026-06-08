package com.example.activityagent.controller;

import com.example.activityagent.common.Result;
import com.example.activityagent.dto.AgentQueryRequest;
import com.example.activityagent.service.AgentService;
import com.example.activityagent.vo.AgentQueryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/query")
    public Result<AgentQueryResponse> query(@Valid @RequestBody AgentQueryRequest request) {
        return Result.success(agentService.query(request));
    }
}
