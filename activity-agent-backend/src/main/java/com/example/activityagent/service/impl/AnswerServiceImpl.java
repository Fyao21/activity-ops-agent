package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.activityagent.common.BusinessException;
import com.example.activityagent.dto.AnswerSubmitRequest;
import com.example.activityagent.entity.AnswerRecord;
import com.example.activityagent.entity.Question;
import com.example.activityagent.mapper.AnswerRecordMapper;
import com.example.activityagent.mapper.CourseMapper;
import com.example.activityagent.mapper.QuestionMapper;
import com.example.activityagent.mapper.SysUserMapper;
import com.example.activityagent.mq.constant.RocketMqConstant;
import com.example.activityagent.mq.dto.AnswerStatMessage;
import com.example.activityagent.mq.dto.LearningEventMessage;
import com.example.activityagent.mq.producer.AnswerStatProducer;
import com.example.activityagent.mq.producer.LearningEventProducer;
import com.example.activityagent.service.AnswerService;
import com.example.activityagent.vo.AnswerResultVO;
import com.example.activityagent.vo.WrongQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerServiceImpl implements AnswerService {

    private final SysUserMapper sysUserMapper;
    private final CourseMapper courseMapper;
    private final QuestionMapper questionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final LearningEventProducer learningEventProducer;
    private final AnswerStatProducer answerStatProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnswerResultVO submit(AnswerSubmitRequest request) {
        validateUserAndCourse(request.getUserId(), request.getCourseId());
        Question question = getQuestionForCourse(request.getQuestionId(), request.getCourseId());

        boolean correct = normalizeAnswer(question.getAnswer()).equals(normalizeAnswer(request.getUserAnswer()));
        AnswerRecord record = new AnswerRecord();
        record.setUserId(request.getUserId());
        record.setCourseId(request.getCourseId());
        record.setQuestionId(request.getQuestionId());
        record.setUserAnswer(request.getUserAnswer());
        record.setCorrect(correct ? 1 : 0);
        answerRecordMapper.insert(record);

        // Answer records are persisted first. MQ is used for async behavior/statistics processing.
        sendLearningEvent(record);
        sendAnswerStat(record);
        return buildResult(record, question, correct);
    }

    @Override
    public List<WrongQuestionVO> listWrong(Long userId, Long courseId) {
        if (userId == null || courseId == null) {
            throw new BusinessException("userId and courseId must not be null");
        }

        List<AnswerRecord> records = answerRecordMapper.selectList(
            new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getUserId, userId)
                .eq(AnswerRecord::getCourseId, courseId)
                .eq(AnswerRecord::getCorrect, 0)
                .orderByDesc(AnswerRecord::getCreateTime)
        );
        if (records.isEmpty()) {
            return List.of();
        }

        List<Long> questionIds = records.stream().map(AnswerRecord::getQuestionId).distinct().toList();
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds)
            .stream()
            .collect(Collectors.toMap(Question::getId, Function.identity()));

        return records.stream()
            .map(record -> toWrongQuestionVO(record, questionMap.get(record.getQuestionId())))
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) {
        if (id == null) {
            throw new BusinessException("answer record id must not be null");
        }
        AnswerRecord record = answerRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("Answer record does not exist: " + id);
        }
        answerRecordMapper.deleteById(id);
        return true;
    }

    private void validateUserAndCourse(Long userId, Long courseId) {
        if (sysUserMapper.selectById(userId) == null) {
            throw new BusinessException("User does not exist: " + userId);
        }
        if (courseMapper.selectById(courseId) == null) {
            throw new BusinessException("Course does not exist: " + courseId);
        }
    }

    private Question getQuestionForCourse(Long questionId, Long courseId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException("Question does not exist: " + questionId);
        }
        if (!courseId.equals(question.getCourseId())) {
            throw new BusinessException("Question does not belong to course: " + courseId);
        }
        return question;
    }

    private String normalizeAnswer(String answer) {
        return StringUtils.hasText(answer) ? answer.trim().toUpperCase(Locale.ROOT) : "";
    }

    private void sendLearningEvent(AnswerRecord record) {
        LearningEventMessage message = new LearningEventMessage();
        message.setUserId(record.getUserId());
        message.setCourseId(record.getCourseId());
        message.setEventType(RocketMqConstant.TAG_ANSWER);
        message.setEventTime(LocalDateTime.now());
        message.setMessageKey("learning-event:" + record.getUserId() + ":" + record.getCourseId()
            + ":ANSWER:" + record.getId() + ":" + UUID.randomUUID());
        learningEventProducer.send(message);
    }

    private void sendAnswerStat(AnswerRecord record) {
        AnswerStatMessage message = new AnswerStatMessage();
        message.setAnswerRecordId(record.getId());
        message.setUserId(record.getUserId());
        message.setCourseId(record.getCourseId());
        message.setQuestionId(record.getQuestionId());
        message.setCorrect(Integer.valueOf(1).equals(record.getCorrect()));
        message.setAnswerTime(LocalDateTime.now());
        answerStatProducer.send(message);
    }

    private AnswerResultVO buildResult(AnswerRecord record, Question question, boolean correct) {
        AnswerResultVO vo = new AnswerResultVO();
        vo.setAnswerRecordId(record.getId());
        vo.setQuestionId(record.getQuestionId());
        vo.setQuestionContent(question.getQuestionContent());
        vo.setOptionA(question.getOptionA());
        vo.setOptionB(question.getOptionB());
        vo.setOptionC(question.getOptionC());
        vo.setOptionD(question.getOptionD());
        vo.setUserAnswer(record.getUserAnswer());
        vo.setCorrectAnswer(question.getAnswer());
        vo.setCorrect(correct);
        vo.setAnalysis(question.getAnalysis());
        return vo;
    }

    private WrongQuestionVO toWrongQuestionVO(AnswerRecord record, Question question) {
        WrongQuestionVO vo = new WrongQuestionVO();
        vo.setAnswerRecordId(record.getId());
        vo.setQuestionId(record.getQuestionId());
        vo.setCourseId(record.getCourseId());
        vo.setUserAnswer(record.getUserAnswer());
        vo.setAnswerTime(record.getCreateTime());
        if (question != null) {
            vo.setKnowledgePoint(question.getKnowledgePoint());
            vo.setQuestionContent(question.getQuestionContent());
            vo.setOptionA(question.getOptionA());
            vo.setOptionB(question.getOptionB());
            vo.setOptionC(question.getOptionC());
            vo.setOptionD(question.getOptionD());
            vo.setCorrectAnswer(question.getAnswer());
            vo.setAnalysis(question.getAnalysis());
        }
        return vo;
    }
}
