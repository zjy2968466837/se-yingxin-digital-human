# 大学生迎新导引服务数字人系统

面向高校新生报到全流程的 AI 数字人智能服务系统。以 3D/2D 数字人为交互入口，整合迎新政策、报到流程、校园导航、资助、军训、宿舍、生活、社团等综合服务。

## 系统组成

| 子系统 | 技术栈 | 目录 |
|--------|--------|------|
| 学生端网页（主入口，移动浏览器优先） | uni-app 3 + Vue3（取 H5 构建产物；工程方案待定，见《需求分析文档》附录 B Q-9） | `frontend/miniprogram` |
| 管理后台（学校运维） | Vue3 + Vite 8 + Element Plus | `frontend/admin-web` |
| 后端微服务 | Java 17 + Spring Boot 3.5 + Spring Cloud 2025（9 个服务，Maven 多模块） | `backend/services` |
| 基础设施 | 本地 Docker `mysql:8.0.46` + `redis:7.0.15-alpine`；云端 Azure MySQL PaaS + VM Redis 7.0.15 | `backend/docker-compose.yml` |

> 本地与云上共用同一份配置，只靠环境变量区分（代码零改动）。版本基线详见
> [docs/开发规范.md](docs/开发规范.md) §10，云端编排见 [`backend/docker-compose.prod.yml`](backend/docker-compose.prod.yml)。

## 核心功能

1. 身份核验与个性化迎新（对接学校统一身份认证）
2. 报到全流程导引（线上预报到→缴费→绿色通道→领物资→入住→人脸采集→校园卡激活）
3. 智能问答（问答库 + 意图理解 + 多轮对话，语音/文字）
4. 校园 AR/3D 导航（室内外路径规划、数字人带路）
5. 资助与绿色通道服务
6. 生活服务聚合（报修、缴费、食堂、快递、就医）
7. 安全与防诈骗教育
8. 社团与入学适应
9. 管理后台（话术配置、问答库管理、数据统计、分权维护）
10. 双端适配（学生端网页 + 管理后台；小程序 / H5 / 官网嵌入 / PC 大屏等多端方案已裁剪，见《需求分析文档》§8.4.1）

## 当前实现进度（feat/core-chain 分支）

核心链路已可端到端演示：

| 模块 | 状态 | 说明 |
|------|------|------|
| 统一鉴权（auth-service） | 已完成 | 学号/后台登录、JWT + Redis 会话（复用脚手架 JwtService/SessionService）、注册→审批流 |
| 禅道式 RBAC 权限中心 | 已完成 | user→group→grouppriv 模型、权限矩阵在线调整、审计日志、鉴权链路追踪 |
| 网关统一鉴权（gateway） | 已完成 | 脚手架 AuthGlobalFilter 基础上放行 /api/auth/register |
| 报到服务（checkin-service） | 已完成 | 步骤清单、幂等打卡、进度、报到凭证 |
| 问答服务（qa-service） | 已完成 | 问答库检索（2-gram 相似度）、Redis 热点缓存、未解答记录、管理端 CRUD |
| 消息队列事件流 | 已完成 | RabbitMQ 事件队列 + event-worker 攒批落库 + 重试 3 次进死信队列 |
| 数据看板（admin-service） | 已完成 | KPI/分时趋势/学院进度/热门问题/命中分布，Redis 版本号缓存 5 秒轮询 |
| 管理后台（admin-web） | 已完成 | 登录、数据看板、用户审批、角色与权限矩阵、问答库、报到管理、审计日志 |
| 导航/资助/生活/内容 | 骨架 | ping 接口，按接口文档待实现 |

表结构变更已同步 [docs/数据建模.md](docs/数据建模.md) §10「二期新增表」。

## 快速启动（本地演示）

前置：JDK 17+、Node 22+、pnpm、Docker Desktop。

```bash
cd backend
scripts/start-dev.cmd          # 基础设施(MySQL/Redis/RabbitMQ) + Flyway 迁移 + 7 个服务
# 可选：灌入演示数据（40 名新生、历史报到/问答埋点），看板数据更饱满
docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < scripts/seed-demo-data.sql
docker exec yingxin-redis redis-cli INCR yx:admin:dash:version
# 可选：模拟一批新生真实登录/打卡/提问（事件经 MQ → worker → 看板刷新）
node scripts/demo-load.mjs 12

cd ../frontend/admin-web && pnpm install && pnpm dev   # http://localhost:5173
```

演示账号（密码均为 `123456`）：`admin`（管理员入口）、`ST2026001`–`ST2026040`（学生）、
`T2026001`（辅导员组）、`C2026001`（学院管理员组，初始待审批，用于演示审批流）。
RabbitMQ 控制台 http://localhost:15672（guest/guest）。

## 文档索引

- [环境搭建指南](docs/环境搭建指南.md) ← **新人先看这个**
- [开发规范](docs/开发规范.md)（横向能力框架、分层、接口 / 数据 / 前端契约、**§10 环境与版本基线**）← **写代码前先看这个**
- [需求规格说明书](docs/需求规格说明书.md)（立项阶段原始需求 v1.0，范围调整以需求分析文档为准）
- [需求分析文档](docs/需求分析文档.md)（FR / NFR / IR / DR 编号体系 + 追踪矩阵；**§2.7 里程碑 / 分工 / 风险**、**§8.4 范围裁剪**；9 张架构图已嵌入）
- [功能清单与分解](docs/功能清单.md)（按模块拆分的功能项，含 ID/优先级/状态，可直接用于 Issue 拆分）
- [数据建模](docs/数据建模.md)（**一期 24 张表**字段级定义 + ER 图 + 唯一键与状态机 + 无外键下的完整性对策 + 数据字典）／模型文件 [数据建模.dbml](docs/数据建模.dbml)（导入 dbdiagram.io 出图并导出 DDL）
- [技术难点分析](docs/技术难点分析.md)（难点拆解、降级方案与 M4 前置 Spike 计划）
- [贡献指南](CONTRIBUTING.md)

> **文档合并说明（2026-10-09）**：原《项目计划》已并入《需求分析文档》§2.7；原《开发环境版本》已并入《开发规范》§10；
> 原《部署指南》已从仓库移除（云端编排的事实来源改为 `backend/docker-compose.prod.yml`，部署说明待补）。

> 环境一致性已由仓库内的机器可读配置保证：`.gitattributes`（换行符）、`.editorconfig`（编码缩进）、
> `.nvmrc`/`.node-version`（Node）、`.tool-versions`（JDK/Node）、`backend/mvnw`（Maven）、
> 前端 `packageManager` + 锁文件。CI 会在推送与 PR 时于干净环境验证构建。
