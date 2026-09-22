# AGENTS.md

本文件供 AI 编码代理使用。项目概览、核心功能与文档索引见 [README.md](README.md)，从零搭环境见 [docs/环境搭建指南.md](docs/环境搭建指南.md)。

## 当前实现状态（写代码前必读）

仓库仍处于**骨架阶段**。以下基础设施**尚不存在**，不要假设可以直接调用：

- 后端：每个服务只有 `XxxApplication` 启动类 + 一个 `PingController` 自检接口。
  没有统一返回体、全局异常处理、Service/Mapper/Entity 分层、参数校验、日志规范，也**没有任何测试类**。
- 鉴权：`auth-service` 目前没有任何认证逻辑——没有 JWT 依赖、没有签发/校验、没有拦截器或 Filter，
  网关也没有鉴权过滤器。前端发的 `Bearer` token 暂时无人验证。
- 数据库：**没有表结构版本管理**（无 Flyway/Liquibase），`backend/init-sql/01-init.sql` 只执行 `CREATE DATABASE`。
- 前端：`admin-web` 只有登录页与连通性自检页（`QaView.vue` 是空占位），没有列表/表单等 CRUD 范例可抄；
  `miniprogram` 只注册了首页与「我的」两个页面。

补充这些基础设施时，请先按 [docs/开发规范.md](docs/开发规范.md) 定下的横向能力框架落地，**不要在每个服务里各写一套**。
该文档已确定：`common-core` + `common-web` 两模块划分（网关是 WebFlux、业务服务是 MVC，不可混用同一个公共模块）、
错误码分段与段位所有权、网关鉴权与用户上下文透传（`X-User-*` 头先剥离再注入）、表命名与集中式 Flyway 迁移、缓存 key 与日志规范。

## 常用命令

```bash
# 本地基础设施：只把 MySQL + Redis 跑在容器里（9 个微服务跑在宿主机）
cd backend && docker compose up -d

# 后端：全量构建 / 单服务启动（务必用仓库自带的 mvnw）
cd backend && ./mvnw clean install -DskipTests
cd backend && ./mvnw -pl services/qa-service spring-boot:run

# 前端：两个项目相互独立，各自安装依赖，不是 pnpm monorepo
cd frontend/admin-web && pnpm install && pnpm dev              # http://localhost:5173
cd frontend/miniprogram && pnpm install && pnpm dev:mp-weixin   # 产物 dist/dev/mp-weixin，用微信开发者工具导入
```

国内网络可先把 `docs/maven-settings.xml` 复制到 `~/.m2/settings.xml` 加速；CI 故意不配置镜像，因此构建不得依赖它。

## 架构边界

- 后端：`backend/pom.xml` 只做模块聚合，**没有 common 公共模块**；包根 `com.sicnu.yingxin.<服务名>`。
  端口：gateway `8080`、auth `8100`、qa `8101`、checkin `8102`、navigation `8103`、aid `8104`、life `8105`、content `8106`、admin `8107`。
- 网关路由写在 `backend/services/gateway/src/main/resources/application.yml`，前缀 `/api/<服务>/**`，
  **转发不裁剪前缀**——直连 `8101` 与走网关 `8080` 的路径完全一致。CORS 也在这份 yml 里全局配置。
- 前端两个项目不共享代码：`admin-web` 用 Vue 3 + Vite 8 + Element Plus（`main.js` 里**全量注册**，无按需自动导入）+ Pinia + vue-router；
  `miniprogram` 用 uni-app 3 + Vue 3 `<script setup>`，构建目标 `mp-weixin`。

## 项目约定

- **配置**：`application.yml` 一律用 `${环境变量:本地默认值}` 占位，本地与云端共用同一份文件、代码零改动。
  沿用既有变量名：`MYSQL_HOST/PORT/DB/USER/PASSWORD/SSL_MODE`、`REDIS_HOST/PORT/PASSWORD/DB`。
- **前端请求**：管理后台统一走 `src/api/request.js` 的 axios 实例（`VITE_API_BASE`，默认 `/api`），
  按服务拆分到 `src/api/<服务>.js`。token 同时存在 Pinia (`src/stores/user.js`) 与 `localStorage` 的
  `yingxin_token` 两处，改动时必须同步；响应拦截器目前只弹错误提示，**不会**因 401 自动跳登录。
- **Java 风格**：注释用中文，类名 `XxxApplication` / `XxxController`。
- **提交**：Conventional Commits；分支前缀 `feat/` `fix/` `docs/`；禁止直接 push `main` / `dev`。
  详见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 陷阱

- `frontend/miniprogram` 的 **Vite 锁死 `5.2.8`**（`@dcloudio/vite-plugin-uni` 的 peerDependency 精确锁定），
  升到 6/7/8 直接编译失败；`admin-web` 用的是 Vite 8。**两者版本本就不同，不要"统一升级"**。
  uni-app 各 `@dcloudio/*` 包必须用同一批次号（`3.0.0-5020420260813001`），npm 的 `latest` 标签是 2021 年的陈旧 alpha。
- 网关配置前缀必须是 `spring.cloud.gateway.server.webflux.*`；写回旧版的 `spring.cloud.gateway.routes` 会**静默失效**。
- 不要用系统 `mvn`：Maven 版本由 `backend/mvnw` 固定（首次运行需联网下载）。
- CI 用 `pnpm install --frozen-lockfile`：改了 `package.json` 就必须本地 `pnpm install` 同步锁文件，否则 CI 直接失败。
- 本地 MySQL 的 `lower_case_table_names=1` **只在数据卷首次初始化时生效**，改动后需 `docker compose down -v` 重建。
- 本地与云端环境相反：云端 MySQL 强制 SSL（`MYSQL_SSL_MODE=REQUIRED`）、Redis 带密码；本地都是无密码/无 SSL。
  涉及数据库与缓存的改动要同时兼顾两种环境，见 [docs/部署指南.md](docs/部署指南.md)。
- `frontend/miniprogram/src/api/request.js` 的 baseURL 目前硬编码 `http://localhost:8080/api`，
  真机调试需改域名或关闭合法域名校验。
- 本地同时启动 9 个服务约需 3.8G 内存，按需只起目标服务即可。
