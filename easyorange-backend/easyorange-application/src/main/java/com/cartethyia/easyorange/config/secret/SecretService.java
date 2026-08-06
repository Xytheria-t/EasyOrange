package com.cartethyia.easyorange.config.secret;

/**
 * 密钥解析抽象 — 业务代码只依赖此接口取密钥，不关心密钥实际来源。
 * <p>
 * 当前默认实现 {@link EnvSecretService} 从环境变量读取；后续接 Vault / KMS / 云密钥管理时，
 * 只需新增实现并替换装配，无需改动任何业务代码。
 */
public interface SecretService {

    /**
     * 解析指定密钥的值；未配置返回 {@code null}。
     *
     * @param key 密钥名（如环境变量名 {@code EASYORANGE_DB_PASSWORD}）
     */
    String resolve(String key);
}
