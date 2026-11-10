#!/usr/bin/env bash
# 一键验证登录数据链路（VS Code 任务：Terminal → Run Task → verify: 登录数据链路）
#
# 覆盖：验证码 → Redis → 登录（BCrypt）→ HttpOnly Cookie → /me → 角色授权(403) → 登出失效(401)
# 依赖：gateway(8080)、admin-service(8107)、auth-service(8100)、Redis；连接参数从环境/私有文件读取，不写死口令。
#
# 可选环境变量：
#   GATEWAY / AUTH_BASE / REDIS_HOST / REDIS_PORT / REDIS_CLI
#   ADMIN_USER / ADMIN_PASSWORD（缺省从 ~/.yingxin/db.env 读取）
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GATEWAY="${GATEWAY:-http://127.0.0.1:8080}"
AUTH_BASE="${AUTH_BASE:-http://127.0.0.1:8100}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6380}"
INTERNAL_SECRET="${AUTH_INTERNAL_SECRET:-yingxin-local-dev-internal-secret-change-me}"

if [ -f "$HOME/.yingxin/db.env" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$HOME/.yingxin/db.env"
  set +a
fi
: "${ADMIN_USER:=admin}"
if [ -z "${ADMIN_PASSWORD:-}" ]; then
  read -rsp "管理员口令（$ADMIN_USER）: " ADMIN_PASSWORD
  echo
fi

if [ -n "${REDIS_CLI:-}" ]; then
  :
elif command -v redis-cli >/dev/null 2>&1; then
  REDIS_CLI="redis-cli"
elif [ -x "/d/Documents/chandao/ZenTao/bin/redis/redis-cli.exe" ]; then
  REDIS_CLI="/d/Documents/chandao/ZenTao/bin/redis/redis-cli.exe"
else
  echo "找不到 redis-cli：请安装，或设置 REDIS_CLI 指向可执行文件" >&2
  exit 1
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
PASS=0
FAIL=0
ok() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
bad() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }
jget() { python -c "import sys,json;d=json.load(sys.stdin);print(d$1)" 2>/dev/null; }

echo "== 0. 进程端口 =="
for p in 8080 8100 8107; do
  if (echo > "/dev/tcp/127.0.0.1/$p") >/dev/null 2>&1; then ok "$p 监听中"; else bad "$p 未启动"; fi
done
if [ "$FAIL" -gt 0 ]; then
  echo "端口不全，先启动 auth/admin/gateway（见 README / AGENTS）" >&2
  exit 1
fi

echo "== 1. 验证码：网关 → admin-service → Redis =="
curl -s -c "$TMP/jar" "$GATEWAY/api/admin/auth/captcha" -o "$TMP/cap.json"
CID=$(jget "['data']['captchaId']" < "$TMP/cap.json")
if [ -n "$CID" ]; then ok "captchaId 下发（${#CID} 位）"; else bad "验证码接口失败"; exit 1; fi
if grep -q '"token"' "$TMP/cap.json"; then bad "响应体泄露了令牌"; else ok "响应体不含令牌"; fi
CODE=$("$REDIS_CLI" -h "$REDIS_HOST" -p "$REDIS_PORT" get "yx:admin:captcha:$CID" | tr -d '\r')
if [ -n "$CODE" ]; then ok "Redis 中存在验证码（服务端存储）"; else bad "Redis 中无验证码"; exit 1; fi

echo "== 2. 登录：BCrypt 比对 + Set-Cookie =="
printf '{"username":"%s","password":"%s","captchaId":"%s","captchaCode":"%s"}' \
  "$ADMIN_USER" "$ADMIN_PASSWORD" "$CID" "$CODE" > "$TMP/login-body.json"
curl -s -D "$TMP/login.h" -b "$TMP/jar" -c "$TMP/jar" -X POST "$GATEWAY/api/admin/auth/login" \
  -H "Content-Type: application/json; charset=UTF-8" --data-binary @"$TMP/login-body.json" -o "$TMP/login.json"
