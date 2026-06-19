package com.example.activityagent.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.activityagent.entity.LearningEvent;
import com.example.activityagent.mapper.LearningEventMapper;
import com.example.activityagent.mq.constant.RocketMqConstant;
import com.example.activityagent.mq.dto.LearningEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = RocketMqConstant.LEARNING_EVENT_TOPIC,
    consumerGroup = RocketMqConstant.LEARNING_EVENT_CONSUMER_GROUP,
    selectorExpression = RocketMqConstant.LEARNING_EVENT_SELECTOR
)
public class LearningEventConsumer implements RocketMQListener<LearningEventMessage> {

    private final LearningEventMapper learningEventMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(LearningEventMessage message) {
        log.info("Received learning event message: {}", message);
        try {
            if (existsByMessageKey(message.getMessageKey())) {
                log.info("Learning event already consumed, messageKey={}", message.getMessageKey());
                return;
            }

            LearningEvent event = new LearningEvent();
            event.setUserId(message.getUserId());
            event.setCourseId(message.getCourseId());
            event.setEventType(message.getEventType());
            event.setEventTime(message.getEventTime());
            event.setMessageKey(message.getMessageKey());
            learningEventMapper.insert(event);
            log.info("Learning event saved, messageKey={}", message.getMessageKey());
        } catch (DuplicateKeyException ex) {
            // Unique key uk_message_key makes repeated RocketMQ delivery idempotent.
            log.info("Learning event duplicate message ignored, messageKey={}", message.getMessageKey());
        } catch (Exception ex) {
            log.error("Consume learning event message failed, payload={}", message, ex);
            throw ex;
        }
    }

    private boolean existsByMessageKey(String messageKey) {
        Long count = learningEventMapper.selectCount(
            new LambdaQueryWrapper<LearningEvent>()
                .eq(LearningEvent::getMessageKey, messageKey)
        );
        return count != null && count > 0;
    }
}
