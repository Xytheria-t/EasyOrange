package com.cartethyia.easyorange.user.domain.port;

import java.time.Duration;

/**
 * 短信验证码端口 - 验证码的核心存储和验证
 * <p>
 * 职责：
 * <ul>
 *   <li>验证码的存储和获取</li>
 *   <li>验证码的生命周期管理</li>
 * </ul>
 * 
 * <p>实现类通常基于 Redis 等缓存存储，支持 TTL 自动过期
 */
public interface SmsCodePort {

    /**
     * 保存验证码
     * 
     * @param phone 手机号
     * @param code 验证码
     * @param ttl 过期时长
     */
    void save(String phone, String code, Duration ttl);

    /**
     * 获取验证码
     * 
     * @param phone 手机号
     * @return 验证码，若不存在或已过期返回 null
     */
    String get(String phone);

    /**
     * 删除验证码（验证成功后调用）
     * 
     * @param phone 手机号
     */
    void delete(String phone);
}