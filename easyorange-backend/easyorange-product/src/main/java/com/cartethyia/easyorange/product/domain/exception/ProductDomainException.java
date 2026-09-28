package com.cartethyia.easyorange.product.domain.exception;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;

/**
 * 资产域业务异常 — 模块唯一领域异常类，构造走 {@link #of} 与具名工厂（不新增叶子类）。
 * <p>
 * 资产与分类共用这一个根类：分类是资产域的概念（商品挂在分类上），若为它单开
 * {@code CategoryDomainException} 就成了「第二套异常层级」，catch 本类会漏掉它
 * （ArchUnit 规则 11 就是拦这个）。语义靠 {@code ProductResultCode} 按码值段区分。
 * <p>
 * 判据见《架构参考》异常细则（doc/agents/架构参考.md）：调用方需要按类型分支（降级/重试/格式化展示）才值得独立异常类，
 * 只负责把错误码与文案送到客户端的语义一律用本类的具名工厂，错误码统一进 {@link ProductResultCode}。
 */
public class ProductDomainException extends BaseBusinessException {

    protected ProductDomainException(String message) {
        super(message);
    }

    protected ProductDomainException(IResultCode resultCode) {
        super(resultCode);
    }

    protected ProductDomainException(IResultCode resultCode, String message) {
        super(resultCode, message);
    }

    protected ProductDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    protected String defaultCode() {
        return ProductResultCode.PRODUCT_ERROR.getCode();
    }

    public static ProductDomainException of(String message) {
        return new ProductDomainException(message);
    }

    public static ProductDomainException of(IResultCode resultCode) {
        return new ProductDomainException(resultCode);
    }

    public static ProductDomainException of(IResultCode resultCode, String message) {
        return new ProductDomainException(resultCode, message);
    }

    // ==================== 资产 ====================

    /** 资产不存在（B2001）— 消息带 id 便于定位。 */
    public static ProductDomainException notFound(ProductId id) {
        return new ProductDomainException(
                ProductResultCode.PRODUCT_NOT_FOUND, "资产不存在: id=" + (id != null ? id.value() : "null"));
    }

    /** 资产不存在（B2001）— id 未经值对象包装时的重载。 */
    public static ProductDomainException notFound(String id) {
        return new ProductDomainException(ProductResultCode.PRODUCT_NOT_FOUND, "资产不存在: id=" + id);
    }

    /** 非资产所有者（B2005）— 文案由调用场景补充，附 productId 便于定位。 */
    public static ProductDomainException notOwner(ProductId productId, String message) {
        return new ProductDomainException(
                ProductResultCode.PRODUCT_NOT_OWNER,
                message + " (productId=" + (productId != null ? productId.value() : "null") + ")");
    }

    /** 资产状态不合法（B2009）— 携带当前状态辅助定位状态机误用。 */
    public static ProductDomainException invalidStatus(
            String message, ProductId productId, ProductStatus currentStatus) {
        return new ProductDomainException(
                ProductResultCode.PRODUCT_STATUS_INVALID,
                message + " (productId=" + (productId != null ? productId.value() : "null") + ", currentStatus="
                        + (currentStatus != null ? currentStatus : "null") + ")");
    }

    /** 资产状态不合法（B2009）— 无 productId 时的重载。 */
    public static ProductDomainException invalidStatus(String message, ProductStatus currentStatus) {
        return new ProductDomainException(
                ProductResultCode.PRODUCT_STATUS_INVALID,
                message + " (currentStatus=" + (currentStatus != null ? currentStatus : "null") + ")");
    }

    /** 资产库存不足（B2003）— 携带当前库存辅助判断超卖边界。 */
    public static ProductDomainException insufficientStock(
            String message, ProductId productId, StockQuantity currentStock) {
        return new ProductDomainException(
                ProductResultCode.PRODUCT_OUT_OF_STOCK,
                message + " (productId=" + (productId != null ? productId.value() : "null") + ", stock="
                        + (currentStock != null ? currentStock.value() : "null") + ")");
    }

    // ==================== 分类 ====================

    /** 分类不存在（B2020）— 消息带 id 便于定位。 */
    public static ProductDomainException categoryNotFound(String categoryId) {
        return new ProductDomainException(
                ProductResultCode.CATEGORY_NOT_FOUND,
                "分类不存在: id=" + (categoryId != null ? categoryId : "null"));
    }

    /** 父分类不存在（B2021）。 */
    public static ProductDomainException categoryParentNotFound(String parentId) {
        return new ProductDomainException(
                ProductResultCode.CATEGORY_PARENT_NOT_FOUND,
                "父分类不存在: parentId=" + (parentId != null ? parentId : "null"));
    }

    /** 同级重名（B2022）— 附父分类区分是哪一层的重名。 */
    public static ProductDomainException categoryDuplicatedName(String name, String parentId) {
        return new ProductDomainException(
                ProductResultCode.CATEGORY_NAME_DUPLICATED,
                "同级下已存在同名分类: name=" + name + ", parentId=" + (parentId != null ? parentId : "<root>"));
    }

    /** 层级越界（B2023）— 消息带实际与上限，便于运营定位。 */
    public static ProductDomainException categoryLevelExceeded(int level, int maxLevel) {
        return new ProductDomainException(
                ProductResultCode.CATEGORY_LEVEL_EXCEEDED, "分类层级不能超过" + maxLevel + "级（实际 " + level + " 级）");
    }

    /** 成环（B2024）— 附目标父分类 id。 */
    public static ProductDomainException categoryCycleDetected(String categoryId, String targetParentId) {
        return new ProductDomainException(
                ProductResultCode.CATEGORY_CYCLE_DETECTED,
                "不能把分类挂到它自己的子分类下: id=" + categoryId + ", 目标父分类=" + targetParentId);
    }

    /** 存在子分类（B2025）。 */
    public static ProductDomainException categoryHasChildren(String categoryId) {
        return new ProductDomainException(
                ProductResultCode.CATEGORY_HAS_CHILDREN, "该分类下存在子分类，无法删除: id=" + categoryId);
    }

    /** 存在关联商品（B2026）— 附商品数便于运营判断要先下架还是先迁走。 */
    public static ProductDomainException categoryHasProducts(String categoryId, long productCount) {
        return new ProductDomainException(
                ProductResultCode.CATEGORY_HAS_PRODUCTS,
                "该分类下存在关联商品，无法删除: id=" + categoryId + ", 商品数=" + productCount);
    }
}
