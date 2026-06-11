package com.example.activityagent.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * Test consumer for manually verifying that messages sent by RocketMqSendTest
 * can be consumed from the local RocketMQ broker.
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqTestConstants.TEST_TOPIC,
    consumerGroup = RocketMqTestConstants.TEST_CONSUMER_GROUP
)
public class RocketMqTestConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("RocketMQ test consumer received message: {}", message);
    }
}
