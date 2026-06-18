USE edu_agent;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE answer_record;
TRUNCATE TABLE question;
TRUNCATE TABLE learning_event;
TRUNCATE TABLE agent_qa_record;
TRUNCATE TABLE knowledge_chunk;
TRUNCATE TABLE knowledge_document;
TRUNCATE TABLE course;
TRUNCATE TABLE sys_user;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO sys_user (id, username, password, role, create_time, update_time) VALUES
(1, 'student001', '123456', 'STUDENT', '2026-06-10 09:00:00', '2026-06-10 09:00:00'),
(2, 'teacher001', '123456', 'TEACHER', '2026-06-10 09:05:00', '2026-06-10 09:05:00'),
(3, 'admin001', '123456', 'ADMIN', '2026-06-10 09:10:00', '2026-06-10 09:10:00');

INSERT INTO course (id, course_name, teacher_id, description, status, create_time, update_time) VALUES
(1, 'Java 后端开发', 2, '覆盖 SpringBoot、JVM、并发编程、接口设计与工程化实践。', 1, '2026-06-10 10:00:00', '2026-06-10 10:00:00'),
(2, 'MySQL 数据库', 2, '覆盖索引、事务、锁、SQL 优化和数据库设计。', 1, '2026-06-10 10:10:00', '2026-06-10 10:10:00'),
(3, 'Redis 实战', 2, '覆盖缓存、数据结构、分布式锁、缓存穿透和缓存一致性。', 1, '2026-06-10 10:20:00', '2026-06-10 10:20:00');

INSERT INTO knowledge_document (id, course_id, file_name, file_type, file_path, status, chunk_count, error_message, create_time, update_time) VALUES
(1, 1, 'Java 后端开发复习资料.md', 'md', 'uploads/knowledge/java-backend.md', 1, 3, NULL, '2026-06-12 09:00:00', '2026-06-12 09:05:00'),
(2, 2, 'MySQL 索引与事务.md', 'md', 'uploads/knowledge/mysql-index-transaction.md', 1, 3, NULL, '2026-06-12 09:10:00', '2026-06-12 09:15:00'),
(3, 3, 'Redis 缓存实战.md', 'md', 'uploads/knowledge/redis-cache.md', 1, 3, NULL, '2026-06-12 09:20:00', '2026-06-12 09:25:00');

INSERT INTO knowledge_chunk (id, document_id, course_id, chunk_index, content, vector_id, create_time) VALUES
(1, 1, 1, 0, 'SpringBoot 通过自动配置、起步依赖和约定优于配置降低 Java Web 项目搭建成本。', 'java-1-0', '2026-06-12 09:05:00'),
(2, 1, 1, 1, 'JVM 垃圾回收需要关注对象可达性、分代收集、停顿时间和吞吐量。', 'java-1-1', '2026-06-12 09:05:00'),
(3, 1, 1, 2, 'Java 并发开发需要理解线程池、锁、CAS、可见性和原子性。', 'java-1-2', '2026-06-12 09:05:00'),
(4, 2, 2, 0, 'MySQL B+Tree 索引适合范围查询和排序，联合索引需要遵守最左前缀原则。', 'mysql-2-0', '2026-06-12 09:15:00'),
(5, 2, 2, 1, '事务 ACID 包括原子性、一致性、隔离性和持久性。', 'mysql-2-1', '2026-06-12 09:15:00'),
(6, 2, 2, 2, 'SQL 优化需要结合执行计划、索引选择性、回表和扫描行数分析。', 'mysql-2-2', '2026-06-12 09:15:00'),
(7, 3, 3, 0, 'Redis 缓存穿透通常由查询不存在的数据导致，可使用布隆过滤器或空值缓存缓解。', 'redis-3-0', '2026-06-12 09:25:00'),
(8, 3, 3, 1, 'Redis 分布式锁需要设置过期时间、唯一标识和释放锁校验。', 'redis-3-1', '2026-06-12 09:25:00'),
(9, 3, 3, 2, '缓存一致性常见方案包括先更新数据库再删除缓存、延迟双删和消息异步重试。', 'redis-3-2', '2026-06-12 09:25:00');

