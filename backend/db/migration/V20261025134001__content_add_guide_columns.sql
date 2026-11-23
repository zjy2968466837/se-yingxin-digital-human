-- content_article 扩展：承载 F2.9 异常处理指引（DR-24）
-- 原独立表 checkin_exception_guide 已按需求分析文档 §8.4.1 裁剪，改由本表 module=6 复用承载（B-3 决议闭环）
-- 前向兼容：新增列均可空，不影响已有数据；唯一索引末位含 deleted（删除后可重建同类型指引）
ALTER TABLE `content_article`
    ADD COLUMN `guide_type` TINYINT NULL COMMENT '异常指引类型（仅 module=6 使用，其余为 NULL）：1 未缴费 / 2 材料缺失 / 3 暂缓报到 / 4 改期' AFTER `module`,
    ADD COLUMN `contact` VARCHAR(128) NULL COMMENT '联系人及电话（F2.9 指引页展示，可空表示无专属联系人）' AFTER `poi_code`,
    ADD UNIQUE KEY `uk_guide_type` (`module`, `guide_type`, `deleted`);
