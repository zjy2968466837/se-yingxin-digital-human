# 交接文档：物理建表与数据持久化（Windows 继续）

> **状态更新（2026-10-27）**：本文 §7 的 S5 样板已落地到 `qa-service`
> （`QaSession` Entity/Mapper/Service/Controller + Redis 缓存 + 单测），建表已在干净容器验证（24 表 / 0 外键）。
> 本文保留作为当时的核查快照与操作手册；最新状态以 [AGENTS.md](../AGENTS.md) 为准。
>
> **用途**：把 2026-10-27 对“各组把自己的表建到物理库 + 能写持久化代码”的核查结论固化下来，供在 Windows 上继续执行。
>
> **适用仓库**：`se-yingxin-digital-human`
> **核查分支**：`docs/review-fixes-20261009`（当前工作分支）
> **结论一句话**：任务可以完成，24 张表的模型和建表 SQL 已经齐了；当前缺的是“可重复执行的建表入口”和“第一份持久化代码样板”。

---

## 0. 先看这里（TL;DR）

1. **模型与建表 SQL 已齐**：`docs/数据建模.dbml` 有 24 个 `Table`；`backend/db/migration/` 有 7 个 init 脚本（合计 24 张表）+ 3 个增量脚本，全部无物理外键。
2. **物理建表目前只能手工执行**：Flyway 未接入，`backend/init-sql/01-init.sql` 只建库；没有迁移执行入口脚本。
3. **持久化基础设施齐、样板为零**：`common-web` 已自动装配 MyBatis-Plus、审计字段填充、Redis、Jackson；但所有服务都没有 Entity/Mapper/Service，`AuthUser` 样板已在 commit `5419405` 删除。
4. **dbdiagram.io 是网页端 SaaS**：不存在“装进仓库/CI”的集成，也不需要；仓库集成的是它的产物（`.dbml`、拆图脚本、ER 图、迁移脚本）。如果课程要求“工具直接连库正向工程建表”，dbdiagram 只能导出 DDL，需要换 Navicat / PowerDesigner / MySQL Workbench。
5. **Windows 上先做三件事**：起 MySQL/Redis 容器 → 按顺序执行 10 个 SQL 脚本 → 校验 24 张表（含 3 个增量）。然后按第 7 节写 `qa_session` 的最小持久化闭环。

---

## 1. 任务与验收口径

### 1.1 任务原文（理解后的口径）

> 把各组自己的表建立在物理数据库里，可以做到写数据持久化的代码；前提是完成上节课的数据建表；物理建表必须使用辅助数据建模工具。

拆成三个可验收项：

| 验收项 | 判定标准 |
|---|---|
| 物理建表 | MySQL 的 `yingxin` 库中存在 24 张表，表名/字段名全小写，无 `FOREIGN KEY` |
| 持久化代码 | 至少一个服务能完成 `Controller → Service → Mapper → MySQL` 的写入与查询闭环 |
| 建模工具 | 能拿出“用建模工具生成 DDL 再落库”的证据链，而不是纯手写 SQL |

### 1.2 本项目的既定口径

- 单库 `yingxin`，按服务前缀分表，不拆多库（`docs/开发规范.md:262`）。
- 禁止物理外键，关联由应用层保证（`docs/开发规范.md:266`）。
- 主键统一雪花 ID，非自增；`deleted` 为 `BIGINT`，`0` 表示未删除，删除时写本行 `id`（`docs/开发规范.md:273-276`）。
- 地图数据（POI / 路网）是静态 JSON，**不建表**（`docs/数据建模.md:1017`）。
- 单人/课程阶段允许先用 init-sql 编号脚本 + `docker compose down -v` 重建，多人并行后再上 Flyway（`docs/需求分析文档.md:1019`）。

---

## 2. 现状盘点（证据）

### 2.1 数据模型

- 权威源：`docs/数据建模.md`（§2 表清单 / §3 字段定义 / §5 唯一键与状态机）。
- 模型文件：`docs/数据建模.dbml`，由文档生成，**禁止手改**（`docs/数据建模.md:1002`）。
- 实测：`docs/数据建模.dbml` 有 **24 个 `Table`**，另有 7 条 `Ref` 仅用于画关系线（`docs/数据建模.dbml:587-593`）。
- 出图视图：`docs/diagrams/er-1-auth-checkin.dbml`、`er-2-qa.dbml`、`er-4-aid-life.dbml`、`er-5-content-admin.dbml`；由 `scripts/split_dbml.py` 生成，只用于出图，**不要从视图导出 DDL**。

