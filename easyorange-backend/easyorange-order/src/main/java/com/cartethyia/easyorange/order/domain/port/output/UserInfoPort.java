package com.cartethyia.easyorange.order.domain.port.output;

import java.util.List;
import java.util.Optional;

public interface UserInfoPort extends OutboundPort {

    Optional<UserInfo> getUserInfo(Long userId);

    record UserInfo(
            Long id,
            String username,
            String email
    ) {}
}
