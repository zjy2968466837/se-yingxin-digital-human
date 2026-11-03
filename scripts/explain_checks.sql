-- =============================================================================
-- 执行计划核对脚本（对应《MySQL存储引擎与执行计划》§5）
--
-- 用途：在本地/云端任一套已建表的 yingxin 库上复跑本文演示的查询，核对
--       type / key / rows / Extra 是否与文档一致。只读，不修改任何数据。
--
-- 用法（Windows CMD 或 Git Bash 均可）：
--   docker exec -i yingxin-mysql mysql -uroot -proot123 --default-character-set=utf8mb4 \
--       yingxin < scripts/explain_checks.sql
--   mysql -h127.0.0.1 -P3306 -uyingxin -p --default-character-set=utf8mb4 \
--       yingxin < scripts/explain_checks.sql
--
-- 注意：
--   1) 必须带 --default-character-set=utf8mb4（文件里 Q4 含中文检索词）。若客户端默认是
--      latin1/gbk，中文会变成 '?'，全文检索命中 0 行而不是文档里的 100 行；下面的 SET NAMES
--      只是兜底，命令行参数才是权威。
--   2) 空库上执行也能跑，但 rows 估算没有参考价值；要复现文档中的数字，
--      先按《MySQL存储引擎与执行计划》附录 A 灌入同规模合成数据。
--   3) EXPLAIN 不会真正执行语句；EXPLAIN ANALYZE 会执行（本文只对 SELECT 用）。
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- Q0 环境核对：引擎、字符集、行格式、页大小、独立表空间
-- ---------------------------------------------------------------------------
SELECT @@version AS version, @@default_storage_engine AS engine,
       @@character_set_server AS charset, @@collation_server AS collation,
       @@innodb_page_size AS page_size, @@innodb_file_per_table AS file_per_table,
       @@innodb_buffer_pool_size / 1024 / 1024 AS buffer_pool_mb,
       @@innodb_flush_log_at_trx_commit AS flush_log_at_trx_commit,
       @@transaction_isolation AS isolation, @@lower_case_table_names AS lctn;

SELECT engine, row_format, table_collation, COUNT(*) AS tables
FROM information_schema.tables
WHERE table_schema = 'yingxin'
GROUP BY engine, row_format, table_collation;

-- 物理外键数量必须是 0
SELECT COUNT(*) AS foreign_keys
FROM information_schema.referential_constraints
WHERE constraint_schema = 'yingxin';

-- ---------------------------------------------------------------------------
-- Q1 学号登录：唯一索引等值查询，期望 type=const、key=uk_student_no、rows=1
-- ---------------------------------------------------------------------------
EXPLAIN SELECT id, password_hash, status
FROM auth_user
WHERE student_no = '00001234' AND deleted = 0;

-- ---------------------------------------------------------------------------
-- Q2 学生报到进度：组合唯一索引前缀等值，期望 type=ref、key=uk_user_step
-- ---------------------------------------------------------------------------
EXPLAIN SELECT step_code, status
FROM checkin_progress
WHERE user_id = 1000123 AND deleted = 0;

-- ---------------------------------------------------------------------------
-- Q3 按步骤统计：期望 key=idx_step_status、Extra 含 Using index（覆盖索引）
-- ---------------------------------------------------------------------------
EXPLAIN SELECT step_code, status, COUNT(*)
FROM checkin_progress
WHERE step_code = 'S3'
GROUP BY step_code, status;

-- ---------------------------------------------------------------------------
-- Q4 问答全文检索（ngram）：期望 type=fulltext、key=idx_question
--     rows 估算恒为 1 左右，不可信；真实命中行数看 EXPLAIN ANALYZE
-- ---------------------------------------------------------------------------
EXPLAIN SELECT id, question
FROM qa_question
WHERE MATCH(question, synonyms) AGAINST('被子' IN BOOLEAN MODE) AND status = 1;

-- ---------------------------------------------------------------------------
-- Q5 会话消息倒序取 20 条：期望 type=ref、Backward index scan、无 filesort
-- ---------------------------------------------------------------------------
EXPLAIN SELECT id, role, content, create_time
FROM qa_message
WHERE session_id = 3100001
ORDER BY create_time DESC
LIMIT 20;

-- ---------------------------------------------------------------------------
-- Q6 反例：低基数列单独过滤（status 只有 3 个取值）
--     期望 type=index、扫描全表行数；说明"低基数列不建单列索引"的取舍
-- ---------------------------------------------------------------------------
EXPLAIN SELECT id, user_id
FROM checkin_progress
WHERE status = 1;

-- ---------------------------------------------------------------------------
-- Q7 反例：深翻页（LIMIT 100000,20）vs 游标翻页
--     前者 ALL + filesort；后者 range + 索引有序
-- ---------------------------------------------------------------------------
EXPLAIN SELECT * FROM qa_ask_log ORDER BY create_time LIMIT 100000, 20;

EXPLAIN SELECT * FROM qa_ask_log
WHERE create_time > '2026-09-20 00:00:00'
ORDER BY create_time
LIMIT 20;

-- ---------------------------------------------------------------------------
-- Q8 跨表逻辑关联（库内无外键，但查询仍会 join）：
--     qa_session 驱动、qa_message 被驱动；ORDER BY 跨表列会产生
--     Using temporary; Using filesort
-- ---------------------------------------------------------------------------
EXPLAIN SELECT s.id AS session_id, m.id AS msg_id, m.content
FROM qa_session s
JOIN qa_message m ON m.session_id = s.id
WHERE s.user_id = 1000123
ORDER BY m.create_time DESC
LIMIT 20;

-- ---------------------------------------------------------------------------
-- Q9 埋点看板聚合：组合索引列序与访问方式不匹配（按 event_time 过滤、
--     按 event_type 分组），期望 type=index、扫整个 idx_type_time
-- ---------------------------------------------------------------------------
EXPLAIN SELECT event_type, COUNT(*)
FROM admin_behavior_event
WHERE event_time >= '2026-09-01 00:00:00' AND event_time < '2026-09-08 00:00:00'
GROUP BY event_type;

-- ---------------------------------------------------------------------------
-- Q10 真实耗时：EXPLAIN ANALYZE（会执行，仅用于 SELECT）
-- ---------------------------------------------------------------------------
EXPLAIN ANALYZE
SELECT step_code, status FROM checkin_progress
WHERE user_id = 1000123 AND deleted = 0;

EXPLAIN ANALYZE
SELECT id, question FROM qa_question
WHERE MATCH(question, synonyms) AGAINST('被子' IN BOOLEAN MODE) AND status = 1;

-- ---------------------------------------------------------------------------
-- Q11 存储结构盘点：每表数据/索引页数、碎片
-- ---------------------------------------------------------------------------
SELECT table_name,
       table_rows,
       ROUND(avg_row_length) AS avg_row_bytes,
       ROUND(data_length / 1024 / 1024, 2) AS data_mb,
       ROUND(index_length / 1024 / 1024, 2) AS index_mb,
       ROUND(data_free / 1024 / 1024, 2) AS free_mb
FROM information_schema.tables
WHERE table_schema = 'yingxin'
ORDER BY (data_length + index_length) DESC;
