package com.example.activityagent.mq.producer;

import com.example.activityagent.mq.constant.RocketMqConstant;
import com.example.activityagent.mq.dto.AgentTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTaskProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public SendResult sendAgentTaskMessage(AgentTaskMessage message) {
        String destination = RocketMqConstant.AGENT_TASK_TOPIC + ":" + resolveTag(message.getEventType());
        SendResult sendResult = rocketMQTemplate.syncSend(destination, message);
        log.info("Sent RocketMQ agent task message, destination={}, sendResult={}, payload={}", destination, sendResult, message);
        return sendResult;
    }

    private String resolveTag(String eventType) {
        if (RocketMqConstant.AGENT_TASK_TAG_REWARD.equalsIgnoreCase(eventType)) {
            return RocketMqConstant.AGENT_TASK_TAG_REWARD;
        }
        return RocketMqConstant.AGENT_TASK_TAG_PARTICIPATE;
    }
}
