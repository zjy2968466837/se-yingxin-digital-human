SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `yingxin`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yingxin`;

CREATE TABLE IF NOT EXISTS `yx_user_account` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_no` VARCHAR(20) DEFAULT NULL COMMENT '学号，与学校身份源一致',
  `open_id` VARCHAR(64) DEFAULT NULL COMMENT '微信 openid',
  `union_id` VARCHAR(64) DEFAULT NULL COMMENT '微信 unionid',
  `password` VARCHAR(100) DEFAULT NULL COMMENT '密码哈希（BCrypt）',
  `mobile` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `bind_status` TINYINT NOT NULL DEFAULT 0 COMMENT '绑定状态 0未绑定 1已绑定',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态 0禁用 1正常',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  UNIQUE KEY `uk_open_id` (`open_id`),
  KEY `idx_union_id` (`union_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-1 用户账号';

CREATE TABLE IF NOT EXISTS `yx_user_profile` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户账号ID',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '姓名',
  `edu_level` TINYINT DEFAULT NULL COMMENT '培养层次 1本科 2硕士 3博士',
  `college` VARCHAR(60) DEFAULT NULL COMMENT '学院',
  `major` VARCHAR(60) DEFAULT NULL COMMENT '专业',
  `class_name` VARCHAR(60) DEFAULT NULL COMMENT '班级',
  `dorm_building` VARCHAR(30) DEFAULT NULL COMMENT '宿舍楼栋',
  `dorm_room` VARCHAR(20) DEFAULT NULL COMMENT '宿舍房间',
  `checkin_batch` VARCHAR(30) DEFAULT NULL COMMENT '报到批次',
  `cert_type` TINYINT DEFAULT NULL COMMENT '证件类型',
  `cert_no_enc` VARBINARY(255) DEFAULT NULL COMMENT '证件号密文',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_college_batch` (`college`, `checkin_batch`),
  CONSTRAINT `fk_profile_user` FOREIGN KEY (`user_id`) REFERENCES `yx_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-2 身份档案';

CREATE TABLE IF NOT EXISTS `yx_session_token` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户账号ID',
  `token_id` VARCHAR(64) NOT NULL COMMENT '令牌唯一标识 jti',
  `issued_at` DATETIME NOT NULL COMMENT '签发时间',
  `expire_at` DATETIME NOT NULL COMMENT '过期时间',
  `client_type` TINYINT DEFAULT NULL COMMENT '客户端类型 1小程序 2H5 3后台',
  `revoked` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已登出 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_id` (`token_id`),
  KEY `idx_user_expire` (`user_id`, `expire_at`),
  CONSTRAINT `fk_token_user` FOREIGN KEY (`user_id`) REFERENCES `yx_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-3 会话令牌（运行时存于 Redis，本表为令牌元数据留痕）';

CREATE TABLE IF NOT EXISTS `yx_checkin_step` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `step_code` VARCHAR(40) NOT NULL COMMENT '步骤编码',
  `step_name` VARCHAR(60) NOT NULL COMMENT '步骤名称',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `apply_scope` VARCHAR(120) DEFAULT NULL COMMENT '适用人群，逗号分隔',
  `location` VARCHAR(120) DEFAULT NULL COMMENT '办理地点',
  `office_hours` VARCHAR(120) DEFAULT NULL COMMENT '办公时间',
  `contact_phone` VARCHAR(40) DEFAULT NULL COMMENT '联系人电话',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0否 1是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_step_code` (`step_code`),
  KEY `idx_sort` (`sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-4 报到步骤模板';

CREATE TABLE IF NOT EXISTS `yx_checkin_progress` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户账号ID',
  `step_id` BIGINT UNSIGNED NOT NULL COMMENT '步骤模板ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '完成状态 0待完成 1已完成',
  `finish_time` DATETIME DEFAULT NULL COMMENT '完成打卡时间',
  `finish_channel` TINYINT DEFAULT NULL COMMENT '打卡方式 1用户确认 2扫码',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_step` (`user_id`, `step_id`),
  CONSTRAINT `fk_progress_user` FOREIGN KEY (`user_id`) REFERENCES `yx_user_account` (`id`),
  CONSTRAINT `fk_progress_step` FOREIGN KEY (`step_id`) REFERENCES `yx_checkin_step` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-5 报到进度记录';

CREATE TABLE IF NOT EXISTS `yx_checkin_material` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `step_id` BIGINT UNSIGNED NOT NULL COMMENT '所属步骤ID',
  `material_name` VARCHAR(80) NOT NULL COMMENT '材料名称',
  `required` TINYINT NOT NULL DEFAULT 1 COMMENT '是否必需 0否 1是',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '补充说明',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_step` (`step_id`),
  CONSTRAINT `fk_material_step` FOREIGN KEY (`step_id`) REFERENCES `yx_checkin_step` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-6 材料清单项';

CREATE TABLE IF NOT EXISTS `yx_qa_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `question` VARCHAR(500) NOT NULL COMMENT '标准问题',
  `answer` TEXT COMMENT '答案，结构化 block 列表（JSON）',
  `category_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '分类ID',
  `keywords` VARCHAR(500) DEFAULT NULL COMMENT '关键词，逗号分隔',
  `alias` VARCHAR(500) DEFAULT NULL COMMENT '同义说法与别名',
  `answer_type` TINYINT NOT NULL DEFAULT 1 COMMENT '答案类型 1文字 2图片 3流程图 4视频 5地图点位',
  `priority` INT NOT NULL DEFAULT 0 COMMENT '命中优先级',
  `hit_count` INT NOT NULL DEFAULT 0 COMMENT '命中次数',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0否 1是',
  `version` INT NOT NULL DEFAULT 1 COMMENT '内容版本号，用于缓存失效',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_enabled_priority` (`enabled`, `priority`),
  FULLTEXT KEY `ft_question` (`question`, `keywords`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-7 问答条目';

CREATE TABLE IF NOT EXISTS `yx_qa_session` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户账号ID',
  `channel` TINYINT NOT NULL DEFAULT 1 COMMENT '会话渠道 1小程序 2H5 3大屏',
  `msg_count` INT NOT NULL DEFAULT 0 COMMENT '消息条数',
  `last_msg_time` DATETIME DEFAULT NULL COMMENT '最后一条消息时间',
  `closed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已结束 0否 1是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `last_msg_time`),
  CONSTRAINT `fk_qasession_user` FOREIGN KEY (`user_id`) REFERENCES `yx_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-8 问答会话';

CREATE TABLE IF NOT EXISTS `yx_qa_message` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  `role` TINYINT NOT NULL COMMENT '消息角色 1用户 2数字人',
  `content_enc` BLOB COMMENT '消息内容密文',
  `qa_item_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '命中的问答条目ID',
  `confidence` DECIMAL(5,4) DEFAULT NULL COMMENT '置信度',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_time` (`session_id`, `create_time`),
  CONSTRAINT `fk_qamsg_session` FOREIGN KEY (`session_id`) REFERENCES `yx_qa_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-8 问答消息（内容加密存储）';

CREATE TABLE IF NOT EXISTS `yx_qa_unanswered` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '提问用户ID',
  `session_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '所属会话ID',
  `question` VARCHAR(500) NOT NULL COMMENT '未解答问题原文',
  `intent` VARCHAR(80) DEFAULT NULL COMMENT '识别出的意图',
  `confidence` DECIMAL(5,4) DEFAULT NULL COMMENT '最高置信度',
  `handle_status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态 0待处理 1已处理 2已转人工',
  `handler_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人（志愿者或辅导员）',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`handle_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-9 未解答记录';

CREATE TABLE IF NOT EXISTS `yx_qa_hot_cache` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `question_hash` CHAR(32) NOT NULL COMMENT '问题哈希值',
  `qa_item_id` BIGINT UNSIGNED NOT NULL COMMENT '对应问答条目ID',
  `content_version` INT NOT NULL COMMENT '内容版本号，改库时用于失效',
  `hit_count` INT NOT NULL DEFAULT 0 COMMENT '缓存命中次数',
  `expire_at` DATETIME DEFAULT NULL COMMENT '过期时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_hash` (`question_hash`),
  KEY `idx_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-10 热点缓存项（运行时存于 Redis，本表为缓存元数据）';

CREATE TABLE IF NOT EXISTS `yx_nav_poi` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `poi_code` VARCHAR(40) NOT NULL COMMENT '点位编码',
  `poi_name` VARCHAR(80) NOT NULL COMMENT '点位名称',
  `alias` VARCHAR(200) DEFAULT NULL COMMENT '别名，如 校医院/医务室',
  `category` VARCHAR(40) DEFAULT NULL COMMENT '类别，如 报到点/宿舍/食堂',
  `building` VARCHAR(60) DEFAULT NULL COMMENT '所属建筑',
  `floor` VARCHAR(20) DEFAULT NULL COMMENT '楼层',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `intro` VARCHAR(500) DEFAULT NULL COMMENT '地标讲解内容',
  `open_hours` VARCHAR(120) DEFAULT NULL COMMENT '开放时间',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0否 1是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_poi_code` (`poi_code`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-11 POI 数据';

CREATE TABLE IF NOT EXISTS `yx_nav_road_node` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `poi_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联点位ID',
  `node_name` VARCHAR(80) NOT NULL COMMENT '节点名称',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `node_type` TINYINT NOT NULL DEFAULT 1 COMMENT '节点类型 1路口 2室内 3室外点',
  PRIMARY KEY (`id`),
  KEY `idx_poi` (`poi_id`),
  CONSTRAINT `fk_node_poi` FOREIGN KEY (`poi_id`) REFERENCES `yx_nav_poi` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-12 路网节点';

CREATE TABLE IF NOT EXISTS `yx_nav_road_edge` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `from_node_id` BIGINT UNSIGNED NOT NULL COMMENT '起点节点ID',
  `to_node_id` BIGINT UNSIGNED NOT NULL COMMENT '终点节点ID',
  `distance` DECIMAL(10,2) NOT NULL COMMENT '距离（米）',
  `weight` DECIMAL(10,2) NOT NULL DEFAULT 1 COMMENT '路径权重',
  `bidirectional` TINYINT NOT NULL DEFAULT 1 COMMENT '是否双向 0否 1是',
  `walkable` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可步行 0否 1是',
  `bus_line` VARCHAR(60) DEFAULT NULL COMMENT '通勤车线路，为空表示步行道',
  PRIMARY KEY (`id`),
  KEY `idx_from` (`from_node_id`),
  KEY `idx_to` (`to_node_id`),
  CONSTRAINT `fk_edge_from` FOREIGN KEY (`from_node_id`) REFERENCES `yx_nav_road_node` (`id`),
  CONSTRAINT `fk_edge_to` FOREIGN KEY (`to_node_id`) REFERENCES `yx_nav_road_node` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-12 路网边';

CREATE TABLE IF NOT EXISTS `yx_aid_policy` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `policy_name` VARCHAR(120) NOT NULL COMMENT '政策名称，如 国家助学金',
  `policy_type` TINYINT NOT NULL COMMENT '政策类型 1助学金 2助学贷款 3勤工助学 4困难补助 5缓交',
  `apply_condition` VARCHAR(1000) DEFAULT NULL COMMENT '适用条件与匹配规则',
  `material_list` VARCHAR(1000) DEFAULT NULL COMMENT '所需材料清单',
  `process_desc` VARCHAR(1000) DEFAULT NULL COMMENT '办理流程说明',
  `location` VARCHAR(120) DEFAULT NULL COMMENT '办理地点',
  `contact_phone` VARCHAR(40) DEFAULT NULL COMMENT '咨询电话',
  `online_entry` VARCHAR(255) DEFAULT NULL COMMENT '在线办理入口',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0否 1是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`policy_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-13 资助政策';

CREATE TABLE IF NOT EXISTS `yx_aid_application` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_no` VARCHAR(40) NOT NULL COMMENT '申请单号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '申请用户ID',
  `policy_id` BIGINT UNSIGNED NOT NULL COMMENT '申请的政策ID',
  `apply_reason` VARCHAR(1000) DEFAULT NULL COMMENT '申请理由',
  `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '材料附件地址',
  `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态 0待审核 1通过 2驳回',
  `audit_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回理由',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`),
  KEY `idx_user_status` (`user_id`, `audit_status`),
  CONSTRAINT `fk_app_user` FOREIGN KEY (`user_id`) REFERENCES `yx_user_account` (`id`),
  CONSTRAINT `fk_app_policy` FOREIGN KEY (`policy_id`) REFERENCES `yx_aid_policy` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-14 资助申请';

CREATE TABLE IF NOT EXISTS `yx_life_repair` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `repair_no` VARCHAR(40) NOT NULL COMMENT '报修单号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '报修用户ID',
  `dorm_building` VARCHAR(30) DEFAULT NULL COMMENT '楼栋',
  `dorm_room` VARCHAR(20) DEFAULT NULL COMMENT '房间',
  `fault_type` VARCHAR(40) DEFAULT NULL COMMENT '故障类型 水电/家具/网络等',
  `content` VARCHAR(1000) DEFAULT NULL COMMENT '故障描述',
  `photo_url` VARCHAR(500) DEFAULT NULL COMMENT '照片地址，多张逗号分隔',
  `progress` TINYINT NOT NULL DEFAULT 0 COMMENT '处理进度 0待受理 1处理中 2已完成 3已关闭',
  `accept_time` DATETIME DEFAULT NULL COMMENT '受理时间',
  `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_repair_no` (`repair_no`),
  KEY `idx_user_progress` (`user_id`, `progress`),
  CONSTRAINT `fk_repair_user` FOREIGN KEY (`user_id`) REFERENCES `yx_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-15 报修单';

CREATE TABLE IF NOT EXISTS `yx_content_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content_type` TINYINT NOT NULL COMMENT '内容类型 1安全教育 2社团 3入学教育 4军训 5校规 6生活服务',
  `category` VARCHAR(60) DEFAULT NULL COMMENT '分类',
  `body` MEDIUMTEXT COMMENT '正文内容',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图地址',
  `source` VARCHAR(120) DEFAULT NULL COMMENT '内容来源',
  `push_stage` VARCHAR(40) DEFAULT NULL COMMENT '推送阶段，如 预报到/到校日',
  `publish_status` TINYINT NOT NULL DEFAULT 0 COMMENT '发布状态 0未发布 1已发布 2已下线',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '阅读量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`content_type`, `publish_status`),
  KEY `idx_push_stage` (`push_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-16 内容条目';

CREATE TABLE IF NOT EXISTS `yx_admin_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code` VARCHAR(40) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(60) NOT NULL COMMENT '角色名称，如 学工处/后勤/保卫/学院',
  `data_scope` TINYINT NOT NULL DEFAULT 1 COMMENT '数据权限范围 1全部 2本部门 3本学院 4仅本人',
  `menu_perm` VARCHAR(1000) DEFAULT NULL COMMENT '菜单与功能权限编码，逗号分隔',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0否 1是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-18 角色';

CREATE TABLE IF NOT EXISTS `yx_admin_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(60) NOT NULL COMMENT '管理员账号',
  `password` VARCHAR(100) NOT NULL COMMENT '密码哈希（BCrypt）',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '姓名',
  `department` VARCHAR(60) DEFAULT NULL COMMENT '所属部门',
  `mobile` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态 0禁用 1正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_department` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-18 管理员账号';

CREATE TABLE IF NOT EXISTS `yx_admin_user_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `admin_user_id` BIGINT UNSIGNED NOT NULL COMMENT '管理员ID',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`admin_user_id`, `role_id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`admin_user_id`) REFERENCES `yx_admin_user` (`id`),
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `yx_admin_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-18 管理员角色关联（RBAC）';

CREATE TABLE IF NOT EXISTS `yx_admin_announcement` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
  `body` MEDIUMTEXT COMMENT '公告正文',
  `publish_status` TINYINT NOT NULL DEFAULT 0 COMMENT '发布状态 0草稿 1已发布 2已撤回',
  `plan_time` DATETIME DEFAULT NULL COMMENT '定时发布时间',
  `publish_time` DATETIME DEFAULT NULL COMMENT '实际发布时间',
  `publisher_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '发布人ID',
  `top_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶 0否 1是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`publish_status`, `publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-17 公告';

CREATE TABLE IF NOT EXISTS `yx_admin_oper_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `admin_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID',
  `oper_module` VARCHAR(60) DEFAULT NULL COMMENT '操作模块',
  `oper_type` VARCHAR(40) DEFAULT NULL COMMENT '操作类型 新增/修改/删除/审核',
  `target_id` VARCHAR(60) DEFAULT NULL COMMENT '操作对象ID',
  `oper_desc` VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
  `request_ip` VARCHAR(60) DEFAULT NULL COMMENT '请求IP',
  `result` TINYINT NOT NULL DEFAULT 1 COMMENT '操作结果 0失败 1成功',
  `cost_ms` INT DEFAULT NULL COMMENT '耗时（毫秒）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`admin_user_id`, `create_time`),
  KEY `idx_module` (`oper_module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-19 操作日志';

CREATE TABLE IF NOT EXISTS `yx_admin_config` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` VARCHAR(80) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(500) DEFAULT NULL COMMENT '配置值',
  `config_group` VARCHAR(40) DEFAULT NULL COMMENT '配置分组，如 报到批次/时间窗口/开关项',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
  `editable` TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许后台修改 0否 1是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DR-20 系统配置';

SET FOREIGN_KEY_CHECKS = 1;
