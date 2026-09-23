#!/usr/bin/env bash
# =============================================================================
# 一键重置数据库 — dev 演示库（${EASYORANGE_DB_NAME:-easyorange}）与 IT 库（easyorange_it）
#
# 用法：scripts/db-reset.sh [-y]     # -y 跳过交互确认（非交互环境必带，否则默认取消）
# 前置：Docker 可用（脚本自动 docker compose up -d mysql）
# 之后：重启 dev 应用让 Flyway 重跑 V1 + 种子；需要 ES 检索时按输出提示全量重建索引。
#
# 为什么 IT 库一起重置：两库都是种子可再生的 disposable 库（未上线，无备份价值），
# IT 库的历史夹具/副产物同样一并清掉。
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# 凭据与 compose 同源：.env 优先，回落值与 compose.yaml 的 ${VAR:-default} 一致
if [ -f .env ]; then
    set -a
    # shellcheck disable=SC1091
    . ./.env
    set +a
fi
ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root123456}"
DB_NAME="${EASYORANGE_DB_NAME:-easyorange}"

if [ "${1:-}" != "-y" ]; then
    read -r -p "将删除并重建库 ${DB_NAME} 与 easyorange_it 的全部数据，继续? [y/N] " ans || ans=""
    case "$ans" in
        y | Y | yes | YES) ;;
        *) echo "已取消"; exit 1 ;;
    esac
fi

docker compose up -d mysql >/dev/null

echo "等待 MySQL 就绪..."
for _ in $(seq 1 60); do
    if docker exec easyorange-mysql mysqladmin ping -h localhost -uroot -p"$ROOT_PASSWORD" --silent >/dev/null 2>&1; then
        break
    fi
    sleep 2
done

docker exec -i easyorange-mysql mysql -uroot -p"$ROOT_PASSWORD" <<SQL
DROP DATABASE IF EXISTS \`${DB_NAME}\`;
CREATE DATABASE \`${DB_NAME}\` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
DROP DATABASE IF NOT EXISTS \`easyorange_it\`;
SQL
# IT 库建库授权与 docker-entrypoint-initdb.d 同一份（存量卷不会重跑 initdb，这里幂等补执行）
docker exec -i easyorange-mysql mysql -uroot -p"$ROOT_PASSWORD" < infra/mysql/init/01-easyorange-it.sql

cat <<'EOF'

重置完成。后续步骤：
  1. 启动 dev 应用（Flyway 自动执行 V1 + 种子，含 db/dev 演示数据）：
       cd easyorange-backend && ./mvnw spring-boot:run -pl easyorange-application
  2. 需要 ES 检索时全量重建索引（应用启动后，需 admin token，可在 /swagger-ui.html 获取）：
       POST /api/admin/search/reindex
EOF
