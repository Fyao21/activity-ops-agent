package com.example.activityagent.mq;

public final class RedisStreamKeys {

    // Primary streams
    public static final String ACTIVITY_EVENT_STREAM = "stream:activity:event";
    public static final String REWARD_EVENT_STREAM = "stream:reward:event";

    // Dead-letter streams
    public static final String ACTIVITY_EVENT_DLQ = "stream:activity:event:dlq";
    public static final String REWARD_EVENT_DLQ = "stream:reward:event:dlq";

    // Consumer groups
    public static final String ACTIVITY_STAT_GROUP = "group:activity:stat";
    public static final String REWARD_SEND_GROUP = "group:reward:send";

    // Consumer names (instance-specific suffix appended at startup)
    public static String ACTIVITY_STAT_CONSUMER = "consumer-activity-stat";
    public static String REWARD_SEND_CONSUMER = "consumer-reward-send";

    // Max retry attempts before moving to DLQ
    public static final int MAX_RETRY_COUNT = 3;

    // Redis key prefix for retry counters: retry:activity:{messageId}, retry:reward:{messageId}
    public static final String RETRY_KEY_PREFIX_ACTIVITY = "retry:activity:";
    public static final String RETRY_KEY_PREFIX_REWARD = "retry:reward:";

    private RedisStreamKeys() {
    }
}
