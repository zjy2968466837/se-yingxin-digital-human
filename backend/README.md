# 后端

Java 17 + Spring Boot 3 微服务架构。

## 目录

```
backend/
├── pom.xml                   # Maven 父 POM（聚合 9 个服务，统一版本与依赖管理）
├── docker-compose.yml        # 本地 MySQL 8 + Redis 7（版本与云端对齐）
├── docker-compose.prod.yml   # 云上只编排后端服务（MySQL/Redis 用 Azure 托管资源）
├── .dockerignore
├── init-sql/                 # 首次启动自动执行的建库脚本
└── services/                 # 9 个微服务（Maven 多模块）
    ├── gateway/              # API 网关（8080，Spring Cloud Gateway / WebFlux）
    ├── auth-service/         # 统一身份认证、人脸轻核验、会话（8100）
    ├── qa-service/           # 智能问答（8101）
    ├── checkin-service/      # 报到流程（8102）
    ├── navigation-service/   # 校园导航（8103）
    ├── aid-service/          # 资助与绿色通道（8104）
    ├── life-service/         # 生活服务聚合（8105）
    ├── content-service/      # 安全教育、社团、公告（8106）
    └── admin-service/        # 管理后台支撑（8107）
```

每个业务服务结构一致：`XxxApplication` 启动类 + `controller/PingController`（连通性自检）+ `application.yml` + `mapper/`（MyBatis XML 目录）。

## 版本基线

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | Spring Boot 3 要求 |
| Spring Boot | 3.5.16 | 3.5.x 最新维护版 |
| Spring Cloud | 2025.0.3 | 必须与 Boot 3.5.x 配套 |
| MyBatis-Plus | 3.5.17 | `mybatis-plus-spring-boot3-starter` |
| springdoc | 2.9.1 | **2.x 对应 Boot 3；3.x 对应 Boot 4，切勿混用** |
| mysql-connector-j | 9.7.0 | 由 Boot BOM 管理，兼容云端 MySQL 8.0.46 |
| Lettuce | 6.6.0 | 由 Boot BOM 管理，兼容云端 Redis 7.0.15 |

> 与云端一致性（2026-09 实测）：云端 MySQL 为 Azure PaaS `8.0.46-azure`，云端 Redis 为
> `7.0.15`；`docker-compose.yml` 的镜像版本已按此钉死（`mysql:8.0.46`、`redis:7.0.15-alpine`）。

## 构建与运行

```bash
cd backend
mvn clean install -DskipTests      # 首次构建（父 POM 会聚合全部 9 个模块）

# 启动网关
cd services/gateway && mvn spring-boot:run
# 启动业务服务（示例）
cd services/auth-service && mvn spring-boot:run
```

也可直接跑打包后的可执行 jar：

```bash
java -jar services/gateway/target/gateway-1.0.0-SNAPSHOT.jar
```

## 网关路由约定

网关按路径前缀转发，**转发时不做前缀裁剪**，因此服务直连与走网关的路径完全一致：

| 路径前缀 | 目标服务 | 直连端口 |
|----------|----------|----------|
| `/api/auth/**` | auth-service | 8100 |
| `/api/qa/**` | qa-service | 8101 |
| `/api/checkin/**` | checkin-service | 8102 |
| `/api/navigation/**` | navigation-service | 8103 |
| `/api/aid/**` | aid-service | 8104 |
| `/api/life/**` | life-service | 8105 |
| `/api/content/**` | content-service | 8106 |
| `/api/admin/**` | admin-service | 8107 |

自检示例：`curl http://localhost:8080/api/auth/ping`（经网关）与
`curl http://localhost:8100/api/auth/ping`（直连）返回相同结果。

各服务 Swagger 直连查看：`http://localhost:8100/swagger-ui.html`。

> ⚠️ Spring Cloud Gateway 4.3+（2025.0.x）配置前缀已变更为
> `spring.cloud.gateway.server.webflux.*`，旧的 `spring.cloud.gateway.routes` 不再生效。

## 配置与端口

- 端口：网关 8080，业务服务 8100–8107
- 数据库：`yingxin` 库内按服务分表（课程项目规模不拆多库），Redis 缓存热点问答
- 接口：RESTful + WebSocket（数字人实时交互）
- 文档：每个服务集成 springdoc，路径 `/swagger-ui.html`
- 配置：`application.yml` 全部走环境变量占位，本地用默认值、云上用环境变量覆盖，**代码零改动**

常用环境变量（均有本地默认值，云上覆盖）：

| 变量 | 本地默认 | 云上 |
|------|----------|------|
| `MYSQL_HOST` / `MYSQL_PORT` | `localhost` / `3306` | Azure MySQL 主机 |
| `MYSQL_DB` / `MYSQL_USER` / `MYSQL_PASSWORD` | `yingxin` / `yingxin` / `yingxin123` | Azure 库与账号 |
| `MYSQL_SSL_MODE` | `PREFERRED` | `REQUIRED`（Azure 强制 SSL） |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | `127.0.0.1` / `6379` |
| `REDIS_PASSWORD` | 空（本地无密码） | 云上 Redis 的 requirepass |

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

1. `docker compose up -d`（backend/ 目录下，起本地 MySQL + Redis）
2. `services/gateway`
3. 其余按需启动

云上部署见 [../docs/部署指南.md](../docs/部署指南.md)。
