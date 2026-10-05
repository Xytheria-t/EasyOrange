#!/usr/bin/env bash
# =============================================================================
# 开发栈一键启动 — 基础设施 / 后端 / 前端并行拉起，按需重建
#
# 本项目的所有操作都走 CLI，起服务只认这一个入口：别再逐条拼
# docker / mvnw / java / npm。需要知道跑没跑就跑 --status，要看选项就跑 --help。
#
# 为什么值得写这个脚本（2026-10 实测，缓存热的空闲机）：
#   systemctl start docker            ~4s
#   docker compose up -d（返回）        3.4s
#   └ 容器全部 healthy                 ~25s   ← 后端必须等它：ES 没 ready 时
#                                           ElasticsearchIndexManager 初始化失败，java 起不来
#   mvnw clean package -am             13.5s  ← 去掉 clean 只要 4s
#   java -jar → Started                10.4s
#   npm run dev → ready                0.3s
#   浏览器首访 → 应用挂载               0.8s
# 串行合计 55~65s。可压缩的是：把 maven 打包塞进「等容器 healthy」的窗口里，
# 源码没改就整段省掉。
#
# 不做的事：不给 Docker 配自启。docker.service 保持 disabled，日常 WSL 不占内存；
# 需要时本脚本按需拉起 dockerd，收工用 scripts/dev-down.sh。
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

RUN_DIR="/tmp/easyorange-dev"
BACKEND_DIR="$ROOT/easyorange-backend"
FRONTEND_DIR="$ROOT/easyorange-frontend"
BACKEND_LOG="$RUN_DIR/backend.log"
FRONTEND_LOG="$RUN_DIR/frontend.log"

usage() {
    cat <<'EOF'
用法：scripts/dev-up.sh [选项]
  （无参数）      基础设施 + 后端 + 前端；后端源码无改动则跳过打包
  --status        只报告当前状态，不做任何启停
  --no-es         不起 Elasticsearch（省 ~1.0G 常驻内存，搜索/AI 找货不可用）
  --skip-build    直接用磁盘上已有的 jar，完全不碰后端源码。
                  源码改到一半编译不过、又还想起服务看效果时用
  --no-backend    只起基础设施 + 前端（后端在 IDE 里跑时用这条）
  --force-build   强制重打后端 jar
EOF
}

# 端口被绑 ≠ 后端活着：dev-down 发的是 SIGTERM，JVM 退完还要几秒，
# 这期间端口仍 LISTEN。照端口判断会「报告说跳过、实际没后端」
backend_alive() {
    curl -s -o /dev/null --max-time 2 "http://localhost:8080/api/products?pageNum=1&pageSize=1"
}

report_status() {
    local fe=down be=down es=stopped
    ss -ltn "sport = :5173" 2>/dev/null | grep -q LISTEN && fe=up
    backend_alive && be=up
    docker ps --format '{{.Names}}' 2>/dev/null | grep -q easyorange-es && es=up
    echo "status: frontend=$fe backend=$be elasticsearch=$es"
    echo "（frontend 取自监听端口 5173，backend 取自 API 探活，elasticsearch 取自运行中的容器）"
}

WITH_ES=1
FORCE_BUILD=0
WITH_BACKEND=1
SKIP_BUILD=0
STATUS_ONLY=0
mkdir -p "$RUN_DIR"

while [ $# -gt 0 ]; do
    case "$1" in
        --no-es) WITH_ES=0 ;;
        --force-build) FORCE_BUILD=1 ;;
        --skip-build) SKIP_BUILD=1 ;;
        --no-backend) WITH_BACKEND=0 ;;
        --status)
            STATUS_ONLY=1
            ;;
        -h | --help)
            usage
            exit 0
            ;;
        *)
            echo "未知参数：$1（--help 看用法）" >&2
            exit 64
            ;;
    esac
    shift
done

[ "$STATUS_ONLY" = "1" ] && {
    report_status
    exit 0
}

