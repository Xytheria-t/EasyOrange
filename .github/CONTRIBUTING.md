# 贡献指南

感谢参与 EasyOrange 项目！本文档描述如何向本项目提交贡献。

## 分支策略

- `main` — 稳定发布分支，始终保持可运行状态
- `develop` — 集成分支，新功能先合并到此
- `feature/<scope>-<short-desc>` — 功能分支（如 `feature/ai-token-budget`）
- `fix/<scope>-<short-desc>` — 修复分支（如 `fix/order-saga-deadlock`）
- `refactor/<scope>-<short-desc>` — 重构分支

## 提交规范（Conventional Commits）

本项目使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范，由 `commit-msg` hook 强制校验。

```
<type>(<scope>): <description>

<optional body>
```

### Types

| Type | 用途 |
|---|---|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `refactor` | 重构（不改变外部行为） |
| `docs` | 文档变更 |
| `test` | 测试相关 |
| `chore` | 构建/工具/依赖 |
| `perf` | 性能优化 |
| `ci` | CI/CD 配置 |
| `style` | 代码风格（不影响逻辑） |
| `build` | 构建系统或外部依赖 |

### 示例

```
feat: add user login endpoint
fix(order): resolve Saga compensation deadlock
refactor(ai): extract Prompt YAML registry
docs: update README architecture section
test(payment): add PaymentTransition unit tests
chore: bump Spring Boot to 4.0.3
perf: cache product list with Caffeine
ci: enable JaCoCo gate in workflow
```

### 标题规则

- 第一行 ≤ 72 字符
- 使用祈使句（`add` 而非 `added`）
- 不以句号结尾
- scope 可选，使用 kebab-case

## 开发流程

1. **Fork & Clone** 仓库
2. **创建分支**：`git checkout -b feature/<scope>-<desc>`
3. **安装 Git hooks**（一次性）：
   ```bash
   git config core.hooksPath .githooks
   ```
4. **开发**：遵循 [`.claude/rules/ecc/`](.claude/rules/ecc/) 下的编码规范（ECC 分层规则集，common 通用 + java/typescript/react/web 语言专用）
5. **本地验证**：
   ```bash
   # 后端
   cd easyorange-backend && ./mvnw test
   # 前端
   cd easyorange-frontend && npm test
   ```
6. **提交**：`git commit -m "feat: ..."`
7. **推送**：`git push origin feature/<scope>-<desc>`
8. **创建 PR** 到 `develop` 分支

## PR 要求

### 必须通过
- [ ] CI 全量 11 模块测试全绿
- [ ] JaCoCo 覆盖率门禁（line ≥ 80%, branch ≥ 60%）
- [ ] 前端 `npm run lint:check` 0 errors
- [ ] 前端 `npm test` 全绿
- [ ] 提交信息符合 Conventional Commits

### 强烈建议
- [ ] 新增功能有对应单元测试（覆盖率 ≥ 80%）
- [ ] 公共 API 变更有对应文档更新
- [ ] 架构决策变更补充 ADR（`doc/adr/`）
- [ ] 数字类变更同步 `doc/工程指标.md`（项目数字单一来源）

### 不接受
- 提交包含密钥/凭证/`.env` 文件
- 引入未在 `pom.xml` / `package.json` 声明的依赖
- 使用 `console.log` / `System.out.println` 调试代码
- 破坏现有测试（除非测试本身有误）

## 测试要求

### 后端
- 单元测试：JUnit 5 + AssertJ + Mockito（`src/test/java/`）
- 测试夹具：domain 层用 `*TestFixture`（如 `OrderTestFixture`）
- 命名：`methodName_scenario_expectedBehavior()` + `@DisplayName`
- 模式：AAA（Arrange-Act-Assert）
- 覆盖率：domain 层 ≥ 80%，全模块聚合 ≥ 70%

### 前端
- 单元测试：Vitest + @testing-library/react（`*.test.tsx`）
- E2E：Playwright（`e2e/` 目录）
- Lint：Biome（`npm run lint:check`）

## 架构约束

本项目使用 DDD 六边形架构，由 ArchUnit 守卫（`ArchitectureRulesTest`）。提交前请确保：

- **domain 层零框架依赖**（不能 import Spring/MyBatis/Jackson）
- **port 接口在 domain 层**，adapter 实现在 infrastructure 层
- **聚合根不可变**（字段 `final`，状态转换返回新实例）
- **领域事件实现 `DomainEvent` 接口**
- **CQRS 限于 product/order/payment/message 四模块**

详见 [`doc/架构/架构-DDD规范.md`](doc/架构/架构-DDD规范.md) 与 [`AGENTS.md`](AGENTS.md)。

## 问题与讨论

- Bug 报告：创建 Issue，使用 Bug 模板
- 功能建议：创建 Issue，标注 `enhancement`
- 安全漏洞：参见 [`SECURITY.md`](SECURITY.md)

## 行为准则

请保持尊重和专业。不接受任何形式的骚扰、歧视或攻击性行为。
