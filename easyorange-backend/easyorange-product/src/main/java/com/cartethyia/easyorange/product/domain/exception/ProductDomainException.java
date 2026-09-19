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

    // ==================== 评价 ====================

    /** 评价不存在（B2010）。 */
    public static ProductDomainException ratingNotFound(String reviewId) {
        return new ProductDomainException(ProductResultCode.RATING_NOT_FOUND, "评价不存在 (reviewId=" + reviewId + ")");
    }

    /** 非评价作者（B2011）。 */
    public static ProductDomainException ratingNotOwner(String reviewId) {
        return new ProductDomainException(ProductResultCode.RATING_NOT_OWNER, "只能删除自己的评价 (reviewId=" + reviewId + ")");
    }
}
