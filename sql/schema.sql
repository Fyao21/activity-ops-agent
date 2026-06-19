CREATE DATABASE IF NOT EXISTS edu_agent
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE edu_agent;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码',
    role VARCHAR(32) NOT NULL COMMENT '角色 STUDENT/TEACHER/ADMIN',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
    course_name VARCHAR(128) NOT NULL COMMENT '课程名称',
    teacher_id BIGINT NOT NULL COMMENT '授课教师ID',
    description TEXT COMMENT '课程描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_teacher (teacher_id),
    KEY idx_status (status),
    CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(32) NOT NULL COMMENT '文件类型',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态 0待处理 1成功 2失败',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '切片数量',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_course (course_id),
    KEY idx_status (status),
    KEY idx_course_status (course_id, status),
    CONSTRAINT fk_knowledge_document_course FOREIGN KEY (course_id) REFERENCES course (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '片段ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    chunk_index INT NOT NULL COMMENT '片段序号',
    content TEXT NOT NULL COMMENT '片段内容',
    vector_id VARCHAR(128) DEFAULT NULL COMMENT '向量库ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_document_chunk_index (document_id, chunk_index),
    KEY idx_document (document_id),
    KEY idx_course (course_id),
    KEY idx_vector_id (vector_id),
    CONSTRAINT fk_knowledge_chunk_document FOREIGN KEY (document_id) REFERENCES knowledge_document (id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_chunk_course FOREIGN KEY (course_id) REFERENCES course (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库切片表';

CREATE TABLE IF NOT EXISTS agent_qa_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    course_id BIGINT DEFAULT NULL COMMENT '课程ID',
    question TEXT NOT NULL COMMENT '用户问题',
    route_type VARCHAR(32) DEFAULT NULL COMMENT '路由类型 SQL/RAG/HYBRID',
    generated_sql TEXT COMMENT '生成SQL',
    retrieved_context TEXT COMMENT '检索上下文',
    answer TEXT COMMENT '最终回答',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功 1成功 0失败',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_time (user_id, create_time),
    KEY idx_course_time (course_id, create_time),
    KEY idx_route_type (route_type),
    CONSTRAINT fk_agent_qa_record_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_agent_qa_record_course FOREIGN KEY (course_id) REFERENCES course (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent问答记录表';

CREATE TABLE IF NOT EXISTS learning_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '事件ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    event_type VARCHAR(64) NOT NULL COMMENT '行为类型 QUESTION/ANSWER/VIEW_COURSE/UPLOAD_DOC',
    event_time DATETIME NOT NULL COMMENT '行为时间',
    message_key VARCHAR(128) DEFAULT NULL COMMENT 'RocketMQ消息唯一键',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_message_key (message_key),
    KEY idx_user_course (user_id, course_id),
    KEY idx_course_time (course_id, event_time),
    KEY idx_event_type_time (event_type, event_time),
    CONSTRAINT fk_learning_event_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_learning_event_course FOREIGN KEY (course_id) REFERENCES course (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习行为记录表';

CREATE TABLE IF NOT EXISTS question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    knowledge_point VARCHAR(128) COMMENT '知识点',
    question_content TEXT NOT NULL COMMENT '题目内容',
    option_a VARCHAR(255) NOT NULL COMMENT '选项A',
    option_b VARCHAR(255) NOT NULL COMMENT '选项B',
    option_c VARCHAR(255) NOT NULL COMMENT '选项C',
    option_d VARCHAR(255) NOT NULL COMMENT '选项D',
    answer VARCHAR(16) NOT NULL COMMENT '正确答案 A/B/C/D',
    analysis TEXT COMMENT '解析',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_course (course_id),
    KEY idx_knowledge_point (knowledge_point),
    CONSTRAINT fk_question_course FOREIGN KEY (course_id) REFERENCES course (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

CREATE TABLE IF NOT EXISTS answer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '答题记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    user_answer VARCHAR(255) COMMENT '用户答案',
    correct TINYINT NOT NULL COMMENT '是否正确 1正确 0错误',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_course (user_id, course_id),
    KEY idx_question (question_id),
    KEY idx_course_correct_time (course_id, correct, create_time),
    KEY idx_user_correct_time (user_id, correct, create_time),
    CONSTRAINT fk_answer_record_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_answer_record_course FOREIGN KEY (course_id) REFERENCES course (id),
    CONSTRAINT fk_answer_record_question FOREIGN KEY (question_id) REFERENCES question (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生答题记录表';
