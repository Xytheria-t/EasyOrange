package com.cartethyia.easyorange.admin.domain.model;

import java.util.List;

/**
 * 商品审核命令 — 审核动作（1 通过 / 2 拒绝）+ 原因 + 备注 + 命中的问题维度。
 * <p>
 * 单条审核与批量审核的每一条是同一个动作，抽出命令后端口那七个参数只有一处映射。
 */
public record ProductAuditCommand(Integer action, String reason, String remark, List<String> dimensions) {}
