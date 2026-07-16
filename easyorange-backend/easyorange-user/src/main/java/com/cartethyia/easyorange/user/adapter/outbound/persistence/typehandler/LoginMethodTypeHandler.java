package com.cartethyia.easyorange.user.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.user.domain.enums.LoginMethod;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(LoginMethod.class)
public class LoginMethodTypeHandler extends CodeEnumTypeHandler<LoginMethod> {

    public LoginMethodTypeHandler() {
        super(LoginMethod::getCode, LoginMethod::fromCode);
    }
}
