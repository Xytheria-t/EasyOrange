package com.cartethyia.easyorange.admin.application.service;

import com.cartethyia.easyorange.admin.domain.exception.AdminDomainException;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端用户查询编排 — 只做「端口调用 + 存在性裁决」两件事。
 * <p>
 * <b>取舍</b>：出入参都是 {@link AdminUserPort} 的记录（{@code UserDetail} / {@code UserQueryResult}），
 * 字段命名与分页包装在 web 侧的 assembler 完成 —— 服务层不碰 {@code adapter} 包，
 * 「端口翻译」这条边界因此能被 ArchUnit 规则验证，而不是靠命名约定。
 * <p>
 * <b>边界</b>：查不到一律 {@link AdminDomainException#userNotFound}（带 id 便于定位）；
 * 过滤条件由调用方给，服务既不兜底也不改写。
 */
@Service
@RequiredArgsConstructor
public class AdminUserAppService {

    private final AdminUserPort adminUserPort;

    @Transactional(readOnly = true)
    public UserQueryResult listUsers(UserQueryCondition condition) {
        return adminUserPort.queryUsers(condition);
    }

    @Transactional(readOnly = true)
    public UserDetail getUserDetail(String id) {
        UserDetail user = adminUserPort.getUserDetail(id);
        if (user == null) {
            throw AdminDomainException.userNotFound(id);
        }
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(String id, String status, String reason, String operatorId) {
        adminUserPort.updateUserStatus(id, status, reason, operatorId);
    }
}
