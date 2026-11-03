# 迁移到 Windows 指南

> 适用对象：当前在 **WSL2** 下运行本项目，计划迁到 **Windows 原生**环境的成员。
> 本文所有"实测"结论均取自 2026-10-27 的一次真实环境勘察，机器配置见附录 A。

---

## 零、先读这段：迁移前必须知道的三件事

### 1. 你的机器上已经有一套禅道，它占用了本项目需要的两个端口

实测 Windows 监听端口：

| 端口 | 占用进程 | 可执行文件 |
|---|---|---|
| 80 / 443 | `httpd.exe` | `D:\Documents\chandao\ZenTao\bin\apache\bin\httpd.exe` |
| **3306** | `mysqld.exe` | `D:\Documents\chandao\ZenTao\bin\mysql\bin\mysqld.exe` |
| **6379** | `redis-server.exe` | `D:\Documents\chandao\ZenTao\bin\redis\redis-server.exe` |

而本项目的 `backend/docker-compose.yml` 恰好要映射 **3306（MySQL）** 和 **6379（Redis）**。

> **结论：不处理端口冲突，`docker compose up -d` 必然失败（`port is already allocated`）。**
> 这是本次迁移的**第一阻塞项**，处理方案见 §二。

### 2. Windows 侧工具链与项目声明不一致（但基本可用）

| 项 | 项目声明 | Windows 实际 | 判断 |
|---|---|---|---|
| JDK | 17（`pom.xml` 里 `release=17`） | **Zulu 21.0.10** | ✅ 可用。`release=17` 保证字节码是 17，用 JDK 21 编译不违规 |
| Node | 22.22.1（`.node-version`/`.nvmrc`）；`engines: ^20.19.0 \|\| >=22.12.0` | **v24.14.1** | ✅ 满足 `engines`。但与 `.node-version` 声明不一致，建议用 nvm-windows 装 22.22.1 |
| pnpm | 12.4.1（`packageManager`） | **未安装** | ❌ 必须装 |
| Git | — | 已装 | ✅ |
| Docker | — | Docker Desktop 29.7.2 已装，**但守护进程未运行** | ⚠️ 需启动 |
| 长路径 | — | `LongPathsEnabled = 1` | ✅ 已开，`node_modules` 深路径不会炸 |

### 3. 代码本身没有平台绑定

实测扫描结果：

- 源码里 `/home/administrator`、`/mnt/c`、`wsl.localhost` 的硬编码命中数 **0**
- 所有路径都走相对路径或环境变量（`${MYSQL_HOST:localhost}` 这类占位）
- `.gitattributes` 已强制"入库 LF、工作区 LF"，Windows 成员不需要额外配置
- 仓库根有 `backend/mvnw`（Linux）和 `backend/mvnw.cmd`（Windows）两份包装器

> **结论：迁移的难点不在代码，而在"端口"和"工具链"两件事上。**

---

## 一、迁移前检查清单

在新环境（Windows）上逐项确认：

```powershell
# 1. 版本确认
java -version          # 期望 17.x 或 21.x
node -v                # 期望 >= 22.12.0
pnpm -v                # 期望 12.4.1（未装见 §三）
git --version

# 2. 端口占用（关键）
netstat -ano | findstr ":3306 :6379 :8080 :8100 :8101 :8102 :8103 :8104 :8105 :8106 :8107 :5173"
```

期望结果：

| 端口 | 期望状态 | 实测（本机 2026-10-27） |
|---|---|---|
| 3306 | **冲突** | ❌ 禅道 mysqld 占用 |
| 6379 | **冲突** | ❌ 禅道 redis-server 占用 |
| 8080（网关） | 空闲 | ✅ 空闲 |
| 8100–8107（8 个服务） | 空闲 | ✅ 空闲 |
| 5173（admin-web） | 空闲 | ✅ 空闲 |

---

## 二、处理端口冲突（二选一）

### 方案 A：保留禅道，改本项目端口映射（推荐）

不碰禅道，只让本项目错开。**不要直接改 `docker-compose.yml`**（那是入库文件，改了就污染仓库），改用 compose 的 override 机制。

在 `backend/` 下新建 `docker-compose.override.yml`：

