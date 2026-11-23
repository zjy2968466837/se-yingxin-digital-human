#!/usr/bin/env bash
# 集中式数据库迁移（开发规范 §5.3）：只操作 backend/db/migration/，业务服务不引 Flyway。
#
# 用法：
#   scripts/db-migrate.sh info        # 看当前版本与待执行脚本
#   scripts/db-migrate.sh migrate     # 执行未应用的脚本
#   scripts/db-migrate.sh validate    # 校验脚本与历史表（checksum）一致
#   scripts/db-migrate.sh baseline -Dflyway.baselineVersion=<已执行的最高版本>
#
# 连接参数只从环境变量读取（参考 ~/.yingxin/db.env），禁止写进仓库：
#   MYSQL_HOST / MYSQL_PORT / MYSQL_DB / MYSQL_USER / MYSQL_PASSWORD
set -euo pipefail

cd "$(dirname "$0")/../backend"

for name in MYSQL_HOST MYSQL_PORT MYSQL_DB MYSQL_USER MYSQL_PASSWORD; do
  if [ -z "${!name:-}" ]; then
    echo "缺少环境变量 $name（可先 source ~/.yingxin/db.env）" >&2
    exit 1
  fi
done

action="${1:-info}"
shift || true

# -pl . 必须保留：db profile 只挂在聚合工程上，子模块会因为没有 url 而报错
exec ./mvnw -B -Pdb -pl . "flyway:${action}" "$@"
