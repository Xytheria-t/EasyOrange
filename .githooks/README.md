# EasyOrange Git Hooks

本地 git 钩子，安装方式：

```bash
git config core.hooksPath .githooks
```

## 文件

| 文件 | 用途 | 耗时 |
|------|------|------|
| `pre-commit` | staged 内容快速检查（密钥 + 空白 + 冲突标记 + 大文件 + 前端 lint） | <1s~几秒 |
| `pre-push` | 重门禁（后端 `mvn test` + 前端 `npm test`，按推送变更分发） | 数秒~数分钟 |
| `commit-msg` | Conventional Commits 格式校验（标题 + breaking change） | <100ms |
| `_lib.sh` | 共享工具（颜色、日志、SKIP、staged 文件、密钥扫描、快检函数） | — |

**职责分层**：`pre-commit` 只放秒级快检，构建/测试的重活放 `pre-push`，避免每次提交付全量编译成本。

## 跳过

任意 hook 都可通过 `SKIP` 环境变量跳过：

```bash
SKIP=git-hooks git commit -m "feat: ..."
SKIP=git-hooks git push
SKIP=1        git commit -m "..."   # 任何非空值都视为跳过
```

## Pre-commit 行为

按 **staged 内容**做快速检查，不做全量构建：

| Staged 内容 | 触发检查 | 工具 |
|-------------|----------|------|
| 任意文本 | 密钥扫描 | `gitleaks protect --staged`（硬门禁）；未安装则 grep 回退（仅 warn） |
| 任意文本 | 尾随空白 / 文件末尾空行 | `git diff --cached --check` |
| 任意文本 | merge 冲突标记残留（`<<<<<<<`/`>>>>>>>`） | grep（staged diff） |
| 任意文本 | 大文件 >2MB | `git cat-file -s`（staged blob） |
| `easyorange-frontend/{src,tests}/**/*.{ts,tsx,js,jsx}` | `biome check`（仅变更文件） | `node_modules/.bin/biome` |
| 纯文档/Markdown/YAML | 跳过（仅过密钥扫描） | — |

> 快检全部基于 **staged 内容**（`git diff --cached` / `git cat-file :path`），不受工作区未暂存改动影响。

## Pre-push 行为

按 **被推送 commit** 的变更文件分发（非全量）：

| 推送变更 | 触发检查 | 工具 |
|----------|----------|------|
| `*.java` / `*.kt` / `pom.xml` | `./mvnw test`（含编译，`-T` 无、`-fae` 聚合） | `mvnw` |
| `easyorange-frontend/{src,tests}/**/*.{ts,tsx,js,jsx}` | `npm test`（`vitest run`） | npm |

新分支推送（远端无基线）→ 全量跑。

## Commit-msg 规则

基于 [Conventional Commits 1.0.0](https://www.conventionalcommits.org/)：

```
<type>[(<scope>)][!]: <description>
```

- **type**：`feat | fix | refactor | docs | test | chore | perf | ci | style | build | revert`
- **scope**：可选，小写字母/数字 + `-` / `_`
- **breaking change**：`feat!:` / `feat(scope)!:` 或 body 写 `BREAKING CHANGE: ...`
- **长度**：硬上限 100 字符（>72 仅 warn）
- **例外**：`Merge*` / `Revert*` / `fixup!` / `squash!` / `amend!` 直接放行

## 密钥扫描

- **gitleaks 已安装**（推荐）：`gitleaks protect --staged` 为权威硬门禁，发现即阻断提交。
- **未安装**：回退内置 grep（高置信 key=value 模式 + 已知 provider key 前缀），发现仅 warn（防"误提交"型泄漏），CI 用 gitleaks 兜底。

安装 gitleaks（macOS）：

```bash
brew install gitleaks
```

## 手动测试

```bash
# 1. 语法检查
bash -n .githooks/pre-commit
bash -n .githooks/pre-push
bash -n .githooks/commit-msg
bash -n .githooks/_lib.sh

# 2. 端到端 dry-run（用空提交触发 hook）
git commit --allow-empty -m "feat: ok"        # 应通过
git commit --allow-empty -m "bad message"     # 应被 commit-msg 拒绝
SKIP=git-hooks git commit --allow-empty -m "x"  # 应跳过所有 hook

# 3. 单独跑 pre-commit 的"密钥扫描"分支
echo 'diff --git a/x b/x
+++ b/x
+const k = "ghp_abcdefghijklmnopqrstuvwxyz0123456789"
' | git hash-object -w --stdin
# 暂存该 blob 触发扫描
```

## 故障排查

| 现象 | 原因 | 解决 |
|------|------|------|
| `mvnw: Permission denied` | `.githooks/` 下文件未设可执行位 | `chmod +x .githooks/{pre-commit,pre-push,commit-msg}` |
| `biome: not found` | `node_modules` 未安装 | `cd easyorange-frontend && npm install` |
| Pre-push 慢 | `mvn test` 全量测试 | 属预期（重门禁）；只推相关提交可减少触发；`SKIP=git-hooks` 单次绕过 |
| 误报密钥 | 含 `API_KEY=${ENV}` 引用 | 已过滤 `${VAR}` 语法；若仍误报 `SKIP=git-hooks` 单次绕过 |
| Hook 没生效 | `core.hooksPath` 未设 | `git config core.hooksPath .githooks` |

## 未来扩展

- **commit-msg 模板**：`git config commit.template .gitmessage`（与本 hook 解耦）
- **pre-push 加重**：后端从 `test` 升 `verify`（含 JaCoCo 报告；`-Djacoco.haltOnFailure=true` 可作 CI 门禁）
