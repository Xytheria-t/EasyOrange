package com.cartethyia.easyorange.user.domain.port;

/**
 * 登录尝试次数端口 - 用于管理登录失败计数和账户锁定
 * <p>
 * 职责：
 * <ul>
 *   <li>记录和查询登录失败次数</li>
 *   <li>支持登录尝试次数的递增（自动管理 TTL）和清除</li>
 *   <li>用于实现账户锁定、防暴力破解等安全策略</li>
 * </ul>
 * 
 * <p>实现类通常基于 Redis 等缓存存储
 */
public interface LoginAttemptPort {

    Long getAttempts(String identifier);

    long incrementAndExpire(String identifier, long expireMinutes);

    void clearAttempts(String identifier);

    /**
     * 查询登录锁定剩余时间（秒）
     * @param identifier 登录标识（用户名/手机号）
     * @return 剩余秒数，未锁定时返回 0 或负数
     */
    long getRemainingLockSeconds(String identifier);
}