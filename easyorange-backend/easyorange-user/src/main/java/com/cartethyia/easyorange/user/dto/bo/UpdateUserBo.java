package com.cartethyia.easyorange.user.dto.bo;

import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.enums.Sex;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/**
 * 更新用户信息业务对象
 * 职责：封装用户信息更新逻辑，支持增量更新
 */
public record UpdateUserBo(
        String email,
        String phone,
        Integer gender
) {

    /**
     * 是否有邮箱更新
     */
    public boolean hasEmailUpdate() {
        return StringUtils.isNotBlank(email);
    }

    /**
     * 是否有手机号更新
     */
    public boolean hasPhoneUpdate() {
        return StringUtils.isNotBlank(phone);
    }

    /**
     * 是否有性别更新
     */
    public boolean hasGenderUpdate() {
        return gender != null;
    }

    /**
     * 获取性别枚举
     */
    public Sex getSex() {
        return hasGenderUpdate() ? Sex.fromOrdinal(gender) : null;
    }

    /**
     * 应用更新到现有用户实体（增量更新）
     * 业务规则：只更新非空字段，保留原有值
     */
    public void applyTo(User existingUser) {
        if (hasEmailUpdate()) {
            existingUser.setEmail(email);
        }
        if (hasPhoneUpdate()) {
            existingUser.setPhone(phone);
        }
        if (hasGenderUpdate()) {
            existingUser.setSex(getSex());
        }
        existingUser.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 检查是否有任何字段需要更新
     */
    public boolean hasAnyUpdate() {
        return hasEmailUpdate() || hasPhoneUpdate() || hasGenderUpdate();
    }
}
