package com.cartethyia.easyorange.config.secret;

import org.springframework.stereotype.Component;

/**
 * 环境变量实现的 {@link SecretService} — 从 {@code System.getenv} 读取密钥。
 * <p>
 * 这是默认实现；生产环境如需集中密钥管理，替换此 bean 的装配即可（预留 Vault/KMS 接入点）。
 */
@Component
public class EnvSecretService implements SecretService {

    @Override
    public String resolve(String key) {
        var value = System.getenv(key);
        return (value == null || value.isBlank()) ? null : value;
    }
}
