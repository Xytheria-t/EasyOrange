package com.cartethyia.easyorange.config.secret;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 生产环境密钥启动校验（fail-fast）— 启动期校验必需密钥是否就绪，缺失直接抛异常阻止启动，
 * 避免「静默用空值起服务」导致的连接失败/降级隐患。
 * <p>
 * 仅 {@code prod} profile 启用（dev 环境密钥有默认值或自动生成，无需强制）。
 * AI 密钥（DEEPSEEK/QWEN_VL/EMBEDDING）按既定契约可选、降级装配，不在此校验。
 */
@Slf4j
@Component
@Profile("prod")
public class SecretValidationRunner implements ApplicationRunner {

    /** 生产必需密钥（与 application-prod.yaml 的 ${...:} 无默认值项对齐）。 */
    private static final List<String> REQUIRED_SECRETS = List.of(
            "EASYORANGE_DB_HOST",
            "EASYORANGE_DB_USERNAME",
            "EASYORANGE_DB_PASSWORD",
            "REDIS_HOST",
            "REDIS_PASSWORD",
            "RABBITMQ_HOST",
            "RABBITMQ_USER",
            "RABBITMQ_PASSWORD",
            "JWT_RSA_PRIVATE_KEY",
            "JWT_RSA_PUBLIC_KEY");

    private final SecretService secretService;

    public SecretValidationRunner(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public void run(ApplicationArguments args) {
        var missing = REQUIRED_SECRETS.stream()
                .filter(key -> secretService.resolve(key) == null)
                .toList();
        if (missing.isEmpty()) {
            log.info("action=secret_validation, result=ok, required={}", REQUIRED_SECRETS.size());
            return;
        }
        throw new IllegalStateException("生产环境缺少必需密钥，启动中止: " + missing + " — 请通过环境变量/密钥管理配置后重启");
    }
}
