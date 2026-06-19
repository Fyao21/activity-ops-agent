package com.example.activityagent.controller;

import com.example.activityagent.common.Result;
import com.example.activityagent.dto.AnswerSubmitRequest;
import com.example.activityagent.service.AnswerService;
import com.example.activityagent.vo.AnswerResultVO;
import com.example.activityagent.vo.WrongQuestionVO;
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
@RequestMapping("/answer")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping("/submit")
    public Result<AnswerResultVO> submit(@Valid @RequestBody AnswerSubmitRequest request) {
        return Result.success(answerService.submit(request));
    }

    @GetMapping("/wrong/list")
    public Result<List<WrongQuestionVO>> listWrong(
        @RequestParam Long userId,
        @RequestParam Long courseId
    ) {
        return Result.success(answerService.listWrong(userId, courseId));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(answerService.delete(id));
    }
}
