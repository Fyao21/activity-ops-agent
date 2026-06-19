package com.example.activityagent.mq.producer;

import com.example.activityagent.mq.constant.RocketMqConstant;
import com.example.activityagent.mq.dto.AnswerStatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerStatProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public SendResult send(AnswerStatMessage message) {
        String destination = RocketMqConstant.ANSWER_STAT_TOPIC + ":" + RocketMqConstant.TAG_ANSWER_STAT;
        SendResult sendResult = rocketMQTemplate.syncSend(destination, message);
        log.info("Sent answer stat message, destination={}, sendResult={}, answerRecordId={}, payload={}",
            destination, sendResult, message.getAnswerRecordId(), message);
        return sendResult;
    }
}
