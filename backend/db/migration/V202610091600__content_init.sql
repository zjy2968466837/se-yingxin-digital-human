-- content-service：初始表结构（一期）
-- 依据 docs/数据建模.md 首次生成（24 张表；裁剪与合并见《需求分析文档》§8.4.1）；后续结构变更请在本目录新增迁移脚本，并同步更新该文档
-- 纪律：禁止物理外键、表名字段名全小写、id 为雪花 ID（非自增）、deleted 为 BIGINT（0=未删，删除写行 id）

-- 本期按《需求分析文档》§8.4.1 裁剪，未建：content_club、content_file、content_learning_record

-- content_article
CREATE TABLE `content_article` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `module` TINYINT NOT NULL COMMENT '1 安全教育 / 2 入学教育 / 3 校规校纪 / 4 军训安排 / 5 招新信息',
  `title` VARCHAR(128) NOT NULL COMMENT '标题',
  `summary` VARCHAR(255) COMMENT '摘要（列表页）',
  `content` MEDIUMTEXT NOT NULL COMMENT '正文',
  `cover_url` VARCHAR(255) COMMENT '封面图',
  `source` VARCHAR(128) COMMENT '来源（NFR 可注明出处）',
  `push_stage` TINYINT COMMENT '推送阶段（报到前/中/后，F7.2）',
  `start_time` DATETIME(3) COMMENT '有效期（招新、军训用）',
  `end_time` DATETIME(3) COMMENT '有效期（招新、军训用）',
  `poi_code` VARCHAR(32) COMMENT '关联地点（引用 poi.json 的 poi_code，招新/军训地点，可跳导航）',
  `status` TINYINT NOT NULL COMMENT '1 草稿 / 2 已发布 / 3 已下线',
  `publish_time` DATETIME(3) COMMENT '发布时间（F9.5 定时发布）',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_module_status` (`module`, `status`),
  KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容条目';