```yaml
# 本机专用覆盖文件：把 MySQL/Redis 映射到非冲突端口
# 因为 Windows 上 3306/6379 已被禅道占用
services:
  mysql:
    ports:
      - "3307:3306"
  redis:
    ports:
      - "6380:6379"
```

然后把它加进 `.gitignore`（避免误提交）：

```gitignore
backend/docker-compose.override.yml
```

启动后，**所有后端服务的启动命令都要带上环境变量**：

```powershell
$env:MYSQL_PORT="3307"
$env:REDIS_PORT="6380"
.\mvnw.cmd -pl services/gateway spring-boot:run
```

> ⚠️ `application.yml` 里是 `${MYSQL_PORT:3306}`、`${REDIS_PORT:6379}` 这样的占位，
> **不改代码**，只靠环境变量就能切换。这也是本项目"本地与云端共用同一份配置"的设计初衷。

### 方案 B：迁移期间停掉禅道

如果你当前不需要禅道，可以停掉它的服务（**注意这会中断禅道自身的业务，别在有人用时做**）：

```powershell
# 需管理员权限
Get-Service | Where-Object { $_.Name -match 'zentao' } | Stop-Service
# 或直接结束进程（不推荐，可能导致数据未落盘）
# Stop-Process -Id 50828,51068,21336
```

停掉后 3306/6379 释放，本项目的 `docker-compose.yml` 可以原样使用，无需 override。

> **建议选 A。** 隔离性更好，也不影响禅道里已有的 679 MB 数据。

---

## 三、安装缺失的工具链

### 3.1 pnpm（必装）

```powershell
# 用 corepack（Node 自带，最省事）
corepack enable
corepack prepare pnpm@12.4.1 --activate

# 或者用 npm
npm install -g pnpm@12.4.1

# 验证
pnpm -v   # 应输出 12.4.1
```

> **版本必须对齐 12.4.1。** `package.json` 里写的是 `"packageManager": "pnpm@12.4.1"`，
> 且 pnpm 12 有个行为变更：**构建脚本审批配置只能放在 `pnpm-workspace.yaml` 的 `allowBuilds`**
> （`package.json` 的 `pnpm` 字段已不再读取）。版本不一致会导致 `pnpm install` 行为差异。

### 3.2 Node 22.22.1（建议，非必须）

当前 v24.14.1 满足 `engines` 的 `>=22.12.0`，能跑。但项目声明的是 22.22.1，为减少"为什么我这里和别人不一样"的排查成本，建议：

```powershell
# 装 nvm-windows 后
nvm install 22.22.1
nvm use 22.22.1
```

### 3.3 JDK（通常不需要动）

现有 Zulu JDK 21 可用，因为 `pom.xml` 里：

```xml
<java.version>17</java.version>
<maven.compiler.release>17</maven.compiler.release>
```

`release=17` 会让编译器产出 17 字节码，用 JDK 21 编译不会引入 21 的 API。

> 如果启动时报 `UnsupportedClassVersionError`，才需要补装 JDK 17。

### 3.4 Docker Desktop（需启动 + 确认引擎）

```powershell
# 启动 Docker Desktop（首次会拉起 WSL 后端引擎）
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# 等待就绪后验证
docker version
docker compose version
```

本机当前状态（实测）：Docker Desktop 已安装 29.7.2，但 `docker` 命令报
`failed to connect to the docker API at npipe:...dockerDesktopLinuxEngine`——**守护进程没起**。

> **注意**：`WslEngineEnabled = true`、`EnableIntegrationWithDefaultWslDistro = false`。
> 意思是 Docker Desktop 用的是它自己的 `docker-desktop` 发行版，**不会**给你的 Ubuntu 发行版注入 `docker` 命令。
> 迁到 Windows 后你在 PowerShell 里用 `docker` 是正常的，不要以为坏了。

---

## 四、迁移代码

### 4.1 代码位置怎么选

| 方案 | 路径 | 评价 |
|---|---|---|
| **重新 clone**（推荐） | `D:\CodeProject\se-yingxin-digital-human` | 最干净，`.gitattributes` 自动生效，无需手动处理换行符 |
| 从 WSL 拷贝 | 同上 | 可行，但**必须排除** `node_modules`、`target`、`.git/index` |

**不要**把项目放在 `\\wsl.localhost\...` 上（即"留在 WSL 里、从 Windows 打开"）——走 9p 协议，IDE 索引和 Node 文件监听都会明显变慢。

