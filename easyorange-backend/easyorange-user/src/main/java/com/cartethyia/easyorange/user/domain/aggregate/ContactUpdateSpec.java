package com.cartethyia.easyorange.user.domain.aggregate;

/**
 * User 聚合根联系方式更新参数对象 — 收敛 email/phone 长参数为单一 record。
 * <p>
 * 提升调用点可读性并避免参数顺序错配。纯 VO 字段，domain 层零框架依赖。
 */
public record ContactUpdateSpec(String email, String phone) {}
