package com.example.activityagent.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
/**
 * @deprecated Legacy activity statistics DTO retained only for old demos.
 */
@Deprecated(forRemoval = false)
public class StatisticsQueryRequest {
    private Long activityId;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