### 2.2 迁移脚本

目录：`backend/db/migration/`，实测 **10 个文件**：

| 文件 | 建表数 | 表 |
|---|---:|---|
| `V20261009160001__admin_init.sql` | 7 | `admin_user`、`admin_role`、`admin_user_role`、`admin_announcement`、`admin_config`、`admin_operation_log`、`admin_behavior_event` |
| `V20261009160002__aid_init.sql` | 2 | `aid_policy`、`aid_application` |
| `V20261009160003__auth_init.sql` | 3 | `auth_user`、`auth_identity`、`auth_push_record` |
| `V20261009160004__checkin_init.sql` | 4 | `checkin_step_template`、`checkin_material_item`、`checkin_progress`、`checkin_certificate` |
| `V20261009160005__content_init.sql` | 1 | `content_article` |
| `V20261009160006__life_init.sql` | 2 | `life_repair_order`、`life_service_info` |
| `V20261009160007__qa_init.sql` | 5 | `qa_question`、`qa_session`、`qa_message`、`qa_unanswered`、`qa_ask_log` |
| `V20261025134001__content_add_guide_columns.sql` | 0 | `ALTER TABLE content_article` 增加 `guide_type`、`contact`、`uk_guide_type` |
| `V20261025134002__admin_add_idx_role.sql` | 0 | `ALTER TABLE admin_user_role` 增加 `idx_role` |
| `V20261103120001__auth_password_hash_not_null.sql` | 0 | `ALTER TABLE auth_user` 将 `password_hash` 收紧为 `NOT NULL`（全量账号必填 BCrypt 散列） |

合计建表 **24 张**（3 个增量均为 `ALTER`，不新增表）。脚本共同特征：

- 头部统一声明：禁止物理外键、表名字段名全小写、`id` 为雪花 ID、`deleted` 为 BIGINT（各 `*_init.sql:3`）。
- 全部 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`。
- 全部带表级/字段级中文 `COMMENT`。
- 全局检索 `FOREIGN KEY` / `REFERENCES` 无结果。
- 脚本内**没有** `CREATE DATABASE` / `USE yingxin`，执行时必须显式指定库。

### 2.3 迁移执行机制

- **Flyway 未接入**：`backend/pom.xml` 全文无 Flyway，也没有 `db` profile；各服务 pom 也没有 Flyway。
- `backend/init-sql/01-init.sql:1` 只执行 `CREATE DATABASE IF NOT EXISTS yingxin ...`，不建表。
- `backend/docker-compose.yml:32` 只把 `./init-sql` 挂到 MySQL 的 `/docker-entrypoint-initdb.d`，不会自动执行 `db/migration`。
- 因此，**当前唯一可行方式是手工按版本号顺序执行 9 个脚本**。
- 由于没有 `flyway_schema_history`，无法判断某个脚本是否执行过；重复执行 `CREATE TABLE` 会报 “Table already exists”。

### 2.4 持久化代码现状

**已具备的能力**（`common-web` 自动装配，见 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`）：

| 能力 | 位置 | 说明 |
|---|---|---|
| 分页插件 + 防全表更新/删除 | `common-web/.../config/MybatisPlusConfig.java` | `PaginationInnerInterceptor(DbType.MYSQL)` + `BlockAttackInnerInterceptor` |
| 审计字段自动填充 | `common-web/.../config/AuditMetaObjectHandler.java` | 插入填 `createTime/updateTime/createBy/updateBy/deleted=0`；更新填 `updateTime/updateBy` |
| 用户上下文 | `common-web/.../context/UserContextHolder.java` | `getUserIdOrSystem()`，无登录态时写系统用户 |
| Redis JSON 序列化 | `common-web/.../config/RedisConfig.java` | key/hashKey 用 String，value 用 GenericJackson2Json |
| Jackson 时间格式 | `common-web/.../config/JacksonConfig.java` | `Asia/Shanghai`、`yyyy-MM-dd HH:mm:ss` |

**依赖与配置**：

- 8 个业务服务的 pom 都引入了 `mybatis-plus-spring-boot3-starter`、`mysql-connector-j`（runtime）、Redis starter。
- 各服务 `application.yml` 已配置 `map-underscore-to-camel-case: true`、`mapper-locations: classpath*:/mapper/**/*.xml`、`type-aliases-package: com.sicnu.yingxin.<svc>.entity`。
- `resources/mapper/` 目录已存在，但只有 `.gitkeep`，没有 XML。

