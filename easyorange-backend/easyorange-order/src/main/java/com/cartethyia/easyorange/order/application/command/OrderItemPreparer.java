package com.cartethyia.easyorange.order.application.command;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItemSnapshot;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 订单项准备组件 — 负责资产数据准备、校验、构建订单项。
 * <p>
 * 作为 {@code OrderCommandHandler} 创建流水线的支持组件，非独立服务。
 */
@Component
@RequiredArgsConstructor
public class OrderItemPreparer {

    private final ProductInventoryPort productInventoryPort;
    private final IdGenerator idGenerator;

    /**
     * 准备订单项数据
     *
     * @param items 订单项请求列表
     * @return 准备结果
     * @throws OrderDomainException 如果资产不存在、已下架或库存不足
     */
    public PreparationResult prepareOrderItems(List<CreateOrderCommand.CreateOrderItem> items) {
        // 一次批量读齐资产快照（实时状态 + 展示信息）并校验，构建只消费校验后的快照
        ValidatedSnapshots validated = loadAndValidateSnapshots(items);

        return new PreparationResult(validated.sellerId(), buildOrderItems(items, validated.snapshots()));
    }

    /**
     * 批量加载资产快照，校验（存在、在线、库存、同一资产方）并返回资产方 ID 与校验后的快照。
     * 买家不可认领自己的资产由 {@code Order.createOrder} 的领域不变量统一把关。
     */
    private ValidatedSnapshots loadAndValidateSnapshots(List<CreateOrderCommand.CreateOrderItem> items) {
        // 按 id 去重后一次批量读齐（去重避免同一资产出现两次时 toMap 重复键冲突）
        List<String> productIds = items.stream()
                .map(CreateOrderCommand.CreateOrderItem::productId)
                .distinct()
                .toList();
        Map<String, ProductInventoryPort.ProductSnapshot> snapshotMap =
                productInventoryPort.getSnapshots(productIds).stream()
                        .collect(
                                Collectors.toMap(ProductInventoryPort.ProductSnapshot::productId, Function.identity()));

        String sellerId = null;
        for (CreateOrderCommand.CreateOrderItem item : items) {
            ProductInventoryPort.ProductSnapshot snapshot = snapshotMap.get(item.productId());
            if (snapshot == null) {
                throw OrderDomainException.of("资产不存在: " + item.productId());
            }
            BizRequire.requireTrue(snapshot.isOnline(), "资产已下架: " + item.productId());
            BizRequire.requireTrue(snapshot.hasStock(), "资产库存不足: " + item.productId());

            if (sellerId == null) {
                sellerId = snapshot.sellerId();
            } else {
                BizRequire.requireTrue(Objects.equals(snapshot.sellerId(), sellerId), "订单中的资产必须来自同一资产方");
            }
        }
        return new ValidatedSnapshots(UserId.of(sellerId), snapshotMap);
    }

    /**
     * 构建订单项
     */
    private List<OrderItem> buildOrderItems(
            List<CreateOrderCommand.CreateOrderItem> items,
            Map<String, ProductInventoryPort.ProductSnapshot> snapshotMap) {
        return items.stream()
                .map(item -> buildOrderItem(item, snapshotMap.get(item.productId())))
                .toList();
    }

    /**
     * 构建单个订单项 — 下单价格以库存快照为准（锁内读到的那次），并把展示信息固化为订单留痕。
     */
    private OrderItem buildOrderItem(
            CreateOrderCommand.CreateOrderItem item, ProductInventoryPort.ProductSnapshot snapshot) {
        Money unitPrice = Money.of(snapshot.price());

        return OrderItem.builder()
                .id(idGenerator.generateId())
                .productId(ProductId.of(snapshot.productId()))
                .snapshot(toOrderItemSnapshot(snapshot, unitPrice))
                .unitPrice(unitPrice)
                .quantity(item.quantity())
                .subtotal(unitPrice.multiply(item.quantity()))
                .build();
    }

    /**
     * 构建商品快照（订单留痕：下单时的价格与展示信息，之后资产改名改价也不影响已下的订单）
     */
    private static OrderItemSnapshot toOrderItemSnapshot(ProductInventoryPort.ProductSnapshot source, Money price) {
        return OrderItemSnapshot.builder()
                .productId(source.productId())
                .name(nullToEmpty(source.title()))
                .image(nullToEmpty(source.image()))
                .description(nullToEmpty(source.description()))
                .price(price)
                .conditionLevel(nullToEmpty(source.conditionLevel()))
                .build();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 校验后的快照集（内部传递用：已确认每个商品 id 都存在于快照 map 中）
     */
    private record ValidatedSnapshots(UserId sellerId, Map<String, ProductInventoryPort.ProductSnapshot> snapshots) {}

    /**
     * 准备结果
     */
    public record PreparationResult(UserId sellerId, List<OrderItem> orderItems) {}
}