# ---------------------------------------------------------------------------
# 0. .env：根 .env 被文档定义为「单一来源」，但只有 docker compose 原生读得到它，
#    终端 JVM 起后端时读不到 —— 改了 .env 会「容器用新值、后端用默认值」错配，
#    症状是连接被拒/密码错误，指向不到「配置没生效」。显式导入使两边同源。
# ---------------------------------------------------------------------------
if [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi

# 用 ss 判监听，不要用 bash 的 /dev/tcp：后者在本机 WSL2 上连接被拒时会挂住而不是立即返回
port_busy() { ss -ltn "sport = :$1" 2>/dev/null | grep -q LISTEN; }

# ---------------------------------------------------------------------------
# 1. dockerd：只在没跑时拉起，不改自启配置
# ---------------------------------------------------------------------------
if ! docker info >/dev/null 2>&1; then
    echo "▶ 启动 dockerd …"
    if ! sudo -n systemctl start docker 2>/dev/null; then
        echo "dockerd 拉不起来。手动执行：sudo systemctl start docker" >&2
        exit 1
    fi
    for _ in $(seq 1 20); do
        docker info >/dev/null 2>&1 && break
        sleep 1
    done
fi

# ---------------------------------------------------------------------------
# 2. 基础设施：--wait 一直等到容器 healthy 再返回（compose v2.17+）
#    必须等：后端启动时 ElasticsearchIndexManager 会连 ES 建索引，
#    ES 没 ready 时 java 会以 ConnectionClosedException 直接启动失败
# ---------------------------------------------------------------------------
COMPOSE_ARGS=(compose)
[ "$WITH_ES" = "1" ] && COMPOSE_ARGS+=(--profile search)

echo "▶ 拉起基础设施（$([ "$WITH_ES" = "1" ] && echo '含 ES' || echo '不含 ES')），等 healthy…"
docker "${COMPOSE_ARGS[@]}" up -d --wait --wait-timeout 180 >"$RUN_DIR/compose.log" 2>&1 &
COMPOSE_PID=$!

# ---------------------------------------------------------------------------
# 3. 前端：无依赖，立刻起。启动后自请求一次，让 vite 的入口模块转换在后台跑完
# ---------------------------------------------------------------------------
start_frontend() {
    if port_busy 5173; then
        echo "▶ 前端已在 5173 运行，跳过"
        return
    fi
    # 整组重定向（含 stdin、含 exec 后的子进程）：只重定向 npm 的话，
    # 包装用的 subshell 仍攥着调用方的 stdout，脚本被管道接住时会一直不返回
    (cd "$FRONTEND_DIR" && exec npm run dev) >"$FRONTEND_LOG" 2>&1 </dev/null &
    echo $! >"$RUN_DIR/frontend.pid"
    FRONTEND_STARTED=1
    echo "▶ 前端 dev server 启动中（日志：$FRONTEND_LOG）"
}

# ---------------------------------------------------------------------------
# 4. 后端打包：塞进「等容器 healthy」这段窗口里跑，纯赚时间
# ---------------------------------------------------------------------------
jar_fresh() {
    local stamp="$RUN_DIR/backend-build.stamp"
    [ -f "$stamp" ] || return 1
    # 跟「上一次构建完成的时刻」比，而不是跟 jar 比：构建里跑的 spotless 会重写
    # 源文件，jar 的 mtime 永远比它自己刚格式化过的源码旧，于是每次都判定「不新鲜」
    # 而白重打包 13.5s（2026-10 实测 ChatTools.java 比它产出的 jar 新 29s）
    # -print -quit 命中即返回：有输出说明之后又改过源码，得重打
    [ -z "$(find "$BACKEND_DIR" \( -name '*.java' -o -name 'pom.xml' -o -name '*.yaml' \
        -o -name '*.yml' -o -name '*.properties' \) -not -path '*/target/*' -newer "$stamp" -print -quit)" ]
}

build_backend() {
    if [ "$SKIP_BUILD" = "1" ]; then
        echo "▶ --skip-build：直接用已有的 jar，不校验源码新鲜度"
    elif [ "$FORCE_BUILD" = "1" ] || ! jar_fresh; then
        # 刻意不带 clean：全量 clean 只在「怀疑 target 有陈旧产物」时才值得付 13.5s
        # 输出必须落文件：后台 job 不重定向会一直攥着调用方的 stdout 管道不放
        echo "▶ 打包后端（无 clean，增量），与等容器并行…"
        (cd "$BACKEND_DIR" && ./mvnw -q package -DskipTests -pl easyorange-application -am) \
            >"$RUN_DIR/build.log" 2>&1
        touch "$RUN_DIR/backend-build.stamp"
    else
        echo "▶ 后端无源码改动，跳过打包"
    fi
}

start_backend() {
    if backend_alive; then
        echo "▶ 后端已在 8080 运行，跳过"
        return
    fi
    if port_busy 8080; then
        echo -n "▶ 等 8080 上的旧进程退出"
        for _ in $(seq 1 30); do
            backend_alive || ! port_busy 8080 && break
            echo -n "."
            sleep 1
        done
        if port_busy 8080; then
            echo " ✗" >&2
            echo "  8080 被非本项目的进程占着，先处理它：ss -ltnp | grep 8080" >&2
            return 1
        fi
        echo " ✓"
    fi
    local jar
    jar="$(ls -t "$BACKEND_DIR"/easyorange-application/target/easyorange-application-*.jar | head -1)"
    (cd "$BACKEND_DIR" && exec java --sun-misc-unsafe-memory-access=allow -jar "$jar") \
        >"$BACKEND_LOG" 2>&1 </dev/null &
    echo $! >"$RUN_DIR/backend.pid"
    BACKEND_STARTED=1
    echo "▶ 后端启动中（日志：$BACKEND_LOG）"
}

start_frontend
if [ "$WITH_BACKEND" = "1" ]; then
    # 后端已在跑就别打包：13.5s 换一个用不上的 jar 是纯浪费。
    # 判据用 HTTP 探活而非端口——正在退出的 JVM 仍占着端口
    if backend_alive; then
        echo "▶ 后端已在 8080 运行，跳过"
    else
        build_backend &   # 与容器健康检查同时进行
        BUILD_PID=$!
    fi
fi
if ! wait "$COMPOSE_PID"; then
    echo "容器未能全部 healthy，看 $RUN_DIR/compose.log" >&2
    exit 1
fi
if [ "${BUILD_PID:-}" != "" ]; then
    if wait "$BUILD_PID"; then
        start_backend || exit 1
    else
        echo "后端打包失败，没起 java。看 $RUN_DIR/build.log" >&2
        exit 1
    fi
fi

# ---------------------------------------------------------------------------
# 5. 预热 + 汇报
#    判据用「本脚本是否真的拉起了它」，而不是「端口现在通不通」——
#    端口恰恰在自己启动后才通，用它当条件会永远跳过等待、报出一个假的就绪时间
# ---------------------------------------------------------------------------
if [ "${FRONTEND_STARTED:-0}" = "1" ] && port_busy 5173; then
    # 自请求一次：让 vite 的入口模块转换在后台跑完，别把冷转换的等待留到你打开浏览器
    curl -s -o /dev/null --max-time 5 http://localhost:5173/ || true
fi

if [ "${BACKEND_STARTED:-0}" = "1" ]; then
    echo -n "▶ 等待后端就绪"
    READY=0
    for _ in $(seq 1 90); do
        if curl -s -o /dev/null --max-time 2 "http://localhost:8080/api/products?pageNum=1&pageSize=1"; then
            READY=1
            echo " ✓"
            break
        fi
        echo -n "."
        sleep 1
    done
    [ "$READY" = "1" ] || echo " ✗（超时，看 $BACKEND_LOG）"
fi

echo
echo "──────────────────────────────────────────────"
echo "  前端   http://localhost:5173"
[ "$WITH_BACKEND" = "1" ] && echo "  后端   http://localhost:8080  (Swagger: /swagger-ui.html)"
echo "  账号   testuser / Password123"
echo "  收工   scripts/dev-down.sh"
if [ "$WITH_ES" = "1" ]; then
    docker stats --no-stream --format "  内存  {{.Name}} {{.MemUsage}}" 2>/dev/null | sed 's|easyorange-||'
else
    echo "  ES 未启动：搜索与 AI 找货不可用（需要时去掉 --no-es）"
fi
echo "──────────────────────────────────────────────"
# 末行固定格式：接脚本的一方（人或 AI）不用再 grep ss/端口来判断成没成
report_status