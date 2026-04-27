# Agent Orchestration Guide

This document defines the agent orchestration strategy for the EasyOrange project. It ensures consistent, high-quality development across the Java Spring Boot backend and TypeScript/React frontend.

---

## Available Agents

| Agent | Purpose | When to Use |
|-------|---------|-------------|
| **planner** | Implementation planning | Complex features, refactoring, multi-phase tasks |
| **architect** | System design | Architectural decisions, technology evaluation |
| **tdd-guide** | Test-driven development | New features, bug fixes (enforces 80%+ coverage) |
| **code-reviewer** | Code review | After writing or modifying code |
| **security-reviewer** | Security analysis | Auth, payments, user data, API endpoints |
| **build-error-resolver** | Fix build errors | Maven/Gradle or TypeScript build failures |
| **e2e-runner** | E2E testing | Critical user flows (Playwright) |
| **refactor-cleaner** | Dead code cleanup | Removing unused code, consolidation |
| **doc-updater** | Documentation | Updating docs after changes |
| **java-code-reviewer** | Java/Spring Boot review | Java backend code quality |
| **springboot-tdd-expert** | Spring Boot TDD | Java backend feature development |
| **database-migration-expert** | Database migrations | Schema changes, zero-downtime migrations |
| **api-designer** | REST API design | New endpoints, API contracts |
| **frontend-architect** | Frontend development | React components, state management |
| **ui-designer** | UI/UX design | Component design, visual systems |
| **performance-expert** | Performance optimization | Bottlenecks, profiling, load testing |

---

## Immediate Agent Usage

**No user prompt needed** — agents are triggered automatically based on context:

1. **Complex feature requests** → Use **planner** agent
2. **Code just written/modified** → Use **code-reviewer** agent
3. **Bug fix or new feature** → Use **tdd-guide** agent
4. **Architectural decision** → Use **architect** agent
5. **Build failure** → Use **build-error-resolver** agent
6. **Security-sensitive code** → Use **security-reviewer** agent

---

## Common Workflows

### 1. New Feature Development Workflow

Use when implementing new features:

```
User Request
    │
    ▼
┌─────────────────┐
│  planner agent  │  ← Create implementation plan
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  tdd-guide agent │  ← Write tests first (RED-GREEN-REFACTOR)
└────────┬────────┘
         │
         ▼
┌──────────────────┐
│  code-reviewer   │  ← Quality and security review
└──────────────────┘
```

**Trigger conditions:**
- `planner` — Complex features, refactoring, multi-phase tasks
- `tdd-guide` — Bug fixes, new feature implementation
- `code-reviewer` — After code is written

---

### 2. Code Review Workflow

Use for quality assurance after code changes:

```
Code Changes Complete
    │
    ▼
┌─────────────────────┐
│  code-reviewer agent │  ← General code quality review
└──────────┬──────────┘
           │
           ▼ (Security issues detected)
┌──────────────────────┐
│  security-reviewer   │  ← Deep security analysis
└──────────────────────┘
```

**Trigger conditions:**
- `code-reviewer` — After every code modification
- `security-reviewer` — Authentication, payments, user data, API endpoints

---

### 3. Backend Development Workflow (Java Spring Boot)

Use for Java backend development:

```
Backend Feature Request
    │
    ▼
┌──────────────────────┐
│ springboot-tdd-expert │  ← TDD with JUnit 5, Mockito
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  java-code-reviewer  │  ← Spring Boot best practices
└──────────┬───────────┘
           │
           ▼ (Database changes)
┌───────────────────────────┐
│  database-migration-expert │  ← Schema migration
└───────────────────────────┘
```

**Trigger conditions:**
- `springboot-tdd-expert` — New REST endpoints, service layer, repositories
- `java-code-reviewer` — Java code quality, Spring Boot patterns
- `database-migration-expert` — PostgreSQL schema changes

---

### 4. Frontend Development Workflow (TypeScript/React)

Use for frontend development:

```
Frontend Feature Request
    │
    ▼
┌─────────────────────┐
│  frontend-architect  │  ← Component design, state management
└──────────┬──────────┘
           │
           ▼
┌─────────────────┐
│   ui-designer    │  ← Visual design, accessibility
└────────┬─────────┘
         │
         ▼
┌───────────────────┐
│  ui-visual-validator │  ← Visual regression check
└───────────────────┘
```

**Trigger conditions:**
- `frontend-architect` — React components, state management, performance
- `ui-designer` — Component design, design systems
- `ui-visual-validator` — After UI modifications

---

### 5. Parallel Multi-Dimensional Analysis Workflow

Use for complex problems requiring multiple perspectives:

```
Complex Problem
    │
    ├─── Agent 1 (Security): Security vulnerability analysis
    ├─── Agent 2 (Performance): Performance bottleneck review
    ├─── Agent 3 (Quality): Code quality and patterns
    └─── Agent 4 (Consistency): API consistency check
```

**Advantage:** Parallel execution saves time and provides comprehensive coverage.

---

### 6. Build Error Resolution Workflow

Use when builds fail:

```
Build Failure
    │
    ▼
┌─────────────────────┐
│ build-error-resolver │  ← Diagnose and fix errors
└─────────────────────┘
```

**Trigger conditions:**
- Maven/Gradle compilation errors
- TypeScript type errors
- Module resolution failures
- Annotation processor errors

