package com.cartethyia.easyorange.product.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.product.domain.enums.ReportReasonType;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(ReportReasonType.class)
@SuppressWarnings("unused")
public class ReportReasonTypeTypeHandler extends CodeEnumTypeHandler<ReportReasonType> {

    public ReportReasonTypeTypeHandler() {
        super(ReportReasonType::getCode, ReportReasonType::fromCode);
    }
}
