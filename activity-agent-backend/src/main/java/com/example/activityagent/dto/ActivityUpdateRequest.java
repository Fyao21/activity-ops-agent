package com.example.activityagent.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityUpdateRequest {
    @NotNull(message = "活动ID不能为空")
    private Long id;

    @Size(min = 1, max = 128, message = "活动名称长度1-128")
    private String activityName;

    @Pattern(regexp = "^(NEW_USER|MEMBER_ACTIVE|SALES_PROMOTION|FLASH_SALE|RECALL)$",
             message = "活动类型必须是 NEW_USER/MEMBER_ACTIVE/SALES_PROMOTION/FLASH_SALE/RECALL")
    private String activityType;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Min(value = 0, message = "状态值必须是0/1/2")
    @Max(value = 2, message = "状态值必须是0/1/2")
    private Integer status;

    private String ruleDesc;

    @AssertTrue(message = "结束时间必须晚于开始时间")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }
}
