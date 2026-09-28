package com.cartethyia.easyorange.admin.domain.model;

import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductQueryResult;
import java.util.List;
import java.util.Map;

/**
 * 商品列表读模型 — 一页商品 + 按商品 id 索引的图片列表。
 * <p>
 * <b>取舍</b>：图片存在独立的表/键，端口的 {@code ProductSummary} 不带它；列表要显示主图就得
 * 整页批量取一次，摊进分页结果返回，web 层就不用自己收集 id。
 */
public record ProductListView(ProductQueryResult page, Map<String, List<String>> images) {}
