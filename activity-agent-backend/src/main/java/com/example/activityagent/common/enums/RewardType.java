package com.example.activityagent.common.enums;

/**
 * Reward types.
 */
public enum RewardType {
    COUPON,
    POINT,
    CASH;

    public static boolean isValid(String value) {
        if (value == null) return false;
        for (RewardType t : values()) {
            if (t.name().equalsIgnoreCase(value)) return true;
        }
        return false;
    }
}
