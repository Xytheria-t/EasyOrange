package com.cartethyia.easyorange.ai.domain.model;

/**
 * 公开类目摘要 — 类目浏览工具的返回单元：仅公开展示字段，
 * 不含排序、状态、时间戳等平台内部管理字段。
 *
 * @param id           类目 ID
 * @param name         类目名
 * @param level        类目层级（1 = 一级）
 * @param productCount 该类目下的在售资产数
 */
public record CategorySummary(String id, String name, int level, int productCount) {}
