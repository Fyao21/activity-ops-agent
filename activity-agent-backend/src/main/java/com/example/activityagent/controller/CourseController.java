package com.example.activityagent.controller;

import com.example.activityagent.common.Result;
import com.example.activityagent.dto.CourseCreateRequest;
import com.example.activityagent.dto.CourseUpdateRequest;
import com.example.activityagent.service.CourseService;
import com.example.activityagent.vo.CourseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping("/create")
    public Result<CourseVO> create(@Valid @RequestBody CourseCreateRequest request) {
        return Result.success(courseService.create(request));
    }

    @GetMapping("/list")
    public Result<List<CourseVO>> list(
        @RequestParam(defaultValue = "1") long pageNum,
        @RequestParam(defaultValue = "10") long pageSize
    ) {
        return Result.success(courseService.list(pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<CourseVO> detail(@PathVariable Long id) {
        return Result.success(courseService.getById(id));
    }

    @PutMapping("/update")
    public Result<CourseVO> update(@Valid @RequestBody CourseUpdateRequest request) {
        return Result.success(courseService.update(request));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(courseService.delete(id));
    }
}
