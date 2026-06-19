package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activityagent.common.BusinessException;
import com.example.activityagent.dto.CourseCreateRequest;
import com.example.activityagent.dto.CourseUpdateRequest;
import com.example.activityagent.entity.AgentQaRecord;
import com.example.activityagent.entity.AnswerRecord;
import com.example.activityagent.entity.Course;
import com.example.activityagent.entity.KnowledgeDocument;
import com.example.activityagent.entity.LearningEvent;
import com.example.activityagent.entity.Question;
import com.example.activityagent.mapper.AgentQaRecordMapper;
import com.example.activityagent.mapper.AnswerRecordMapper;
import com.example.activityagent.mapper.CourseMapper;
import com.example.activityagent.mapper.KnowledgeDocumentMapper;
import com.example.activityagent.mapper.LearningEventMapper;
import com.example.activityagent.mapper.QuestionMapper;
import com.example.activityagent.service.CourseService;
import com.example.activityagent.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final String COURSE_INFO_KEY_PREFIX = "course:info:";
    private static final Duration COURSE_CACHE_TTL = Duration.ofMinutes(30);

    private final CourseMapper courseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final QuestionMapper questionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final LearningEventMapper learningEventMapper;
    private final AgentQaRecordMapper agentQaRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseVO create(CourseCreateRequest request) {
        Course course = new Course();
        course.setCourseName(request.getCourseName());
        course.setTeacherId(request.getTeacherId());
        course.setDescription(request.getDescription());
        course.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        courseMapper.insert(course);
        return toVO(course);
    }

    @Override
    public List<CourseVO> list(long pageNum, long pageSize) {
        Page<Course> page = new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100));
        return courseMapper.selectPage(
                page,
                new LambdaQueryWrapper<Course>().orderByDesc(Course::getCreateTime)
            )
            .getRecords()
            .stream()
            .map(this::toVO)
            .toList();
    }

    @Override
    public CourseVO getById(Long id) {
        if (id == null) {
            throw new BusinessException("course id must not be null");
        }

        String cacheKey = buildCourseInfoKey(id);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof CourseVO courseVO) {
            return courseVO;
        }

        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("Course not found");
        }

        CourseVO courseVO = toVO(course);
        // Course detail is read-heavy. Cache it for 30 minutes and evict on update.
        redisTemplate.opsForValue().set(cacheKey, courseVO, COURSE_CACHE_TTL);
        return courseVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseVO update(CourseUpdateRequest request) {
        Course existing = courseMapper.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException("Course not found");
        }

        Course update = new Course();
        update.setId(request.getId());
        update.setCourseName(request.getCourseName());
        update.setTeacherId(request.getTeacherId());
        update.setDescription(request.getDescription());
        update.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        courseMapper.updateById(update);

        // Remove stale course detail cache after updating MySQL.
        redisTemplate.delete(buildCourseInfoKey(request.getId()));
        return getById(request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) {
        if (id == null) {
            throw new BusinessException("course id must not be null");
        }
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("Course not found");
        }

        assertNoCourseDependencies(id);
        courseMapper.deleteById(id);
        redisTemplate.delete(buildCourseInfoKey(id));
        return true;
    }

    private void assertNoCourseDependencies(Long courseId) {
        if (countKnowledgeDocuments(courseId) > 0) {
            throw new BusinessException("Course has knowledge documents, delete documents first");
        }
        if (countQuestions(courseId) > 0) {
            throw new BusinessException("Course has questions, delete questions first");
        }
        if (countAnswerRecords(courseId) > 0) {
            throw new BusinessException("Course has answer records, delete answer records first");
        }
        if (countLearningEvents(courseId) > 0) {
            throw new BusinessException("Course has learning events, delete learning events first");
        }
        if (countAgentQaRecords(courseId) > 0) {
            throw new BusinessException("Course has agent QA records, delete QA records first");
        }
    }

    private Long countKnowledgeDocuments(Long courseId) {
        return knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocument>()
            .eq(KnowledgeDocument::getCourseId, courseId));
    }

    private Long countQuestions(Long courseId) {
        return questionMapper.selectCount(new LambdaQueryWrapper<Question>()
            .eq(Question::getCourseId, courseId));
    }

    private Long countAnswerRecords(Long courseId) {
        return answerRecordMapper.selectCount(new LambdaQueryWrapper<AnswerRecord>()
            .eq(AnswerRecord::getCourseId, courseId));
    }

    private Long countLearningEvents(Long courseId) {
        return learningEventMapper.selectCount(new LambdaQueryWrapper<LearningEvent>()
            .eq(LearningEvent::getCourseId, courseId));
    }

    private Long countAgentQaRecords(Long courseId) {
        return agentQaRecordMapper.selectCount(new LambdaQueryWrapper<AgentQaRecord>()
            .eq(AgentQaRecord::getCourseId, courseId));
    }

    private String buildCourseInfoKey(Long courseId) {
        return COURSE_INFO_KEY_PREFIX + courseId;
    }

    private CourseVO toVO(Course course) {
        CourseVO vo = new CourseVO();
        BeanUtils.copyProperties(course, vo);
        return vo;
    }
}
