package com.cartethyia.easyorange.admin.domain.model;

import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.PartyProfile;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductDetail;
import java.util.List;

/**
 * 商品详情读模型 — 商品本体 + 图片列表 + 卖家/分类档案。
 * <p>
 * 三者都与商品本体分属不同存储，详情要一次给全，故在服务层合并；主图取哪一张属展示口径，
 * 留给 assembler。
 */
public record ProductDetailView(ProductDetail product, List<String> images, PartyProfile party) {}
