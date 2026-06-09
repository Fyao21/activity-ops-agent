package com.example.activityagent.mq;

public final class RedisStreamKeys {

    public static final String ACTIVITY_EVENT_STREAM = "stream:activity:event";
    public static final String REWARD_EVENT_STREAM = "stream:reward:event";
    public static final String ACTIVITY_STAT_GROUP = "group:activity:stat";
    public static final String REWARD_SEND_GROUP = "group:reward:send";
    public static final String ACTIVITY_STAT_CONSUMER = "consumer-activity-stat";
    public static final String REWARD_SEND_CONSUMER = "consumer-reward-send";

    private RedisStreamKeys() {
    }
}
