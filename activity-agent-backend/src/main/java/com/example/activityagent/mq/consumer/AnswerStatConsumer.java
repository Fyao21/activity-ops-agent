package com.example.activityagent.mq.consumer;

import com.example.activityagent.mq.constant.RocketMqConstant;
import com.example.activityagent.mq.dto.AnswerStatMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = RocketMqConstant.ANSWER_STAT_TOPIC,
    consumerGroup = RocketMqConstant.ANSWER_STAT_CONSUMER_GROUP,
    selectorExpression = RocketMqConstant.TAG_ANSWER_STAT
)
public class AnswerStatConsumer implements RocketMQListener<AnswerStatMessage> {

    private final Set<Long> consumedAnswerRecordIds = ConcurrentHashMap.newKeySet();

    @Override
    public void onMessage(AnswerStatMessage message) {
        log.info("Received answer stat message: {}", message);
        try {
            Long answerRecordId = message.getAnswerRecordId();
            if (answerRecordId != null && !consumedAnswerRecordIds.add(answerRecordId)) {
                log.info("Answer stat message already consumed, answerRecordId={}", answerRecordId);
                return;
            }

            // Reserved extension point: update answer statistics table in later steps.
            log.info("Answer stat consumed, answerRecordId={}, userId={}, courseId={}, correct={}",
                answerRecordId, message.getUserId(), message.getCourseId(), message.getCorrect());
        } catch (Exception ex) {
            log.error("Consume answer stat message failed, payload={}", message, ex);
            throw ex;
        }
    }
}
