package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;

/**
 * Product 聚合根工厂参数对象 — 创建场景。
 * <p>
 * 收敛长参数为单一 record，提升调用点可读性并避免参数顺序错配。
 * 纯 VO 字段，domain 层零框架依赖。
 *
 * @param aiSuggestedPrice AI 建议售价（拍照识别给出）。只写不改，不参与定价与状态流转 ——
 *                         它的唯一用途是让「AI 建议 vs 资产方最终价」这条质量数字可被查询
 */
public record ProductCreateSpec(
        SellerId sellerId,
        CategoryId categoryId,
        ProductTitle title,
        Money price,
        Money originalPrice,
        StockQuantity stock,
        ConditionLevel conditionLevel,
        TradeLocation location,
        ContactMethod contactMethod,
        ProductDescription description,
        ImageSet images,
        Money aiSuggestedPrice) {}
