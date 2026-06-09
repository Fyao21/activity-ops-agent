package com.example.activityagent.common;

/**
 * Unified business error codes.
 */
public enum ErrorCode {

    // General
    SUCCESS(0, "success"),
    SYSTEM_ERROR(500, "系统异常，请稍后重试"),

    // Auth (1xxx)
    AUTH_INVALID_TOKEN(1001, "未提供有效的认证Token"),
    AUTH_TOKEN_EXPIRED(1002, "Token已过期或无效"),
    AUTH_WRONG_CREDENTIALS(1003, "用户名或密码错误"),
    AUTH_FORBIDDEN(1004, "权限不足"),

    // Validation (2xxx)
    VALIDATION_ERROR(2001, "请求参数不合法"),
    VALIDATION_FIELD_ERROR(2002, "字段校验失败"),

    // Activity (3xxx)
    ACTIVITY_NOT_FOUND(3001, "活动不存在"),
    ACTIVITY_NOT_ACTIVE(3002, "活动当前不可参与"),
    ACTIVITY_ALREADY_JOINED(3003, "用户已参与该活动"),
    ACTIVITY_TIME_INVALID(3004, "活动时间不合法"),
    ACTIVITY_STATUS_INVALID(3005, "活动状态不合法"),
    ACTIVITY_CANNOT_MODIFY(3006, "已结束的活动不可修改"),

    // Reward (4xxx)
    REWARD_NOT_ELIGIBLE(4001, "用户未参与该活动，不能发放奖励"),
    REWARD_ALREADY_SENT(4002, "该类型奖励已发放"),
    REWARD_AMOUNT_INVALID(4003, "奖励金额不合法"),

    // Agent (5xxx)
    AGENT_CALL_FAILED(5001, "Agent服务调用失败"),
    AGENT_SQL_GUARD_REJECTED(5002, "SQL校验未通过"),

    // Resource (6xxx)
    RESOURCE_NOT_FOUND(6001, "请求资源不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
