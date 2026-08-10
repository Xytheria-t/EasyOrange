package com.cartethyia.easyorange.order.application.service;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.ProductOrderPort;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.ProductSnapshot;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单项准备组件 — 负责资产数据准备、校验、构建订单项。
 * <p>
 * 作为 {@code OrderCreationService} 创建流水线的支持组件，非独立服务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPreparation {

    private final ProductOrderPort productOrderPort;
    private final ProductQueryPort productQueryPort;
    private final IdGenerator idGenerator;

    /**
     * 准备订单项数据
     *
     * @param items 订单项请求列表
     * @return 准备结果
     * @throws OrderDomainException 如果资产不存在、已下架或库存不足
     */
    public PreparationResult prepareOrderItems(List<CreateOrderCommand.CreateOrderItem> items) {
        // 批量获取快照并校验，返回已确认存在的快照集与资产方 ID
        ValidatedSnapshots validated = loadAndValidateSnapshots(items);

        // 批量获取资产详情并构建订单项（构建只消费校验后的快照，消除隐式非空契约）
        Map<String, ProductDetail> productDetailMap =
                fetchByIds(items, productQueryPort::getProductsByIds, ProductDetail::id);
        List<OrderItem> orderItems = buildOrderItems(items, validated.snapshots(), productDetailMap);

        return new PreparationResult(validated.sellerId(), orderItems);
    }

    /**
     * 批量加载资产快照，校验（存在、在线、库存、同一资产方）并返回资产方 ID 与校验后的快照。
     * 买家不可认领自己的资产由 {@code Order.createOrder} 的领域不变量统一把关。
     */
    private ValidatedSnapshots loadAndValidateSnapshots(List<CreateOrderCommand.CreateOrderItem> items) {
        Map<String, ProductOrderPort.ProductSnapshot> snapshotMap =
                fetchByIds(items, productOrderPort::getSnapshots, ProductOrderPort.ProductSnapshot::productId);

        String sellerId = null;
        for (CreateOrderCommand.CreateOrderItem item : items) {
            ProductOrderPort.ProductSnapshot snapshot = snapshotMap.get(item.productId());
            if (snapshot == null) {
                throw new OrderDomainException("资产不存在: " + item.productId());
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
     * 批量按 id 去重拉取并组装为 map（去重避免 toMap 重复键冲突）
     */
    private <T> Map<String, T> fetchByIds(
            List<CreateOrderCommand.CreateOrderItem> items,
            Function<List<String>, List<T>> fetcher,
            Function<T, String> idExtractor) {
        List<String> productIds = items.stream()
                .map(CreateOrderCommand.CreateOrderItem::productId)
                .distinct()
                .toList();
        return fetcher.apply(productIds).stream().collect(Collectors.toMap(idExtractor, Function.identity()));
    }

    /**
     * 构建订单项
     */
    private List<OrderItem> buildOrderItems(
            List<CreateOrderCommand.CreateOrderItem> items,
            Map<String, ProductOrderPort.ProductSnapshot> snapshotMap,
            Map<String, ProductDetail> productDetailMap) {
        return items.stream()
                .map(item -> buildOrderItem(item, snapshotMap.get(item.productId()), productDetailMap))
                .toList();
    }

    /**
     * 构建单个订单项
     */
    private OrderItem buildOrderItem(
            CreateOrderCommand.CreateOrderItem item,
            ProductOrderPort.ProductSnapshot snapshot,
            Map<String, ProductDetail> productDetailMap) {
        Money unitPrice = Money.of(snapshot.price());
        Money subtotal = unitPrice.multiply(item.quantity());
        String productId = snapshot.productId();

        return OrderItem.builder()
                .id(idGenerator.generateId())
                .productId(ProductId.of(productId))
                .snapshot(buildProductSnapshot(productId, productDetailMap.get(productId), unitPrice))
                .unitPrice(unitPrice)
                .quantity(item.quantity())
                .subtotal(subtotal)
                .build();
    }

    /**
     * 构建商品快照（含从商品详情回填的标题/图片/描述/成色）
     */
    private static ProductSnapshot buildProductSnapshot(String productId, ProductDetail detail, Money price) {
        if (detail == null) {
            log.warn("商品详情缺失，快照字段将使用空值回退: productId={}", productId);
            return ProductSnapshot.builder().productId(productId).price(price).build();
        }
        return ProductSnapshot.builder()
                .productId(productId)
                .name(nullToEmpty(detail.title()))
                .image(firstImage(detail.images()))
                .description(nullToEmpty(detail.description()))
                .price(price)
                .conditionLevel(nullToEmpty(detail.conditionLevel()))
                .build();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String firstImage(List<String> images) {
        return images == null || images.isEmpty() ? "" : images.getFirst();
    }

    /**
     * 校验后的快照集（内部传递用：已确认每个商品 id 都存在于快照 map 中）
     */
    private record ValidatedSnapshots(UserId sellerId, Map<String, ProductOrderPort.ProductSnapshot> snapshots) {}

    /**
     * 准备结果
     */
    public record PreparationResult(UserId sellerId, List<OrderItem> orderItems) {}
}
