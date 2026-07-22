package com.cartethyia.easyorange.user.domain.aggregate;

import com.cartethyia.easyorange.user.domain.enums.Sex;

/**
 * User 聚合根个人资料更新参数对象 — 收敛 realName/nickName/sex/studentId 长参数为单一 record。
 * <p>
 * 提升调用点可读性并避免参数顺序错配。纯 VO 字段，domain 层零框架依赖。
 */
public record PersonalUpdateSpec(String realName, String nickName, Sex sex, String studentId) {}
