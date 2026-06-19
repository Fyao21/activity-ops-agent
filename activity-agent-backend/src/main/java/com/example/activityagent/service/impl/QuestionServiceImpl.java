package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.activityagent.common.BusinessException;
import com.example.activityagent.dto.QuestionCreateRequest;
import com.example.activityagent.entity.AnswerRecord;
import com.example.activityagent.entity.Question;
import com.example.activityagent.mapper.AnswerRecordMapper;
import com.example.activityagent.mapper.CourseMapper;
import com.example.activityagent.mapper.QuestionMapper;
import com.example.activityagent.service.QuestionService;
import com.example.activityagent.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final Set<String> SUPPORTED_ANSWERS = Set.of("A", "B", "C", "D");

    private final CourseMapper courseMapper;
    private final QuestionMapper questionMapper;
    private final AnswerRecordMapper answerRecordMapper;

    @Override
    public QuestionVO create(QuestionCreateRequest request) {
        if (courseMapper.selectById(request.getCourseId()) == null) {
            throw new BusinessException("Course does not exist: " + request.getCourseId());
        }
        String answer = normalizeAnswer(request.getAnswer());
        if (!SUPPORTED_ANSWERS.contains(answer)) {
            throw new BusinessException("answer must be one of A/B/C/D");
        }

        Question question = new Question();
        question.setCourseId(request.getCourseId());
        question.setKnowledgePoint(request.getKnowledgePoint());
        question.setQuestionContent(request.getQuestionContent());
        question.setOptionA(request.getOptionA());
        question.setOptionB(request.getOptionB());
        question.setOptionC(request.getOptionC());
        question.setOptionD(request.getOptionD());
        question.setAnswer(answer);
        question.setAnalysis(request.getAnalysis());
        questionMapper.insert(question);
        return toVO(question);
    }

    @Override
    public List<QuestionVO> list(Long courseId) {
        if (courseId == null) {
            throw new BusinessException("courseId must not be null");
        }
        return questionMapper.selectList(
            new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .orderByDesc(Question::getCreateTime)
        ).stream().map(this::toVO).toList();
    }

    @Override
    public Boolean delete(Long id) {
        if (id == null) {
            throw new BusinessException("question id must not be null");
        }
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException("Question does not exist: " + id);
        }
        Long answerCount = answerRecordMapper.selectCount(
            new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getQuestionId, id)
        );
        if (answerCount != null && answerCount > 0) {
            throw new BusinessException("Question has answer records, delete answer records first");
        }
        questionMapper.deleteById(id);
        return true;
    }

    private QuestionVO toVO(Question question) {
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(question, vo);
        return vo;
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toUpperCase(Locale.ROOT);
    }
}
