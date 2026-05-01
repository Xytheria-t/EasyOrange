# Agent 编排指南

本文档定义 EasyOrange 项目的 Agent 编排策略，确保 Java Spring Boot 后端和 TypeScript/React 前端的一致性、高质量开发。

---

## 核心原则

### 自动触发机制

**无需用户确认** — Agent 根据上下文自动触发：

| 场景 | Agent | 触发时机 |
|------|-------|---------|
| 复杂功能请求 | `implementation-planner` | 多阶段任务、架构决策 |
| 代码刚写完/修改 | `code-reviewer` | 每次 code change 后 |
| Bug 修复/新功能 | `tdd-specialist` | 功能开发、bug 修复 |
| 安全敏感代码 | `security-reviewer` | 认证、支付、用户数据 |
| 构建失败 | `build-error-resolver` | 编译错误、类型错误 |
| 代码清理 | `refactor-cleaner` | 死代码、重复代码 |

### 并行执行优先

**始终并行**执行独立的 Agent 任务：

```markdown
✅ 正确：并行执行
同时启动 3 个 agents：
1. Agent 1: 安全分析
2. Agent 2: 性能审查
3. Agent 3: 类型检查

❌ 错误：不必要的串行
先 agent 1，然后 agent 2，最后 agent 3
```

---

## 可用 Agents

### 通用开发 Agents

| Agent | 用途 | 关键场景 |
|-------|------|---------|
| **implementation-planner** | 实施规划 | 复杂功能、重构、多阶段任务 |
| **software-architect** | 系统设计 | 架构决策、技术评估 |
| **code-architect** | 代码架构 | 功能架构设计、实施蓝图 |
| **code-explorer** | 代码探索 | 理解现有系统、追踪执行路径 |
| **code-reviewer** | 代码审查 | 代码质量、最佳实践 |
| **code-simplifier** | 代码简化 | 重构、清理复杂代码 |
| **tdd-specialist** | 测试驱动开发 | 新功能、bug 修复（80%+ 覆盖率） |
| **security-reviewer** | 安全审查 | 认证、支付、用户输入、API |
| **build-error-resolver** | 构建错误修复 | TypeScript、编译错误 |
| **refactor-cleaner** | 代码清理 | 死代码、重复代码、未使用依赖 |

### Java/Spring Boot Agents

| Agent | 用途 | 关键场景 |
|-------|------|---------|
| **java-build-resolver** | Java 构建错误 | Maven/Gradle 错误、依赖问题 |
| **java-code-reviewer** | Java 代码审查 | Spring Boot 最佳实践、安全 |
| **springboot-tdd-expert** | Spring Boot TDD | JUnit 5、Mockito、Testcontainers |
| **database-migration-expert** | 数据库迁移 | Schema 变更、零停机迁移 |

### 前端 Agents

| Agent | 用途 | 关键场景 |
|-------|------|---------|
| **frontend-architect** | 前端架构 | React 组件、状态管理、性能 |
| **ui-designer** | UI/UX 设计 | 组件设计、视觉系统 |
| **e2e-test-runner** | E2E 测试 | Playwright、关键用户流程 |

### 专项 Agents

| Agent | 用途 | 关键场景 |
|-------|------|---------|
| **api-designer** | REST API 设计 | 新端点、API 契约、分页/过滤 |
| **backend-architect** | 后端架构 | API 设计、数据库、服务器逻辑 |
| **devops-architect** | DevOps 架构 | CI/CD、云基础设施、监控 |
| **doc-codemap-specialist** | 文档更新 | 重大功能添加、架构变更 |

---

## 关键工作流程

### 1. 新功能开发流程

```
用户请求新功能
    │
    ▼
┌────────────────────────┐
│ implementation-planner │  ← 创建实施计划
└───────────┬────────────┘
            │
            ▼
┌────────────────────┐
│   tdd-specialist   │  ← 测试先行（RED-GREEN-REFACTOR）
└───────────┬────────┘
            │
            ▼
┌────────────────────┐
│   code-reviewer    │  ← 质量和安全审查
└────────────────────┘
```

**触发条件：**
- `implementation-planner` — 复杂功能、重构、多阶段任务
- `tdd-specialist` — Bug 修复、新功能实现
- `code-reviewer` — 代码写完后

---

### 2. 代码审查流程

```
代码修改完成
    │
    ▼
┌────────────────────┐
│   code-reviewer    │  ← 通用代码质量审查
└───────────┬────────┘
            │
            ▼ (发现安全问题)
┌─────────────────────┐
│  security-reviewer  │  ← 深度安全分析
└─────────────────────┘
```

**触发条件：**
- `code-reviewer` — 每次代码修改后
- `security-reviewer` — 认证、支付、用户数据、API 端点

---

### 3. Java 后端开发流程

```
后端功能请求
    │
    ▼
┌─────────────────────────┐
│  springboot-tdd-expert  │  ← TDD with JUnit 5, Mockito
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│   java-code-reviewer    │  ← Spring Boot 最佳实践
└───────────┬─────────────┘
            │
            ▼ (数据库变更)
┌──────────────────────────────┐
│  database-migration-expert   │  ← Schema 迁移
└──────────────────────────────┘
```

**触发条件：**
- `springboot-tdd-expert` — 新 REST 端点、服务层、repositories
- `java-code-reviewer` — Java 代码质量、Spring Boot 模式
- `database-migration-expert` — PostgreSQL schema 变更

---

### 4. 前端开发流程

