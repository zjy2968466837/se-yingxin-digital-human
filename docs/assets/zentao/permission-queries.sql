-- =============================================================================
-- 禅道 22.6 角色权限取证查询（只读）
--
-- 连接：本机禅道自带 MariaDB（127.0.0.1:3306，库 zentao，表前缀 zt_）
-- 客户端：D:\Documents\chandao\ZenTao\bin\mysql\bin\mysql.exe
-- 凭据：见 D:\Documents\chandao\ZenTao\app\zentao\config\mysql.php（不写入仓库）
--
-- 用法：
--   mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 < permission-queries.sql
-- 说明：zt_user.password 是 MD5 摘要，本文件不查询该列。
-- =============================================================================

-- 1. 版本与规模（精确 COUNT，不用 table_rows 估算）
SELECT VERSION() AS db_version;
SELECT (SELECT COUNT(*) FROM zt_group)      AS groups,
       (SELECT COUNT(*) FROM zt_grouppriv)  AS group_privs,
       (SELECT COUNT(*) FROM zt_usergroup)  AS user_groups,
       (SELECT COUNT(*) FROM zt_user)       AS users,
       (SELECT COUNT(*) FROM zt_module)     AS modules,
       (SELECT COUNT(*) FROM zt_project)    AS projects;

-- 2. 角色组清单与每组权限点数量
SELECT g.id, g.name, g.role, g.project, g.devopsSpace, g.developer, g.vision, COUNT(p.id) AS privs
FROM zt_group g
LEFT JOIN zt_grouppriv p ON p.`group` = g.id
GROUP BY g.id, g.name, g.role, g.project, g.devopsSpace, g.developer, g.vision
ORDER BY g.id;

-- 3. 自定义只读角色的权限点（module = 控制器，method = 动作）
SELECT p.module, GROUP_CONCAT(p.method ORDER BY p.method) AS methods
FROM zt_grouppriv p
JOIN zt_group g ON g.id = p.`group`
WHERE g.name = '迎新项目-只读角色'
GROUP BY p.module
ORDER BY p.module;

-- 4. 用户与角色组的绑定关系
SELECT u.account, u.realname, u.role, u.visions, u.deleted, ug.`group`, g.name AS group_name
FROM zt_user u
LEFT JOIN zt_usergroup ug ON ug.account = u.account
LEFT JOIN zt_group g ON g.id = ug.`group`
ORDER BY u.id;

-- 5. 超级管理员名单（Kernel 层绕过所有权限判定）
SELECT id, name, admins FROM zt_company;

-- 6. 表物理属性（引擎 / 行格式 / 数据与索引大小）
SELECT table_name, engine, row_format, table_rows,
       ROUND(data_length/1024, 1) AS data_kb,
       ROUND(index_length/1024, 1) AS index_kb
FROM information_schema.tables
WHERE table_schema = 'zentao'
  AND table_name IN ('zt_user','zt_group','zt_grouppriv','zt_usergroup','zt_module','zt_project','zt_company');

-- 7. 关键索引（zt_grouppriv 的唯一索引保证"同组同模块同方法"不重复）
SELECT table_name, index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS cols, non_unique
FROM information_schema.statistics
WHERE table_schema = 'zentao'
  AND table_name IN ('zt_user','zt_group','zt_grouppriv','zt_usergroup')
GROUP BY table_name, index_name, non_unique
ORDER BY table_name, index_name;
