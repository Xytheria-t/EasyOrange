#!/usr/bin/env bash
# =============================================================================
# EasyOrange 后端冷启动时间测量脚本
# =============================================================================
# 用法:
#   ./scripts/measure-startup.sh                       # 测当前优化版镜像
#   ./scripts/measure-startup.sh baseline              # 临时禁用 CDS 测基线
#   ./scripts/measure-startup.sh optimized 5           # 跑 5 次取中位数
#   IMAGE=easyorange-backend:custom ./measure-startup.sh
#
# 测量方法: 从 docker run 完成 -> /actuator/health 返回 200 的墙钟时间
# 取 3 次中位数, 排除首轮冷启偏差
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../easyorange-backend" && pwd)"
IMAGE="${IMAGE:-easyorange-backend:optimized}"
CONTAINER_NAME="eo-startup-bench"
ROUNDS="${2:-3}"
HEALTH_URL="http://localhost:8080/actuator/health"
HEALTH_TIMEOUT=120
PROFILE="${1:-optimized}"

# ---- 颜色输出 ----------------------------------------------------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail()  { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

# ---- 前置检查 ----------------------------------------------------------
command -v docker >/dev/null || fail "docker 未安装"
command -v bc >/dev/null      || fail "bc 未安装 (apt install bc)"
command -v curl >/dev/null    || fail "curl 未安装"

docker info >/dev/null 2>&1 || fail "Docker daemon 未运行"

# ---- 清理旧容器 -------------------------------------------------------
cleanup() {
    docker rm -f "$CONTAINER_NAME" 2>/dev/null || true
}
trap cleanup EXIT
cleanup

# ---- 构建镜像 (按 profile 决定是否用 AppCDS archive) ------------------
build_image() {
    local tag="$1" use_cds="$2"
    info "构建镜像 $tag (AppCDS=$use_cds) ..."

    if [[ "$use_cds" == "true" ]]; then
        DOCKER_BUILDKIT=1 docker build \
            --build-arg ENABLE_CDS=true \
            -f "$PROJECT_DIR/Dockerfile" \
            -t "$tag" "$PROJECT_DIR"
    else
        # 基线: 跳过 trainer 阶段, 用旧的单阶段 Dockerfile 行为
        DOCKER_BUILDKIT=1 docker build \
            --target=builder \
            --build-arg ENABLE_CDS=false \
            -f "$PROJECT_DIR/Dockerfile" \
            -t "$tag" "$PROJECT_DIR"
    fi
}

# ---- 跑一轮启动测试 ---------------------------------------------------
measure_once() {
    local image="$1"
    info "启动容器, 等待 health endpoint ..."

    # 后台启动, 记录毫秒级时间戳
    local start_ns end_ns elapsed
    start_ns=$(date +%s%N)

    docker run -d --rm \
        --name "$CONTAINER_NAME" \
        -p 8080:8080 \
        -e SPRING_PROFILES_ACTIVE=prod \
        -e EASYORANGE_DB_HOST=127.0.0.1 \
        -e EASYORANGE_DB_PORT=13306 \
        -e REDIS_HOST=127.0.0.1 \
        -e REDIS_PORT=16379 \
        -e ES_URIS=http://127.0.0.1:19200 \
        "$image" >/dev/null

    # 轮询 health, 每 100ms 检查一次
    local deadline=$((SECONDS + HEALTH_TIMEOUT))
    while (( SECONDS < deadline )); do
        if curl -fsS -o /dev/null "$HEALTH_URL" 2>/dev/null; then
            end_ns=$(date +%s%N)
            elapsed=$(( (end_ns - start_ns) / 1000000 ))
            docker logs "$CONTAINER_NAME" 2>&1 | grep -E "Started.*in.*seconds" | head -1 || true
            cleanup
            echo "$elapsed"
            return 0
        fi
        sleep 0.1
    done

    warn "Health endpoint ${HEALTH_TIMEOUT}s 内未就绪, 提取最后日志:"
    docker logs --tail 50 "$CONTAINER_NAME" 2>&1 || true
    cleanup
    return 1
}

# ---- 跑多轮取中位数 ---------------------------------------------------
measure_rounds() {
    local image="$1" rounds="$2"
    local results=()
    for ((i=1; i<=rounds; i++)); do
        info "第 $i / $rounds 轮 ..."
        local ms
        if ms=$(measure_once "$image"); then
            results+=("$ms")
            info "  本轮: ${ms} ms"
        else
            warn "  本轮失败, 不计入统计"
        fi
    done

    if (( ${#results[@]} == 0 )); then
        fail "所有轮次都失败, 无法测量"
    fi

    # 中位数
    IFS=$'\n' sorted=($(sort -n <<<"${results[*]}"))
    unset IFS
    local mid=$(( ${#sorted[@]} / 2 ))
    echo "${sorted[$mid]}"
}

# ---- 入口 ------------------------------------------------------------
case "$PROFILE" in
    optimized|baseline)
        ;;
    *) fail "未知 profile: $PROFILE (只支持 optimized | baseline)" ;;
esac

USE_CDS="false"
if [[ "$PROFILE" == "optimized" ]]; then
    USE_CDS="true"
fi

# 基线需要重新构建 (跳过 trainer), 优化版用现有镜像
if [[ "$PROFILE" == "baseline" ]]; then
    build_image "$IMAGE" "false"
else
    if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then
        build_image "$IMAGE" "$USE_CDS"
    else
        info "使用已存在的镜像 $IMAGE"
    fi
fi

info "==== 启动期测量 (profile=$PROFILE, rounds=$ROUNDS) ===="
MEDIAN=$(measure_rounds "$IMAGE" "$ROUNDS")
info "==== 中位数: ${MEDIAN} ms ($PROFILE) ===="

# 只跑了一个 profile 就不比较
if [[ "${COMPARE_ONLY:-false}" == "true" ]]; then
    echo "$MEDIAN"
    exit 0
fi
