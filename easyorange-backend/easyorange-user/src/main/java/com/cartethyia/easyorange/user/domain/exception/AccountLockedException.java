package com.cartethyia.easyorange.user.domain.exception;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import lombok.Getter;

/**
 * 账号登录锁定异常 — 由领域层抛出，不含 UI 文案。
 * <p>
 * 调用方（application 层）可按需格式化展示信息。
 */
@Getter
public class AccountLockedException extends BaseBusinessException {

    private final String identifier;
    private final long remainingSeconds;

    protected AccountLockedException(String identifier, long remainingSeconds) {
        super(UserResultCode.USER_LOCKED, identifier);
        this.identifier = identifier;
        this.remainingSeconds = remainingSeconds;
    }

    public static AccountLockedException of(String identifier, long remainingSeconds) {
        return new AccountLockedException(identifier, remainingSeconds);
    }
}
