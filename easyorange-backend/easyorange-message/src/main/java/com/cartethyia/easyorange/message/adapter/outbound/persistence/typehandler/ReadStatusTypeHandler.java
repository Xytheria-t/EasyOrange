package com.cartethyia.easyorange.message.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.message.enums.ReadStatus;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(ReadStatus.class)
@SuppressWarnings("unused")
public class ReadStatusTypeHandler extends CodeEnumTypeHandler<ReadStatus> {

    public ReadStatusTypeHandler() {
        super(ReadStatus::getCode, ReadStatus::fromCode);
    }
}
