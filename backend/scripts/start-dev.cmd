@echo off
setlocal
rem ============================================================================
rem Local dev bootstrap (Windows CMD): preflight -> containers -> Flyway -> services
rem Usage: scripts\start-dev.cmd
rem
rem NOTE: keep this file ASCII-only. cmd.exe parses .cmd with the console OEM
rem codepage; UTF-8 Chinese bytes shift the byte pairing and turn parts of a
rem comment line into executed commands. Chinese docs live in docs/.
rem
rem Design rules (AGENTS.md, docs/开发规范.md section 5.3):
rem   1. Infrastructure goes through docker compose only; this script starts no container itself.
rem   2. All connection parameters come from env vars; nothing is hard-coded here.
rem      Override personal differences before running, e.g.
rem        set MYSQL_PORT=3307            local port clash; also edit compose ports
rem        set MYSQL_HOST=43.x.x.x  set MYSQL_DB=xxx  set MYSQL_SSL_MODE=PREFERRED
rem        set REDIS_PORT=6380      set REDIS_PASSWORD=xxx
rem        set RABBITMQ_HOST=...    set RABBITMQ_PORT=...
rem      Do NOT use a .env file: CMD would expand percent signs inside passwords.
rem   3. Preflight first and stop on failure, printing expected vs actual.
rem   4. Schema is owned by Flyway; this script never creates tables.
rem      Demo data: docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin < scripts\seed-demo-data.sql
rem ============================================================================

cd /d %~dp0..

rem ---------- Defaults (must match backend/docker-compose.yml) ----------
if not defined MYSQL_HOST      set MYSQL_HOST=127.0.0.1
if not defined MYSQL_PORT      set MYSQL_PORT=3306
if not defined MYSQL_DB        set MYSQL_DB=yingxin
if not defined MYSQL_USER      set MYSQL_USER=yingxin
if not defined MYSQL_PASSWORD  set MYSQL_PASSWORD=yingxin123
if not defined MYSQL_SSL_MODE  set MYSQL_SSL_MODE=PREFERRED
if not defined REDIS_HOST      set REDIS_HOST=127.0.0.1
if not defined REDIS_PORT      set REDIS_PORT=6379
if not defined RABBITMQ_HOST   set RABBITMQ_HOST=127.0.0.1
if not defined RABBITMQ_PORT   set RABBITMQ_PORT=5672
if not defined JWT_SECRET      set JWT_SECRET=yingxin-dev-secret-key-must-be-over-32-bytes-ok

echo ============ Effective environment ============
echo   MYSQL      %MYSQL_HOST%:%MYSQL_PORT%/%MYSQL_DB%   user=%MYSQL_USER%   ssl=%MYSQL_SSL_MODE%
echo   REDIS      %REDIS_HOST%:%REDIS_PORT%
echo   RABBITMQ   %RABBITMQ_HOST%:%RABBITMQ_PORT%
echo ===============================================
echo.

rem ---------- [1/5] Preflight ----------
echo [1/5] Preflight
where docker >nul 2>&1
if errorlevel 1 (
  echo   [X] docker not found: install Docker Desktop and put it on PATH
  exit /b 1
)
docker info >nul 2>&1
if errorlevel 1 (
  echo   [X] Docker engine not running: start Docker Desktop and wait for Running
  exit /b 1
)
echo   [OK] Docker engine reachable
call :check_port MySQL    "%MYSQL_HOST%"    %MYSQL_PORT%    yingxin-mysql
if errorlevel 1 exit /b 1
call :check_port Redis    "%REDIS_HOST%"    %REDIS_PORT%    yingxin-redis
if errorlevel 1 exit /b 1
call :check_port RabbitMQ "%RABBITMQ_HOST%" %RABBITMQ_PORT% yingxin-rabbitmq
if errorlevel 1 exit /b 1
echo.

rem ---------- [2/5] Infrastructure containers ----------
echo [2/5] Start infrastructure containers
docker compose up -d
if errorlevel 1 (
  echo   [X] docker compose up failed. Usual causes:
  echo       1. host port in compose ports is taken - edit backend\docker-compose.yml ports
  echo          AND set the same env var here, the two must agree;
  echo       2. image missing and network blocked - configure registry-mirrors or pull manually.
  exit /b 1
)
echo   [OK] containers submitted, waiting for health
set /a WAIT=0
:wait_infra
set /a WAIT+=1
docker compose ps --format "{{.Name}} {{.Health}}" | findstr /i "starting" >nul
if errorlevel 1 goto infra_ready
if %WAIT% GEQ 30 goto infra_ready
timeout /t 2 >nul
goto wait_infra
:infra_ready
docker compose ps

rem Guard against "db on 3306 but service connects 3307"
call :check_mapping mysql 3306 %MYSQL_PORT%
if errorlevel 1 exit /b 1
call :check_mapping redis 6379 %REDIS_PORT%
if errorlevel 1 exit /b 1
echo.

