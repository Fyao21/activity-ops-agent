package com.example.activityagent.service;

import com.example.activityagent.dto.CourseCreateRequest;
import com.example.activityagent.dto.CourseUpdateRequest;
import com.example.activityagent.vo.CourseVO;

import java.util.List;

public interface CourseService {
    CourseVO create(CourseCreateRequest request);

    List<CourseVO> list(long pageNum, long pageSize);

    CourseVO getById(Long id);

    CourseVO update(CourseUpdateRequest request);
}
