#!/usr/bin/env bash
# EasyOrange git hooks — 共享工具函数
#
# 由 pre-commit / commit-msg / pre-push 共同 source。
# 严禁在此文件中执行任何 side-effect（仅定义函数与常量）。
#
# shellcheck shell=bash

# 颜色（仅在连到 TTY 且未禁用颜色时启用）
if [[ -t 1 ]] && [[ -z "${NO_COLOR:-}" ]]; then
  _C_RED='\033[0;31m'
  _C_YELLOW='\033[0;33m'
  _C_GREEN='\033[0;32m'
  _C_DIM='\033[2m'
  _C_RESET='\033[0m'
else
  _C_RED='' _C_YELLOW='' _C_GREEN='' _C_DIM='' _C_RESET=''
fi

# 强制契约：调用方 source 本文件前必须设置这两个变量
# - HOOK_NAME: 人类可读的 hook 名（如 "pre-commit"），用于日志前缀
# - HOOK_CMD:  对应 git 子命令（如 "commit" / "push"），用于失败提示
# 用 :? 守卫：未设置时立即报错并打印修复指引
: "${HOOK_NAME:?HOOK_NAME must be set before sourcing _lib.sh (e.g. HOOK_NAME=\$(basename \"\$0\"))}"
: "${HOOK_CMD:?HOOK_CMD must be set before sourcing _lib.sh (e.g. HOOK_CMD=commit)}"

# 通用 hook 日志（统一前缀 [hook-name]）
hook_log() {
  printf '%b[%s]%b %s\n' "$_C_DIM" "$HOOK_NAME" "$_C_RESET" "$*"
}

hook_warn() {
  printf '%b[%s] WARN%b %s\n' "$_C_YELLOW" "$HOOK_NAME" "$_C_RESET" "$*" >&2
}

hook_ok() {
  printf '%b[%s] OK%b %s\n' "$_C_GREEN" "$HOOK_NAME" "$_C_RESET" "$*"
}

# 失败并打印跳过提示（统一收口，调用方无需重复写 SKIP 文档）
hook_fail() {
  printf '%b[%s] FAIL%b %s\n' "$_C_RED" "$HOOK_NAME" "$_C_RESET" "$*" >&2
  printf '  Skip with: SKIP=git-hooks git %s\n' "$HOOK_CMD" >&2
  exit 1
}

# SKIP 开关：SKIP=git-hooks / SKIP=1 / SKIP=anything 都视为跳过
hook_should_skip() {
  [[ -n "${SKIP:-}" ]]
}

# 返回暂存区文件列表（行分隔；ACMR = Added/Copied/Modified/Renamed）
# quotePath=false：中文路径原样输出，便于 grep 匹配（默认会转义为 \xxx）
staged_files() {
  git -c core.quotePath=false diff --cached --name-only --diff-filter=ACMR
}

# ---------------------------------------------------------------------------
# 密钥扫描
#   - gitleaks 已安装：protect --staged，权威硬门禁（发现即返回 1）
#   - 未安装：回退 grep best-effort（同样返回 1 表示"发现疑似"）
# 调用方用 gitleaks_available() 判断走 fail（硬门禁）还是仅 warn（回退）。
# ---------------------------------------------------------------------------
gitleaks_available() {
  command -v gitleaks >/dev/null 2>&1
}

scan_secrets_staged() {
  if gitleaks_available; then
    gitleaks protect --staged --redact
    return $?
  fi
  # 高置信度模式：仅匹配 key=value / key: value 形式的真实凭据
  # 值必须 >= 16 字符的 base64-ish 内容，避免误报注释/字段名/env-var 名
  local pattern='(API[_-]?KEY|SECRET[_-]?KEY|ACCESS[_-]?KEY|PRIVATE[_-]?KEY|GITHUB[_-]?TOKEN|AUTH[_-]?TOKEN|JWT[_-]?SECRET)[[:space:]]*[:=][[:space:]]*[A-Za-z0-9_/+\-]{16,}'
  # 已知高熵 provider key 前缀（独立于上面的 key=value 模式）
  local provider_pattern='\b(ghp_[A-Za-z0-9]{36}|gho_[A-Za-z0-9]{36}|sk-[A-Za-z0-9]{20,}|sk-proj-[A-Za-z0-9_\-]{20,}|xox[baprs]-[A-Za-z0-9-]{10,}|AIzaSy[A-Za-z0-9_-]{33})\b'
  local hits
  hits=$(git diff --cached -U0 \
    | grep -hE "^\+.*($pattern|$provider_pattern)" \
    | grep -vE '^\+\s*\*' \
    | grep -vE '^\+.*\$\{[A-Z_]+(:[^}]*)?\}' \
    || true)
  if [[ -n "$hits" ]]; then
    printf '%s\n' "$hits" | head -5 >&2
    return 1
  fi
  return 0
}

# ---------------------------------------------------------------------------
# 快速内容检查（pre-commit）——全部基于 staged 内容，不与工作区未暂存改动耦合。
# 各函数把发现打印到 stdout 并始终返回 0；调用方判空聚合，一次收口。
# ---------------------------------------------------------------------------

# 尾随空白 / 文件末尾空行：git 原生 --check
check_staged_whitespace() {
  git diff --cached --check 2>&1 || true
}

# merge 冲突标记残留：staged diff 中新增的 <<<<<<< / >>>>>>> 行
# （======= 会误报 markdown 标题下划线，不检测；两侧标记足以暴露未解决冲突）
check_conflict_markers() {
  git diff --cached -U0 | grep -hE '^\+<{7} |^\+>{7} ' || true
}

# 大文件：默认阈值 2MiB（第一个参数可覆盖，如 check_large_files 1048576）
check_large_files() {
  local max_bytes=${1:-2097152}
  local f size
  while IFS= read -r -d '' f; do
    size=$(git cat-file -s ":$f" 2>/dev/null || echo 0)
    if (( size > max_bytes )); then
      printf '%s  (%s bytes)\n' "$f" "$size"
    fi
  done < <(git diff --cached --name-only --diff-filter=ACMR -z)
}
