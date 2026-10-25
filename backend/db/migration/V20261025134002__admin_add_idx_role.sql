-- admin_user_role 补 idx_role(role_id)：按角色查管理员（角色管理页）
-- 《数据建模》§3.8 已声明该索引，init 脚本（V20261009160001）漏建，本脚本补齐
ALTER TABLE `admin_user_role`
    ADD INDEX `idx_role` (`role_id`);
