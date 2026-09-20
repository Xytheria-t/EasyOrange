package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.AiListingAdoptionReport;

/**
 * AI 建议采纳率读取端口。
 * <p>
 * 建议快照落在商品表上（拍照识别发生在资产创建之前，调用日志的 {@code subject_id} 那时还没有值），
 * 而商品表属 product 模块 —— 所以这里只声明读需求，实现放在 application 模块的 outbound 适配器里，
 * 由它去读商品侧的表，ai 模块不直接碰别人的表。
 */
public interface AiListingAdoptionPort {

    AiListingAdoptionReport report();
}
