# 后端

Java 17 + Spring Boot 3 微服务架构。

## 目录

```
backend/
├── docker-compose.yml        # 本地 Redis 7（仅开发缓存）
├── .env.example              # 环境变量模板（可提交）
├── application-dev.example.yml
├── application-prod.example.yml
├── init-sql/                 # 首次启动自动执行的建库建表脚本
└── services/                 # 微服务（Maven 多模块，骨架待建）
    ├── gateway/              # API 网关（8080）
    ├── auth-service/         # 统一身份认证、人脸轻核验、会话
    ├── qa-service/           # 智能问答：问答库管理、意图理解、多轮对话
    ├── checkin-service/      # 报到流程：步骤引导、状态查询、待办推送
    ├── navigation-service/   # 校园导航：室内外地图、路径规划
    ├── aid-service/          # 资助与绿色通道
    ├── life-service/         # 生活服务聚合：报修、缴费、食堂、快递
    ├── content-service/      # 安全教育、社团、公告、话术/问答库发布
    └── admin-service/        # 管理后台支撑：统计、权限、运维
```

每个服务统一约定：

- 端口：网关 8080，业务服务从 8100 起分配
- 数据库：`yingxin` 库内按服务分表（课程项目规模不拆多库），Redis 缓存热点问答
- 接口：RESTful + WebSocket（数字人实时交互）
- 文档：每个服务集成 springdoc，路径 `/swagger-ui.html`
- 配置：公共配置放 `application.yml`，环境差异用 `application-*.yml` + `.env` 注入；敏感信息不入库

## Maven 阿里云镜像

`~/.m2/settings.xml` 缺失或下载慢时：

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

## 启动顺序

1. 复制模板：`cp .env.example .env`，按开发环境填 Azure 开发库连接信息
2. `docker compose up -d`（backend/ 目录下，仅启动本地 Redis）
3. 启动 `services/gateway`
4. 其余按需启动
