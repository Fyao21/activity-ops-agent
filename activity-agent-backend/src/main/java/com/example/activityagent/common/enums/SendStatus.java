package com.example.activityagent.common.enums;

/**
 * Reward send status.
 * 0 = initial/pending, 1 = success, 2 = failed
 */
public enum SendStatus {
    PENDING(0, "待处理"),
    SUCCESS(1, "成功"),
    FAILED(2, "失败");

    private final int code;
    private final String desc;

    SendStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static boolean isValid(Integer code) {
        if (code == null) return false;
        for (SendStatus s : values()) {
            if (s.code == code) return true;
        }
        return false;
    }
}
