#!/usr/bin/env bash
# =============================================================================
# 联调 API 请求 — 查询参数自动 percent-encode，裸写中文不再 400
#
# 用法：scripts/api.sh <METHOD> <PATH> [key=value ...] ['<json body>']
#   scripts/api.sh GET  /api/products/search        keyword=机械键盘 limit=10
#   scripts/api.sh GET  /api/products/search/suggestions keyword=机械键盘
#   scripts/api.sh POST /api/products/search/record keyword=机械键盘
#   scripts/api.sh POST /api/products '{"title":"机械键盘","price":199}'
#   scripts/api.sh --dry-run GET /api/products/search keyword=机械键盘   # 只打印请求，不发
#
# 环境变量：
#   BASE_URL          默认 http://localhost:8080
#   EASYORANGE_TOKEN  有则自动加 Authorization: Bearer；管理端接口需要
#                     （POST /api/auth/login 拿 accessToken）
#
# 为什么需要这个脚本：Tomcat 11 解析请求行时就把非 ASCII 字节判为非法
# （Http11InputBuffer.parseRequestLine 抛 "Invalid character found in the request
# target"），在进 Spring 之前就回 400。relaxedQueryChars / relaxedPathChars 只覆盖
# ASCII 区间，把 0x80-0xFF 全放宽也无效 —— 实测过，别再往 server.tomcat 上加配置。
# 浏览器会自动编码，所以前端从不出问题，只有手写 URL 的联调会踩。
# =============================================================================
set -euo pipefail

DRY_RUN=0
if [ "${1:-}" = "--dry-run" ]; then
    DRY_RUN=1
    shift
fi

if [ $# -lt 2 ]; then
    echo "用法：scripts/api.sh [--dry-run] <METHOD> <PATH> [key=value ...] ['<json>']" >&2
    echo "示例：scripts/api.sh GET /api/products/search keyword=机械键盘 limit=10" >&2
    exit 64
fi

METHOD="$(echo "$1" | tr '[:lower:]' '[:upper:]')"
PATH_PART="$2"
shift 2

# 位置参数先分流：以 { 开头的那个当 JSON body，其余一律当查询参数 key=value
PAIRS=()
JSON_BODY=""
for arg in "$@"; do
    case "$arg" in
        '{'*) JSON_BODY="$arg" ;;
        *'='*) PAIRS+=("$arg") ;;
        *)
            echo "参数需写成 key=value，收到：${arg}" >&2
            exit 64
            ;;
    esac
done

# 查询串自己编码而不用 curl -G：-G 会把请求强制降成 GET，POST + @RequestParam 就发不出去
URL="${BASE_URL:-http://localhost:8080}${PATH_PART}"
if [ ${#PAIRS[@]} -gt 0 ]; then
    QUERY="$(python3 - "${PAIRS[@]}" <<'PY'
import sys, urllib.parse

print(urllib.parse.urlencode(
    [tuple(arg.split("=", 1)) for arg in sys.argv[1:]], quote_via=urllib.parse.quote))
PY
)"
    URL="${URL}?${QUERY}"
fi

CURL_ARGS=(-sS -X "$METHOD" "$URL")
if [ -n "${EASYORANGE_TOKEN:-}" ]; then
    CURL_ARGS+=(-H "Authorization: Bearer ${EASYORANGE_TOKEN}")
fi
if [ -n "$JSON_BODY" ]; then
    CURL_ARGS+=(-H 'Content-Type: application/json' --data "$JSON_BODY")
fi

if [ "$DRY_RUN" -eq 1 ]; then
    echo "curl ${CURL_ARGS[*]}"
    exit 0
fi

if ! RESPONSE="$(curl "${CURL_ARGS[@]}" -w $'\n%{http_code}')"; then
    echo "请求失败：${METHOD} ${URL}" >&2
    exit 2
fi
CODE="${RESPONSE##*$'\n'}"
PAYLOAD="${RESPONSE%$'\n'*}"

if command -v python3 >/dev/null 2>&1; then
    printf '%s' "$PAYLOAD" | python3 -m json.tool 2>/dev/null || printf '%s\n' "$PAYLOAD"
else
    printf '%s\n' "$PAYLOAD"
fi
echo "HTTP ${CODE}"

# 4xx/5xx 让脚本非零退出，便于串进校验流程
case "$CODE" in
    2* | 3*) exit 0 ;;
    *) exit 1 ;;
esac
