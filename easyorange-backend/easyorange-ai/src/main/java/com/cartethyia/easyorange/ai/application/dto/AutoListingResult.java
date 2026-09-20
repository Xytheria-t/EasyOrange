package com.cartethyia.easyorange.ai.application.dto;

import java.math.BigDecimal;

/**
 * 拍照识别的产出 — 与上架表单直接对接的六个字段。
 * <p>
 * 只有这六个：每一个都流向上架表单或落库（标题/描述/建议价/类目/成色/所在地）。
 * 早先还带 {@code categoryId}（模型猜的 ID，前端本来就按名称反查真实 ID）、
 * {@code tags}（商品标签由规则版 ProductTagger 出，AI 不重复造）、
 * {@code imageDescriptions} 三个字段，全链路无人消费，已删 —— 少一个字段就少一份输出 token
 * 和一处「这字段干什么用的」的追问。
 */
public record AutoListingResult(
        String title,
        String description,
        BigDecimal price,
        String categoryName,
        String conditionLevel,
        String location) {}
