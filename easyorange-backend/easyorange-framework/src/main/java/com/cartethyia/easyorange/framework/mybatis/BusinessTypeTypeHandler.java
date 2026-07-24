package com.cartethyia.easyorange.framework.mybatis;

import com.cartethyia.easyorange.common.enums.BusinessType;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(BusinessType.class)
@SuppressWarnings("unused")
public class BusinessTypeTypeHandler extends CodeEnumTypeHandler<BusinessType> {

    public BusinessTypeTypeHandler() {
        super(BusinessType::getCode, BusinessType::fromCode);
    }
}
