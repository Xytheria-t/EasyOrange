package com.cartethyia.easyorange.product.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(ConditionLevel.class)
@SuppressWarnings("unused")
public class ConditionLevelTypeHandler extends CodeEnumTypeHandler<ConditionLevel> {

    public ConditionLevelTypeHandler() {
        super(ConditionLevel::getCode, ConditionLevel::fromCode);
    }
}
