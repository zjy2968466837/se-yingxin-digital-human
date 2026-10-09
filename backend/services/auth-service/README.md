# auth-service（统一身份认证）

统一身份认证、人脸轻核验、会话管理。端口 `8100`，路径前缀 `/api/auth`。

## 自检与文档

```bash
curl http://localhost:8100/api/auth/ping          # 直连
curl http://localhost:8080/api/auth/ping            # 经网关
```

Swagger：<http://localhost:8100/swagger-ui.html>

## 结构

```
src/main/java/com/sicnu/yingxin/auth/
├── AuthServiceApplication.java   # 启动类
├── config/AuthProperties.java    # JWT_SECRET / 令牌 TTL / 夹具开关
├── service/JwtService.java       # JWT 签发与校验（HS256，负载只放 userId/role）
├── service/SessionService.java   # Redis 会话（yx:auth:session:<jti>，TTL 与令牌一致）
├── fixture/DevTokenFixtureController.java   # 【夹具】本地验证用，默认关闭
└── controller/PingController.java
src/main/resources/
└── application.yml               # 连接信息走环境变量，本地有默认值
```

数据库表统一建在 `yingxin` 库（按服务分表），Redis 用于会话与缓存。

## 当前状态（阶段界碑见 AGENTS.md）

- **已具备的能力**：JWT 签发与校验、Redis 会话（TTL 与令牌一致）。
- **尚未实现**（属业务阶段，按 FR-1）：学号密码登录、微信登录、人脸轻核验、令牌续期。
- **开发夹具**（默认关闭）：`AUTH_FIXTURE_ENABLED=true` 启动后暴露
  `POST /api/dev/token?userId=&role=` 与 `DELETE /api/dev/session/{jti}`，用于验证网关鉴权与"登出即失效"；
  该路径不在网关白名单，只能直连本服务。**上云前必须为 false；业务登录实现后删除夹具。**

约定与版本基线见 [../README.md](../README.md)，云上部署编排见 [../../docker-compose.prod.yml](../../docker-compose.prod.yml)。
