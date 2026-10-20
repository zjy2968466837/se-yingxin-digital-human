-- 种子数据：开发/演示用账号与基础配置
-- 生产环境请勿直接执行本脚本，或执行后删除测试账号

SET NAMES utf8mb4;

-- 后台管理员测试账号：admin / 123456
INSERT INTO admin_role (id, role_code, role_name, remark, status, create_time, update_time, create_by, update_by, deleted)
VALUES (1000000000000000001, 'SUPER_ADMIN', '超级管理员', '全部功能与数据权限', 1, NOW(3), NOW(3), 0, 0, 0);

INSERT INTO admin_user (id, username, password_hash, real_name, dept, status, last_login_time, create_time, update_time, create_by, update_by, deleted)
VALUES (1000000000000000002, 'admin', '$2a$10$tIJ9kznXMt/iQx.dXviWFeWrDrwcjHIYoGU.1pyI8CkWnPy7Vo/Wa', '系统管理员', 5, 1, NULL, NOW(3), NOW(3), 0, 0, 0);

INSERT INTO admin_user_role (id, admin_user_id, role_id, create_time, update_time, create_by, update_by, deleted)
VALUES (1000000000000000003, 1000000000000000002, 1000000000000000001, NOW(3), NOW(3), 0, 0, 0);

-- 学生测试账号：2024001 / 123456
INSERT INTO auth_user (id, student_no, openid, unionid, real_name, phone, password_hash, status, last_login_time, create_time, update_time, create_by, update_by, deleted)
VALUES (1000000000000000004, '2024001', NULL, NULL, '测试学生', '13800138000', '$2a$10$WqaJwlOQ3jPFA0H6vPgGWuuS9.lrVQgeYnZRZhmfiTE56J1fMaZhq', 1, NULL, NOW(3), NOW(3), 0, 0, 0);

-- 报到批次配置
INSERT INTO admin_config (id, config_key, config_value, config_group, value_type, remark, editable, create_time, update_time, create_by, update_by, deleted)
VALUES (1000000000000000005, 'checkin.batch.2026', '{"year":2026,"startDate":"2026-08-20","endDate":"2026-09-10"}', 'batch', 4, '2026 级迎新报到批次时间窗口', 1, NOW(3), NOW(3), 0, 0, 0);

-- 报到步骤模板
INSERT INTO checkin_step_template (id, step_code, step_name, seq, apply_scope, location, office_hours, contact, enabled, create_time, update_time, create_by, update_by, deleted)
VALUES
    (1000000000000000011, 'PRE_REG',    '网上预报到', 1,  NULL, '线上',       '全天',          NULL,        1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000000012, 'PAY',        '缴纳学杂费', 2,  NULL, '财务处窗口', '08:30-17:30', '028-84480001', 1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000000013, 'DORM',       '宿舍入住',   3,  NULL, '各宿舍楼栋', '08:00-22:00', '宿管值班电话',  1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000000014, 'ARCHIVE',    '档案交接',   4,  NULL, '学院报到处', '08:30-17:30', '辅导员',        1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000000015, 'CERT',       '报到确认',   5,  NULL, '学院报到处', '08:30-17:30', '辅导员',        1, NOW(3), NOW(3), 0, 0, 0);

-- 异常分支指引
INSERT INTO checkin_exception_guide (id, exception_type, title, guide_content, contact, poi_id, create_time, update_time, create_by, update_by, deleted)
VALUES
    (1000000000000000021, 1, '未缴费', '请前往财务处窗口或登录缴费平台完成学杂费缴纳，保留缴费凭证。', '028-84480001', NULL, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000000022, 2, '材料缺失', '请按材料清单补交缺失项，可先办理已完成项，材料补齐后重新核验。', '辅导员', NULL, NOW(3), NOW(3), 0, 0, 0);

-- 问答示例
INSERT INTO qa_question (id, question, answer, answer_type, category, tags, synonyms, media, status, hit_count, version, create_time, update_time, create_by, update_by, deleted)
VALUES
    (1000000000000000031, '报到需要带哪些材料？', '录取通知书、身份证、档案、党团组织关系、一寸照片若干。具体以学院通知为准。', 1, '报到流程', '材料,报到', '入学要带什么,需要准备什么', NULL, 1, 0, 1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000000032, '宿舍在哪里？', '本科生宿舍主要集中在狮子山校区学生公寓区，报到当天可在导航中搜索"宿舍楼"查看路线。', 1, '生活服务', '宿舍,导航', '住哪里,寝室位置', NULL, 1, 0, 1, NOW(3), NOW(3), 0, 0, 0);
