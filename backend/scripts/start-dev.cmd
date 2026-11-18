@echo off
rem 本地一键启动：基础设施（docker compose）+ 7 个核心服务（含 event-worker）
rem 前置条件：已安装 Docker Desktop 并运行；已执行 mvnw -DskipTests package
rem 用法：scripts\start-dev.cmd
rem
rem Flyway 迁移说明：
rem   -N 关闭严格模式（跳过Flyway 版本校验告警），-Pdb 激活 db 迁移 profile；
rem   迁移文件为 V202610091600__*_init.sql（各服务初始表结构）与
rem   V202610191200__auth_rbac.sql（RBAC/用户组与审核列），执行后再灌演示数据：
rem   docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < scripts\seed-demo-data.sql
rem
rem 网关白名单说明（本地演示）：services\gateway\application.yml 的 extra-whitelist
rem 已放行 /api/auth/register 与 /actuator/**，方便演示注册与健康检查；
rem 云端部署必须收敛该白名单。

cd /d %~dp0..

echo [1/3] 启动基础设施容器（MySQL 13306 / Redis 6379 / RabbitMQ 5672）...
docker compose up -d
if errorlevel 1 (
  echo Docker 未运行或启动失败，请先启动 Docker Desktop。
  exit /b 1
)

echo [2/3] 初始化数据库迁移（Flyway V202610091600__*_init + V202610191200__auth_rbac）...
call mvnw -q -N -Pdb flyway:migrate "-Dflyway.url=jdbc:mysql://127.0.0.1:13306/yingxin?useSSL=false^&allowPublicKeyRetrieval=true^&serverTimezone=Asia/Shanghai" -Dflyway.user=root -Dflyway.password=root123
if errorlevel 1 echo 迁移可能已执行过，继续启动服务...

echo [3/3] 启动微服务（每个服务一个独立窗口）...
rem MYSQL_PORT=13306：本地 MySQL 容器端口避开 3306（本机常装 MySQL 服务，端口易冲突）
set MYSQL_PORT=13306
set JWT_SECRET=yingxin-dev-secret-key-must-be-over-32-bytes-ok

start "gateway-8080"       /min cmd /c "java -jar services\gateway\target\gateway-1.0.0-SNAPSHOT.jar & pause"
start "auth-8100"          /min cmd /c "java -jar services\auth-service\target\auth-service-1.0.0-SNAPSHOT.jar & pause"
start "qa-8101"            /min cmd /c "java -jar services\qa-service\target\qa-service-1.0.0-SNAPSHOT.jar & pause"
start "checkin-8102"       /min cmd /c "java -jar services\checkin-service\target\checkin-service-1.0.0-SNAPSHOT.jar & pause"
start "admin-8107"         /min cmd /c "java -jar services\admin-service\target\admin-service-1.0.0-SNAPSHOT.jar & pause"
start "event-worker-8110"  /min cmd /c "java -jar services\event-worker\target\event-worker-1.0.0-SNAPSHOT.jar & pause"

timeout /t 3 >nul
echo 全部服务已拉起，健康检查：http://localhost:8080/actuator/health
echo 管理后台：cd frontend\admin-web ^&^& pnpm dev （默认 5173，代理到 8080）
