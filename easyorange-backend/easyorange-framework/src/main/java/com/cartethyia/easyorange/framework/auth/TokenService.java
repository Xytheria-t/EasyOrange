package com.cartethyia.easyorange.framework.auth;

import java.util.Collection;

/**
 * 认证令牌门面：access（JWT）与 refresh（opaque）统一入口。
 * <p>
 * access token 为 RSA 签名的短时 JWT；refresh token 为不透明随机串（Redis 存储，
 * 存 SHA-256 哈希），支持轮换、复用检测与按用户吊销。
 */
public interface TokenService {

    /** 签发 access JWT（type=access）。 */
    String createAccessToken(String userId, String username, Collection<String> authorities);

    /** 签发 opaque refresh token（新会话）。 */
    String createRefreshToken(String userId);

    /** 轮换 refresh token（消费旧并签发新）；复用/无效/过期由存储层抛业务异常。 */
    TokenRotation rotateRefreshToken(String refreshToken);

    /** 吊销单个 refresh token（登出）。幂等。 */
    void revokeRefreshToken(String refreshToken);

    /** 吊销 access token（jti 加入黑名单）。 */
    void revokeAccessToken(String accessToken);

    /** 吊销指定用户全部会话：refresh 全部失效 + access 强制下线。 */
    void revokeAllUserSessions(String userId);
}
