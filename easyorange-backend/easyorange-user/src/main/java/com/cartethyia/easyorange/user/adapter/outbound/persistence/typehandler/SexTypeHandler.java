package com.cartethyia.easyorange.user.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(Sex.class)
@SuppressWarnings("unused")
public class SexTypeHandler extends CodeEnumTypeHandler<Sex> {

    public SexTypeHandler() {
        super(Sex::getCode, Sex::fromCode);
    }
}
