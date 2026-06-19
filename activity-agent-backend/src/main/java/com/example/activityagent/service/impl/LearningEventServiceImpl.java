package com.example.activityagent.service.impl;

import com.example.activityagent.common.BusinessException;
import com.example.activityagent.dto.LearningEventRequest;
import com.example.activityagent.dto.LearningEventResponse;
import com.example.activityagent.mapper.LearningEventMapper;
import com.example.activityagent.mapper.CourseMapper;
import com.example.activityagent.mapper.SysUserMapper;
import com.example.activityagent.mq.constant.RocketMqConstant;
import com.example.activityagent.mq.dto.LearningEventMessage;
import com.example.activityagent.mq.producer.LearningEventProducer;
import com.example.activityagent.service.LearningEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningEventServiceImpl implements LearningEventService {

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
        RocketMqConstant.TAG_QUESTION,
        RocketMqConstant.TAG_ANSWER,
        RocketMqConstant.TAG_VIEW_COURSE,
        RocketMqConstant.TAG_UPLOAD_DOC
    );

    private final SysUserMapper sysUserMapper;
    private final CourseMapper courseMapper;
    private final LearningEventMapper learningEventMapper;
    private final LearningEventProducer learningEventProducer;

    @Override
    public LearningEventResponse record(LearningEventRequest request) {
        String eventType = normalizeEventType(request.getEventType());
        validateUserAndCourse(request.getUserId(), request.getCourseId());

        LearningEventMessage message = new LearningEventMessage();
        message.setUserId(request.getUserId());
        message.setCourseId(request.getCourseId());
        message.setEventType(eventType);
        message.setEventTime(LocalDateTime.now());
        message.setMessageKey(buildMessageKey(request.getUserId(), request.getCourseId(), eventType));

        // The API only sends RocketMQ messages. MySQL writes happen in LearningEventConsumer.
        learningEventProducer.send(message);

        LearningEventResponse response = new LearningEventResponse();
        response.setUserId(message.getUserId());
        response.setCourseId(message.getCourseId());
        response.setEventType(message.getEventType());
        response.setMessageKey(message.getMessageKey());
        response.setStatus("SENT");
        return response;
    }

    @Override
    public Boolean delete(Long id) {
        if (id == null) {
            throw new BusinessException("learning event id must not be null");
        }
        if (learningEventMapper.selectById(id) == null) {
            throw new BusinessException("Learning event does not exist: " + id);
        }
        learningEventMapper.deleteById(id);
        return true;
    }

    private String normalizeEventType(String eventType) {
        String normalized = eventType == null ? "" : eventType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_EVENT_TYPES.contains(normalized)) {
            throw new BusinessException("Unsupported learning event type: " + eventType);
        }
        return normalized;
    }

    private void validateUserAndCourse(Long userId, Long courseId) {
        if (sysUserMapper.selectById(userId) == null) {
            throw new BusinessException("User does not exist: " + userId);
        }
        if (courseMapper.selectById(courseId) == null) {
            throw new BusinessException("Course does not exist: " + courseId);
        }
    }

    private String buildMessageKey(Long userId, Long courseId, String eventType) {
        return "learning-event:" + userId + ":" + courseId + ":" + eventType + ":" + UUID.randomUUID();
    }
}
