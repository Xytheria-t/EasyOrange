package com.cartethyia.easyorange.product.domain.enums;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * product 模块错误码 — 资产（B2001~）与分类（B2020~）共用本枚举，范围 B2001-B2999，HTTP 状态映射见
 * {@link IResultCode#resolveStatus(String)}。
 * <p>
 * <b>一个模块一个码枚举</b>：分类是资产域的概念（商品挂在分类上），不另开 {@code CategoryResultCode}，
 * 模块内多个领域概念靠码值段区分，异常侧同样只留一个根类 {@code ProductDomainException}（见 ArchUnit 规则 11）。
 * <p>
 * 码值空洞（B2002 / B2004 / B2006~B2008 / B2010 / B2011 / B2013~B2018）是刻意的：状态拒绝统一走
 * {@link #PRODUCT_STATUS_INVALID}（携带当前状态辅助定位状态机误用），其余属已下线的商品评价与举报功能，
 * 从未被外部契约引用故已删除——已删除的码值不再复用。
 */
@Getter
@AllArgsConstructor
public enum ProductResultCode implements IResultCode {
    PRODUCT_NOT_FOUND("B2001", "资产不存在"),
    PRODUCT_OUT_OF_STOCK("B2003", "资产库存不足"),
    PRODUCT_NOT_OWNER("B2005", "非资产所有者"),
    PRODUCT_STATUS_INVALID("B2009", "资产状态不合法"),
    INVALID_CONDITION_LEVEL("B2012", "成色等级不合法"),
    PRODUCT_ERROR("B2019", "资产业务异常"),

    // ── 分类（B2020 起） ──
    // 分类原先由 admin 模块用裸中文串抛 BusinessException，既无错误码也无法聚合观测；
    // 归入本枚举后前端与 Langfuse 观测都能按码聚合。
    CATEGORY_NOT_FOUND("B2020", "分类不存在"),
    CATEGORY_PARENT_NOT_FOUND("B2021", "父分类不存在"),
    CATEGORY_NAME_DUPLICATED("B2022", "同级下已存在同名分类"),
    CATEGORY_LEVEL_EXCEEDED("B2023", "分类层级超过上限"),
    CATEGORY_CYCLE_DETECTED("B2024", "不能把分类挂到它自己的子分类下"),
    CATEGORY_HAS_CHILDREN("B2025", "该分类下存在子分类，无法删除"),
    CATEGORY_HAS_PRODUCTS("B2026", "该分类下存在关联商品，无法删除");

    private final String code;
    private final String message;
}
