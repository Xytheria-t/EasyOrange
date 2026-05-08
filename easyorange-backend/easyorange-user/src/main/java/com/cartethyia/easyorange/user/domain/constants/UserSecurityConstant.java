package com.cartethyia.easyorange.user.domain.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserSecurityConstant {

    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final int LOGIN_LOCK_MINUTES = 30;
    public static final long ATTEMPTS_EXPIRE_TIME = 30L;
}
