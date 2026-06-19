package com.example.activityagent.mq.constant;

public final class RocketMqConstant {

    public static final String KNOWLEDGE_INDEX_TOPIC = "edu_knowledge_index_topic";
    public static final String KNOWLEDGE_INDEX_CONSUMER_GROUP = "edu_knowledge_index_consumer_group";
    public static final String TAG_DOC_INDEX = "DOC_INDEX";

    public static final String AGENT_TASK_TOPIC = "agent-task-topic";
    public static final String AGENT_TASK_PRODUCER_GROUP = "agent-task-producer-group";
    public static final String AGENT_TASK_CONSUMER_GROUP = "agent-task-consumer-group";

    public static final String AGENT_TASK_TAG_PARTICIPATE = "PARTICIPATE";
    public static final String AGENT_TASK_TAG_REWARD = "REWARD";
    public static final String AGENT_TASK_SELECTOR = AGENT_TASK_TAG_PARTICIPATE + " || " + AGENT_TASK_TAG_REWARD;

    private RocketMqConstant() {
    }
}
