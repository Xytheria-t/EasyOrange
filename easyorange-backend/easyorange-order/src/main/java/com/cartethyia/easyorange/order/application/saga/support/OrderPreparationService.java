package com.cartethyia.easyorange.order.application.saga.support;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.idgen.IdGenerator;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

    private final ProductInventoryPort productInventoryPort;
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
    public PreparationResult prepareOrderItems(List<OrderItemRequest> items, String buyerId) {
        // 获取商品快照并校验
        List<ItemPreparation> preparations = prepareAndValidateItems(items);

        // 获取资产方 ID（所有资产必须来自同一资产方）
        String sellerId = validateAndGetSellerId(preparations, buyerId);

        // 批量获取资产详情
        Map<String, ProductDetail> productDetailMap = loadProductDetails(preparations);

        // 构建订单项
        List<OrderItem> orderItems = buildOrderItems(preparations, productDetailMap);

        return new PreparationResult(sellerId, orderItems);
    }

    /**
     * 准备并校验资产项
     */
    private List<ItemPreparation> prepareAndValidateItems(List<OrderItemRequest> items) {
        return items.stream()
            .map(item -> {
                ProductInventoryPort.ProductSnapshot snapshot = productInventoryPort.getSnapshot(item.productId())
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

        ProductDetail detail = productDetailMap.get(prep.snapshot().productId());
        String name = detail != null ? detail.title() : "";
        String image = (detail != null && detail.images() != null && !detail.images().isEmpty())
            ? detail.images().getFirst() : "";
        String description = detail != null && detail.description() != null ? detail.description() : "";
        String conditionLevel = detail != null && detail.conditionLevel() != null ? detail.conditionLevel() : "";

        return OrderItem.builder()
            .id(idGenerator.generateId())
            .productId(ProductId.of(prep.snapshot().productId()))
            .snapshot(com.cartethyia.easyorange.order.domain.valueobject.ProductSnapshot.builder()
                .productId(prep.snapshot().productId())
                .name(name)
                .image(image)
                .description(description)
                .price(unitPrice)
                .conditionLevel(conditionLevel)
                .build())
            .unitPrice(unitPrice)
            .quantity(prep.quantity())
            .subtotal(subtotal)
            .build();
    }

    /**
     * 获取资产的默认地址（如果未指定）
     */
    public String resolveDefaultAddress(List<ItemPreparation> preparations, String requestedAddress) {
        if (requestedAddress != null && !requestedAddress.isBlank()) {
            return requestedAddress;
        }

        ProductInventoryPort.ProductSnapshot firstSnapshot = preparations.getFirst().snapshot();
        return (firstSnapshot.location() != null && !firstSnapshot.location().isBlank())
            ? firstSnapshot.location()
            : "未指定";
    }

    /**
     * 订单项请求
     */
    public record OrderItemRequest(String productId, int quantity) {}

    /**
     * 资产准备结果
     */
    public record ItemPreparation(ProductInventoryPort.ProductSnapshot snapshot, int quantity) {}

    /**
     * 准备结果
     */
    public record PreparationResult(String sellerId, List<OrderItem> orderItems) {}
}