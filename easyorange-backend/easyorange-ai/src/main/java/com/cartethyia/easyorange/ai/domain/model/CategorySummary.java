package com.cartethyia.easyorange.ai.domain.model;

/** 公开类目摘要 — 类目浏览工具的返回单元，只含公开展示字段（不含排序、状态、时间戳等平台内部字段）；level 1 为一级，productCount 为该类目在售资产数。 */
public record CategorySummary(String id, String name, int level, int productCount) {}
