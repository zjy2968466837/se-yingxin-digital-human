-- aid-service：初始表结构（一期）
-- 依据 docs/数据建模.md 首次生成（24 张表；裁剪与合并见《需求分析文档》§8.4.1）；后续结构变更请在本目录新增迁移脚本，并同步更新该文档
-- 纪律：禁止物理外键、表名字段名全小写、id 为雪花 ID（非自增）、deleted 为 BIGINT（0=未删，删除写行 id）

-- 已合并：aid_application_material → aid_application.materials(JSON)

-- aid_policy
CREATE TABLE `aid_policy` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `name` VARCHAR(128) NOT NULL COMMENT '政策名称',
  `category` TINYINT NOT NULL COMMENT '1 国家助学金 / 2 助学贷款 / 3 勤工助学 / 4 困难补助 / 5 缓交学费 / 6 其他',
  `content` MEDIUMTEXT NOT NULL COMMENT '政策正文',
  `match_rule` JSON COMMENT '适用条件（学历/批次/困难等级），F5.2 匹配用',
  `material_list` TEXT COMMENT '所需材料明细（F5.3）',
  `contact` VARCHAR(64) COMMENT '咨询电话与办理地点（F5.4）',
  `location` VARCHAR(128) COMMENT '咨询电话与办理地点（F5.4）',
  `online_entry` VARCHAR(255) COMMENT '在线办理入口',
  `need_apply` TINYINT(1) NOT NULL COMMENT '是否需要线上申请（区分"仅查询"与"可申请"）',
  `status` TINYINT NOT NULL COMMENT '1 启用 / 2 停用',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_category_status` (`category`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='aid_policy';

-- aid_application
CREATE TABLE `aid_application` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `user_id` BIGINT NOT NULL COMMENT '申请学生',
  `policy_id` BIGINT NOT NULL COMMENT '申请的政策',
  `batch_id` BIGINT NOT NULL COMMENT '报到批次',
  `apply_type` TINYINT NOT NULL COMMENT '1 绿色通道 / 2 缓交学费 / 3 其他',
  `reason` VARCHAR(512) COMMENT '申请理由（敏感，加密）',
  `status` TINYINT NOT NULL COMMENT '1 草稿 / 2 待审核 / 3 已通过 / 4 已驳回 / 5 已撤回',
  `submit_time` DATETIME(3) COMMENT '提交与审核时间',
  `audit_time` DATETIME(3) COMMENT '提交与审核时间',
  `auditor_id` BIGINT COMMENT '审核人（ACT-4）',
  `reject_reason` VARCHAR(255) COMMENT '驳回必填（F5.8 验收准则）',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁，防并发审核覆盖（默认 0）',
  `materials` JSON COMMENT '材料明细（原 aid_application_material 合并）：[{name,url,hash,status}]，见 §2.1 落地范围',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_policy_batch` (`user_id`, `policy_id`, `batch_id`, `deleted`),
  KEY `idx_status_time` (`status`, `create_time`),
  KEY `idx_policy_status` (`policy_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='aid_application';

