package com.cartethyia.easyorange.user.domain.valueobject;

import java.time.LocalDateTime;

public record AuditInfo(
    LocalDateTime createTime,
    LocalDateTime updateTime,
    Long createBy,
    Long updateBy,
    Integer delFlag,
    int version
) {
    public static final int NOT_DELETED = 0;
    public static final int DELETED = 1;

    public static AuditInfo create(Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        return new AuditInfo(now, now, operatorId, operatorId, NOT_DELETED, 0);
    }

    public AuditInfo update(Long operatorId) {
        return new AuditInfo(
            createTime,
            LocalDateTime.now(),
            createBy,
            operatorId,
            delFlag,
            version
        );
    }

    public AuditInfo markDeleted(Long operatorId) {
        return new AuditInfo(
            createTime,
            LocalDateTime.now(),
            createBy,
            operatorId,
            DELETED,
            version
        );
    }
}
