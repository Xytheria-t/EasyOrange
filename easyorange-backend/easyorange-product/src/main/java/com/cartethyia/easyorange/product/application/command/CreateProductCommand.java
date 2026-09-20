package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.product.domain.valueobject.AiSuggestion;
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
        /** AI 建议快照（拍照识别给出）。只写不改，不参与定价逻辑 —— 用于统计字段级采纳率 */
        AiSuggestion aiSuggestion) {}
