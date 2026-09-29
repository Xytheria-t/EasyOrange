package com.cartethyia.easyorange.ai.domain.port;

import java.util.List;

/**
 * 平台类目清单读取端口 — 拍照识别的类目约束来源；类目表属 product 模块，这里只声明读需求，实现放 application
 * 的 outbound 适配器（与 {@code AiListingAdoptionPort} 同规矩）。
 * <p>
 * 清单必须进 prompt：不写时模型会编一个「看起来对」的类目名，与平台类目对不上整条被丢，类别填充率折半。
 */
public interface CategoryCatalogPort {

    /**
     * 启用中的一级类目名称清单（按平台排序）— 必须与发布页下拉同一口径（一级、6 项），返回叶子名的话
     * 前端 {@code categories.find(name)} 匹配不到 ID，类别回填必空。
     */
    List<String> listAvailableCategoryNames();
}
