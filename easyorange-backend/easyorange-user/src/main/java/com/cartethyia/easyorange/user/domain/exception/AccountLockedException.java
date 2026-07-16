package com.cartethyia.easyorange.user.domain.exception;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;

/**
 * 账户登录锁定异常 — 由领域层抛出，不含 UI 文案。
 * <p>
 * 调用方（application 层）可按需格式化展示信息。
 */
public class AccountLockedException extends BaseBusinessException {

    private final long remainingSeconds;

    protected AccountLockedException(String identifier, long remainingSeconds) {
        super("登录锁定: " + identifier);
        this.remainingSeconds = remainingSeconds;
    }

    public static AccountLockedException of(String identifier, long remainingSeconds) {
        return new AccountLockedException(identifier, remainingSeconds);
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}
