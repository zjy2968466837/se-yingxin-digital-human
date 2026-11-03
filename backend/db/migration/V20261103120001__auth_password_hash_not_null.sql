-- auth-service：口令全量必填（2026-11-03 决议，取代"仅管理员/特殊账号用"）
-- 依据：docs/数据建模.md §8.2.1、docs/需求分析文档.md DR-1 与 §7.1.4
-- 全量账号（学生 / 管理员 / 本地模拟账号 / 微信首登建号）一律存 BCrypt 散列（随机盐内置），
-- 口令原文不入库、不进日志、不出接口；本脚本只收紧非空约束，不改字段类型与长度。
-- 前置条件：auth_user 尚无业务数据（当前无持久化代码），执行前确认不存在 password_hash IS NULL 的行。

ALTER TABLE `auth_user`
  MODIFY COLUMN `password_hash` VARCHAR(100) NOT NULL COMMENT '全量账号必填；BCrypt 散列（随机盐内置），禁止明文与可逆加密';
