package com.cartethyia.easyorange.user.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(UserStatus.class)
public class UserStatusTypeHandler extends CodeEnumTypeHandler<UserStatus> {

    public UserStatusTypeHandler() {
        super(UserStatus::getCode, UserStatus::fromCode);
    }
}
