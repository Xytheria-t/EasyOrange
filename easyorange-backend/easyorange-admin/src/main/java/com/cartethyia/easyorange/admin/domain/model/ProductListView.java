package com.cartethyia.easyorange.admin.domain.model;

import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.PartyProfile;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductQueryResult;
import java.util.List;
import java.util.Map;

/**
 * 商品列表读模型 — 一页商品 + 按商品 id 索引的图片与卖家/分类档案。
 * <p>
 * <b>取舍</b>：图片、卖家昵称、分类名都不在商品记录里（各自独立存储），列表三样都要显示，
 * 就得整页按 id 各批量取一次摊进结果返回，web 层不用自己收集 id，也不会退化成逐条 N+1。
 */
public record ProductListView(
        ProductQueryResult page, Map<String, List<String>> images, Map<String, PartyProfile> parties) {

    /** 卖家或分类查不到时给空档案，前端各留兜底，不让整列消失 */
    public PartyProfile profileOf(String productId) {
        return parties.getOrDefault(productId, PartyProfile.empty());
    }
}
