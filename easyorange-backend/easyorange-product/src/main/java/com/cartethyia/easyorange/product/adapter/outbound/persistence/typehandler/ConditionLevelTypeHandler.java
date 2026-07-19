package com.cartethyia.easyorange.product.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.IntegerCodeEnumTypeHandler;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(ConditionLevel.class)
@SuppressWarnings("unused")
public class ConditionLevelTypeHandler extends IntegerCodeEnumTypeHandler<ConditionLevel> {

    public ConditionLevelTypeHandler() {
        super(ConditionLevel::getCode, ConditionLevel::fromCode);
    }
}