**缺口**：

- 所有服务都没有**数据库相关的** `entity/`、`mapper/`、`service/` 业务代码（各服务只有 Application + PingController；auth-service 另有 JWT/Session 能力，但操作的是 Redis，不是 MySQL）。
- 全仓没有 `@MapperScan`；历史 `AuthUserMapper` 用的是 `@Mapper` 注解。
- 没有逻辑删除全局配置（项目刻意不用 `@TableLogic`）。
- 没有 H2 / Testcontainers / `@SpringBootTest` 数据库测试；现有测试都是纯单元测试或 `MockMvcBuilders.standaloneSetup`。
- `AuthUser` + `AuthUserMapper` 曾在仓库中存在，但已在 commit `5419405`（“收回越界的业务代码”）删除；`AGENTS.md:10` 的描述已过时。

### 2.5 建模工具集成情况

**结论：工具本身没有“集成”进工程，也不需要；集成的是它的产物与流程。**

- dbdiagram.io 是网页端 SaaS：在浏览器里导入 `.dbml`、出关系图、导出 MySQL DDL，不提供仓库依赖、Maven/npm 插件或 CI 集成。
- 仓库里已有的建模产物：
  - `docs/数据建模.dbml`（24 表 + 7 条逻辑 Ref）
  - `scripts/split_dbml.py`（把源模型拆成 4 份出图视图）
  - `docs/diagrams/*.dbml`（4 份 ER 视图）
  - `docs/assets/er/*.svg` / `*.png`（4 张 ER 成图）
  - `backend/db/migration/*.sql`（按“导出 DDL → 删外键/核对小写/补通用字段”改写后的落库脚本）
- 文档化的落地流程：`docs/数据建模.md:1008`（§9 落地流程）与 `docs/数据建模.md:1010`（导出 DDL 后必须做的三件事）。
- 证据链缺口：
  - 仓库没有保存 dbdiagram 导出的**原始 DDL**（只有改写后的迁移脚本）。
  - `dbdiagram-initial.png` 实测 8,881 字节，打开基本空白；`dbml-error.log` 为 0 字节。两者都不能作为“已用建模工具”的有效证据。
- 如果课程要求的是“工具直接连库、正向工程建表”（Navicat / PowerDesigner / MySQL Workbench 的 Forward Engineering），dbdiagram 做不到：它只导出 DDL，落库仍要执行 SQL。这种口径下需要换工具；当前方案属于“建模工具导出 DDL → 执行 SQL 建表”。

---

## 3. 各组表归属（可直接分给各组）

| 组 / 服务 | 表 | 数量 |
|---|---|---:|
| auth | `auth_user`、`auth_identity`、`auth_push_record` | 3 |
| checkin | `checkin_step_template`、`checkin_material_item`、`checkin_progress`、`checkin_certificate` | 4 |
| qa | `qa_question`、`qa_session`、`qa_message`、`qa_unanswered`、`qa_ask_log` | 5 |
| aid | `aid_policy`、`aid_application` | 2 |
| life | `life_repair_order`、`life_service_info` | 2 |
| content | `content_article` | 1 |
| admin | `admin_user`、`admin_role`、`admin_user_role`、`admin_announcement`、`admin_config`、`admin_operation_log`、`admin_behavior_event` | 7 |
| navigation | 无数据库表（POI / 路网为静态 JSON，收藏为前端本地存储） | 0 |
| **合计** | | **24** |

关键点：

- 表按服务前缀划分，来源见 `docs/数据建模.md:1046-1105`。
- **没有物理外键**，所以各组只执行自己服务的 `*_init.sql` 就能建表，不需要等其他组先建。
- 但业务数据仍可能逻辑引用 `auth_user` 等表；建表本身没有依赖，联调时再按需准备数据。
- `navigation-service` 没有表，不参与本次物理建表；它的数据是 `navigation-service/src/main/resources/static-data/` 下的静态 JSON。

---

## 4. Windows 环境准备

### 4.1 版本基线

