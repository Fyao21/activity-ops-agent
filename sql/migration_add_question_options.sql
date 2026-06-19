USE edu_agent;

ALTER TABLE question
    ADD COLUMN option_a VARCHAR(255) NOT NULL DEFAULT '' COMMENT '选项A' AFTER question_content,
    ADD COLUMN option_b VARCHAR(255) NOT NULL DEFAULT '' COMMENT '选项B' AFTER option_a,
    ADD COLUMN option_c VARCHAR(255) NOT NULL DEFAULT '' COMMENT '选项C' AFTER option_b,
    ADD COLUMN option_d VARCHAR(255) NOT NULL DEFAULT '' COMMENT '选项D' AFTER option_c,
    MODIFY COLUMN answer VARCHAR(16) NOT NULL COMMENT '正确答案 A/B/C/D';

UPDATE question
SET option_a = '简化配置',
    option_b = '手动创建所有 Bean',
    option_c = '替代 JVM',
    option_d = '删除配置文件',
    answer = 'A'
WHERE id = 1;

UPDATE question
SET option_a = '引用计数',
    option_b = '可达性分析',
    option_c = '轮询检测',
    option_d = '哈希取模',
    answer = 'B'
WHERE id = 2;

UPDATE question
SET option_a = '复用线程',
    option_b = '关闭所有线程',
    option_c = '替代数据库连接',
    option_d = '只用于排序',
    answer = 'A'
WHERE id = 3;

UPDATE question
SET option_a = '随机匹配原则',
    option_b = '最右前缀原则',
    option_c = '最左前缀原则',
    option_d = '全表扫描原则',
    answer = 'C'
WHERE id = 4;

UPDATE question
SET option_a = '读未提交',
    option_b = '读已提交',
    option_c = '可重复读',
    option_d = '串行化',
    answer = 'C'
WHERE id = 5;

UPDATE question
SET option_a = 'SHOW TABLES',
    option_b = 'EXPLAIN',
    option_c = 'COMMIT',
    option_d = 'ROLLBACK',
    answer = 'B'
WHERE id = 6;

UPDATE question
SET option_a = '跳表',
    option_b = '布隆过滤器',
    option_c = '慢查询日志',
    option_d = 'AOF 文件',
    answer = 'B'
WHERE id = 7;

UPDATE question
SET option_a = '为了续期',
    option_b = '提高内存命中率',
    option_c = '避免误删他人锁',
    option_d = '开启持久化',
    answer = 'C'
WHERE id = 8;

UPDATE question
SET option_a = '删除缓存',
    option_b = '永久保留缓存',
    option_c = '关闭数据库',
    option_d = '只更新本地变量',
    answer = 'A'
WHERE id = 9;

UPDATE answer_record SET user_answer = 'A', correct = 1 WHERE id = 1;
UPDATE answer_record SET user_answer = 'A', correct = 0 WHERE id = 2;
UPDATE answer_record SET user_answer = 'A', correct = 1 WHERE id = 3;
UPDATE answer_record SET user_answer = 'C', correct = 1 WHERE id = 4;
UPDATE answer_record SET user_answer = 'B', correct = 0 WHERE id = 5;
UPDATE answer_record SET user_answer = 'A', correct = 0 WHERE id = 6;
UPDATE answer_record SET user_answer = 'B', correct = 1 WHERE id = 7;
UPDATE answer_record SET user_answer = 'A', correct = 0 WHERE id = 8;
UPDATE answer_record SET user_answer = 'A', correct = 1 WHERE id = 9;
UPDATE answer_record SET user_answer = 'C', correct = 0 WHERE id = 10;
