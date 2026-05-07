---
tags:
  - always-on
---

# EasyOrange 项目指南

**EasyOrange** 是一个基于 Spring Boot + React 的全栈电商平台项目。

## 项目结构

```
easy-orange/
├── easyorange-backend/     # Spring Boot 后端 (Java 25)
│   ├── easyorange-application/   # 应用启动入口
│   ├── easyorange-common/        # 通用组件
│   ├── easyorange-framework/      # 框架配置
│   ├── easyorange-user/          # 用户模块
│   ├── easyorange-product/       # 商品模块
│   ├── easyorange-order/         # 订单模块
│   ├── easyorange-payment/       # 支付模块
│   ├── easyorange-message/       # 消息模块
│   └── easyorange-favorite/      # 收藏模块
├── easyorange-frontend/     # React 前端 (TypeScript)
└── .trae/rules/            # AI 规则文件
```

## 规则激活机制

AI 规则存放在 `.trae/rules/` 目录，根据以下条件自动激活：

### 1. 文件路径激活（paths）

当编辑对应路径的文件时自动启用：

| 路径模式 | 激活规则 |
|---------|---------|
| `**/*.java` | Java 编码规范、模式、安全、测试 |
| `**/*.ts` | TypeScript 编码规范、模式、安全、测试 |
| `**/*.tsx` | TypeScript 编码规范、模式、安全、测试 |
| `**/*.css` | Web 设计规范、性能、安全 |
| `**/*.html` | Web 设计规范、性能、安全 |

### 2. 关键词激活（tags）

当用户输入包含以下关键词时自动启用对应规则：

| 关键词 | 激活规则 |
|-------|---------|
| auth, password, token, payment, encrypt, credential | 安全规则 |
| test, TDD, coverage, mock, unit test, e2e | 测试规则 |
| performance, optimize, cache, latency | 性能规则 |
| review, PR, pull request | 代码审查规则 |
| git, commit, branch, merge | Git 工作流规则 |
| pattern, architecture, refactor, repository | 设计模式规则 |
| agent, planner, tdd-guide | Agent 编排规则 |
| UI, component, design, layout | Web 设计规则 |

### 3. 始终激活（always-on）

所有任务都会加载的基础规则：
- `.trae/rules/karpathy-guidelines.md` - 行为准则
- `.trae/rules/common/coding-style.md` - 核心编码原则

### 4. 区域规则

当用户使用中文时自动加载：
- `.trae/rules/zh/*` - 中文版规则

## 快速参考

| 任务类型 | 加载规则 |
|---------|---------|
| 编写 Java 代码 | `java/coding-style.md`, `java/patterns.md` |
| 编写 TypeScript 代码 | `typescript/coding-style.md`, `typescript/patterns.md` |
| 编写前端样式 | `web/coding-style.md`, `web/design-quality.md` |
| 安全相关任务 | `common/security.md`, `*/security.md` |
| 测试相关任务 | `common/testing.md`, `*/testing.md` |
| 性能优化任务 | `common/performance.md` |
| 代码审查 | `common/code-review.md` |
| Git 操作 | `common/git-workflow.md` |

## 开发建议

1. **遵循规则**：查看对应规则文件获取详细的编码规范
2. **提问确认**：遇到不确定的情况，先问再做
3. **简洁优先**：最小代码解决问题，不做过度设计
4. **测试验证**：功能完成后运行测试确保质量

---

# 行为准则

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
