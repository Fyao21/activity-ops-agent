package com.example.activityagent.controller;

import com.example.activityagent.common.Result;
import com.example.activityagent.dto.QuestionCreateRequest;
import com.example.activityagent.service.QuestionService;
import com.example.activityagent.vo.QuestionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping("/create")
    public Result<QuestionVO> create(@Valid @RequestBody QuestionCreateRequest request) {
        return Result.success(questionService.create(request));
    }

    @GetMapping("/list")
    public Result<List<QuestionVO>> list(@RequestParam Long courseId) {
        return Result.success(questionService.list(courseId));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(questionService.delete(id));
    }
}
