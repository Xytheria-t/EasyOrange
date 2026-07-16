package com.cartethyia.easyorange.user.domain.port;

import java.time.Duration;

/**
 * 登录尝试次数端口 — 管理登录失败计数和账户锁定。
 * <p>
 * 职责：递增并检查失败次数、清除记录、查询剩余锁定时间。
 * 实现类通常基于 Redis 等缓存存储。
 */
public interface LoginAttemptPort {

    /**
     * 递增登录失败次数并返回当前值。首次递增时自动设置过期时间。
     *
     * @param identifier 登录标识（用户名/手机号）
     * @param expireAfter 锁定窗口时长
     * @return 递增后的当前失败次数
     */
    long incrementAndGet(String identifier, Duration expireAfter);

    /**
     * 清除指定标识的登录失败记录（登录成功后调用）。
     */
    void clear(String identifier);

    /**
     * 查询登录锁定剩余时间。
     *
     * @param identifier 登录标识（用户名/手机号）
     * @return 剩余秒数，未锁定时返回 0
     */
    long getRemainingLockSeconds(String identifier);
}