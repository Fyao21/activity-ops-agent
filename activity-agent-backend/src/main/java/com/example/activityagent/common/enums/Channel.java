package com.example.activityagent.common.enums;

/**
 * Participation channel.
 */
public enum Channel {
    APP,
    H5,
    WEB;

    public static boolean isValid(String value) {
        if (value == null) return false;
        for (Channel c : values()) {
            if (c.name().equalsIgnoreCase(value)) return true;
        }
        return false;
    }
}