| 工具 | 版本 | 说明 |
|---|---|---|
| JDK | 17（17.0.20 已验证） | `JAVA_HOME` 指向 JDK 17 |
| Maven | 3.9.12 | 不用单独安装，`backend\mvnw.cmd` 自动下载 |
| Node | 22.22.1 | 管理后台与学生端 H5 都需要 |
| pnpm | 12.4.1 | 用 Corepack 激活，或 `npm i -g pnpm@12` |
| Docker Desktop | 最新稳定版 | 必须启用 WSL2 后端 |
| MySQL | 8.0.46 | 由 `backend/docker-compose.yml` 的 `mysql:8.0.46` 提供 |
| Redis | 7.0.15 | 由 `redis:7.0.15-alpine` 提供 |

版本事实来源：`docs/开发规范.md:406-434`；前端版本以各 `package.json` 的 `packageManager` / `engines` 为准。

### 4.2 获取代码

推荐在 Windows 上重新 clone，而不是直接拷贝 WSL 目录：

```powershell
cd D:\code
git clone https://github.com/zjy2968466837/se-yingxin-digital-human.git
cd se-yingxin-digital-human
git checkout docs/review-fixes-20261009   # 当前工作分支；也可以切 main / dev
```

注意：

- 仓库路径建议纯英文（如 `D:\code\se-yingxin-digital-human`），避免中文路径触发 Maven / Node 的编码问题。
- `git clone` 只包含已跟踪文件。以下**未跟踪的课程交付物**需要从 WSL 手工拷贝：
  - `docs/数据库设计说明书-大学生迎新导引服务数字人系统.docx`
  - `docs/assets/er/er-1-auth-checkin.png`、`er-2-qa.png`、`er-4-aid-life.png`、`er-5-content-admin.png`
  - `dbdiagram-initial.png`（建议顺便替换成真实截图）
- `.playwright-mcp/`、`.serena/` 是工具临时目录，**不要拷、不要提交**。
- 换行符：`.gitattributes` 已强制入库 LF、工作区 LF，`.cmd/.bat/.ps1` 为 CRLF；Windows 上不需要额外配置 `core.autocrlf`。

### 4.3 Maven 镜像（国内建议）

```powershell
mkdir "$env:USERPROFILE\.m2" -Force
Copy-Item docs\maven-settings.xml "$env:USERPROFILE\.m2\settings.xml"
```

如果已有 `settings.xml`，手工合并 `<mirror>` 段，不要直接覆盖。

### 4.4 Docker Desktop

1. 安装 Docker Desktop，启用 **WSL2 backend**。
2. Settings → Resources → WSL Integration，打开你使用的 WSL 发行版。
3. 确认端口未被占用：`3306`（MySQL）、`6379`（Redis）；如果本机已装 MySQL，先停掉或改映射。
4. 验证：

```powershell
docker --version
docker compose version
```

---

## 5. 启动基础设施

```powershell
cd D:\code\se-yingxin-digital-human\backend
docker compose up -d
docker compose ps
```

期望：`yingxin-mysql`、`yingxin-redis` 两个容器都是 `healthy`。

连接信息（本地）：

| 项 | 值 |
|---|---|
| MySQL 主机/端口 | `127.0.0.1:3306` |
| 库 | `yingxin` |
| 用户/密码 | `yingxin` / `yingxin123` |
| root 密码 | `root123`（仅本地容器） |
| Redis | `127.0.0.1:6379`，无密码 |

> `lower_case_table_names=1` 只在数据卷**首次初始化**时生效。改了 compose 的该参数后，必须 `docker compose down -v` 重建卷，否则不生效。

### 5.1 如果要从零重建

```powershell
cd D:\code\se-yingxin-digital-human\backend
docker compose down -v
docker compose up -d
```

> ⚠️ `down -v` 会删除本地 MySQL/Redis 数据卷。只对开发库使用；课程虚构数据可以丢，生产库禁止。

---

## 6. 物理建表

### 6.1 执行顺序

必须按版本号从小到大执行：先 7 个 init，再 3 个增量。

1. `V20261009160001__admin_init.sql`
2. `V20261009160002__aid_init.sql`
3. `V20261009160003__auth_init.sql`
4. `V20261009160004__checkin_init.sql`
5. `V20261009160005__content_init.sql`
6. `V20261009160006__life_init.sql`
7. `V20261009160007__qa_init.sql`
8. `V20261025134001__content_add_guide_columns.sql`
9. `V20261025134002__admin_add_idx_role.sql`

### 6.2 Windows 执行命令

