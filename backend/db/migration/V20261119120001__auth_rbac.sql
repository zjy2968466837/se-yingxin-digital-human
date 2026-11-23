-- auth-service：禅道式 RBAC 权限中心（二期业务落地）
-- 权限模型：user → usergroup → group → grouppriv(module, method)，与禅道 grouppriv 语义一致
-- 纪律同数据建模 §1.3：禁止物理外键、id 为雪花 ID、deleted 为 BIGINT（0=未删，删除写行 id）
-- 同步文档：docs/数据建模.md「二期新增表」章节

SET NAMES utf8mb4;

-- 1. 用户审批状态（RBAC 注册审批流：敏感组注册后待审批，审批通过前禁止登录）
ALTER TABLE auth_user
    ADD COLUMN approve_status TINYINT NOT NULL DEFAULT 1 COMMENT '0 待审批 / 1 通过 / 2 驳回' AFTER status;

-- 2. 用户组（角色）
CREATE TABLE `auth_group` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `code` VARCHAR(32) NOT NULL COMMENT '组编码，唯一',
  `name` VARCHAR(64) NOT NULL COMMENT '组名称',
  `builtin` TINYINT(1) NOT NULL COMMENT '1 内置组（不可删除、权限矩阵只读）',
  `sort` INT NOT NULL COMMENT '展示顺序',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户组（角色）';

-- 3. 用户-用户组关联
CREATE TABLE `auth_user_group` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `user_id` BIGINT NOT NULL COMMENT '用户',
  `group_id` BIGINT NOT NULL COMMENT '用户组',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_group` (`user_id`, `group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-用户组关联';

-- 4. 权限模块
CREATE TABLE `auth_module` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `code` VARCHAR(32) NOT NULL COMMENT '模块编码，唯一',
  `name` VARCHAR(64) NOT NULL COMMENT '模块名称',
  `sort` INT NOT NULL COMMENT '展示顺序',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限模块';

