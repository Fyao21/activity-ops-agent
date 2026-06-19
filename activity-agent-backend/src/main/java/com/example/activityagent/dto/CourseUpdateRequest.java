package com.example.activityagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseUpdateRequest {
    @NotNull(message = "id must not be null")
    private Long id;
    @NotBlank(message = "courseName must not be blank")
    private String courseName;
    @NotNull(message = "teacherId must not be null")
    private Long teacherId;
    private String description;
    private Integer status = 1;
}
