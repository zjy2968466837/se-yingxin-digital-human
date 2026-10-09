-- checkin-service：初始表结构（一期）
-- 依据 docs/数据建模.md 首次生成（24 张表）；后续结构变更请在本目录新增迁移脚本，并同步更新该文档
-- 纪律：禁止物理外键、表名字段名全小写、id 为雪花 ID（非自增）、deleted 为 BIGINT（0=未删，删除写行 id）

-- checkin_step_template
CREATE TABLE `checkin_step_template` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `step_code` VARCHAR(32) NOT NULL COMMENT '步骤编码，唯一（如 PRE_REG、PAY）',
  `step_name` VARCHAR(64) NOT NULL COMMENT '步骤名称',
  `seq` INT NOT NULL COMMENT '顺序号，从 1 递增',
  `apply_scope` JSON COMMENT '适用人群（学历/批次/学院），空=全部',
  `location` VARCHAR(128) COMMENT '办理地点',
  `office_hours` VARCHAR(64) COMMENT '办公时间',
  `contact` VARCHAR(64) COMMENT '联系人电话',
  `enabled` TINYINT(1) NOT NULL COMMENT '启停开关',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_step_code` (`step_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报到步骤模板';

-- checkin_material_item
CREATE TABLE `checkin_material_item` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `step_code` VARCHAR(32) NOT NULL COMMENT '归属步骤',
  `name` VARCHAR(64) NOT NULL COMMENT '材料名称（通知书/档案/党团关系/照片/户口）',
  `required` TINYINT(1) NOT NULL COMMENT '是否必需',
  `remark` VARCHAR(255) COMMENT '补交说明',
  `seq` INT NOT NULL COMMENT '展示顺序',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_step` (`step_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='材料清单项';

-- checkin_progress
CREATE TABLE `checkin_progress` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `user_id` BIGINT NOT NULL COMMENT '学生',
  `step_code` VARCHAR(32) NOT NULL COMMENT '步骤',
  `status` TINYINT NOT NULL COMMENT '1 待办 / 2 已完成 / 3 不适用',
  `checkin_time` DATETIME(3) COMMENT '打卡时间',
  `checkin_channel` TINYINT COMMENT '1 自助确认 / 2 现场扫码',
  `operator_id` BIGINT COMMENT '代打卡的经办人',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_step` (`user_id`, `step_code`, `deleted`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_step_status` (`step_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报到进度记录';

-- checkin_certificate
CREATE TABLE `checkin_certificate` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `user_id` BIGINT NOT NULL COMMENT '学生',
  `batch_id` BIGINT NOT NULL COMMENT '报到批次（签发时写入，DI-5「同批次一张有效凭证」的判定依据）',
  `cert_no` VARCHAR(32) NOT NULL COMMENT '凭证编号，唯一',
  `qr_token` VARCHAR(64) NOT NULL COMMENT '二维码令牌（签名值，不可反推个人信息）',
  `issued_time` DATETIME(3) NOT NULL COMMENT '签发时间',
  `expire_time` DATETIME(3) NOT NULL COMMENT '过期时间',
  `verify_status` TINYINT NOT NULL COMMENT '1 未核销 / 2 已核销 / 3 已作废',
  `verify_by` BIGINT COMMENT '核销人与时间',
  `verify_time` DATETIME(3) COMMENT '核销人与时间',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cert_no` (`cert_no`),
  UNIQUE KEY `uk_qr_token` (`qr_token`),
  KEY `idx_user_batch` (`user_id`, `batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报到凭证';

