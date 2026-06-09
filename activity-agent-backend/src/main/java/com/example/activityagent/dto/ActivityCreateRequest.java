package com.example.activityagent.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityCreateRequest {
    @NotBlank(message = "活动名称不能为空")
    @Size(min = 1, max = 128, message = "活动名称长度1-128")
    private String activityName;

    @NotBlank(message = "活动类型不能为空")
    @Pattern(regexp = "^(NEW_USER|MEMBER_ACTIVE|SALES_PROMOTION|FLASH_SALE|RECALL)$",
             message = "活动类型必须是 NEW_USER/MEMBER_ACTIVE/SALES_PROMOTION/FLASH_SALE/RECALL")
    private String activityType;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值必须是0/1/2")
    @Max(value = 2, message = "状态值必须是0/1/2")
    private Integer status;

    private String ruleDesc;

    @AssertTrue(message = "结束时间必须晚于开始时间")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true; // let @NotNull handle nulls
        }
        return endTime.isAfter(startTime);
    }
}