脚本里没有 `USE yingxin`，所以命令末尾必须带库名 `yingxin`。

#### 方式 A：CMD（推荐，文件按原字节送入，不受 PowerShell 编码影响）

```cmd
cd /d D:\code\se-yingxin-digital-human\backend

docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < db\migration\V20261009160001__admin_init.sql
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < db\migration\V20261009160002__aid_init.sql
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < db\migration\V20261009160003__auth_init.sql
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < db\migration\V20261009160004__checkin_init.sql
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < db\migration\V20261009160005__content_init.sql
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < db\migration\V20261009160006__life_init.sql
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < db\migration\V20261009160007__qa_init.sql
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < db\migration\V20261025134001__content_add_guide_columns.sql
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < db\migration\V20261025134002__admin_add_idx_role.sql
```

#### 方式 B：PowerShell

PowerShell 里 `<` 是保留字符，不能直接写 `mysql ... < file`。两种写法：

```powershell
cd D:\code\se-yingxin-digital-human\backend

# B1：交给 cmd 执行重定向（推荐）
cmd /c "docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < db\migration\V20261009160001__admin_init.sql"

# B2：Get-Content 管道（注意 UTF-8）
Get-Content -Raw -Encoding UTF8 .\db\migration\V20261009160001__admin_init.sql |
    docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin
```

> 含中文 `COMMENT` 的 SQL 建议用 B1；PowerShell 5.1 的管道编码可能把中文注释写坏。

#### 方式 C：PowerShell 批量执行

```powershell
cd D:\code\se-yingxin-digital-human\backend

$files = @(
  'V20261009160001__admin_init.sql',
  'V20261009160002__aid_init.sql',
  'V20261009160003__auth_init.sql',
  'V20261009160004__checkin_init.sql',
  'V20261009160005__content_init.sql',
  'V20261009160006__life_init.sql',
  'V20261009160007__qa_init.sql',
  'V20261025134001__content_add_guide_columns.sql',
  'V20261025134002__admin_add_idx_role.sql'
)

foreach ($f in $files) {
  Write-Host "==> $f"
  cmd /c "docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < db\migration\$f"
  if ($LASTEXITCODE -ne 0) { throw "执行失败：$f" }
}
```

#### 方式 D：本机已装 MySQL 客户端

```cmd
mysql -h127.0.0.1 -P3306 -uyingxin -pyingxin123 yingxin < db\migration\V20261009160001__admin_init.sql
```

PowerShell 下仍要注意 `<` 的问题，用 `cmd /c` 包一层。

### 6.3 只建本组表

- 只做本组：执行对应服务的 `*_init.sql` 即可（无外键，互不依赖）。
- 但建议至少把 `auth_*` 也建上：多数业务表逻辑引用 `user_id`，联调时要用。
- 如果做完整演示：9 个脚本全执行。

### 6.4 校验

```powershell
# 表数量：应为 24
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin -e "SELECT COUNT(*) AS tables_count FROM information_schema.tables WHERE table_schema='yingxin';"

# 表清单
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin -e "SELECT table_name FROM information_schema.tables WHERE table_schema='yingxin' ORDER BY table_name;"

# 物理外键数量：应为 0
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin -e "SELECT COUNT(*) AS fk_count FROM information_schema.table_constraints WHERE constraint_schema='yingxin' AND constraint_type='FOREIGN KEY';"

# 三个增量是否生效
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin -e "SHOW COLUMNS FROM content_article LIKE 'guide_type';"
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin -e "SHOW INDEX FROM admin_user_role WHERE Key_name='idx_role';"
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin -e "SHOW COLUMNS FROM auth_user LIKE 'password_hash';"
```

重点：两个 2026-10-25 的增量脚本与 2026-11-03 的口令增量最容易漏。如果库是 2026-10-09 建的，`content_article` 可能缺 `guide_type/contact`，`admin_user_role` 可能缺 `idx_role`，`auth_user.password_hash` 可能仍可空（`Null` 列为 `YES`）。

### 6.5 常见失败

