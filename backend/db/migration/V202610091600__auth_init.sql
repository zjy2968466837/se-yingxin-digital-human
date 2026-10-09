-- auth-service：初始表结构（一期）
-- 依据 docs/数据建模.md 首次生成（24 张表；裁剪与合并见《需求分析文档》§8.4.1）；后续结构变更请在本目录新增迁移脚本，并同步更新该文档
-- 纪律：禁止物理外键、表名字段名全小写、id 为雪花 ID（非自增）、deleted 为 BIGINT（0=未删，删除写行 id）

-- 本期按《需求分析文档》§8.4.1 裁剪，未建：auth_login_log

-- auth_user
CREATE TABLE `auth_user` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `student_no` VARCHAR(32) COMMENT '学号，与 openid 至少一个非空',
  `openid` VARCHAR(64) COMMENT '微信 openid，唯一',
  `unionid` VARCHAR(64) COMMENT '微信 unionid，有则唯一',
  `real_name` VARCHAR(32) COMMENT '姓名（敏感，脱敏展示）',
  `phone` VARCHAR(20) COMMENT '手机号（加密存储）',
  `id_card` VARCHAR(255) COMMENT '身份证号（密文存储，仅实名核验与审计使用，禁止出接口与进日志）',
  `password_hash` VARCHAR(100) COMMENT '仅管理员/特殊账号用；BCrypt',
  `status` TINYINT NOT NULL COMMENT '1 正常 / 2 禁用 / 3 未激活',
  `last_login_time` DATETIME(3) COMMENT '最近登录时间',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='auth_user';

-- auth_identity
CREATE TABLE `auth_identity` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `user_id` BIGINT NOT NULL COMMENT '唯一',
  `edu_level` TINYINT NOT NULL COMMENT '1 本科 / 2 研究生 / 3 其他',
  `college` VARCHAR(64) COMMENT '学院',
  `major` VARCHAR(64) COMMENT '专业',
  `class_name` VARCHAR(64) COMMENT '班级',
  `dorm_building` VARCHAR(32) COMMENT '宿舍楼栋',
  `dorm_room` VARCHAR(32) COMMENT '宿舍房间',
  `batch_id` BIGINT NOT NULL COMMENT '报到批次，引用 admin_config 中的批次定义',
  `admission_type` TINYINT COMMENT '1 统招 / 2 专项 / 3 其他（影响迎新清单）',
  `source` TINYINT NOT NULL COMMENT '1 学校身份源 / 2 模拟数据（C-6、A-2）',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_college_batch` (`college`, `batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='auth_identity';

-- auth_push_record
CREATE TABLE `auth_push_record` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `user_id` BIGINT NOT NULL COMMENT '接收人',
  `channel` TINYINT NOT NULL COMMENT '1 站内信 / 2 公众号模板消息',
  `title` VARCHAR(64) NOT NULL COMMENT '推送内容',
  `content` VARCHAR(512) NOT NULL COMMENT '推送内容',
  `biz_type` TINYINT COMMENT '关联业务（如报到步骤）',
  `biz_id` BIGINT COMMENT '关联业务（如报到步骤）',
  `status` TINYINT NOT NULL COMMENT '1 待发 / 2 成功 / 3 失败',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '默认 0，失败重试计数',
  `fail_reason` VARCHAR(255) COMMENT '失败原因',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='auth_push_record';