---

### 7. API Development Workflow

Use for REST API development:

```
API Endpoint Request
    │
    ▼
┌─────────────────┐
│   api-designer   │  ← Design REST patterns
└────────┬─────────┘
         │
         ▼
┌──────────────────────┐
│ springboot-tdd-expert │  ← Implement with TDD
└──────────┬───────────┘
           │
           ▼
┌─────────────────────┐
│   api-test-pro       │  ← Contract & load testing
└─────────────────────┘
```

**Trigger conditions:**
- `api-designer` — New endpoints, pagination, filtering, error handling
- `springboot-tdd-expert` — Implementation with tests
- `api-test-pro` — Performance and contract validation

---

### 8. Database Migration Workflow

Use for schema changes:

```
Schema Change Request
    │
    ▼
┌───────────────────────────┐
│  database-migration-expert │  ← Plan zero-downtime migration
└──────────┬────────────────┘
           │
           ▼
┌──────────────────────┐
│ springboot-tdd-expert │  ← Update repositories
└──────────────────────┘
```

**Trigger conditions:**
- Adding/removing columns or tables
- Renaming columns in production
- Index optimization
- Data migrations

---

### 9. E2E Testing Workflow

Use for critical user flow validation:

```
Critical Feature Complete
    │
    ▼
┌─────────────────┐
│  e2e-runner agent │  ← Playwright E2E tests
└─────────────────┘
```

**Trigger conditions:**
- New authentication flows
- Checkout/payment flows
- Critical user journeys
- Pre-deployment validation

---

### 10. Maintenance Cleanup Workflow

Use for code maintenance:

```
Dead Code / Redundancy Detected
    │
    ▼
┌─────────────────────┐
│  refactor-cleaner    │  ← Remove unused code
└─────────────────────┘
```

**Trigger conditions:**
- After feature completion
- Before production deployment
- Bundle size optimization
- Duplicate code consolidation

---

## Parallel Task Execution

**ALWAYS** use parallel Task execution for independent operations:

```markdown
# GOOD: Parallel execution
Launch 3 agents in parallel:
1. Agent 1: Security analysis of auth module
2. Agent 2: Performance review of cache system
3. Agent 3: Type checking of utilities

# BAD: Sequential when unnecessary
First agent 1, then agent 2, then agent 3
```

---

## Multi-Perspective Analysis

For complex problems, use split-role sub-agents:

- **Factual reviewer** — Verify correctness
- **Senior engineer** — Architecture and patterns
- **Security expert** — OWASP Top 10, vulnerabilities
- **Consistency reviewer** — API consistency
- **Redundancy checker** — Duplicate code detection

---

## Agent Trigger Matrix

| Scenario | Agent | Reason |
|----------|-------|--------|
| Complex feature request | `planner` | Plan before implementation |
| Code just written/modified | `code-reviewer` | Quality review |
| Bug fix or new feature | `tdd-guide` | Test-driven development |
| Architectural decision | `architect` | System design |
| Security-sensitive code | `security-reviewer` | Security audit |
| Build failure | `build-error-resolver` | Quick error resolution |
| Code cleanup | `refactor-cleaner` | Dead code removal |
| Critical user flow | `e2e-runner` | End-to-end testing |
| Documentation update | `doc-updater` | Documentation maintenance |
| Java backend feature | `springboot-tdd-expert` | Spring Boot TDD |
| Database schema change | `database-migration-expert` | Safe migrations |
| REST API design | `api-designer` | REST best practices |
| Frontend component | `frontend-architect` | React/TypeScript patterns |
| UI/UX design | `ui-designer` | Visual quality |
| Performance issue | `performance-expert` | Bottleneck analysis |

---

## Project-Specific Agents

### Backend (Java Spring Boot)

| Agent | Purpose |
|-------|---------|
| `springboot-tdd-expert` | JUnit 5, Mockito, Testcontainers |
| `java-code-reviewer` | Spring Boot best practices |
| `database-migration-expert` | PostgreSQL migrations |
| `api-designer` | REST API contracts |

### Frontend (TypeScript/React)

| Agent | Purpose |
|-------|---------|
| `frontend-architect` | React components, state management |
| `ui-designer` | Component design, accessibility |
| `ui-visual-validator` | Visual regression testing |
| `e2e-runner` | Playwright E2E tests |

---

## Best Practices

1. **Always use parallel execution** when agents are independent
2. **Trigger agents proactively** — don't wait for user confirmation
3. **Use domain-specific agents** for specialized tasks (e.g., `springboot-tdd-expert` for Java)
4. **Run security reviews** for auth, payments, and user data code
5. **Enforce TDD** for all new features and bug fixes (80%+ coverage)
6. **Review code immediately** after writing or modifying
7. **Use build-error-resolver** for any compilation failures
8. **Run E2E tests** for critical user flows before deployment

---

## References

- [Development Workflow](./.trae/rules/common/development-workflow.md)
- [Testing Requirements](./.trae/rules/common/testing.md)
- [Security Guidelines](./.trae/rules/common/security.md)
- [Code Review Standards](./.trae/rules/common/code-review.md)
- [Java Coding Style](./.trae/rules/java/coding-style.md)
- [TypeScript Coding Style](./.trae/rules/typescript/coding-style.md)
