package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.domain.Money;
import lombok.Builder;

/**
 * 订单项留痕快照 — 下单那一刻读到的价格与展示信息副本，JSON 落 {@code eo_order_item.product_snapshot}，
 * 资产之后改名改价都不影响已下的订单。
 * <p>
 * 与端口读到的实时状态（{@code ProductInventoryPort.ProductSnapshot} / {@code ProductSnapshotPort.ProductSnapshot}）
 * 不是一回事：那两者是「当前」库存与在架状态，本类型是「下单时」的冻结副本。
 */
@Builder
public record OrderItemSnapshot(
        String productId, String name, String image, String description, Money price, String conditionLevel) {}
