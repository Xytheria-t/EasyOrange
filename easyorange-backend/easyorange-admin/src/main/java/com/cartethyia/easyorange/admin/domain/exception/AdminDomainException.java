package com.cartethyia.easyorange.admin.domain.exception;

import com.cartethyia.easyorange.admin.domain.enums.AdminResultCode;
import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import lombok.Getter;

/**
 * 管理域业务异常 — 模块唯一领域异常类（构造走 {@link #of} 与具名工厂，不新增叶子类）。
 * <p>
 * admin 侧的「不存在」统一在这里成型：带 id 便于定位，错误码可让前端按资源类型分支。
 */
@Getter
public class AdminDomainException extends BaseBusinessException {

    protected AdminDomainException(String message) {
        super(message);
    }

    protected AdminDomainException(IResultCode resultCode) {
        super(resultCode);
    }

    protected AdminDomainException(IResultCode resultCode, String message) {
        super(resultCode, message);
    }

    @Override
    protected String defaultCode() {
        return AdminResultCode.USER_NOT_FOUND.getCode();
    }

    public static AdminDomainException of(String message) {
        return new AdminDomainException(message);
    }

    /** 用户不存在 — 带 id 便于定位。 */
    public static AdminDomainException userNotFound(String userId) {
        return new AdminDomainException(AdminResultCode.USER_NOT_FOUND, "用户不存在: id=" + userId);
    }

    /** 订单不存在 — 带 id 便于定位。 */
    public static AdminDomainException orderNotFound(String orderId) {
        return new AdminDomainException(AdminResultCode.ORDER_NOT_FOUND, "订单不存在: id=" + orderId);
    }

    /** 商品不存在 — 带 id 便于定位。 */
    public static AdminDomainException productNotFound(String productId) {
        return new AdminDomainException(AdminResultCode.PRODUCT_NOT_FOUND, "商品不存在: id=" + productId);
    }

    /** 分类不存在 — 带 id 便于定位。 */
    public static AdminDomainException categoryNotFound(String categoryId) {
        return new AdminDomainException(AdminResultCode.CATEGORY_NOT_FOUND, "分类不存在: id=" + categoryId);
    }

    /** 审核记录不存在 — 带 id 便于定位。 */
    public static AdminDomainException auditLogNotFound(String logId) {
        return new AdminDomainException(AdminResultCode.AUDIT_LOG_NOT_FOUND, "审核记录不存在: id=" + logId);
    }
}
