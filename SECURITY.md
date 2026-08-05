# 安全策略

## 报告漏洞

EasyOrange 重视安全问题。如果您发现安全漏洞，请按以下流程报告。

### 报告方式

**请勿通过公开 Issue 报告安全漏洞。**

请通过以下方式之一私密报告：

1. **GitHub Security Advisory**（推荐）：
   - 访问仓库 → Security → Report a vulnerability
   - 填写漏洞详情、影响范围、复现步骤

2. **邮件**（如 GitHub Security 不可用）：
   - 标题：`[SECURITY] EasyOrange — <简短描述>`
   - 包含：漏洞类型、影响组件、复现步骤、建议修复方案

### 响应时间

| 阶段 | 时间 |
|---|---|
| 确认收到 | 48 小时内 |
| 初步评估 | 5 个工作日内 |
| 修复方案 | 严重漏洞 7 天内，一般漏洞 30 天内 |
| 公开披露 | 修复发布后 90 天（或与报告者协商） |

## 支持版本

本项目提供安全更新的范围：仅对最新 `main` 分支提供安全更新。

## 已知安全特性

EasyOrange 已实现的安全机制：

- **认证**：JWT (RS256) + Spring Security oauth2ResourceServer
- **密码**：BCrypt（可配置强度 4-31，默认 10）
- **授权**：方法级 `@PreAuthorize` + RBAC
- **限流**：Redis 令牌桶 + 本地 fallback
- **SQL 注入**：MyBatis `#{}` 参数化（全量 grep 校验）
- **CSRF**：Stateless JWT API 禁用 CSRF
- **CORS**：环境变量驱动 allowlist
- **安全头**：X-Frame-Options DENY / HSTS / CSP
- **输入校验**：Bean Validation（`@NotBlank/@NotNull/@Size`）
- **审计日志**：AOP 自动记录 + 敏感字段脱敏
- **依赖扫描**：OWASP Dependency-Check（CI 强制）

## 安全最佳实践（贡献者）

提交代码前请确保：

- [ ] 不硬编码密钥/密码/Token（使用环境变量或 Vault）
- [ ] 用户输入全部经过 Bean Validation 校验
- [ ] SQL 查询使用 `#{}` 参数化（禁止 `${}` 拼接用户输入）
- [ ] 不返回敏感字段（密码、Token、内部 ID）给客户端
- [ ] 错误信息不泄露内部实现（堆栈、SQL、文件路径）
- [ ] 新增依赖通过 `./mvnw org.owasp:dependency-check-maven:check` 扫描

## 联系方式

- 项目维护者：@cartethyia
- 安全相关 Issue 标签：`security`
