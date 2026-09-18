package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.List;

/**
 * 搜索增强工具的上下文 — 全部工具共享的输入。
 * <p>
 * 只有 {@code keyword} 与 {@code topProducts}：市场分析一路原先还需要一份由编排器拼好的价格文本，
 * 现在它直接从 {@code topProducts} 自己算，那份预拼字符串（及其「不可信内容包标签块」的处理）一并去掉。
 */
public record SearchToolContext(String keyword, List<ProductReadModel> topProducts) {}
