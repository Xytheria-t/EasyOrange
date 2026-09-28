package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;

/**
 * 管理端用户写操作领域服务 — 持有状态/角色/密码变更的业务规则（用户存在性、解锁前置、
 * 最后一个管理员保护），并完成聚合根状态迁移。持久化由端口适配器负责。
 * <p>
 * 三个写方法都收 {@code reason} 与 {@code operatorId}：管理端对用户下手的每一笔都要能回答
 * 「谁、为什么」—— 前者落 {@code eo_user.remark}，后者落 {@code audit_info.update_by}。
 * {@code reason} 为 null（系统自身发起）时只记操作人，不覆盖已有备注。
 */
public class AdminUserManagementService {

    private final UserRepository userRepository;

    public AdminUserManagementService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 更新用户状态，用户不存在或已删除时抛出业务异常。 */
    public User updateStatus(String userId, UserStatus newStatus, String reason, String operatorId) {
        return requireActiveUser(userId).changeStatus(newStatus, operatorId).withChangeReason(reason);
    }

    /** 解锁/启用用户：仅当状态为 LOCKED 或 DISABLED 时置为 NORMAL，否则抛出业务异常。 */
    public User unlock(String userId, String operatorId) {
        User user = requireActiveUser(userId);
        if (user.getStatus() != UserStatus.LOCKED && user.getStatus() != UserStatus.DISABLED) {
            throw BusinessException.of("该用户未被锁定或禁用");
        }
        return user.changeStatus(UserStatus.NORMAL, operatorId);
    }

    /** 变更用户角色：禁止改为当前角色，且不能降级最后一个管理员。 */
    public User changeUserType(String userId, UserType target, String reason, String operatorId) {
        User user = requireActiveUser(userId);
        if (target == user.getUserType()) {
            throw BusinessException.of("用户已是该角色");
        }
        if (user.getUserType() == UserType.ADMIN && userRepository.countByUserType(UserType.ADMIN) <= 1) {
            throw BusinessException.of("不能修改最后一个管理员的角色");
        }
        return user.changeUserType(target, operatorId).withChangeReason(reason);
    }

    /** 重置用户密码（encodedPassword 为已编码密文），用户不存在或已删除时抛出业务异常。 */
    public User resetPassword(String userId, String encodedPassword, String reason, String operatorId) {
        return requireActiveUser(userId)
                .changePassword(encodedPassword, operatorId)
                .withChangeReason(reason);
    }

    private User requireActiveUser(String userId) {
        return userRepository
                .findById(userId)
                .filter(user ->
                        user.getAuditInfo() == null || user.getAuditInfo().delFlag() == 0)
                .orElseThrow(() -> BusinessException.of("用户不存在"));
    }
}
