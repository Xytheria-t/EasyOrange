package com.cartethyia.easyorange.framework.auth;

/**
 * 不透明 refresh token 存储端口。
 * <p>
 * 负责 refresh token 的签发、轮换（含复用检测）、吊销与按用户吊销。
 * 实现为 Redis 存储，token 以 SHA-256 哈希形式落盘。
 */
public interface RefreshTokenStore {

    /** 为指定用户签发一个新 refresh token（新会话）。 */
    String create(String userId);

    /**
     * 轮换一个 refresh token：消费旧 token 并签发新 token。
     * 复用检测：已被消费过的 token 再次出现且超宽限期，视为凭证被盗，吊销该用户全部会话。
     *
     * @throws com.cartethyia.easyorange.common.exception.BusinessException 无效/过期/复用
     */
    TokenRotation rotate(String refreshToken);

    /** 吊销单个 refresh token（登出）。幂等。 */
    void revoke(String refreshToken);

    /** 吊销指定用户的全部会话（强制下线 / 改密 / 盗用）。 */
    void revokeAllSessions(String userId);
}