package com.cartethyia.easyorange.product.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(AuditAction.class)
@SuppressWarnings("unused")
public class AuditActionTypeHandler extends CodeEnumTypeHandler<AuditAction> {

    public AuditActionTypeHandler() {
        super(AuditAction::getCode, AuditAction::fromCode);
    }
}