### 4.2 推荐做法：重新 clone

```powershell
cd D:\CodeProject
git clone https://github.com/zjy2968466837/se-yingxin-digital-human.git
cd se-yingxin-digital-human
git checkout dev
```

### 4.3 如果必须拷贝（保留未提交的改动）

先确认 WSL 侧有哪些东西还没入库（实测有 8 项未跟踪文件，主要是 `docs/assets/er/*.png`、`.docx`、`.playwright-mcp/`、`.serena/`）。

```powershell
# 在 Windows PowerShell 中，从 WSL 拷贝源码（排除构建产物）
robocopy "\\wsl.localhost\Ubuntu\home\administrator\CodeProject\se-yingxin-digital-human" `
         "D:\CodeProject\se-yingxin-digital-human" `
         /E /XD node_modules target .git .playwright-mcp .serena /XF *.log
```

实测确认：WSL 发行版名是 **`Ubuntu`**，`\\wsl.localhost\Ubuntu\home\administrator\...` 在 Windows 侧可正常访问（`Test-Path` 返回 `True`）。

`/XD` 排除了：`node_modules`（pnpm 符号链接结构，跨平台复制必坏）、`target`（Maven 产物，可重建）、`.git`（**务必不要复制**，跨平台复制 git 内部结构容易出问题）、以及两个 AI 工具目录。

之后在新目录 `git init` 并重新关联远端。

### 4.4 换行符：不需要做任何事

`.gitattributes` 已经写了：

```
* text=auto eol=lf
*.cmd text eol=crlf
*.bat text eol=crlf
*.ps1 text eol=crlf
*.sh  text eol=lf
mvnw  text eol=lf
```

配合 `.vscode/settings.json` 的 `"files.eol": "\n"`，Windows 成员**无需**改 `core.autocrlf`。

> 唯一注意：`backend/mvnw`（无扩展名的那个）在 Windows 下不能直接跑，
> 要用 `mvnw.cmd`。两者内容等价。

---

## 五、启动基础设施（MySQL + Redis）

```powershell
cd backend

# 方案 A：用了 override（3307 / 6380）
docker compose up -d

# 方案 B：禅道已停，端口无冲突
docker compose up -d
```

确认状态：

```powershell
docker compose ps
# 期望：yingxin-mysql 和 yingxin-redis 都是 running (healthy)
```

### 5.1 首次初始化数据库

`init-sql/01-init.sql` 只做 `CREATE DATABASE`。**24 张表要手工执行迁移脚本**（Flyway 尚未接入，见 `AGENTS.md`）。

目录里现有 **9 个脚本**，其中 7 个建表、2 个改表（加列、加索引）：

| 脚本 | CREATE TABLE 数 |
|---|---|
| `V20261009160001__admin_init.sql` | 7 |
| `V20261009160002__aid_init.sql` | 2 |
| `V20261009160003__auth_init.sql` | 3 |
| `V20261009160004__checkin_init.sql` | 4 |
| `V20261009160005__content_init.sql` | 1 |
| `V20261009160006__life_init.sql` | 2 |
| `V20261009160007__qa_init.sql` | 5 |
| `V20261025134001__content_add_guide_columns.sql` | 0（ALTER） |
| `V20261025134002__admin_add_idx_role.sql` | 0（ALTER） |
| **合计** | **24** |

