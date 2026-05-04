package com.cartethyia.easyorange.user.domain.port;

/**
 * 登录尝试次数端口 - 用于管理登录失败计数和账户锁定
 * <p>
 * 职责：
 * <ul>
 *   <li>记录和查询登录失败次数</li>
 *   <li>支持登录尝试次数的递增、过期和清除</li>
 *   <li>用于实现账户锁定、防暴力破解等安全策略</li>
 * </ul>
 * 
 * <p>实现类通常基于 Redis 等缓存存储
 */
public interface LoginAttemptPort extends OutboundPort {

    Long getAttempts(String account);

    long incrementAttempts(String account);

    void expireAttempts(String account, long minutes);

    void clearAttempts(String account);
}
