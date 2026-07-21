package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;

/**
 * Product 聚合根工厂参数对象 — 更新场景。
 * <p>
 * 收敛 10 个长参数为单一 record，null 字段表示不更新该属性。
 * 纯 VO 字段，domain 层零框架依赖。
 */
public record ProductUpdateSpec(
        CategoryId categoryId,
        ProductTitle title,
        Money price,
        Money originalPrice,
        StockQuantity stock,
        ConditionLevel conditionLevel,
        TradeLocation location,
        ContactMethod contactMethod,
        ProductDescription description,
        ImageSet images
) {}
