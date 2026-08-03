package com.cartethyia.easyorange.order.application.service;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.ProductOrderPort;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.ProductId;
import com.cartethyia.easyorange.order.domain.valueobject.ProductSnapshot;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单准备服务
 * <p>
 * 负责资产数据准备、校验、构建订单项
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPreparationService {

    private final ProductOrderPort productOrderPort;
    private final ProductQueryPort productQueryPort;
    private final IdGenerator idGenerator;

    /**
     * 准备订单项数据
     *
     * @param items   订单项请求列表
     * @param buyerId 认领方 ID
     * @return 准备结果
     * @throws OrderDomainException 如果资产不存在、已下架或库存不足
     */
    public PreparationResult prepareOrderItems(List<CreateOrderCommand.CreateOrderItem> items, String buyerId) {
        BizRequire.notEmpty(items, "订单资产不能为空");

        // 批量获取商品快照并校验
        Map<String, ProductOrderPort.ProductSnapshot> snapshotMap = loadSnapshots(items);
        String sellerId = validateAndGetSellerId(items, snapshotMap, buyerId);

        // 批量获取资产详情并构建订单项
        Map<String, ProductDetail> productDetailMap = loadProductDetails(items);
        List<OrderItem> orderItems = buildOrderItems(items, snapshotMap, productDetailMap);

        return new PreparationResult(UserId.of(sellerId), orderItems);
    }

    /**
     * 批量加载资产快照
     */
    private Map<String, ProductOrderPort.ProductSnapshot> loadSnapshots(List<CreateOrderCommand.CreateOrderItem> items) {
        List<String> productIds = items.stream()
            .map(CreateOrderCommand.CreateOrderItem::productId)
            .distinct()
            .toList();
        return productOrderPort.getSnapshots(productIds).stream()
            .collect(Collectors.toMap(ProductOrderPort.ProductSnapshot::productId, Function.identity()));
    }

    /**
     * 校验资产（存在、在线、库存、非自购、同一资产方）并返回资产方 ID
     */
    private String validateAndGetSellerId(List<CreateOrderCommand.CreateOrderItem> items,
                                          Map<String, ProductOrderPort.ProductSnapshot> snapshotMap, String buyerId) {
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
                BizRequire.requireTrue(!Objects.equals(sellerId, buyerId), "不能认领自己的资产");
            } else {
                BizRequire.requireTrue(Objects.equals(snapshot.sellerId(), sellerId), "订单中的资产必须来自同一资产方");
            }
        }
        return sellerId;
    }

    /**
     * 批量加载资产详情
     */
    private Map<String, ProductDetail> loadProductDetails(List<CreateOrderCommand.CreateOrderItem> items) {
        List<String> productIds = items.stream()
            .map(CreateOrderCommand.CreateOrderItem::productId)
            .distinct()
            .toList();
        return productQueryPort.getProductsByIds(productIds).stream()
            .collect(Collectors.toMap(ProductDetail::id, Function.identity()));
    }

    /**
     * 构建订单项
     */
    private List<OrderItem> buildOrderItems(List<CreateOrderCommand.CreateOrderItem> items,
                                            Map<String, ProductOrderPort.ProductSnapshot> snapshotMap,
                                            Map<String, ProductDetail> productDetailMap) {
        return items.stream()
            .map(item -> buildOrderItem(item, snapshotMap.get(item.productId()), productDetailMap))
            .toList();
    }

    /**
     * 构建单个订单项
     */
    private OrderItem buildOrderItem(CreateOrderCommand.CreateOrderItem item,
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
            return ProductSnapshot.builder()
                .productId(productId)
                .price(price)
                .build();
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
     * 准备结果
     */
    public record PreparationResult(UserId sellerId, List<OrderItem> orderItems) {}
}
