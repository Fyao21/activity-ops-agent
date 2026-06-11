package com.example.activityagent.mq;

/**
 * Legacy Redis Stream keys kept only for rollback and source comparison.
 * RocketMQ is now the default message queue implementation.
 * After RocketMQ runs stably in all environments, this class can be removed.
 */
@Deprecated(forRemoval = false)
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