| 现象 | 原因 | 处理 |
|---|---|---|
| `Table 'xxx' already exists` | 脚本已执行过；没有 Flyway 历史表，无法自动判断 | 先按 6.4 校验；已齐全就跳过；不确定就开发库 `down -v` 重建 |
| `Unknown database 'yingxin'` | 库没建 | `docker compose up -d` 会自动建；或手工 `CREATE DATABASE yingxin ...` |
| `Access denied` | 账号/密码不对 | 本地是 `yingxin/yingxin123`；root 是 `root123` |
| `Duplicate key name 'idx_role'` | 增量脚本重复执行 | 说明已生效，跳过 |
| 表名大小写异常 | 卷不是首次初始化，`lower_case_table_names` 没生效 | `docker compose down -v` 重建 |
| PowerShell 报 `<` 相关错误 | PowerShell 不支持 `<` 重定向 | 用 `cmd /c "..."` 或 `Get-Content | ...` |

---

## 7. 持久化代码样板（S5）

### 7.1 为什么选 `qa-service` + `qa_session`

- `AGENTS.md` 建议 S5 纵向打样放 `qa-service`。
- `qa_session` 字段少（无 JSON、无大文本、无 ngram 全文索引），最适合做“能跑通”的最小闭环。
- 不要选 `qa_question` 做 H2 测试：它的 `FULLTEXT ... WITH PARSER ngram` 只有 MySQL 支持。

### 7.2 文件清单

在 `backend/services/qa-service/src/main/java/com/sicnu/yingxin/qa/` 下新增：

```
entity/QaSession.java
mapper/QaSessionMapper.java
service/QaSessionService.java
controller/QaSessionController.java
```

测试放：

```
src/test/java/com/sicnu/yingxin/qa/service/QaSessionServiceTest.java
```

### 7.3 Entity

```java
package com.sicnu.yingxin.qa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话表 qa_session。
 * 本项目不使用 @TableLogic：deleted = 0 表示未删除，删除时写入本行 id。
 */
@Data
@TableName("qa_session")
public class QaSession {

    /** 雪花 ID，非自增 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Integer channel;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer messageCount;
    private Integer satisfied;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT)
    private Long deleted;
}
```

### 7.4 Mapper

```java
package com.sicnu.yingxin.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sicnu.yingxin.qa.entity.QaSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QaSessionMapper extends BaseMapper<QaSession> {
}
```

### 7.5 Service

```java
package com.sicnu.yingxin.qa.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sicnu.yingxin.qa.entity.QaSession;
import com.sicnu.yingxin.qa.mapper.QaSessionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class QaSessionService {

    private final QaSessionMapper mapper;

    public QaSessionService(QaSessionMapper mapper) {
        this.mapper = mapper;
    }

    public QaSession create(Long userId, Integer channel) {
        QaSession session = new QaSession();
        session.setUserId(userId);
        session.setChannel(channel);
        session.setStartTime(LocalDateTime.now());
        session.setMessageCount(0);
        mapper.insert(session);   // id 与审计字段由 MP + AuditMetaObjectHandler 填充
        return session;
    }

    public QaSession get(Long id) {
        return mapper.selectOne(Wrappers.<QaSession>lambdaQuery()
                .eq(QaSession::getId, id)
                .eq(QaSession::getDeleted, 0L));   // 显式逻辑删除条件
    }

    public void delete(Long id) {
        QaSession session = new QaSession();
        session.setId(id);
        session.setDeleted(id);   // 删除写本行 id，不是 1
        mapper.updateById(session);
    }
}
```

### 7.6 Controller

```java
package com.sicnu.yingxin.qa.controller;

import com.sicnu.yingxin.common.core.result.Result;
import com.sicnu.yingxin.qa.entity.QaSession;
import com.sicnu.yingxin.qa.service.QaSessionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qa/sessions")
public class QaSessionController {

    private final QaSessionService service;

    public QaSessionController(QaSessionService service) {
        this.service = service;
    }

    @PostMapping
    public Result<QaSession> create(@RequestParam Long userId,
                                    @RequestParam(defaultValue = "1") Integer channel) {
        return Result.ok(service.create(userId, channel));
    }

    @GetMapping("/{id}")
    public Result<QaSession> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
```

### 7.7 五个硬约束

1. **主键**：`id BIGINT NOT NULL` 且无 `AUTO_INCREMENT`，必须 `@TableId(type = IdType.ASSIGN_ID)`；不写会插入失败。
2. **审计字段**：`create_time/update_time NOT NULL`，实体必须加 `@TableField(fill = ...)`，否则插入报错。
3. **逻辑删除**：`deleted` 是 BIGINT，不用 `@TableLogic`；查询显式 `deleted = 0`，删除写行 `id`。
4. **操作人**：`create_by/update_by` 由 `AuditMetaObjectHandler` 从 `UserContextHolder` 取；无登录态时写系统用户（0）。
5. **分页**：统一用 `Page`，Service 转 `PageResult`；不要在 SQL 里手写 `limit`（`MybatisPlusConfig` 已配分页拦截器）。