-- 5. 权限点（module:priv）
CREATE TABLE `auth_method` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `module_id` BIGINT NOT NULL COMMENT '所属模块',
  `code` VARCHAR(64) NOT NULL COMMENT '权限点编码，格式 module:priv，唯一',
  `priv` VARCHAR(32) NOT NULL COMMENT '权限动作名',
  `name` VARCHAR(64) NOT NULL COMMENT '权限点名称',
  `sort` INT NOT NULL COMMENT '展示顺序',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_module` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限点';

-- 6. 组授权表（禅道 grouppriv：权限矩阵每次勾选即本表一行增删）
CREATE TABLE `auth_group_priv` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `group_id` BIGINT NOT NULL COMMENT '用户组',
  `module_id` BIGINT NOT NULL COMMENT '模块',
  `method_id` BIGINT NOT NULL COMMENT '权限点',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_module_method` (`group_id`, `module_id`, `method_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组授权表';

-- 7. 审计日志（登录/审批/授权等权限动作留痕，只增不改）
CREATE TABLE `auth_audit_log` (
  `id` BIGINT NOT NULL COMMENT '雪花 ID，主键',
  `operator` VARCHAR(64) NOT NULL COMMENT '操作人账号',
  `action` VARCHAR(64) NOT NULL COMMENT '动作：login/register/approve/grant/revoke 等',
  `target` VARCHAR(128) COMMENT '目标对象',
  `detail` VARCHAR(512) COMMENT '详情',
  `ts` DATETIME(3) NOT NULL COMMENT '发生时间',
  `create_time` DATETIME(3) NOT NULL COMMENT '创建时间，持久层自动填充',
  `update_time` DATETIME(3) NOT NULL COMMENT '更新时间，持久层自动填充',
  `create_by` BIGINT COMMENT '创建人 id，系统写入为 0',
  `update_by` BIGINT COMMENT '修改人 id，系统写入为 0',
  `deleted` BIGINT NOT NULL COMMENT '逻辑删除：0=未删除；删除时写入本行 id',
  PRIMARY KEY (`id`),
  KEY `idx_ts` (`ts`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志';

-- 8. 权限字典种子（结构配置，非演示数据；演示账号种子见 scripts/seed-demo-data.sql）
INSERT INTO auth_module (id, code, name, sort, create_time, update_time, create_by, update_by, deleted) VALUES
    (1000000000000300001, 'home',    '迎新首页', 1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000300002, 'checkin', '报到服务', 2, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000300003, 'qa',      '问答服务', 3, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000300004, 'user',    '用户管理', 4, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000300005, 'perm',    '权限配置', 5, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000300006, 'audit',   '审计日志', 6, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000300007, 'stats',   '统计中心', 7, NOW(3), NOW(3), 0, 0, 0);

INSERT INTO auth_method (id, module_id, code, priv, name, sort, create_time, update_time, create_by, update_by, deleted) VALUES
    (1000000000000310001, 1000000000000300001, 'home:view',          'view',    '查看迎新首页', 1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000310002, 1000000000000300002, 'checkin:view',       'view',    '查看报到清单', 1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000310003, 1000000000000300002, 'checkin:checkin',    'checkin', '报到打卡',     2, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000310004, 1000000000000300003, 'qa:ask',             'ask',     '智能问答',     1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000310005, 1000000000000300003, 'qa:manage',          'manage',  '问答库管理',   2, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000310006, 1000000000000300004, 'user:list',          'list',    '用户列表',     1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000310007, 1000000000000300004, 'user:approve',       'approve', '注册审批',     2, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000310008, 1000000000000300004, 'user:role',          'role',    '变更用户组',   3, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000310009, 1000000000000300005, 'perm:matrix',        'matrix',  '查看权限矩阵', 1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000310010, 1000000000000300005, 'perm:grant',         'grant',   '调整角色权限', 2, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000310011, 1000000000000300006, 'audit:view',         'view',    '查看审计日志', 1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000310012, 1000000000000300007, 'stats:view',         'view',    '查看统计看板', 1, NOW(3), NOW(3), 0, 0, 0);

INSERT INTO auth_group (id, code, name, builtin, sort, create_time, update_time, create_by, update_by, deleted) VALUES
    (1000000000000320001, 'admin',           '系统管理员', 1, 1, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000320002, 'freshman',        '新生',       1, 2, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000320003, 'counselor',       '辅导员',     1, 3, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000320004, 'college_admin',   '学院管理员', 1, 4, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000320005, 'student_affairs', '学工处',     1, 5, NOW(3), NOW(3), 0, 0, 0);

-- 组授权：超级组 admin 全量；其余按最小权限原则
INSERT INTO auth_group_priv (id, group_id, module_id, method_id, create_time, update_time, create_by, update_by, deleted) VALUES
    (1000000000000330001, 1000000000000320001, 1000000000000300001, 1000000000000310001, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330002, 1000000000000320001, 1000000000000300002, 1000000000000310002, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330003, 1000000000000320001, 1000000000000300002, 1000000000000310003, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330004, 1000000000000320001, 1000000000000300003, 1000000000000310004, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330005, 1000000000000320001, 1000000000000300003, 1000000000000310005, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330006, 1000000000000320001, 1000000000000300004, 1000000000000310006, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330007, 1000000000000320001, 1000000000000300004, 1000000000000310007, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330008, 1000000000000320001, 1000000000000300004, 1000000000000310008, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330009, 1000000000000320001, 1000000000000300005, 1000000000000310009, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330010, 1000000000000320001, 1000000000000300005, 1000000000000310010, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330011, 1000000000000320001, 1000000000000300006, 1000000000000310011, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330012, 1000000000000320001, 1000000000000300007, 1000000000000310012, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330101, 1000000000000320002, 1000000000000300001, 1000000000000310001, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330102, 1000000000000320002, 1000000000000300002, 1000000000000310002, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330103, 1000000000000320002, 1000000000000300002, 1000000000000310003, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330104, 1000000000000320002, 1000000000000300003, 1000000000000310004, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330201, 1000000000000320003, 1000000000000300001, 1000000000000310001, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330202, 1000000000000320003, 1000000000000300002, 1000000000000310002, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330203, 1000000000000320003, 1000000000000300003, 1000000000000310004, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330204, 1000000000000320003, 1000000000000300004, 1000000000000310006, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330205, 1000000000000320003, 1000000000000300007, 1000000000000310012, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330301, 1000000000000320004, 1000000000000300001, 1000000000000310001, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330302, 1000000000000320004, 1000000000000300002, 1000000000000310002, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330303, 1000000000000320004, 1000000000000300004, 1000000000000310006, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330304, 1000000000000320004, 1000000000000300004, 1000000000000310007, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330305, 1000000000000320004, 1000000000000300007, 1000000000000310012, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330401, 1000000000000320005, 1000000000000300001, 1000000000000310001, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330402, 1000000000000320005, 1000000000000300004, 1000000000000310006, NOW(3), NOW(3), 0, 0, 0),
    (1000000000000330403, 1000000000000320005, 1000000000000300007, 1000000000000310012, NOW(3), NOW(3), 0, 0, 0);
