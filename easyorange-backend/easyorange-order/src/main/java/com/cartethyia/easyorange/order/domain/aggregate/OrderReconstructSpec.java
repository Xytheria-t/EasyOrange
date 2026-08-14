package com.cartethyia.easyorange.order.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order 聚合根重建参数对象 — 从持久层重建场景。
 * <p>
 * 收敛 from/fromRaw 的 13 个长参数为单一 record，统一两个重建入口。
 * 状态字段使用领域枚举类型（{@link OrderStatus}/{@link PaymentStatus}），
 * 由 {@code @EnumValue} 注解完成与 DB VARCHAR 列的互转，消除 String.valueOf/Integer.valueOf 转换代码。
 *
 * @param id             订单 ID
 * @param orderNo        订单号
 * @param buyerId        认领方 ID
 * @param sellerId       资产方 ID
 * @param items          订单资产列表（列表查询时可为空）
 * @param totalAmount    总金额
 * @param status         订单状态
 * @param paymentStatus  支付状态
 * @param address        收货地址
 * @param phone          联系电话
 * @param remark         备注
 * @param cancelReason   取消原因
 * @param cancelTime     取消时间
 * @param refundReason   退款原因
 * @param refundTime     退款时间
 */
public record OrderReconstructSpec(
        OrderId id,
        OrderNo orderNo,
        UserId buyerId,
        UserId sellerId,
        List<OrderItem> items,
        Money totalAmount,
        OrderStatus status,
        PaymentStatus paymentStatus,
        Address address,
        Phone phone,
        String remark,
        String cancelReason,
        LocalDateTime cancelTime,
        String refundReason,
        LocalDateTime refundTime) {}
