package com.cartethyia.easyorange.user.application.dto;

import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;

/**
 * 应用层用户视图 — 跨应用边界的安全投影（剔除凭据与登录信息），供 web 适配层组装响应。
 * 复用不可变值对象，避免与聚合字段重复映射。
 */
public record UserView(
        String id,
        String username,
        UserType userType,
        UserStatus status,
        ContactInfo contactInfo,
        PersonalInfo personalInfo,
        AuditInfo auditInfo) {

    public static UserView from(User user) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getUserType(),
                user.getStatus(),
                user.getContactInfo(),
                user.getPersonalInfo(),
                user.getAuditInfo());
    }
}
