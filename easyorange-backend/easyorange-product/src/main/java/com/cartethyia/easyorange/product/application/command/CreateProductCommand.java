package com.cartethyia.easyorange.product.application.command;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductCommand(
        String categoryId,
        String name,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stock,
        String conditionLevel,
        String location,
        String contactMethod,
        String description,
        List<String> imageUrls,
        /** AI 建议售价（拍照识别给出）。只写不改，不参与定价逻辑 —— 用于统计采纳率与偏离度 */
        BigDecimal aiSuggestedPrice) {}
