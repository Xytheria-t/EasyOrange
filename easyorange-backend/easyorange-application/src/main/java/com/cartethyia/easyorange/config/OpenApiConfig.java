package com.cartethyia.easyorange.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 3 配置 — 自动生成 API 契约文档 + Swagger UI。
 * <p>
 * 访问入口：
 * <ul>
 *   <li>API JSON：{@code /v3/api-docs}</li>
 *   <li>Swagger UI：{@code /swagger-ui.html}</li>
 * </ul>
 * 全局 JWT Bearer 认证方案 — Swagger UI 顶部「Authorize」按钮输入 token 即可调试受保护端点。
 * <p>
 * 路径白名单见 {@code application.yaml} 的 {@code security.ignore-paths}。
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EasyOrange API")
                        .description("""
                                EasyOrange — LLM × DDD：Java 架构工程化实战。
                                AI 工程化 7 件套：可换供应商、可降级、可观测。
                                """)
                        .version("v1")
                        .contact(new Contact().name("cartethyia").url("https://github.com/cartethyia/easy-orange"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(new Server().url("/").description("本地开发环境")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .description("""
                                                JWT Bearer Token 认证。
                                                登录 /api/auth/login 获取 accessToken 后，
                                                在此输入 Bearer <token>。""")));
    }
}
