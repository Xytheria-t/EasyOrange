package com.cartethyia.easyorange.user.domain.port;

import java.time.Duration;

public interface LoginAttemptPort {

    long incrementAndGet(String identifier, Duration expireAfter);

    /** 当前窗口内已累计的失败次数，无记录时为 0。是否锁定由调用方按阈值判定。 */
    long getAttempts(String identifier);

    void clear(String identifier);
}
