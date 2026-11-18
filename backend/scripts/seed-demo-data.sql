-- ============================================================================
-- 演示数据种子脚本（只灌数据，不建表）
--
-- 界碑约定：全部表结构由 Flyway 迁移管理（V202610091600__*_init.sql 与
-- V202610191200__auth_rbac.sql），演示账号/演示数据的种子不进迁移，只放本脚本。
--
-- 依赖迁移：V202610091600__admin_init.sql / auth_init.sql / checkin_init.sql /
--           qa_init.sql，以及 V202610191200__auth_rbac.sql
--           （auth_user.approve_status 列与 auth_user_group 表、freshman 演示组
--            id=1000000000000320002 均来自 auth_rbac 迁移）。
--
-- 公共列口径：main 各表 create_time/update_time 为 DATETIME(3) NOT NULL 无默认值，
-- deleted 为 BIGINT NOT NULL，故所有 INSERT 显式提供
-- create_time=NOW(3)、update_time=NOW(3)、deleted=0（create_by/update_by 可空，省略）。
--
-- 用法（MySQL 容器内执行，数据库名 yingxin）：
--   docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < scripts/seed-demo-data.sql
-- 执行完请失效看板缓存：
--   docker exec yingxin-redis redis-cli INCR yx:admin:dash:version
-- 说明：仅用于课程演示；可重复执行（固定 id + INSERT IGNORE 幂等）。正式环境勿执行。
-- ============================================================================

SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS seed_demo_data $$
CREATE PROCEDURE seed_demo_data()
BEGIN
    DECLARE v_user_id BIGINT;
    DECLARE v_step_count INT;
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_j INT DEFAULT 0;
    DECLARE v_qa_i INT DEFAULT 0;
    DECLARE v_hits INT;
    DECLARE v_hit_type INT;
    DECLARE v_cost INT;
    DECLARE v_minutes_back INT;
    DECLARE v_college INT;
    DECLARE v_question_id BIGINT;
    DECLARE v_hit_inc INT;

    -- 固定 ID 区间（可重复执行，INSERT IGNORE 幂等）
    -- 管理员 1000000000000300001；角色 1000000000000310001；用户角色 1000000000000311001
    -- 学生 1000000000000400001 - 1000000000000400040；用户组关联 1000000000000420001 - 0420040
    -- 身份档案 1000000000000430001 - 0430040；问答条目 1000000000000500001 - 0500012
    -- 打卡进度 1000000000000600000 起；打卡埋点 1000000000000700000 起
    -- 问答日志 1000000000000800000 起；问答埋点 1000000000000900000 起
    -- 未解答 1000000000001000001 / 1000000000001000002

    -- ========================================================================
    -- 1. 管理后台演示账号：admin / 123456（BCrypt），信息中心（dept=5）
    -- ========================================================================
    INSERT IGNORE INTO admin_user
        (id, username, password_hash, real_name, dept, status,
         create_time, update_time, deleted)
    VALUES
        (1000000000000300001, 'admin',
         '$2a$10$tIJ9kznXMt/iQx.dXviWFeWrDrwcjHIYoGU.1pyI8CkWnPy7Vo/Wa',
         '系统管理员', 5, 1, NOW(3), NOW(3), 0);

    INSERT IGNORE INTO admin_role
        (id, role_code, role_name, remark, status,
         create_time, update_time, deleted)
    VALUES
        (1000000000000310001, 'SUPER_ADMIN', '超级管理员', '演示种子：拥有后台全部权限', 1,
         NOW(3), NOW(3), 0);

    INSERT IGNORE INTO admin_user_role
        (id, admin_user_id, role_id, create_time, update_time, deleted)
    VALUES
        (1000000000000311001, 1000000000000300001, 1000000000000310001,
         NOW(3), NOW(3), 0);

    -- ========================================================================
    -- 2. 40 名演示学生 ST2026001 - ST2026040（密码 123456，已审核通过）
    --    approve_status / auth_user_group 依赖 V202610191200__auth_rbac.sql
    -- ========================================================================
    SET v_i = 0;
    WHILE v_i < 40 DO
        INSERT IGNORE INTO auth_user
            (id, student_no, real_name, password_hash, status, approve_status,
             create_time, update_time, deleted)
        VALUES (
            1000000000000400001 + v_i,
            CONCAT('ST2026', LPAD(v_i + 1, 3, '0')),
            CONCAT('演示新生', LPAD(v_i + 1, 2, '0')),
            '$2a$10$tIJ9kznXMt/iQx.dXviWFeWrDrwcjHIYoGU.1pyI8CkWnPy7Vo/Wa',
            1, 1,
            NOW(3), NOW(3), 0);

        -- 关联 freshman 演示组（组 id 与 V202610191200__auth_rbac.sql 种子一致）
        INSERT IGNORE INTO auth_user_group
            (id, group_id, user_id, create_time, update_time, deleted)
        VALUES (
            1000000000000420001 + v_i,
            1000000000000320002,
            1000000000000400001 + v_i,
            NOW(3), NOW(3), 0);
        SET v_i = v_i + 1;
    END WHILE;

    -- ========================================================================
    -- 3. 身份档案：8 个学院 × 5 人，batch_id=2026，source=2 模拟数据
    -- ========================================================================
    SET v_i = 0;
    WHILE v_i < 40 DO
        SET v_college = v_i DIV 5 + 1;   -- 1-8 号学院，每院 5 人
        INSERT IGNORE INTO auth_identity
            (id, user_id, edu_level, college, major, class_name,
             dorm_building, dorm_room, batch_id, admission_type, source,
             create_time, update_time, deleted)
        VALUES (
            1000000000000430001 + v_i,
            1000000000000400001 + v_i,
            1,
            ELT(v_college, '信息科学与工程学院', '机械工程学院', '电气工程学院',
                        '经济管理学院', '外国语学院', '材料科学与工程学院',
                        '土木工程学院', '化学工程学院'),
            CONCAT('演示专业', v_college),
            CONCAT('2026级', LPAD(v_college, 2, '0'), '班', (v_i MOD 5) + 1),
            CONCAT('D', v_college),
            CONCAT('3', LPAD(v_college, 2, '0'), LPAD((v_i MOD 5) * 4 + 1, 2, '0')),
            2026,
            1,
            2,
            NOW(3), NOW(3), 0);
        SET v_i = v_i + 1;
    END WHILE;

    -- ========================================================================
    -- 4. 随机报到进度：每人完成 2-5 个步骤，时间分布在过去 3 小时内
    -- ========================================================================
    SET v_i = 0;
    WHILE v_i < 40 DO
        SET v_user_id = 1000000000000400001 + v_i;
        SET v_step_count = 2 + (v_i MOD 4);      -- 2,3,4,5 循环
        SET v_j = 0;
        WHILE v_j < v_step_count DO
            INSERT IGNORE INTO checkin_progress
                (id, user_id, step_code, status, checkin_time, checkin_channel,
                 create_time, update_time, deleted)
            VALUES (
                1000000000000600000 + v_i * 10 + v_j,
                v_user_id,
                ELT(v_j + 1, 'PRE_REG', 'PAY', 'DORM', 'ARCHIVE', 'CERT'),
                2,
                NOW(3) - INTERVAL (20 + FLOOR(RAND() * 160)) MINUTE,
                1,
                NOW(3), NOW(3), 0);
            -- 同步写一条报到打卡埋点（事件表），时间与打卡一致
            INSERT IGNORE INTO admin_behavior_event
                (id, event_type, user_id, service, ref_id, channel, ext, event_time,
                 create_time, update_time, deleted)
            VALUES (
                1000000000000700000 + v_i * 10 + v_j,
                3, v_user_id, 'checkin', NULL, 1,
                JSON_OBJECT('stepCode', ELT(v_j + 1, 'PRE_REG', 'PAY', 'DORM', 'ARCHIVE', 'CERT'), 'seeded', TRUE),
                NOW(3) - INTERVAL (20 + FLOOR(RAND() * 160)) MINUTE,
                NOW(3), NOW(3), 0);
            SET v_j = v_j + 1;
        END WHILE;
        SET v_i = v_i + 1;
    END WHILE;

    -- ========================================================================
    -- 5. 问答埋点：每名学生 2-4 次提问，命中率约 9 成
    --    （缓存 5 成 / 问答库 4 成 / 未命中 1 成）
    -- ========================================================================
    SET v_i = 0;
    WHILE v_i < 40 DO
        SET v_user_id = 1000000000000400001 + v_i;
        SET v_qa_i = 0;
        WHILE v_qa_i < 2 + (v_i MOD 3) DO
            SET v_hits = FLOOR(RAND() * 10);
            IF v_hits < 5 THEN SET v_hit_type = 1;
            ELSEIF v_hits < 9 THEN SET v_hit_type = 2;
            ELSE SET v_hit_type = 4;
            END IF;
            SET v_cost = 40 + FLOOR(RAND() * 360);
            SET v_minutes_back = FLOOR(RAND() * 170);
            INSERT IGNORE INTO qa_ask_log
                (id, user_id, question_hash, hit_type, cost_ms, session_id,
                 create_time, update_time, deleted)
            VALUES (
                1000000000000800000 + v_i * 10 + v_qa_i,
                v_user_id,
                SHA2(CONCAT('seed-', v_i, '-', v_qa_i), 256),
                v_hit_type, v_cost, NULL,
                NOW(3) - INTERVAL v_minutes_back MINUTE,
                NOW(3), 0);
            INSERT IGNORE INTO admin_behavior_event
                (id, event_type, user_id, service, ref_id, channel, ext, event_time,
                 create_time, update_time, deleted)
            VALUES (
                1000000000000900000 + v_i * 10 + v_qa_i,
                1, v_user_id, 'qa', NULL, 1,
                JSON_OBJECT('hitType', v_hit_type, 'costMs', v_cost, 'seeded', TRUE),
                NOW(3) - INTERVAL v_minutes_back MINUTE,
                NOW(3), NOW(3), 0);
            SET v_qa_i = v_qa_i + 1;
        END WHILE;
        SET v_i = v_i + 1;
    END WHILE;

    -- ========================================================================
    -- 6. 问答条目 12 条（固定 id，INSERT IGNORE 避免与迁移/重复执行冲突）
    -- ========================================================================
    INSERT IGNORE INTO qa_question
        (id, question, answer, answer_type, category, tags, synonyms, status,
         hit_count, version, create_time, update_time, deleted)
    VALUES
        (1000000000000500001, '报到需要带哪些材料？',
         '请携带录取通知书、身份证、高考准考证、一寸免冠照片 4 张，以及档案袋（切勿私自拆封）。', 1,
         '报到', '材料,清单', '需要带什么,报到材料有哪些', 1, 0, 1, NOW(3), NOW(3), 0),
        (1000000000000500002, '宿舍在哪里？',
         '新生宿舍集中在东区 3-6 号楼，报到后可在宿舍楼下扫码领取钥匙。', 1,
         '住宿', '宿舍,位置', '寝室在哪,宿舍楼在哪', 1, 0, 1, NOW(3), NOW(3), 0),
        (1000000000000500003, '学费怎么交？',
         '可通过迎新平台“在线缴费”入口支付，也支持到财务处现场刷卡/扫码。', 1,
         '缴费', '学费,缴费', '怎么交学费,学费缴纳方式', 1, 0, 1, NOW(3), NOW(3), 0),
        (1000000000000500004, '军训什么时候开始？',
         '本科新生军训自报到次日起，为期 14 天，具体连队编排见迎新网通知。', 1,
         '其他', '军训,时间', '军训时间,什么时候军训', 1, 0, 1, NOW(3), NOW(3), 0),
        (1000000000000500005, '校园卡在哪里办？',
         '在大学生活动中心一楼卡务中心办理，需携带身份证，立等可取。', 1,
         '生活', '校园卡,一卡通', '一卡通怎么办,卡在哪办', 1, 0, 1, NOW(3), NOW(3), 0),
        (1000000000000500006, '快递地址怎么填？',
         '填写：XX 省 XX 市 XX 区大学城 1 号菜鸟驿站（校内），取件凭取件码。', 1,
         '生活', '快递,地址', '快递收货地址,包裹寄哪里', 1, 0, 1, NOW(3), NOW(3), 0),
        (1000000000000500007, '绿色通道怎么申请？',
         '家庭经济困难新生可在报到点“绿色通道”窗口提交申请表，缓交学费先行注册。', 1,
         '资助', '绿色通道,助学', '缓交学费,贫困生入学', 1, 0, 1, NOW(3), NOW(3), 0),
        (1000000000000500008, '宿舍有热水和空调吗？',
         '全部宿舍配备空调与独立卫浴，热水 6:00-8:00、17:00-23:00 供应。', 1,
         '住宿', '热水,空调', '寝室有空调吗,热水供应时间', 1, 0, 1, NOW(3), NOW(3), 0),
        (1000000000000500009, '课程表在哪里查？',
         '登录教务系统后在“我的课表”查询，开学第一周内可试听并补退选。', 1,
         '学习', '课表,选课', '怎么查课表,教务系统', 1, 0, 1, NOW(3), NOW(3), 0),
        (1000000000000500010, '图书馆怎么进？',
         '刷校园卡入馆，开放时间 7:30-22:30，考试周延长至 23:30。', 1,
         '学习', '图书馆,开放时间', '图书馆几点关门,进馆方式', 1, 0, 1, NOW(3), NOW(3), 0),
        (1000000000000500011, '校医院在哪里？',
         '校医院位于西区大门内侧 50 米，24 小时急诊，就诊请携带校园卡。', 1,
         '生活', '校医院,看病', '哪里看病,医务室位置', 1, 0, 1, NOW(3), NOW(3), 0),
        (1000000000000500012, '社团怎么加入？',
         '开学第三周有“百团大战”招新活动，也可在迎新平台社团专区在线报名。', 1,
         '其他', '社团,招新', '加社团,社团招新时间', 1, 0, 1, NOW(3), NOW(3), 0);

    -- 热门问题热度：按固定梯度赋值，保证看板 TOP5 有区分度
    SET v_i = 0;
    WHILE v_i < 12 DO
        SET v_question_id = 1000000000000500001 + v_i;
        SET v_hit_inc = 120 - v_i * 9;
        UPDATE qa_question SET hit_count = hit_count + v_hit_inc
            WHERE id = v_question_id AND deleted = 0;
        SET v_i = v_i + 1;
    END WHILE;

    -- ========================================================================
    -- 6b. 报到步骤模板与材料（一期迁移只有结构没有种子，这里补演示步骤）
    INSERT IGNORE INTO checkin_step_template
        (id, step_code, step_name, seq, apply_scope, location, office_hours, contact, enabled,
         create_time, update_time, create_by, update_by, deleted)
    VALUES
        (1000000000000000011, 'PRE_REG', '网上预报到', 1, NULL, '线上',       '全天',        NULL,           1, NOW(3), NOW(3), 0, 0, 0),
        (1000000000000000012, 'PAY',     '缴纳学杂费', 2, NULL, '财务处窗口', '08:30-17:30', '028-84480001', 1, NOW(3), NOW(3), 0, 0, 0),
        (1000000000000000013, 'DORM',    '宿舍入住',   3, NULL, '各宿舍楼栋', '08:00-22:00', '宿管值班电话', 1, NOW(3), NOW(3), 0, 0, 0),
        (1000000000000000014, 'ARCHIVE', '档案交接',   4, NULL, '学院报到处', '08:30-17:30', '辅导员',       1, NOW(3), NOW(3), 0, 0, 0),
        (1000000000000000015, 'CERT',    '报到确认',   5, NULL, '学院报到处', '08:30-17:30', '辅导员',       1, NOW(3), NOW(3), 0, 0, 0);
    INSERT IGNORE INTO checkin_material_item
        (id, step_code, name, required, remark, seq, create_time, update_time, create_by, update_by, deleted)
    VALUES
        (1000000000000000031, 'PRE_REG', '录取通知书', 1, '原件', 1, NOW(3), NOW(3), 0, 0, 0),
        (1000000000000000032, 'PRE_REG', '身份证',     1, '原件及复印件', 2, NOW(3), NOW(3), 0, 0, 0),
        (1000000000000000033, 'PAY',    '缴费凭证',   1, '线上缴费截图或现场凭证', 1, NOW(3), NOW(3), 0, 0, 0);

    -- 7. 未解答记录 2 条（对应 hit_type=4 的补录场景）
    -- ========================================================================
    INSERT IGNORE INTO qa_unanswered
        (id, question, user_id, confidence, status,
         create_time, update_time, deleted)
    VALUES (1000000000001000001, '体检表在哪里交？', 1000000000000400001, 0.18, 1,
            NOW(3), NOW(3), 0),
           (1000000000001000002, '户口本丢了怎么办？', 1000000000000400004, 0.22, 1,
            NOW(3), NOW(3), 0);

    SELECT 'seed done' AS result;
END $$

DELIMITER ;

CALL seed_demo_data();
DROP PROCEDURE IF EXISTS seed_demo_data;

-- 统计确认
SELECT
    (SELECT COUNT(*) FROM admin_user WHERE deleted = 0)           AS admin_rows,
    (SELECT COUNT(*) FROM auth_user WHERE deleted = 0)            AS student_rows,
    (SELECT COUNT(*) FROM auth_identity WHERE deleted = 0)        AS identity_rows,
    (SELECT COUNT(*) FROM checkin_progress WHERE status = 2)      AS progress_rows,
    (SELECT COUNT(*) FROM admin_behavior_event WHERE deleted = 0) AS event_rows,
    (SELECT COUNT(*) FROM qa_ask_log WHERE deleted = 0)           AS ask_rows,
    (SELECT COUNT(*) FROM qa_question WHERE deleted = 0)          AS question_rows,
    (SELECT COUNT(*) FROM qa_unanswered WHERE deleted = 0)        AS unanswered_rows;
