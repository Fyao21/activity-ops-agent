package com.example.activityagent.mq.constant;

public final class RocketMqConstant {

    public static final String KNOWLEDGE_INDEX_TOPIC = "edu_knowledge_index_topic";
    public static final String KNOWLEDGE_INDEX_CONSUMER_GROUP = "edu_knowledge_index_consumer_group";
    public static final String TAG_DOC_INDEX = "DOC_INDEX";

    public static final String LEARNING_EVENT_TOPIC = "edu_learning_event_topic";
    public static final String LEARNING_EVENT_CONSUMER_GROUP = "edu_learning_event_consumer_group";
    public static final String TAG_QUESTION = "QUESTION";
    public static final String TAG_ANSWER = "ANSWER";
    public static final String TAG_VIEW_COURSE = "VIEW_COURSE";
    public static final String TAG_UPLOAD_DOC = "UPLOAD_DOC";
    public static final String LEARNING_EVENT_SELECTOR =
        TAG_QUESTION + " || " + TAG_ANSWER + " || " + TAG_VIEW_COURSE + " || " + TAG_UPLOAD_DOC;

    public static final String ANSWER_STAT_TOPIC = "edu_answer_stat_topic";
    public static final String ANSWER_STAT_CONSUMER_GROUP = "edu_answer_stat_consumer_group";
    public static final String TAG_ANSWER_STAT = "ANSWER_STAT";

    /**
     * @deprecated Legacy activity participate/reward task topic retained only
     * for old activity-analysis demos.
     */
    @Deprecated(forRemoval = false)
    public static final String AGENT_TASK_TOPIC = "agent-task-topic";
    /**
     * @deprecated Legacy activity participate/reward producer group.
     */
    @Deprecated(forRemoval = false)
    public static final String AGENT_TASK_PRODUCER_GROUP = "agent-task-producer-group";
    /**
     * @deprecated Legacy activity participate/reward consumer group.
     */
    @Deprecated(forRemoval = false)
    public static final String AGENT_TASK_CONSUMER_GROUP = "agent-task-consumer-group";

    /**
     * @deprecated Legacy activity participation tag.
     */
    @Deprecated(forRemoval = false)
    public static final String AGENT_TASK_TAG_PARTICIPATE = "PARTICIPATE";
    /**
     * @deprecated Legacy activity reward tag.
     */
    @Deprecated(forRemoval = false)
    public static final String AGENT_TASK_TAG_REWARD = "REWARD";
    /**
     * @deprecated Legacy activity task selector.
     */
    @Deprecated(forRemoval = false)
    public static final String AGENT_TASK_SELECTOR = AGENT_TASK_TAG_PARTICIPATE + " || " + AGENT_TASK_TAG_REWARD;

    private RocketMqConstant() {
    }
}
