package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.ListingAdoptionReport;

/**
 * AI 建议采纳率读取端口 — 建议快照落在商品表上（拍照识别先于资产创建，「一次调用 ↔ 一个商品」的关联
 * 只能在商品创建时回填）；商品表属 product 模块，故这里只声明读需求，实现在 application 的 outbound
 * 适配器（由它去读商品侧的表），ai 不直接碰别人的表。
 */
public interface ListingAdoptionPort {

    ListingAdoptionReport report();
}