INSERT INTO question (id, course_id, knowledge_point, question_content, answer, analysis, create_time) VALUES
(1, 1, 'SpringBoot 自动配置', 'SpringBoot 自动配置的核心作用是什么？', '简化配置', '自动配置根据依赖和条件装配 Bean，减少重复配置。', '2026-06-13 10:00:00'),
(2, 1, 'JVM 垃圾回收', 'JVM 判断对象是否可回收常用什么分析方法？', '可达性分析', '从 GC Roots 出发不可达的对象通常可被回收。', '2026-06-13 10:05:00'),
(3, 1, 'Java 并发', '线程池的主要作用是什么？', '复用线程', '线程池通过复用线程降低创建和销毁线程的开销。', '2026-06-13 10:10:00'),
(4, 2, 'MySQL 索引', '联合索引使用时应优先遵守什么原则？', '最左前缀原则', '查询条件从联合索引最左列开始匹配，索引利用率更高。', '2026-06-13 10:15:00'),
(5, 2, '事务隔离级别', 'MySQL 默认隔离级别通常是什么？', '可重复读', 'InnoDB 默认隔离级别通常为 REPEATABLE READ。', '2026-06-13 10:20:00'),
(6, 2, '执行计划', 'SQL 优化时常用什么命令查看执行计划？', 'EXPLAIN', 'EXPLAIN 可以查看访问类型、索引使用和扫描行数。', '2026-06-13 10:25:00'),
(7, 3, 'Redis 缓存穿透', '缓存穿透通常可以用什么结构拦截不存在的数据？', '布隆过滤器', '布隆过滤器可以在访问缓存和数据库前过滤明显不存在的 key。', '2026-06-13 10:30:00'),
(8, 3, 'Redis 分布式锁', 'Redis 分布式锁释放时为什么要校验唯一标识？', '避免误删他人锁', '校验唯一标识可以避免客户端释放不属于自己的锁。', '2026-06-13 10:35:00'),
(9, 3, '缓存一致性', '更新数据库后常见的缓存处理方式是什么？', '删除缓存', '先更新数据库再删除缓存是常见一致性方案。', '2026-06-13 10:40:00');

INSERT INTO agent_qa_record (id, user_id, course_id, question, route_type, generated_sql, retrieved_context, answer, success, error_message, create_time) VALUES
(1, 1, 1, 'SpringBoot 自动配置是什么？', 'RAG', NULL, 'SpringBoot 自动配置、起步依赖', 'SpringBoot 自动配置用于根据依赖和条件自动装配 Bean，减少手写配置。', 1, NULL, '2026-06-14 09:00:00'),
(2, 1, 1, 'JVM 垃圾回收机制有哪些重点？', 'RAG', NULL, 'JVM 垃圾回收、可达性分析、分代收集', '重点包括对象可达性、分代收集、停顿时间和吞吐量。', 1, NULL, '2026-06-15 09:20:00'),
(3, 1, 2, 'MySQL 索引为什么能提高查询效率？', 'RAG', NULL, 'B+Tree 索引、联合索引、最左前缀', '索引通过有序结构减少扫描行数，提升过滤、排序和范围查询效率。', 1, NULL, '2026-06-15 10:00:00'),
(4, 1, 2, '统计 MySQL 数据库课程的平均正确率', 'SQL', 'SELECT AVG(correct) FROM answer_record WHERE course_id = 2 LIMIT 100;', NULL, 'MySQL 数据库课程当前正确率约为 50%。', 1, NULL, '2026-06-16 11:00:00'),
(5, 1, 3, 'Redis 缓存穿透是什么？', 'RAG', NULL, 'Redis 缓存穿透、布隆过滤器、空值缓存', '缓存穿透是大量查询不存在数据绕过缓存访问数据库，可用布隆过滤器或空值缓存缓解。', 1, NULL, '2026-06-16 14:00:00'),
(6, 3, NULL, '统计最近 7 天每门课程提问次数', 'SQL', 'SELECT c.course_name, COUNT(*) FROM agent_qa_record r JOIN course c ON r.course_id = c.id WHERE r.create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY c.course_name LIMIT 100;', NULL, '最近 7 天 Java 后端开发 2 次、MySQL 数据库 2 次、Redis 实战 1 次。', 1, NULL, '2026-06-17 09:00:00'),
(7, 3, NULL, '对比不同课程学习活跃度', 'SQL', 'SELECT c.course_name, COUNT(*) FROM learning_event e JOIN course c ON e.course_id = c.id GROUP BY c.course_name LIMIT 100;', NULL, 'Redis 实战活跃度最高，其次是 Java 后端开发和 MySQL 数据库。', 1, NULL, '2026-06-17 10:00:00');

