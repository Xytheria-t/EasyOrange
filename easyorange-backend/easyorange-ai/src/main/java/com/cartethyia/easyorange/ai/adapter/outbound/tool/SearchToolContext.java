package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.List;

/** 搜索增强工具的上下文 — 全部工具共享的输入。 */
public record SearchToolContext(String keyword, List<ProductReadModel> topProducts, String marketContext) {}
