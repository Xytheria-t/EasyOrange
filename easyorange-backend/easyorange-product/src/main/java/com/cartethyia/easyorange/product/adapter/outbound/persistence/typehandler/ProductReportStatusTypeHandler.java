package com.cartethyia.easyorange.product.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(ProductReportStatus.class)
@SuppressWarnings("unused")
public class ProductReportStatusTypeHandler extends CodeEnumTypeHandler<ProductReportStatus> {

    public ProductReportStatusTypeHandler() {
        super(ProductReportStatus::getCode, ProductReportStatus::fromCode);
    }
}
