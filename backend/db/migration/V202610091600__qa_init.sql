-- qa-service：初始表结构（一期）
-- 依据 docs/数据建模.md 首次生成（24 张表）；后续结构变更请在本目录新增迁移脚本，并同步更新该文档
-- 纪律：禁止物理外键、表名字段名全小写、id 为雪花 ID（非自增）、deleted 为 BIGINT（0=未删，删除写行 id）

-- qa_question
CREATE TABLE `qa_question` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `question` VARCHAR(255) NOT NULL COMMENT '标准问法',
  `answer` MEDIUMTEXT NOT NULL COMMENT '答案正文',
  `answer_type` TINYINT NOT NULL COMMENT '1 纯文本 / 2 富媒体',
  `category` VARCHAR(32) NOT NULL COMMENT '知识域（对应意图分类）',
  `tags` VARCHAR(255) COMMENT '标签，逗号分隔或 JSON',
  `synonyms` TEXT COMMENT '同义词/口语化表达（F3.4）',
  `media` JSON COMMENT '富媒体块定义（图/流程图/视频/点位，F3.7）',
  `status` TINYINT NOT NULL COMMENT '1 已发布 / 2 草稿 / 3 停用',
  `hit_count` BIGINT NOT NULL DEFAULT 0 COMMENT '命中次数（默认 0）',
  `version` INT NOT NULL COMMENT '内容版本号，用于缓存失效',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_category_status` (`category`, `status`),
  FULLTEXT KEY `idx_question` (`question`, `synonyms`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答条目';

-- qa_session
CREATE TABLE `qa_session` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `user_id` BIGINT NOT NULL COMMENT '学生',
  `channel` TINYINT NOT NULL COMMENT '1 学生端网页 / 2 管理后台',
  `start_time` DATETIME(3) NOT NULL COMMENT '会话开始',
  `end_time` DATETIME(3) COMMENT '会话结束',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息数，默认 0',
  `satisfied` TINYINT COMMENT '满意度（可空）',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话';

-- qa_message
CREATE TABLE `qa_message` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `session_id` BIGINT NOT NULL COMMENT '所属会话',
  `role` TINYINT NOT NULL COMMENT '1 用户 / 2 数字人',
  `content` TEXT NOT NULL COMMENT '消息内容，加密存储',
  `content_type` TINYINT NOT NULL COMMENT '1 文本 / 2 富媒体',
  `question_id` BIGINT COMMENT '命中的问答条目（未命中为空）',
  `confidence` DECIMAL(5,2) COMMENT '置信度 0–100',
  `llm_used` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否走了 LLM 兜底，默认 0',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息';

-- qa_unanswered
CREATE TABLE `qa_unanswered` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `question` VARCHAR(255) NOT NULL COMMENT '原始问题',
  `session_id` BIGINT COMMENT '来源',
  `user_id` BIGINT COMMENT '来源',
  `confidence` DECIMAL(5,2) COMMENT '最高置信度',
  `status` TINYINT NOT NULL COMMENT '1 待处理 / 2 已转人工 / 3 已补录问答库 / 4 已忽略',
  `handler_id` BIGINT COMMENT '处理人（志愿者/辅导员，ACT-3）',
  `handle_note` VARCHAR(255) COMMENT '处理说明',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`status`, `create_time`),
  KEY `idx_user` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='未解答记录';

-- qa_ask_log
CREATE TABLE `qa_ask_log` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `user_id` BIGINT COMMENT '匿名访问时为空',
  `question_hash` CHAR(64) NOT NULL COMMENT '问题指纹（SHA-256 十六进制 64 位，定长），不存原文避免重复存敏感信息',
  `hit_type` TINYINT NOT NULL COMMENT '1 缓存命中 / 2 问答库命中 / 3 LLM 兜底 / 4 未命中',
  `cost_ms` INT NOT NULL COMMENT '响应耗时（NFR-1.1 度量）',
  `session_id` BIGINT COMMENT '关联会话',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提问日志';

