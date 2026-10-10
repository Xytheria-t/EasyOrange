package com.cartethyia.easyorange.ai.application.dto;

import java.math.BigDecimal;

/**
 * 拍照识别的产出 — 与上架表单直接对接的六个字段（标题/描述/建议价/类目/成色/所在地），每个都流向上架表单
 * 或落库；无消费方的字段不进输出 —— 少一个字段就少一份输出 token 和一处「这字段干什么用的」的追问。
 * <p>
 * 类目给名称不给 ID：模型猜的 ID 不可信，真实 ID 由前端按名称反查。
 */
public record AutoListingResult(
        String title,
        String description,
        BigDecimal price,
        String categoryName,
        String conditionLevel,
        String location) {}