```powershell
cd backend

# 用 cmd 的重定向走原生字节流，避免 PowerShell 管道改编码
Get-ChildItem db\migration\*.sql | Sort-Object Name | ForEach-Object {
    Write-Host "执行 $($_.Name)"
    cmd /c "docker exec -i yingxin-mysql mysql -uroot -proot123 yingxin < `"$($_.FullName)`""
}
```

> ⚠️ **不要写成 `Get-Content $_.FullName | docker exec -i ...`。**
> 这些 SQL 里有大量中文注释，而 Windows PowerShell 5.1 的 `Get-Content` 默认按 **ANSI** 读取、
> 管道传给原生程序时又按控制台编码转换，UTF-8 中文会被打成乱码，轻则注释变形、重则语句解析失败。
> `cmd /c "... < file"` 是原始字节直通，最稳。
>
> 若你用的是 PowerShell 7（本机已装 `C:\Program Files\PowerShell\7\pwsh.exe`），
> 它默认 UTF-8，管道方式也可用，但仍建议统一用上面的 `cmd /c` 写法。

> **注意时区与字符集**：容器已配置 `--character-set-server=utf8mb4`、
> `--collation-server=utf8mb4_unicode_ci`、`--lower-case-table-names=1`（与云端一致）。
> `lower_case_table_names=1` **只在数据卷首次初始化时生效**，若要改必须 `docker compose down -v` 重建。

### 5.2 从 WSL 迁移已有数据（可选）

本次勘察未发现 WSL 内有正在使用的 MySQL 数据目录（`/var/lib/docker` 为空），**说明没有需要迁移的数据**，直接按 §5.1 重建即可。

如果将来有数据要迁：

```bash
# WSL 侧导出
docker exec yingxin-mysql mysqldump -uroot -proot123 --databases yingxin > yingxin.sql
```

```powershell
# Windows 侧导入
Get-Content yingxin.sql | docker exec -i yingxin-mysql mysql -uroot -proot123
```

---

## 六、启动后端

### 6.1 全量构建

```powershell
cd backend
.\mvnw.cmd clean install -DskipTests
```

> **必须用 `mvnw.cmd`，不要用系统 `mvn`。** Maven 版本由 wrapper 固定为 3.9.12
> （`distributionUrl` 指向阿里云镜像）。首次运行需联网下载。

### 6.2 起单个服务

```powershell
# 方案 A（override 端口）需要先设环境变量
$env:MYSQL_PORT="3307"
$env:REDIS_PORT="6380"

# 逐个启动（每个服务一个终端窗口）
.\mvnw.cmd -pl services/gateway spring-boot:run
.\mvnw.cmd -pl services/auth-service spring-boot:run
.\mvnw.cmd -pl services/qa-service spring-boot:run
# ... 其余服务同理
```

### 6.3 服务端口表

| 服务 | 端口 |
|---|---|
| gateway | 8080 |
| auth-service | 8100 |
| qa-service | 8101 |
| checkin-service | 8102 |
| navigation-service | 8103 |
| aid-service | 8104 |
| life-service | 8105 |
| content-service | 8106 |
| admin-service | 8107 |

> **内存提醒**：9 个服务全起约需 3.8 GB。本机 WSL 上限是 11.7 GB（宿主 15.7 GB），
> 迁到 Windows 后不再受 WSL 限制，但要留意禅道（Apache + MySQL + Redis）也在吃内存。

### 6.4 验证后端

```powershell
# 直连各服务（不经过网关）
curl http://localhost:8100/api/auth/ping
curl http://localhost:8101/api/qa/ping

# 经网关
curl http://localhost:8080/api/auth/ping
```

---

## 七、启动前端

### 7.1 admin-web（管理后台）

```powershell
cd frontend\admin-web
pnpm install
pnpm dev
```

访问 http://localhost:5173

> `vite.config.js` 里 `server.port = 5173`，`/api` 走 proxy 到 `http://localhost:8080`
> （由 `.env.development` 的 `VITE_API_TARGET` 控制）。**这些都不需要改。**

### 7.2 miniprogram（学生端网页，H5 构建）

```powershell
cd frontend\miniprogram
pnpm install
pnpm dev:h5
```

> ⚠️ **两条必须遵守的约束：**
> 1. **Vite 锁死 5.2.8**，`@dcloudio/vite-plugin-uni` 的 peerDependency 精确锁定。
>    **不要**跟随 admin-web 升到 Vite 8——两个前端的 Vite 版本本就不同，不要"统一升级"。
> 2. `@dcloudio/*` 各包必须**同一批次号**（当前 `3.0.0-5020420260813001`），
>    npm 的 `latest` 标签是 2021 年的陈旧 alpha。

如果 `pnpm install` 报 `Ignored build scripts` 并返回非 0 退出码：

> pnpm 12 默认拦截依赖的构建脚本。`pnpm-workspace.yaml` 里已用 `allowBuilds` 批准了
> `esbuild`、`core-js`、`core-js-pure`。**新增需要构建的依赖时，必须在这个 yaml 里加**，
> 加到 `package.json` 的 `pnpm` 字段里是无效的。

### 7.3 手机/局域网访问（如需）

