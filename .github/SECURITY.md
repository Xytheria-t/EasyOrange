# 安全策略

## 报告漏洞

**请勿通过公开 Issue 报告安全漏洞。** 走 GitHub → Security → Report a vulnerability（私密 Advisory），附漏洞类型、影响组件、复现步骤。

## 已知安全特性

- **认证**：JWT (RS256) + Spring Security `oauth2ResourceServer`
- **密码**：BCrypt（可配置强度 4-31，默认 10）
- **授权**：方法级 `@PreAuthorize` + RBAC
- **限流**：Redis 令牌桶 + 本地 fallback
- **SQL 注入**：MyBatis `#{}` 参数化（全量 grep 校验）
- **CSRF**：Stateless JWT API 禁用 CSRF
- **CORS**：环境变量驱动 allowlist
- **安全头**：X-Frame-Options DENY / HSTS / CSP
- **输入校验**：Bean Validation（`@NotBlank/@NotNull/@Size`）
- **审计日志**：AOP 自动记录 + 敏感字段脱敏
- **依赖扫描**：OWASP Dependency-Check（nightly `security-scan.yml` + 本地 `-Powasp` 按需扫描），误报抑制清单 [dependency-suppression.xml](../easyorange-backend/owasp/dependency-suppression.xml)

## 编码红线

- 不硬编码密钥 / 密码 / Token（走根 `.env` 或部署侧 Secret），禁止提交 `.env`
- 用户输入全部经 Bean Validation；SQL 使用 `#{}`，禁止 `${}` 拼接用户输入
- 不返回敏感字段（密码 / Token / 内部 ID）；错误信息不泄露堆栈与 SQL
