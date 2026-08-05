package com.cartethyia.easyorange.framework.auth;

/**
 * 轮换成功后返回的会话信息：同属一个用户的活跃会话。
 *
 * @param userId    会话所属用户
 * @param newToken  新签发的 refresh token
 */
public record TokenRotation(
    String userId,
    String newToken
) {}