`frontend/miniprogram/src/api/request.js` 里 baseURL 是硬编码的：

```js
const BASE_URL = 'http://localhost:8080/api'
```

真机调试时需改成电脑的局域网 IP，并确认网关 CORS 允许该来源。

> 好消息：`gateway/application.yml` 里 `allowedOriginPatterns: "*"`，CORS 不是障碍。

---

## 八、迁移后验证清单

逐项打勾，全部通过才算迁移成功：

- [ ] `java -version` 显示 17.x 或 21.x
- [ ] `node -v` >= 22.12.0
- [ ] `pnpm -v` 显示 12.4.1
- [ ] `docker compose ps` 显示 mysql / redis 均 `healthy`
- [ ] 数据库 `yingxin` 中表数量为 **24**
- [ ] `.\mvnw.cmd clean install -DskipTests` 成功
- [ ] 网关 8080 可访问，`/api/auth/ping` 返回统一响应体
- [ ] `http://localhost:8100/swagger-ui.html` 可打开
- [ ] admin-web 5173 可打开，登录页正常
- [ ] miniprogram `dev:h5` 构建成功，页面可打开
- [ ] `git status` 干净（或只剩预期的未跟踪文件）
- [ ] `git config core.autocrlf` 为 `false` 或未设置

---

## 九、风险清单与应对

| # | 风险 | 触发条件 | 应对 |
|---|---|---|---|
| 1 | **端口冲突** | 禅道占用 3306/6379 | 用 override 改 3307/6380，或停禅道（§二） |
| 2 | **Docker 守护进程未启动** | 只装了 Docker Desktop 没运行 | 启动 Docker Desktop，等托盘图标变绿（§3.4） |
| 3 | **pnpm 未装 / 版本不符** | 新机器 | `corepack prepare pnpm@12.4.1 --activate` |
| 4 | **JS 堆内存** | 9 个服务同时跑 | 加 `-Dspring-boot.run.jvmArguments="-Xmx512m"` 限流；或分批启动 |
| 5 | **node_modules 跨平台复制** | 直接从 WSL 拷贝整个目录 | **绝对不要复制**，必须在新平台 `pnpm install` 重装（pnpm 用符号链接，跨平台必坏） |
| 6 | **`mvnw` 跑不起来** | 在 PowerShell 里执行 `./mvnw` | 用 `.\mvnw.cmd` |
| 7 | **文件名大小写** | MySQL `lower_case_table_names=1` | 容器已配 `1` 与云端一致；注意该参数只在数据卷首次初始化时生效 |
| 8 | **miniprogram 误升 Vite** | 手快执行了 `pnpm up` | 锁定 5.2.8，见 §7.2 |
| 9 | **Docker Desktop 占用 WSL 内存** | WSL 与 Docker 同时跑 | 迁到 Windows 后本项目不再占 WSL；若还想用 WSL 跑别的，注意 `.wslconfig` 的 11.7 GB 上限 |
| 10 | **禅道基本认证 401** | 禅道 `app/htdocs/.ztaccess` 有 Basic auth | 与本次迁移无关，但会影响"禅道与本项目集成"，需单独处理 |

---

## 十、回滚方案

迁到 Windows 后如遇无法解决的问题，可随时退回 WSL——**因为代码在 Git 里，数据可重建**：

1. WSL 侧仓库未做任何破坏性改动，`git checkout dev` 即可恢复
2. 数据库用 §5.2 的脚本重建
3. `node_modules` / `target` 重新生成

> **回滚成本低的前提**：不要把 Windows 侧生成的文件（尤其是 `node_modules`）反向拷回 WSL。
> 两边各自 `install` / `build`。

---

## 附录 A：本次勘察的环境快照

### WSL 侧（迁移源）

```
内核        Linux 6.18.54.1-microsoft-standard-WSL2
发行版      Ubuntu 26.04 LTS
systemd     可用（PID 1 = systemd 259）
CPU         20 逻辑核
内存上限    11.7 GB（宿主物理内存 15.7 GB）
内存现状    已用 7.3 GB / 可用 4.1 GB
Swap        3.0 GB
磁盘        / 可用 906 GB
glibc       2.43
OpenSSL     3.5.5（仅 libssl.so.3）
Docker      WSL 内未安装；Docker Desktop 守护进程未运行
网络模式    Mirrored（与 Windows 共享端口空间）
WSLg        可用，/usr/bin/google-chrome 已装
项目路径    /home/administrator/CodeProject/se-yingxin-digital-human
仓库体积    22 MB（不含 node_modules/target），.git 6.3 MB
构建产物    target 目录 11 个（约 408 MB）；node_modules 2 处（296 MB）
MySQL 数据  WSL 内无正在使用的数据目录（/var/lib/docker 为空）
```

