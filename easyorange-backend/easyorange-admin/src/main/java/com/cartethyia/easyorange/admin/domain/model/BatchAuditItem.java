package com.cartethyia.easyorange.admin.domain.model;

import java.util.List;

/**
 * 批量审核的一条 — 目标商品 + 审核动作。
 * <p>
 * 不带 {@code remark}：备注是单条审核的字段，批量入口前端不提供，服务侧固定传 null。
 */
public record BatchAuditItem(String productId, Integer action, String reason, List<String> dimensions) {}
