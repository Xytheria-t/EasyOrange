package com.cartethyia.easyorange.config.secret;

import org.springframework.stereotype.Component;

/**
 * 密钥解析 — 从 {@code System.getenv} 读取密钥，未配置返回 {@code null}。
 * <p>
 * 未来接 Vault / KMS / 云密钥管理时，把本类的读取来源替换为对应客户端即可，
 * 使用方 {@code SecretValidationRunner} 无需改动。
 */
@Component
public class SecretService {

    public String resolve(String key) {
        var value = System.getenv(key);
        return (value == null || value.isBlank()) ? null : value;
    }
}
