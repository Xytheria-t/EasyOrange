package com.cartethyia.easyorange.user.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(UserType.class)
@SuppressWarnings("unused")
public class UserTypeTypeHandler extends CodeEnumTypeHandler<UserType> {

    public UserTypeTypeHandler() {
        super(UserType::getCode, UserType::fromCode);
    }
}
