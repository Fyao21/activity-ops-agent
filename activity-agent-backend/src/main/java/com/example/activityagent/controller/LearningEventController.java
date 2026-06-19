package com.example.activityagent.controller;

import com.example.activityagent.common.Result;
import com.example.activityagent.dto.LearningEventRequest;
import com.example.activityagent.dto.LearningEventResponse;
import com.example.activityagent.service.LearningEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/learning")
@RequiredArgsConstructor
public class LearningEventController {

    private final LearningEventService learningEventService;

    @PostMapping("/event")
    public Result<LearningEventResponse> record(@Valid @RequestBody LearningEventRequest request) {
        return Result.success(learningEventService.record(request));
    }

    @DeleteMapping("/event/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(learningEventService.delete(id));
    }
}
