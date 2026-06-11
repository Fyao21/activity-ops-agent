package com.example.activityagent.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Run this test only after local RocketMQ NameServer and Broker are started.
 * Example:
 * 1. mqnamesrv.cmd
 * 2. mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class RocketMqSendTest {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Test
    void sendHelloRocketMqMessage() throws Exception {
        log.info("Sending RocketMQ test message to topic={}", RocketMqTestConstants.TEST_TOPIC);
        SendResult sendResult = rocketMQTemplate.syncSend(
            RocketMqTestConstants.TEST_TOPIC,
            "Hello RocketMQ"
        );
        log.info("RocketMQ test send result: {}", sendResult);
        log.info("Message sent. Waiting 5 seconds for consumer output...");
        Thread.sleep(5000);
    }
}
