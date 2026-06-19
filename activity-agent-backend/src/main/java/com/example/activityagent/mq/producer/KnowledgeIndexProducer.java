package com.example.activityagent.mq.producer;

import com.example.activityagent.mq.constant.RocketMqConstant;
import com.example.activityagent.mq.dto.KnowledgeIndexMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeIndexProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public SendResult send(KnowledgeIndexMessage message) {
        String destination = RocketMqConstant.KNOWLEDGE_INDEX_TOPIC + ":" + RocketMqConstant.TAG_DOC_INDEX;
        SendResult sendResult = rocketMQTemplate.syncSend(destination, message);
        log.info("Sent knowledge index message, destination={}, sendResult={}, payload={}", destination, sendResult, message);
        return sendResult;
    }
}
