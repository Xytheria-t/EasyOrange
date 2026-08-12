package com.cartethyia.easyorange.message.domain.port;

import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import java.util.Collection;
import java.util.Map;

public interface UserInfoPort {

    Map<String, UserInfo> getUserInfoMap(Collection<String> userIds);
}
