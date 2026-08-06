package com.cartethyia.easyorange.user.domain.port;

import java.time.Duration;

public interface LoginAttemptPort {

    long incrementAndGet(String identifier, Duration expireAfter);

    void clear(String identifier);

    long getRemainingLockSeconds(String identifier);
}
