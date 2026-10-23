package com.cartethyia.easyorange.admin.domain.model;

import java.time.LocalDateTime;

/**
 * 仪表板「最近动态」的最终展示项 — 把三路原始行（{@link RecentActivityRow}）合并并生成中文文案后的形态。
 * <p>
 * 文本在服务层成型（那边才知道这条动态在讲什么），时间保持类型化：格式化是 web 层的事。
 *
 * @param type      资源类型：user / product / order
 * @param text      展示文案
 * @param createdAt 发生时间
 */
public record DashboardActivity(String type, String text, LocalDateTime createdAt) {}
