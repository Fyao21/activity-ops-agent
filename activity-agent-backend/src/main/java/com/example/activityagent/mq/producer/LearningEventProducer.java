package com.example.activityagent.mq.producer;

import com.example.activityagent.mq.constant.RocketMqConstant;
import com.example.activityagent.mq.dto.LearningEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LearningEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public SendResult send(LearningEventMessage message) {
        String destination = RocketMqConstant.LEARNING_EVENT_TOPIC + ":" + message.getEventType();
        SendResult sendResult = rocketMQTemplate.syncSend(destination, message);
        log.info("Sent learning event message, destination={}, sendResult={}, messageKey={}, payload={}",
            destination, sendResult, message.getMessageKey(), message);
        return sendResult;
    }
}
