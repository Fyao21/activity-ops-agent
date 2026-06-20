# MyBatis-Plus 复习资料

## MyBatis 与 MyBatis-Plus

MyBatis 是半自动 ORM 框架，开发者可以自己编写 SQL，并把查询结果映射到 Java 对象。它比全自动 ORM 更灵活，适合复杂 SQL 和需要精细控制性能的业务系统。MyBatis-Plus 是 MyBatis 的增强工具，在不改变 MyBatis 基础能力的前提下，提供通用 CRUD、条件构造器、分页插件、代码生成、自动填充和逻辑删除等能力。

MyBatis-Plus 的核心价值是减少简单 CRUD 的重复代码。对于单表新增、按 ID 查询、分页查询、条件查询、更新和删除，可以直接使用 BaseMapper 提供的方法。复杂联表查询、复杂统计和性能敏感 SQL 仍然可以使用 XML 或注解手写。

在课程学习助手项目中，课程、知识库文档、题目、答题记录、学习行为、Agent 问答记录都可以用 MyBatis-Plus Mapper 完成基础操作。对于 Text-to-SQL 生成的统计查询，则由 Python 侧直接执行只读 SQL。

## Entity、DTO 和 VO

Entity 通常对应数据库表结构，字段和表字段保持一致。DTO 用于接收前端请求参数，VO 用于返回前端展示结果。区分 Entity、DTO、VO 可以减少层之间的耦合，避免数据库字段直接暴露给前端，也方便做参数校验和响应裁剪。

例如创建课程时，DTO 可以包含 `courseName`、`teacherId`、`description`、`status`；Entity 对应 `course` 表，包含 id、createTime、updateTime；VO 返回给前端时可以只返回需要展示的字段。Agent 问答记录也是类似，数据库可能保存 generatedSql、retrievedContext、errorMessage，但前端未必每个页面都需要展示。

不要直接把请求 JSON 绑定到 Entity 后写库，尤其是存在敏感字段、状态字段或审计字段时。否则用户可能通过传入额外字段修改不该修改的数据。

## BaseMapper 常用方法

BaseMapper 提供了 `insert`、`deleteById`、`updateById`、`selectById`、`selectList`、`selectPage`、`selectCount` 等方法。简单单表操作可以直接使用这些方法，配合 LambdaQueryWrapper 构造条件。

`selectById` 适合主键查询；`selectList` 适合按条件查询列表；`selectPage` 适合分页；`selectCount` 适合判断是否存在或统计数量。删除数据时要谨慎使用无条件 delete，避免误删全表。更新时也要确保条件准确，避免批量更新过多记录。

插入后如果主键是自增 ID，MyBatis-Plus 会把生成的 id 回填到实体对象中。课程资料上传后，先插入 `knowledge_document`，再用回填的 `document.id` 发送 RocketMQ 文档索引消息，就是典型用法。

## LambdaQueryWrapper

LambdaQueryWrapper 用方法引用构造查询条件，例如 `eq(Course::getStatus, 1)`。相比字符串字段名，它能在重构字段名时获得编译期检查，减少拼写错误。常用条件包括 eq、ne、like、ge、le、between、in、orderByDesc、last 等。

使用 Wrapper 时要注意空值判断。很多条件只有参数不为空时才应该加入，例如课程名称搜索、状态筛选、时间范围筛选。MyBatis-Plus 的条件方法通常支持第一个 boolean 参数，例如 `eq(status != null, Course::getStatus, status)`，可以避免写大量 if。

`last` 会直接拼接 SQL 片段，使用时要非常谨慎，不能拼接用户输入，否则可能造成 SQL 注入。分页场景应优先使用分页插件，而不是手动拼接 limit。

## 分页插件

MyBatis-Plus 分页需要配置 MybatisPlusInterceptor 和 PaginationInnerInterceptor。配置后，调用 `selectPage` 会自动生成分页 SQL，并查询总数。分页接口要限制 pageSize 上限，避免用户传入过大页大小导致数据库压力过大。

分页查询常见问题是深分页性能差。例如 `limit 100000, 20` 需要跳过大量记录。优化方式包括基于主键或时间游标翻页、限制最大页码、使用覆盖索引减少回表、对运营后台导出任务改为异步。

课程列表、知识库文档列表都属于典型分页场景。课程详情可以使用 Redis 缓存，因为它是读多写少数据；课程更新后应删除缓存，避免返回旧数据。

## 自动填充

很多表都有 create_time、update_time 字段。可以通过 MyBatis-Plus MetaObjectHandler 做自动填充，插入时填充创建时间和更新时间，更新时刷新更新时间。这样业务代码不需要每次手动设置时间。

如果数据库字段已经设置了 `DEFAULT CURRENT_TIMESTAMP` 和 `ON UPDATE CURRENT_TIMESTAMP`，也可以交给数据库处理。两种方式都可以，但项目中要保持一致。数据库处理更简单，Java 自动填充更方便在业务层统一控制审计字段。

自动填充不适合所有字段。像文档状态、答题正确与否、问答成功状态这些属于业务语义，应该由业务代码明确设置，而不是自动填充。

## 逻辑删除

逻辑删除是指删除时不真正删除记录，而是把 deleted 字段置为 1。这样可以保留历史数据，方便审计和恢复。MyBatis-Plus 支持逻辑删除配置，查询时会自动过滤已删除记录。

并不是所有表都适合逻辑删除。日志、学习行为、答题记录、问答记录通常更适合保留；中间表或临时表可能可以物理删除。知识库文档删除时，如果 FAISS 中也有向量，就要同时删除向量和数据库切片，否则会出现数据库没有记录但向量库还能检索到旧内容的问题。

使用逻辑删除时要注意唯一索引。例如用户名唯一，如果删除用户后想重新注册同名用户，普通唯一索引会冲突，需要把 deleted 字段纳入唯一索引或使用其他策略。

## 常见面试问题

1. MyBatis 和 MyBatis-Plus 有什么关系？
2. 为什么要区分 Entity、DTO、VO？
3. LambdaQueryWrapper 相比字符串字段名有什么优势？
4. 分页插件如何生效？
5. 深分页为什么慢，怎么优化？
6. 自动填充适合哪些字段？
7. 逻辑删除有什么优缺点？
