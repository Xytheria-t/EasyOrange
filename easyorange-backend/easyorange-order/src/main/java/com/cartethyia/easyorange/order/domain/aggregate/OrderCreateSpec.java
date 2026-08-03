package com.cartethyia.easyorange.order.domain.aggregate;

import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;

import java.util.List;

/**
 * Order 聚合根工厂参数对象 — 创建场景。
 * <p>
 * 收敛 createOrder 的 7 个长参数为单一 record，提升调用点可读性并避免参数顺序错配。
 * 纯 VO 字段，domain 层零框架依赖。
 *
 * @param orderId  订单 ID（由应用层通过 IdGenerator 生成并包装为 {@link OrderId}）
 * @param buyerId  认领方 ID
 * @param sellerId 资产方 ID
 * @param items    订单资产列表
 * @param address  收货地址
 * @param phone    联系电话
 * @param remark   备注
 */
public record OrderCreateSpec(
        OrderId orderId,
        UserId buyerId,
        UserId sellerId,
        List<OrderItem> items,
        Address address,
        Phone phone,
        String remark
) {}