```
前端功能请求
    │
    ▼
┌─────────────────────┐
│  frontend-architect │  ← 组件设计、状态管理
└───────────┬─────────┘
            │
            ▼
┌─────────────────────┐
│     ui-designer     │  ← 视觉设计、可访问性
└───────────┬─────────┘
            │
            ▼
┌─────────────────────┐
│   e2e-test-runner   │  ← E2E 测试（关键流程）
└─────────────────────┘
```

**触发条件：**
- `frontend-architect` — React 组件、状态管理、性能
- `ui-designer` — 组件设计、设计系统
- `e2e-test-runner` — 关键用户流程

---

### 5. 并行多维分析流程

```
复杂问题
    │
    ├─── Agent 1 (安全): 安全漏洞分析
    ├─── Agent 2 (性能): 性能瓶颈审查
    ├─── Agent 3 (质量): 代码质量和模式
    └─── Agent 4 (一致性): API 一致性检查
```

**优势：** 并行执行节省时间，提供全面覆盖。

---

### 6. 构建错误解决流程

```
构建失败
    │
    ▼
┌─────────────────────────┐
│  build-error-resolver   │  ← 诊断并修复错误
└─────────────────────────┘
```

**触发条件：**
- Maven/Gradle 编译错误
- TypeScript 类型错误
- 模块解析失败
- 注解处理器错误

---

### 7. API 开发流程

```
API 端点请求
    │
    ▼
┌─────────────────┐
│   api-designer  │  ← 设计 REST 模式
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│  springboot-tdd-expert  │  ← 用 TDD 实现
└─────────────────────────┘
```

**触发条件：**
- `api-designer` — 新端点、分页、过滤、错误处理
- `springboot-tdd-expert` — 带测试的实现

---

### 8. 数据库迁移流程

```
Schema 变更请求
    │
    ▼
┌──────────────────────────────┐
│  database-migration-expert   │  ← 规划零停机迁移
└───────────┬──────────────────┘
            │
            ▼
┌─────────────────────────┐
│  springboot-tdd-expert  │  ← 更新 repositories
└─────────────────────────┘
```

**触发条件：**
- 添加/删除列或表
- 生产环境重命名列
- 索引优化
- 数据迁移

---

### 9. E2E 测试流程

```
关键功能完成
    │
    ▼
┌─────────────────────┐
│   e2e-test-runner   │  ← Playwright E2E 测试
└─────────────────────┘
```

**触发条件：**
- 新认证流程
- 结账/支付流程
- 关键用户旅程
- 部署前验证

---

### 10. 维护清理流程

```
发现死代码/冗余
    │
    ▼
┌─────────────────────┐
│   refactor-cleaner  │  ← 清理无用代码
└─────────────────────┘
```

**触发条件：**
- 功能完成后
- 生产部署前
- Bundle 大小优化
- 重复代码整合

---

## Agent 触发矩阵

| 场景 | Agent | 原因 |
|------|-------|------|
| 复杂功能请求 | `implementation-planner` | 先规划再实施 |
| 代码刚写完/修改 | `code-reviewer` | 质量审查 |
| Bug 修复/新功能 | `tdd-specialist` | 测试驱动开发 |
| 架构决策 | `software-architect` | 系统设计 |
| 安全敏感代码 | `security-reviewer` | 安全审计 |
| 构建失败 | `build-error-resolver` | 快速定位错误 |
| 代码清理 | `refactor-cleaner` | 死代码移除 |
| 关键用户流程 | `e2e-test-runner` | 端到端测试 |
| 文档更新 | `doc-codemap-specialist` | 文档维护 |
| Java 后端功能 | `springboot-tdd-expert` | Spring Boot TDD |
| 数据库 schema 变更 | `database-migration-expert` | 安全迁移 |
| REST API 设计 | `api-designer` | REST 最佳实践 |
| 前端组件 | `frontend-architect` | React/TypeScript 模式 |
| UI/UX 设计 | `ui-designer` | 视觉质量 |
| 性能问题 | `backend-architect` | 瓶颈分析 |

---

## 项目特定 Agents

### 后端 (Java Spring Boot)

| Agent | 用途 |
|-------|------|
| `springboot-tdd-expert` | JUnit 5, Mockito, Testcontainers |
| `java-code-reviewer` | Spring Boot 最佳实践 |
| `database-migration-expert` | PostgreSQL 迁移 |
| `api-designer` | REST API 契约 |

### 前端 (TypeScript/React)

| Agent | 用途 |
|-------|------|
| `frontend-architect` | React 组件、状态管理 |
| `ui-designer` | 组件设计、可访问性 |
| `e2e-test-runner` | Playwright E2E 测试 |

---

## 最佳实践

1. **始终使用并行执行** — 当 agents 独立时
2. **主动触发 agents** — 不等待用户确认
3. **使用领域特定 agents** — 针对专门任务（如 Java 用 `springboot-tdd-expert`）
4. **运行安全审查** — 针对认证、支付和用户数据代码
5. **强制 TDD** — 所有新功能和 bug 修复（80%+ 覆盖率）
6. **立即审查代码** — 写完或修改后
7. **使用 build-error-resolver** — 任何编译失败
8. **运行 E2E 测试** — 部署前关键用户流程

---

## 参考资料

- [开发工作流](./.trae/rules/common/development-workflow.md)
- [测试要求](./.trae/rules/common/testing.md)
- [安全指南](./.trae/rules/common/security.md)
- [代码审查标准](./.trae/rules/common/code-review.md)
- [Java 编码风格](./.trae/rules/java/coding-style.md)
- [TypeScript 编码风格](./.trae/rules/typescript/coding-style.md)
