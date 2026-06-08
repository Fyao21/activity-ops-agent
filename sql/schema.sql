CREATE DATABASE IF NOT EXISTS activity_agent
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE activity_agent;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码',
    role VARCHAR(32) NOT NULL COMMENT '角色 ADMIN/OPERATOR',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '活动ID',
    activity_name VARCHAR(128) NOT NULL COMMENT '活动名称',
    activity_type VARCHAR(64) NOT NULL COMMENT '活动类型',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status TINYINT NOT NULL COMMENT '状态 0未开始 1进行中 2已结束',
    rule_desc TEXT COMMENT '活动规则描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_status_time (status, start_time, end_time),
    KEY idx_activity_type (activity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

CREATE TABLE IF NOT EXISTS activity_user_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    user_id BIGINT NOT NULL COMMENT '参与用户ID',
    channel VARCHAR(64) NOT NULL COMMENT '参与渠道 APP/H5/WEB',
    participate_status TINYINT NOT NULL COMMENT '参与状态 1成功 0失败',
    participate_time DATETIME NOT NULL COMMENT '参与时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_activity_time (activity_id, participate_time),
    KEY idx_user_activity (user_id, activity_id),
    KEY idx_channel_time (channel, participate_time),
    CONSTRAINT fk_activity_user_record_activity FOREIGN KEY (activity_id) REFERENCES activity (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户参与活动记录表';

CREATE TABLE IF NOT EXISTS reward_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '奖励记录ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    reward_type VARCHAR(64) NOT NULL COMMENT '奖励类型 COUPON/POINT/CASH',
    reward_amount DECIMAL(10,2) NOT NULL COMMENT '奖励数量',
    send_status TINYINT NOT NULL COMMENT '发放状态 0初始化 1成功 2失败',
    fail_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    send_time DATETIME DEFAULT NULL COMMENT '发放时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_activity_status (activity_id, send_status),
    KEY idx_user_activity (user_id, activity_id),
    KEY idx_send_time (send_time),
    CONSTRAINT fk_reward_record_activity FOREIGN KEY (activity_id) REFERENCES activity (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖励发放记录表';

CREATE TABLE IF NOT EXISTS activity_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    participant_count INT NOT NULL DEFAULT 0 COMMENT '参与人数',
    reward_count INT NOT NULL DEFAULT 0 COMMENT '奖励发放数量',
    reward_success_count INT NOT NULL DEFAULT 0 COMMENT '奖励发放成功数量',
    conversion_rate DECIMAL(6,4) NOT NULL DEFAULT 0.0000 COMMENT '转化率',
    retention_rate DECIMAL(6,4) NOT NULL DEFAULT 0.0000 COMMENT '留存率',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_activity_date (activity_id, stat_date),
    KEY idx_stat_date (stat_date),
    CONSTRAINT fk_activity_statistics_activity FOREIGN KEY (activity_id) REFERENCES activity (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动统计表';

CREATE TABLE IF NOT EXISTS agent_qa_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '提问用户ID',
    question TEXT NOT NULL COMMENT '用户问题',
    generated_sql TEXT COMMENT '生成的SQL',
    query_result TEXT COMMENT 'SQL查询结果',
    answer TEXT COMMENT '最终回答',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功 1成功 0失败',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_time (user_id, create_time),
    CONSTRAINT fk_agent_qa_record_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent问答记录表';
