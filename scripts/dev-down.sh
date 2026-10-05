#!/usr/bin/env bash
# =============================================================================
# 收工 — 停后端 / 前端 / 容器，腾回内存
#
# 用法：scripts/dev-down.sh [--keep-infra]
#   （无参数）          全停，包括容器（数据在 volume 里，下次起还在）
#   --keep-infra        只停后端与前端，MySQL/Redis/RabbitMQ/ES 继续跑
#
# 为什么默认连容器一起停：整套常驻约 1.75G（ES 1.07G + MySQL 482M + Rabbit 188M
# + Redis 12M），日常不开开发就别占着。dockerd 本身也随容器停掉后自然退出，
# 不需要动自启配置。
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

KEEP_INFRA=0
[ "${1:-}" = "--keep-infra" ] && KEEP_INFRA=1

RUN_DIR="/tmp/easyorange-dev"

# 递归杀进程树：npm run dev 会 sh -> node dev.mjs -> vite 三层派生，
# 只杀记录的 pid 会留孤儿继续占着 5173
kill_tree() {
    local pid="$1" child
    for child in $(pgrep -P "$pid" 2>/dev/null); do
        kill_tree "$child"
    done
    kill -TERM "$pid" 2>/dev/null || true
}

stop_pidfile() {
    local name="$1" file="$RUN_DIR/$1.pid" port="$2"
    if [ ! -f "$file" ]; then
        echo "▶ $name 无 pid 记录，跳过（手工起的进程请自行 pkill）"
        return
    fi
    local pid
    pid="$(cat "$file")"
    if kill -0 "$pid" 2>/dev/null; then
        # 逐个 pid 往下杀，绝不用 kill -- -PGID：脚本与调用它的 shell 同属一个
        # 进程组，杀组会把调用方一起带走
        kill_tree "$pid"
        echo "▶ 已停 $name (pid $pid)"
    else
        echo "▶ $name 已不在运行"
    fi
    rm -f "$file"

    for _ in $(seq 1 10); do
        # 必须显式 return 0：裸 return 会继承上面 grep 失败时的退出码 1，
        # 撞上 set -e 直接终止脚本 —— 端口已释放（正常情况）反而让后面的容器停不掉
        ss -ltn "sport = :$port" 2>/dev/null | grep -q LISTEN || return 0
        sleep 1
    done
    echo "  ⚠ $port 仍被占用：进程树没清干净，ss -ltnp | grep $port 看一下"
}

stop_pidfile backend 8080
stop_pidfile frontend 5173

if [ "$KEEP_INFRA" = "1" ]; then
    echo "▶ 基础设施保持运行（--keep-infra）"
else
    echo "▶ 停止容器 …"
    docker compose --profile search stop >/dev/null 2>&1 || true
    echo "  数据保留在 volume，下次 scripts/dev-up.sh 直接可用"
fi

echo
free -h | awk 'NR==2 {printf "  WSL 内存：已用 %s / 共 %s，可用 %s\n", $3, $2, $7}'