### 7.8 启动与验证

```powershell
cd D:\code\se-yingxin-digital-human\backend
.\mvnw.cmd -pl services/qa-service spring-boot:run
```

直连 8101 验证（不需要网关 token）：

```powershell
# 写入
Invoke-RestMethod -Method Post "http://localhost:8101/api/qa/sessions?userId=123&channel=1"

# 查询（把上一步返回的 id 填进去）
Invoke-RestMethod -Method Get "http://localhost:8101/api/qa/sessions/<id>"
```

数据库确认：

```powershell
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin -e "SELECT id, user_id, channel, message_count, create_time, deleted FROM qa_session ORDER BY create_time DESC LIMIT 5;"
```

期望：`deleted = 0`，`create_time` 非空，`id` 是雪花 ID。

> 直连 8101 时没有 `X-User-*` 用户上下文，`create_by/update_by` 会是系统用户 0。想验证真实操作人，走网关 8080 + 本地令牌夹具：
> 1. 启动 auth-service 时设置 `AUTH_FIXTURE_ENABLED=true`（`application.yml` 里映射到 `yingxin.auth.fixture.enabled`）；
> 2. `POST http://localhost:8100/api/dev/token?userId=123&role=STUDENT`，响应体是 `{code,message,data}`，令牌在 `data.token`；
> 3. 请求头带 `Authorization: Bearer <token>` 调 `http://localhost:8080/api/qa/sessions`。
>
> PowerShell 示例：
>
> ```powershell
> $resp  = Invoke-RestMethod -Method Post "http://localhost:8100/api/dev/token?userId=123&role=STUDENT"
> $token = $resp.data.token
> Invoke-RestMethod -Method Post "http://localhost:8080/api/qa/sessions?userId=123&channel=1" `
>   -Headers @{ Authorization = "Bearer $token" }
> ```
>
> 该夹具默认关闭，不在网关白名单，业务实现后应删除。

### 7.9 测试

先写不连库的单元测试：

```java
@ExtendWith(MockitoExtension.class)
class QaSessionServiceTest {

    @Mock
    QaSessionMapper mapper;

    @InjectMocks
    QaSessionService service;

