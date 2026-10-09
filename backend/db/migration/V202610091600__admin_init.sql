-- admin-service：初始表结构（一期）
-- 依据 docs/数据建模.md 首次生成（24 张表）；后续结构变更请在本目录新增迁移脚本，并同步更新该文档
-- 纪律：禁止物理外键、表名字段名全小写、id 为雪花 ID（非自增）、deleted 为 BIGINT（0=未删，删除写行 id）

-- admin_user
CREATE TABLE `admin_user` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `username` VARCHAR(32) NOT NULL COMMENT '登录名，唯一',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 散列，禁止明文',
  `real_name` VARCHAR(32) NOT NULL COMMENT '姓名',
  `dept` TINYINT NOT NULL COMMENT '1 学工 / 2 后勤 / 3 保卫 / 4 学院 / 5 信息中心（ACT-4）',
  `status` TINYINT NOT NULL COMMENT '1 正常 / 2 禁用',
  `last_login_time` DATETIME(3) COMMENT '最近登录时间',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员';

-- admin_role
CREATE TABLE `admin_role` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `role_code` VARCHAR(32) NOT NULL COMMENT '角色编码，唯一，权限判定以此为准',
  `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `remark` VARCHAR(255) COMMENT '备注',
  `status` TINYINT NOT NULL COMMENT '1 启用 / 2 停用',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色';

-- admin_user_role
CREATE TABLE `admin_user_role` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `admin_user_id` BIGINT NOT NULL COMMENT '管理员',
  `role_id` BIGINT NOT NULL COMMENT '角色',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`admin_user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员角色关联';

-- admin_announcement
CREATE TABLE `admin_announcement` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `title` VARCHAR(128) NOT NULL COMMENT '标题与正文',
  `content` MEDIUMTEXT NOT NULL COMMENT '标题与正文',
  `audience` JSON COMMENT '可见人群（学院/批次），空=全部',
  `publish_time` DATETIME(3) COMMENT '定时发布与撤回（F9.5）',
  `revoke_time` DATETIME(3) COMMENT '定时发布与撤回（F9.5）',
  `status` TINYINT NOT NULL COMMENT '1 草稿 / 2 已发布 / 3 已撤回',
  `publisher_id` BIGINT NOT NULL COMMENT '发布人',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`, `publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告';

-- admin_config
CREATE TABLE `admin_config` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `config_key` VARCHAR(64) NOT NULL COMMENT '配置键，唯一（如 checkin.batch.no，取值字典见 [需求分析文档](需求分析文档.md) §7.1.4）',
  `config_value` TEXT NOT NULL COMMENT '配置值（JSON/字符串）',
  `config_group` VARCHAR(32) NOT NULL COMMENT '分组（批次 / 时间窗口 / 开关）',
  `value_type` TINYINT NOT NULL COMMENT '1 字符串 / 2 数字 / 3 布尔 / 4 JSON',
  `remark` VARCHAR(255) COMMENT '说明',
  `editable` TINYINT(1) NOT NULL COMMENT '是否允许后台修改',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

-- admin_operation_log
CREATE TABLE `admin_operation_log` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `admin_user_id` BIGINT NOT NULL COMMENT '操作人',
  `module` VARCHAR(32) NOT NULL COMMENT '模块与动作（如 qa_question / publish）',
  `action` VARCHAR(32) NOT NULL COMMENT '模块与动作（如 qa_question / publish）',
  `target_type` VARCHAR(32) COMMENT '操作对象',
  `target_id` BIGINT COMMENT '操作对象',
  `detail` TEXT COMMENT '变更摘要（脱敏后）',
  `before_value` JSON COMMENT '变更前后值快照（仅记录被改动字段，脱敏后）；用于审计回溯（NFR-4.6）',
  `after_value` JSON COMMENT '变更前后值快照（仅记录被改动字段，脱敏后）；用于审计回溯（NFR-4.6）',
  `client_ip` VARCHAR(45) COMMENT '来源 IP',
  `result` TINYINT NOT NULL COMMENT '1 成功 / 2 失败',
  `trace_id` VARCHAR(64) COMMENT '链路 id，便于与日志系统关联',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_admin_time` (`admin_user_id`, `create_time`),
  KEY `idx_module_time` (`module`, `create_time`),
  KEY `idx_trace` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

-- admin_behavior_event
CREATE TABLE `admin_behavior_event` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `event_type` TINYINT NOT NULL COMMENT '1 问答 / 2 导航 / 3 报到打卡 / 4 内容阅读 / 5 页面访问',
  `user_id` BIGINT COMMENT '匿名可空',
  `service` VARCHAR(32) COMMENT '来源服务与业务对象',
  `ref_id` BIGINT COMMENT '来源服务与业务对象',
  `channel` TINYINT NOT NULL COMMENT '1 学生端网页 / 2 管理后台',
  `ext` JSON COMMENT '附加维度（学院、批次、耗时等）',
  `event_time` DATETIME(3) NOT NULL COMMENT '事件发生时间（不用 create_time 代替，允许延迟上报）',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_type_time` (`event_type`, `event_time`),
  KEY `idx_user_time` (`user_id`, `event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='埋点事件';

