package com.cartethyia.easyorange.message.domain.port;

import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface UserInfoPort {

    Optional<UserInfo> getUserInfo(String userId);

    Map<String, UserInfo> getUserInfoMap(Collection<String> userIds);
}
