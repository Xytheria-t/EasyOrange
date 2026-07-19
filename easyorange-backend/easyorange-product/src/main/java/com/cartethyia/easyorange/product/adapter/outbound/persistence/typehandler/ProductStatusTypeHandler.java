package com.cartethyia.easyorange.product.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.IntegerCodeEnumTypeHandler;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(ProductStatus.class)
@SuppressWarnings("unused")
public class ProductStatusTypeHandler extends IntegerCodeEnumTypeHandler<ProductStatus> {

    public ProductStatusTypeHandler() {
        super(ProductStatus::getCode, ProductStatus::fromCode);
    }
}