rem ---------- [3/5] Flyway migration (single source of schema) ----------
echo [3/5] Flyway migrate: mvnw -Pdb -pl . flyway:migrate
call mvnw -q -Pdb -pl . flyway:migrate
if errorlevel 1 (
  echo   [X] migration failed. Check in this order:
  echo       1. connection params above point to the intended database;
  echo       2. an existing non-empty schema needs a baseline first:
  echo          mvnw -Pdb -pl . flyway:baseline -Dflyway.baselineVersion=HIGHEST_APPLIED
  echo       3. "duplicate version" means two scripts share one version number.
  exit /b 1
)
echo   [OK] migration done
echo.

rem ---------- [4/5] Microservices ----------
echo [4/5] Start microservices, one window each
start "gateway-8080"      /min cmd /c "java -jar services\gateway\target\gateway-1.0.0-SNAPSHOT.jar & pause"
start "auth-8100"         /min cmd /c "java -jar services\auth-service\target\auth-service-1.0.0-SNAPSHOT.jar & pause"
start "qa-8101"           /min cmd /c "java -jar services\qa-service\target\qa-service-1.0.0-SNAPSHOT.jar & pause"
start "checkin-8102"      /min cmd /c "java -jar services\checkin-service\target\checkin-service-1.0.0-SNAPSHOT.jar & pause"
start "admin-8107"        /min cmd /c "java -jar services\admin-service\target\admin-service-1.0.0-SNAPSHOT.jar & pause"
start "event-worker-8110" /min cmd /c "java -jar services\event-worker\target\event-worker-1.0.0-SNAPSHOT.jar & pause"
echo   [OK] 6 services launched. navigation/aid/life/content are still skeletons, start on demand.
echo.

rem ---------- [5/5] Wait for gateway ----------
echo [5/5] Wait for gateway, up to 60s
set /a TRY=0
:wait_gateway
set /a TRY+=1
curl -s -f -o nul http://127.0.0.1:8080/actuator/health >nul 2>&1
if not errorlevel 1 goto gateway_ok
if %TRY% GEQ 60 goto gateway_timeout
timeout /t 1 >nul
goto wait_gateway
:gateway_ok
echo   [OK] gateway ready
goto done
:gateway_timeout
echo   [!] gateway not ready in 60s, check the per-service windows
goto done

:done
echo.
echo Gateway     http://localhost:8080/api/auth/ping
echo Admin web   cd frontend\admin-web then pnpm dev     http://localhost:5173
echo Student web cd frontend\miniprogram then pnpm dev:h5
echo RabbitMQ    http://localhost:15672  guest/guest
echo Demo data   docker exec -i yingxin-mysql mysql -uyingxin -pyingxin123 yingxin ^< scripts\seed-demo-data.sql
endlocal
exit /b 0

rem ============================================================================
rem Subroutines
rem ============================================================================

rem :check_port NAME HOST PORT OWN_CONTAINER
rem   Only checks when the target is local; a hit from our own container is fine.
:check_port
setlocal
set "CP_NAME=%~1"
set "CP_HOST=%~2"
set "CP_PORT=%~3"
set "CP_OWN=%~4"
if /i "%CP_HOST%"=="127.0.0.1" goto check_port_local
if /i "%CP_HOST%"=="localhost" goto check_port_local
echo   [SKIP] %CP_NAME% points to remote %CP_HOST%, local port check skipped
endlocal
exit /b 0
:check_port_local
docker ps --format "{{.Names}} {{.Ports}}" | findstr /i /c:"%CP_OWN%" | findstr /c:":%CP_PORT%-" >nul
if not errorlevel 1 (
  echo   [OK] %CP_NAME% port %CP_PORT% served by our container %CP_OWN%
  endlocal
  exit /b 0
)
netstat -ano | findstr ":%CP_PORT% " | findstr /i "LISTENING" >nul
if errorlevel 1 (
  echo   [OK] %CP_NAME% port %CP_PORT% free
  endlocal
  exit /b 0
)
echo   [X] %CP_NAME% port %CP_PORT% already in use by something that is not our container:
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%CP_PORT% " ^| findstr /i "LISTENING"') do @tasklist /fi "PID eq %%p" /fo table /nh
echo       Fix: stop that process, or change the port - edit compose ports AND the env var together.
endlocal
exit /b 1

rem :check_mapping SERVICE CONTAINER_PORT WANTED_HOST_PORT
rem   Compare compose published port with the env var actually used by services.
:check_mapping
setlocal
set "CM_SVC=%~1"
set "CM_CPORT=%~2"
set "CM_WANT=%~3"
set "CM_GOT="
for /f "tokens=2 delims=:" %%p in ('docker compose port %CM_SVC% %CM_CPORT% 2^>nul') do set "CM_GOT=%%p"
if not defined CM_GOT (
  echo   [!] cannot read published port of %CM_SVC%, mapping check skipped
  endlocal
  exit /b 0
)
if "%CM_GOT%"=="%CM_WANT%" (
  echo   [OK] %CM_SVC% published on %CM_WANT%, matches env var
  endlocal
  exit /b 0
)
echo   [X] %CM_SVC% is published on %CM_GOT% but env var says %CM_WANT%
echo       Fix: set the env var to %CM_GOT%, or change compose ports and recreate containers.
endlocal
exit /b 1
