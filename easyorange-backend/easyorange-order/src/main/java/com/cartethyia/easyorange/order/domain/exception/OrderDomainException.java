package com.cartethyia.easyorange.order.domain.exception;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;

/**
 * 订单域业务异常 — 唯一领域异常类，构造统一走 {@link #of} 工厂（与 {@code PaymentDomainException} 一致，
 * 遵循《架构-DDD规范》「推荐统一，而非多叶子类」）；有固定语义的错误以命名工厂表达，见 {@link #notFound}。
 */
public class OrderDomainException extends BaseBusinessException {

    protected OrderDomainException(String message) {
        super(message);
    }

    protected OrderDomainException(IResultCode resultCode) {
        super(resultCode);
    }

    protected OrderDomainException(IResultCode resultCode, String message) {
        super(resultCode, message);
    }

    protected OrderDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    protected OrderDomainException(IResultCode resultCode, String message, Throwable cause) {
        super(resultCode, message, cause);
    }

    @Override
    protected String defaultCode() {
        return OrderResultCode.ORDER_ERROR.getCode();
    }

    public static OrderDomainException of(String message) {
        return new OrderDomainException(message);
    }

    public static OrderDomainException of(IResultCode resultCode) {
        return new OrderDomainException(resultCode);
    }

    public static OrderDomainException of(IResultCode resultCode, String message) {
        return new OrderDomainException(resultCode, message);
    }

    public static OrderDomainException of(String message, Throwable cause) {
        return new OrderDomainException(message, cause);
    }

    /** 订单不存在（B3001）— 消息带订单 ID，命令侧与查询侧共用这一处定义。 */
    public static OrderDomainException notFound(String orderId) {
        return new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND, "订单不存在: id=" + orderId);
    }

    /** 上游（支付网关等）不可用（D0502 → 502）— 与业务失败区分：调用方重试可能成功。 */
    public static OrderDomainException upstream(String message, Throwable cause) {
        return new OrderDomainException(ResultCode.UPSTREAM_ERROR, message, cause);
    }
}
