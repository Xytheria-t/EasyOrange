package com.cartethyia.easyorange.user.domain.port;

import java.time.Duration;

/**
 * 短信发送限制端口 - 控制发送频率和配额
 * <p>
 * 职责：
 * <ul>
 *   <li>发送间隔控制（防止短信轰炸）</li>
 *   <li>每日发送配额管理</li>
 *   <li>验证次数限制</li>
 * </ul>
 * 
 * <p>实现类通常基于 Redis 等缓存存储
 */
public interface SmsRateLimitPort extends OutboundPort {

    /**
     * 检查是否可以发送短信
     * 
     * @param phone 手机号
     * @return true 表示受限（不能发送），false 表示可以发送
     */
    boolean isSendLimited(String phone);

    /**
     * 设置发送间隔限制
     * 
     * @param phone 手机号
     * @param interval 限制间隔
     */
    void setSendInterval(String phone, Duration interval);

    /**
     * 递增每日发送次数
     * 
     * @param phone 手机号
     * @return 递增后的次数
     */
    long incrementDailyCount(String phone);

    /**
     * 设置每日发送计数的过期时间
     * 
     * @param phone 手机号
     * @param ttl 过期时长
     */
    void expireDailyCount(String phone, Duration ttl);

    /**
     * 递增验证尝试次数
     * 
     * @param phone 手机号
     * @return 递增后的次数
     */
    long incrementVerifyCount(String phone);

    /**
     * 设置验证计数的过期时间
     * 
     * @param phone 手机号
     * @param ttl 过期时长
     */
    void expireVerifyCount(String phone, Duration ttl);

    /**
     * 清除验证计数（验证成功后）
     * 
     * @param phone 手机号
     */
    void clearVerifyCount(String phone);
}