    @Test
    void create_fillsBusinessFields() {
        QaSession result = service.create(123L, 1);

        assertThat(result.getUserId()).isEqualTo(123L);
        assertThat(result.getChannel()).isEqualTo(1);
        assertThat(result.getMessageCount()).isZero();
        verify(mapper).insert(result);
    }
}
```

如果要自动化集成测试：

- 用 **Testcontainers MySQL**，不要用 H2 跑真实 DDL（`DATETIME(3)`、索引、`COMMENT`、ngram 解析器兼容性差）。
- 容器起来后执行 `backend/db/migration` 的目标脚本，再跑 Mapper 测试。
- 课程不要求自动化测试时，7.8 的“启动 + curl + SQL 查回”已经足够验收。

---

## 8. 建模工具与证据链

### 8.1 当前流程（已在仓库文档中定稿）

```
改 docs/数据建模.md
  → 重新生成 docs/数据建模.dbml（勿手改）
  → dbdiagram.io 导入 .dbml → 出 ER 图 → Export MySQL DDL
  → 删 FOREIGN KEY（含 Ref 导出的约束）
  → 核对表名/字段名全小写
  → 补齐 id/create_time/update_time/create_by/update_by/deleted
  → 按 docs/开发规范.md §5.3 改写成 backend/db/migration/*.sql
```

依据：`docs/数据建模.md:1002-1018`、`docs/开发规范.md:280-286`。

### 8.2 “工具是否集成”的准确回答

- **工具本身**：dbdiagram.io 是网页 SaaS，没有、也不需要“集成”进仓库或 CI。
- **产物与流程**：已集成。仓库有 `.dbml` 源、拆图脚本、4 份 ER 视图、4 张 ER 图、9 个迁移脚本。
- **证据链**：缺 dbdiagram 导出的原始 DDL 和一张有效截图。建议补：
  - `docs/assets/db/dbdiagram-24-tables.png`（24 张表的完整截图）
  - `docs/assets/db/dbdiagram-export.sql`（原始导出，文件头注明“仅存档，未删外键；权威 DDL 以迁移脚本为准”）
- **如果老师要求工具直接建表**：dbdiagram 只能导出 DDL；需要 Navicat / PowerDesigner / MySQL Workbench 的正向工程。当前方案是“工具导出 DDL → 执行 SQL”，要提前确认老师接受这个口径。

### 8.3 提交前注意

- `docs/数据库设计说明书-*.docx`、`docs/assets/er/*.png`、`dbdiagram-initial.png` 目前是未跟踪文件；如果课程要用 git 提交，记得 `git add`。
- `.playwright-mcp/`、`.serena/` 不要提交。
- 迁移脚本是权威 DDL；不要直接把 dbdiagram 原始导出当最终脚本用（会带外键/驼峰/漏通用字段）。

---

## 9. 已知缺口与文档漂移

| 项 | 现状 | 建议 |
|---|---|---|
| Flyway | 未接入；父 pom 无 Flyway、无 `db` profile | 课程阶段可先手工执行；规范化时再补 `db` profile + `flyway-maven-plugin`，业务服务不引 Flyway |
| 迁移执行入口 | 没有脚本/README 命令 | 可加 `scripts/db-migrate.ps1`（Windows）或按本文第 6 节执行 |
| 三个增量脚本 | 2026-10-25 两个 + 2026-11-03 一个（口令非空），容易漏 | 按 6.4 校验 `guide_type` / `idx_role` / `password_hash` 非空 |
| 持久化样板 | 0 个 Entity/Mapper/Service | 按第 7 节补 `qa_session` |
| `AGENTS.md` 迁移数量 | 已同步为 10 个文件（7 init + 3 增量） | 无需再改 |
| `AGENTS.md:10` | 写“auth 已有 AuthUser 实体 + Mapper” | 已在 `5419405` 删除，应改为“无持久化样板” |
| `docs/数据建模.md:1008` | 写“§9.1 的四个坑”，实际 §9.1 有五条 | 改为五个 |
| `docs/环境搭建指南.md` | 只写起 MySQL/Redis，没有迁移命令 | 补一节“执行 db/migration” |
| dbdiagram 证据 | 截图空白、原始 DDL 未留档 | 按 8.2 补 |

---

## 10. 验收清单

- [ ] `docker compose ps`：`yingxin-mysql`、`yingxin-redis` 均 healthy
- [ ] `SHOW TABLES`：24 张，表名全小写
- [ ] 物理外键数量：0
- [ ] `content_article.guide_type` 存在
- [ ] `admin_user_role.idx_role` 存在
- [ ] `.\mvnw.cmd -pl services/qa-service spring-boot:run` 能启动
- [ ] `POST /api/qa/sessions` 返回雪花 `id`
- [ ] `GET /api/qa/sessions/{id}` 能查回
- [ ] `qa_session` 表中 `deleted = 0`、`create_time` 非空
- [ ] 至少一条 `qa_session` 记录能在 MySQL 中直接 `SELECT` 到
- [ ] dbdiagram 24 表截图 + 导出 DDL 留档（如果课程要求）
- [ ] 各组只执行自己服务的 init 时，表也能独立建出来（无外键依赖）

---

## 11. 参考文档

| 文档 | 用途 |
|---|---|
| `docs/数据建模.md` | 24 张表的权威定义；§9 建模与迁移落地流程 |
| `docs/数据建模.dbml` | 导入 dbdiagram.io 的模型源文件（勿手改） |
| `docs/开发规范.md` §5 | 表命名、通用字段、Flyway、缓存、敏感数据 |
| `docs/需求分析文档.md` §7.2 | DI-1–DI-22 完整性规则 |
| `docs/需求分析文档.md` §8.4 | 24 表裁剪口径；单人阶段允许 init-sql + down -v |
| `docs/环境搭建指南.md` | JDK/Node/Docker/前后端启动 |
| `backend/docker-compose.yml` | MySQL 8.0.46 + Redis 7.0.15 本地编排 |
| `backend/db/migration/` | 9 个迁移脚本（7 init + 2 增量） |
| `AGENTS.md` | 工程阶段界碑、夹具、架构边界 |

---

**文档状态**：2026-10-27 依据当前分支整理；如后续补了 Flyway、迁移入口或持久化样板，请同步更新第 2、7、9 节。