### Windows 侧（迁移目标）

```
物理内存    15.7 GB
C: 盘       可用 183 GB
D: 盘       可用 240 GB
JDK         Zulu 21.0.10（C:\Program Files\Zulu\zulu-21）
Node        v24.14.1（C:\Program Files\nodejs）
pnpm        未安装
Git         已装（C:\Program Files\Git\cmd\git.exe）
Docker      Docker Desktop 29.7.2，守护进程未运行
长路径      LongPathsEnabled = 1
PowerShell  Windows PowerShell 5.1.26100 + PowerShell 7（pwsh.exe 已装）
已占端口    80/443（禅道 httpd）、3306（禅道 mysqld）、6379（禅道 redis）
空闲端口    8080、8100–8107、5173、3307、6380
```

### 关键版本声明（来自仓库）

```
.tool-versions   java 17.0.20 / nodejs 22.22.1
.node-version    22.22.1
.nvmrc           22.22.1
backend/pom.xml  java.version=17, maven.compiler.release=17
Maven wrapper    3.9.12（阿里云镜像）
admin-web        pnpm@12.4.1, vite ^8.3.0, engines: node ^20.19.0 || >=22.12.0
miniprogram      pnpm@12.4.1, vite 5.2.8（锁定）, @dcloudio/* 3.0.0-5020420260813001
MySQL 镜像       mysql:8.0.46
Redis 镜像       redis:7.0.15-alpine
```

---

## 附录 B：常用命令对照（WSL → Windows）

| 用途 | WSL (bash) | Windows (PowerShell) |
|---|---|---|
| 全量构建 | `./mvnw clean install -DskipTests` | `.\mvnw.cmd clean install -DskipTests` |
| 单服务启动 | `./mvnw -pl services/gateway spring-boot:run` | `.\mvnw.cmd -pl services/gateway spring-boot:run` |
| 起基础设施 | `docker compose up -d` | `docker compose up -d` |
| 看容器状态 | `docker compose ps` | `docker compose ps` |
| 进容器 | `docker exec -it yingxin-mysql bash` | `docker exec -it yingxin-mysql bash` |
| 查端口 | `ss -ltnp \| grep 3306` | `netstat -ano \| findstr :3306` |
| 设环境变量 | `export MYSQL_PORT=3307` | `$env:MYSQL_PORT="3307"` |
| 批量执行 SQL | `for f in db/migration/*.sql; do ...` | `Get-ChildItem db\migration\*.sql \| ...` |
| 前端安装 | `pnpm install` | `pnpm install` |
| 前端启动 | `pnpm dev` | `pnpm dev` |

---

## 附录 C：为什么"迁到 Windows"要谨慎（保留意见）

仓库自身的 `docs/环境搭建指南.md` 写过一段选型建议，迁移前值得重读：

> | 你的主力工作方式 | 推荐 | 原因 |
> |---|---|---|
> | AI CLI 辅助开发（在终端里让 AI 改代码、跑命令） | **WSL2** | AI CLI 发的命令（`grep -oP`、`sed`、heredoc、`/dev/tcp`、`docker` 等）都基于 POSIX 假设，PowerShell 下要么没有、要么语义不同 |
> | 重度使用 IntelliJ IDEA 写后端 | **Windows 原生** | 代码放 WSL、却从 Windows 用 `\\wsl.localhost\...` 打开项目，IDE 索引会明显变慢（走 9p 协议） |

也就是说，**如果迁移动机是"IDE 变慢"，那正确解法是"代码放 Windows 原生 + 用 Windows JDK"，而不是"把 WSL 里的项目搬出来"**——两者殊途同归，但后者要额外处理端口、Docker、pnpm 一整套新问题。

本文档按"确实要迁"来写，但建议在动手前确认迁移收益大于上述 10 项风险。
