package com.cartethyia.easyorange.message.domain.port.output;

import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;

import java.util.Collection;
import java.util.Map;

public interface UserInfoPort extends OutboundPort {

    UserInfo getUserInfo(Long userId);

    Map<Long, UserInfo> getUserInfoMap(Collection<Long> userIds);
}
