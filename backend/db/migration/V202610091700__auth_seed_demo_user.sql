-- 演示账号（仅用于本地/答辩环境跑通登录链路，云端部署前请删除或改口令）
-- 学号 20260001 / 口令 123456（BCrypt 散列，见数据建模 §3.1：password_hash 用 BCrypt）
-- 说明：这是种子数据，不属于表结构变更；如需清掉执行
--       DELETE FROM auth_user WHERE student_no = '20260001';
INSERT INTO `auth_user` (`id`, `student_no`, `openid`, `unionid`, `real_name`, `phone`, `id_card`,
                         `password_hash`, `status`, `last_login_time`,
                         `create_time`, `update_time`, `create_by`, `update_by`, `deleted`)
VALUES (1000000000000000001, '20260001', NULL, NULL, '演示学生', NULL, NULL,
        '$2a$10$1EmxEVy69Cl38R9iy0sqHOnjba4M64z/0yNeX7FJA3HfKOHzipBuC', 1, NULL,
        NOW(3), NOW(3), 0, 0, 0);
