package com.cartethyia.easyorange.order.domain.port;

import java.util.List;
import java.util.Optional;

public interface UserInfoPort {

    Optional<UserInfo> getUserInfo(Long userId);

    record UserInfo(
            Long id,
            String username,
            String email
    ) {}
}