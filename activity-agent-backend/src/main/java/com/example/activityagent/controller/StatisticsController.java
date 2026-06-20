package com.example.activityagent.controller;

import com.example.activityagent.common.Result;
import com.example.activityagent.dto.StatisticsQueryRequest;
import com.example.activityagent.entity.ActivityStatistics;
import com.example.activityagent.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
/**
 * @deprecated Legacy activity statistics API. It is kept for old demos and
 * does not belong to the intelligent course learning assistant core flow.
 */
@Deprecated(forRemoval = false)
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/activity")
    public Result<List<ActivityStatistics>> query(@ModelAttribute StatisticsQueryRequest request) {
        return Result.success(statisticsService.query(request));
    }
}
