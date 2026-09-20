package com.cartethyia.easyorange.ai.domain.port;

import java.util.List;

/**
 * 平台类目清单读取端口 — 拍照识别的类目约束来源。
 * <p>
 * 类目表属 product 模块，ai 模块不直接碰别人的表，所以这里只声明读需求，
 * 实现放在 application 模块的 outbound 适配器里（与 {@code AiPricingAdoptionPort} 同规矩）。
 * <p>
 * 为什么要给模型一份清单：prompt 里不写清单时，模型会自由发挥出一个「看起来对」的类目名，
 * 与平台类目对不上就整条丢掉 —— 类目填充率因此平白折半。把平台类目体系作为硬约束注入，
 * 让它从枚举里挑，比事后在前端做模糊匹配靠谱。
 */
public interface CategoryCatalogPort {

    /** 启用中的类目名称清单（按平台排序）。 */
    List<String> listAvailableCategoryNames();
}
