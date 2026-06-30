package com.cartethyia.easyorange.order.domain.port;

import java.util.List;
import java.util.Optional;

public interface UserInfoPort {

    Optional<UserInfo> getUserInfo(String userId);

    record UserInfo(
            String id,
            String username,
            String email
    ) {}
}