-- life-service：初始表结构（一期）
-- 依据 docs/数据建模.md 首次生成（24 张表；裁剪与合并见《需求分析文档》§8.4.1）；后续结构变更请在本目录新增迁移脚本，并同步更新该文档
-- 纪律：禁止物理外键、表名字段名全小写、id 为雪花 ID（非自增）、deleted 为 BIGINT（0=未删，删除写行 id）

-- 本期按《需求分析文档》§8.4.1 裁剪，未建：life_repair_log

-- life_repair_order
CREATE TABLE `life_repair_order` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `order_no` VARCHAR(32) NOT NULL COMMENT '单号，唯一',
  `user_id` BIGINT NOT NULL COMMENT '报修人',
  `dorm_location` VARCHAR(64) NOT NULL COMMENT '楼栋+房间',
  `category` TINYINT NOT NULL COMMENT '1 水电 / 2 家具 / 3 网络 / 4 门窗 / 5 其他',
  `description` VARCHAR(512) COMMENT '故障描述',
  `images` JSON COMMENT '照片地址数组（最多 3 张）',
  `status` TINYINT NOT NULL COMMENT '1 待受理 / 2 处理中 / 3 已完成 / 4 已取消',
  `handler_id` BIGINT COMMENT '处理人',
  `finish_time` DATETIME(3) COMMENT '完成时间',
  `external_order_no` VARCHAR(64) COMMENT '后勤系统单号（对接失败时为空，IR-2.9）',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='life_repair_order';

-- life_service_info
CREATE TABLE `life_service_info` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `category` TINYINT NOT NULL COMMENT '1 洗衣机/热水 / 2 门禁 / 3 食堂 / 4 校园网 / 5 营业厅快递 / 6 就医医保 / 7 心理中心 / 8 FAQ',
  `title` VARCHAR(128) NOT NULL COMMENT '标题',
  `content` MEDIUMTEXT NOT NULL COMMENT '说明内容（支持图文）',
  `location` VARCHAR(128) COMMENT '地点',
  `poi_code` VARCHAR(32) COMMENT '关联导航点位（引用 poi.json 的 poi_code，可跳转 F6.7）',
  `phone` VARCHAR(32) COMMENT '一键拨号号码（F6.9）',
  `open_hours` VARCHAR(64) COMMENT '营业/开放时间',
  `extra` JSON COMMENT '分类特有信息（如套餐、菜单）',
  `status` TINYINT NOT NULL COMMENT '1 启用 / 2 停用',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_category_status` (`category`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='life_service_info';

