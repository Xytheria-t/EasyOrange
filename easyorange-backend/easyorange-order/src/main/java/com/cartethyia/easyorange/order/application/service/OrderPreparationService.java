package com.cartethyia.easyorange.order.application.service;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
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
        // 获取商品快照并校验
        List<ItemPreparation> preparations = prepareAndValidateItems(items);

        // 获取资产方 ID（所有资产必须来自同一资产方）
        String sellerId = validateAndGetSellerId(preparations, buyerId);

        // 批量获取资产详情
        Map<String, ProductDetail> productDetailMap = loadProductDetails(preparations);

        // 构建订单项
        List<OrderItem> orderItems = buildOrderItems(preparations, productDetailMap);

        return new PreparationResult(UserId.of(sellerId), orderItems);
    }

    /**
     * 准备并校验资产项
     */
    private List<ItemPreparation> prepareAndValidateItems(List<CreateOrderCommand.CreateOrderItem> items) {
        return items.stream()
            .map(item -> {
                ProductOrderPort.ProductSnapshot snapshot = productOrderPort.getSnapshot(item.productId())
                    .orElseThrow(() -> new OrderDomainException("资产不存在: " + item.productId()));

                BizRequire.requireTrue(snapshot.isOnline(), "资产已下架: " + item.productId());
                BizRequire.requireTrue(snapshot.hasStock(), "资产库存不足: " + item.productId());

                return new ItemPreparation(snapshot, item.quantity());
            })
            .toList();
    }

    /**
     * 校验资产方 ID 并返回
     */
    private String validateAndGetSellerId(List<ItemPreparation> preparations, String buyerId) {
        String sellerId = preparations.getFirst().snapshot().sellerId();
        BizRequire.requireTrue(!Objects.equals(sellerId, buyerId), "不能认领自己的资产");

        // 校验所有资产来自同一资产方
        boolean allSameSeller = preparations.stream()
            .allMatch(p -> Objects.equals(p.snapshot().sellerId(), sellerId));
        BizRequire.requireTrue(allSameSeller, "订单中的资产必须来自同一资产方");

        return sellerId;
    }

    /**
     * 批量加载资产详情
     */
    private Map<String, ProductDetail> loadProductDetails(List<ItemPreparation> preparations) {
        List<String> productIds = preparations.stream()
            .map(p -> p.snapshot().productId())
            .toList();

        return productQueryPort.getProductsByIds(productIds)
            .stream()
            .collect(Collectors.toMap(ProductDetail::id, d -> d));
    }

    /**
     * 构建订单项
     */
    private List<OrderItem> buildOrderItems(List<ItemPreparation> preparations,
                                              Map<String, ProductDetail> productDetailMap) {
        return preparations.stream()
            .map(prep -> buildOrderItem(prep, productDetailMap))
            .toList();
    }

    /**
     * 构建单个订单项
     */
    private OrderItem buildOrderItem(ItemPreparation prep, Map<String, ProductDetail> productDetailMap) {
        Money unitPrice = Money.of(prep.snapshot().price());
        Money subtotal = unitPrice.multiply(prep.quantity());
        var productId = prep.snapshot().productId();

        return OrderItem.builder()
            .id(idGenerator.generateId())
            .productId(ProductId.of(productId))
            .snapshot(buildProductSnapshot(productId, productDetailMap.get(productId), unitPrice))
            .unitPrice(unitPrice)
            .quantity(prep.quantity())
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
            .name(detail.title() != null ? detail.title() : "")
            .image(detail.images() != null && !detail.images().isEmpty()
                ? detail.images().getFirst() : "")
            .description(detail.description() != null ? detail.description() : "")
            .price(price)
            .conditionLevel(detail.conditionLevel() != null ? detail.conditionLevel() : "")
            .build();
    }

    /**
     * 资产准备结果
     */
    private record ItemPreparation(ProductOrderPort.ProductSnapshot snapshot, int quantity) {}

    /**
     * 准备结果
     */
    public record PreparationResult(UserId sellerId, List<OrderItem> orderItems) {}
}