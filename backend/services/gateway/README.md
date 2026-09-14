# gateway（API 网关）

统一入口，按路径前缀把请求转发到各微服务。端口 `8080`。

## 技术要点

- Spring Cloud Gateway（WebFlux）。**网关不能引入 `spring-boot-starter-web`**，否则与 WebFlux 冲突启动失败。
- 配置前缀为 `spring.cloud.gateway.server.webflux.*`（Spring Cloud Gateway 4.3+ 的新前缀），
  旧的 `spring.cloud.gateway.routes` 会被静默忽略。

## 路由约定

`/api/<服务简称>/**` → 对应服务，**转发不裁剪前缀**，因此直连与走网关路径完全一致。

| 路径 | 目标 | 端口 |
|------|------|------|
| `/api/auth/**` | auth-service | 8100 |
| `/api/qa/**` | qa-service | 8101 |
| `/api/checkin/**` | checkin-service | 8102 |
| `/api/navigation/**` | navigation-service | 8103 |
| `/api/aid/**` | aid-service | 8104 |
| `/api/life/**` | life-service | 8105 |
| `/api/content/**` | content-service | 8106 |
| `/api/admin/**` | admin-service | 8107 |

新增服务时在 `src/main/resources/application.yml` 补一条路由，并开放 `management.endpoints.web.exposure`
中的 `gateway` 端点以便排查。

## 排查

```bash
curl http://localhost:8080/actuator/health              # 健康
curl http://localhost:8080/actuator/gateway/routes      # 已注册路由快照
```