LOGIN_CODE=$(jget "['code']" < "$TMP/login.json")
if [ "$LOGIN_CODE" = "0" ]; then ok "登录成功 code=0"; else bad "登录失败：$(cat "$TMP/login.json")"; exit 1; fi
if grep -qi '^set-cookie:.*httponly' "$TMP/login.h"; then ok "Cookie 带 HttpOnly"; else bad "Cookie 缺 HttpOnly"; fi
if grep -qi '^set-cookie:.*samesite=lax' "$TMP/login.h"; then ok "Cookie 带 SameSite=Lax"; else bad "Cookie 缺 SameSite"; fi
if grep -qi '^set-cookie:.*secure' "$TMP/login.h"; then echo "  [INFO] Cookie 带 Secure（HTTPS 环境）"; else echo "  [INFO] Cookie 无 Secure（本地 HTTP，符合预期）"; fi
if grep -q '"token"' "$TMP/login.json"; then bad "响应体泄露了令牌/会话 id"; else ok "响应体不含令牌与会话 id"; fi

echo "== 3. 携带 Cookie 访问 /me（网关查 Redis 会话并注入用户上下文）=="
ME=$(curl -s -b "$TMP/jar" "$GATEWAY/api/admin/auth/me")
ME_USER=$(printf '%s' "$ME" | jget "['data']['username']")
ME_ROLE=$(printf '%s' "$ME" | jget "['data']['role']")
if [ "$ME_USER" = "$ADMIN_USER" ]; then ok "/me 返回 $ME_USER（role=$ME_ROLE）"; else bad "/me 失败：$ME"; fi

echo "== 4. 授权：STUDENT 会话访问 /api/admin/** 应 403 =="
SID=$(curl -s -X POST "$AUTH_BASE/api/auth/internal/session" \
  -H "X-Internal-Secret: $INTERNAL_SECRET" -H "Content-Type: application/json" \
  -d '{"userId":999,"role":"STUDENT"}' | jget "['data']['sessionId']")
if [ -n "$SID" ]; then
  HTTP=$(curl -s -o "$TMP/forbidden.json" -w "%{http_code}" \
    -H "Cookie: YX_SESSION=$SID" "$GATEWAY/api/admin/auth/me")
  FCODE=$(jget "['code']" < "$TMP/forbidden.json")
  if [ "$HTTP" = "403" ] && [ "$FCODE" = "1005" ]; then ok "越权被拒：403 + 1005"; else bad "越权未被拒：http=$HTTP body=$(cat "$TMP/forbidden.json")"; fi
  "$REDIS_CLI" -h "$REDIS_HOST" -p "$REDIS_PORT" del "yx:auth:session:$SID" >/dev/null 2>&1
else
  bad "无法创建 STUDENT 会话（内部接口不可用？）"
fi

echo "== 5. 登出：删会话 + 清 Cookie，旧 Cookie 立即失效 =="
ADMIN_SID=$(awk '$6=="YX_SESSION"{print $7}' "$TMP/jar" | tail -1)
curl -s -b "$TMP/jar" -X POST "$GATEWAY/api/admin/auth/logout" >/dev/null
if [ -n "$ADMIN_SID" ]; then
  HTTP=$(curl -s -o "$TMP/after.json" -w "%{http_code}" -H "Cookie: YX_SESSION=$ADMIN_SID" "$GATEWAY/api/admin/auth/me")
  ACODE=$(jget "['code']" < "$TMP/after.json")
  if [ "$HTTP" = "401" ] && [ "$ACODE" = "1002" ]; then ok "旧 Cookie 已失效：401 + 1002"; else bad "旧 Cookie 仍有效：http=$HTTP body=$(cat "$TMP/after.json")"; fi
else
  bad "未从 Cookie jar 读到 YX_SESSION"
fi

echo
echo "结果：PASS=$PASS FAIL=$FAIL"
[ "$FAIL" -eq 0 ]