INSERT INTO learning_event (id, user_id, course_id, event_type, event_time, message_key, create_time) VALUES
(1, 1, 1, 'VIEW_COURSE', '2026-06-12 08:30:00', 'learn-1-view-java-202606120830', '2026-06-12 08:30:01'),
(2, 1, 1, 'QUESTION', '2026-06-14 09:00:00', 'learn-1-question-java-202606140900', '2026-06-14 09:00:01'),
(3, 1, 1, 'QUESTION', '2026-06-15 09:20:00', 'learn-1-question-java-202606150920', '2026-06-15 09:20:01'),
(4, 1, 1, 'ANSWER', '2026-06-15 20:00:00', 'learn-1-answer-java-q1-202606152000', '2026-06-15 20:00:01'),
(5, 1, 1, 'ANSWER', '2026-06-16 20:10:00', 'learn-1-answer-java-q2-202606162010', '2026-06-16 20:10:01'),
(6, 1, 2, 'VIEW_COURSE', '2026-06-13 08:40:00', 'learn-1-view-mysql-202606130840', '2026-06-13 08:40:01'),
(7, 1, 2, 'QUESTION', '2026-06-15 10:00:00', 'learn-1-question-mysql-202606151000', '2026-06-15 10:00:01'),
(8, 1, 2, 'QUESTION', '2026-06-16 11:00:00', 'learn-1-question-mysql-202606161100', '2026-06-16 11:00:01'),
(9, 1, 2, 'ANSWER', '2026-06-16 20:20:00', 'learn-1-answer-mysql-q4-202606162020', '2026-06-16 20:20:01'),
(10, 1, 2, 'ANSWER', '2026-06-17 20:30:00', 'learn-1-answer-mysql-q5-202606172030', '2026-06-17 20:30:01'),
(11, 1, 3, 'VIEW_COURSE', '2026-06-14 08:50:00', 'learn-1-view-redis-202606140850', '2026-06-14 08:50:01'),
(12, 1, 3, 'QUESTION', '2026-06-16 14:00:00', 'learn-1-question-redis-202606161400', '2026-06-16 14:00:01'),
(13, 1, 3, 'ANSWER', '2026-06-17 20:40:00', 'learn-1-answer-redis-q7-202606172040', '2026-06-17 20:40:01'),
(14, 1, 3, 'ANSWER', '2026-06-18 09:00:00', 'learn-1-answer-redis-q8-202606180900', '2026-06-18 09:00:01'),
(15, 1, 3, 'ANSWER', '2026-06-18 09:10:00', 'learn-1-answer-redis-q9-202606180910', '2026-06-18 09:10:01'),
(16, 1, 3, 'UPLOAD_DOC', '2026-06-18 09:20:00', 'learn-1-upload-redis-202606180920', '2026-06-18 09:20:01');

INSERT INTO answer_record (id, user_id, course_id, question_id, user_answer, correct, create_time) VALUES
(1, 1, 1, 1, '简化配置', 1, '2026-06-15 20:00:00'),
(2, 1, 1, 2, '引用计数', 0, '2026-06-16 20:10:00'),
(3, 1, 1, 3, '复用线程', 1, '2026-06-16 20:15:00'),
(4, 1, 2, 4, '最左前缀原则', 1, '2026-06-16 20:20:00'),
(5, 1, 2, 5, '读已提交', 0, '2026-06-17 20:30:00'),
(6, 1, 2, 6, 'SHOW TABLES', 0, '2026-06-17 20:35:00'),
(7, 1, 3, 7, '布隆过滤器', 1, '2026-06-17 20:40:00'),
(8, 1, 3, 8, '为了续期', 0, '2026-06-18 09:00:00'),
(9, 1, 3, 9, '删除缓存', 1, '2026-06-18 09:10:00'),
(10, 1, 3, 7, '缓存预热', 0, '2026-06-18 09:15:00');
