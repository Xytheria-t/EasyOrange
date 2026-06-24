package com.cartethyia.easyorange.order.application.saga.support;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.idgen.SnowflakeIdGenerator;
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
 * 负责商品数据准备、校验、构建订单项
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPreparationService {

    private final ProductInventoryPort productInventoryPort;
    private final ProductQueryPort productQueryPort;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 准备订单项数据
     *
     * @param items   订单项请求列表
     * @param buyerId 买家 ID
     * @return 准备结果
     * @throws OrderDomainException 如果商品不存在、已下架或库存不足
     */
    public PreparationResult prepareOrderItems(List<OrderItemRequest> items, Long buyerId) {
        // 获取商品快照并校验
        List<ItemPreparation> preparations = prepareAndValidateItems(items);

        // 获取卖家 ID（所有商品必须来自同一卖家）
        Long sellerId = validateAndGetSellerId(preparations, buyerId);

        // 批量获取商品详情
        Map<Long, ProductDetail> productDetailMap = loadProductDetails(preparations);

        // 构建订单项
        List<OrderItem> orderItems = buildOrderItems(preparations, productDetailMap);

        return new PreparationResult(sellerId, orderItems);
    }

    /**
     * 准备并校验商品项
     */
    private List<ItemPreparation> prepareAndValidateItems(List<OrderItemRequest> items) {
        return items.stream()
            .map(item -> {
                ProductInventoryPort.ProductSnapshot snapshot = productInventoryPort.getSnapshot(item.productId())
                    .orElseThrow(() -> new OrderDomainException("商品不存在: " + item.productId()));

                BizRequire.requireTrue(snapshot.isOnline(), "商品已下架: " + item.productId());
                BizRequire.requireTrue(snapshot.hasStock(), "商品库存不足: " + item.productId());

                return new ItemPreparation(snapshot, item.quantity(), item.unitPriceOverride());
            })
            .toList();
    }

    /**
     * 校验卖家 ID 并返回
     */
    private Long validateAndGetSellerId(List<ItemPreparation> preparations, Long buyerId) {
        Long sellerId = preparations.getFirst().snapshot().sellerId();
        BizRequire.requireTrue(!Objects.equals(sellerId, buyerId), "不能购买自己的商品");

        // 校验所有商品来自同一卖家
        boolean allSameSeller = preparations.stream()
            .allMatch(p -> Objects.equals(p.snapshot().sellerId(), sellerId));
        BizRequire.requireTrue(allSameSeller, "订单中的商品必须来自同一卖家");

        return sellerId;
    }

    /**
     * 批量加载商品详情
     */
    private Map<Long, ProductDetail> loadProductDetails(List<ItemPreparation> preparations) {
        List<Long> productIds = preparations.stream()
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
                                             Map<Long, ProductDetail> productDetailMap) {
        return preparations.stream()
            .map(prep -> buildOrderItem(prep, productDetailMap))
            .toList();
    }

    /**
     * 构建单个订单项
     */
    private OrderItem buildOrderItem(ItemPreparation prep, Map<Long, ProductDetail> productDetailMap) {
        Money unitPrice = prep.unitPriceOverride() != null
                ? Money.of(prep.unitPriceOverride())
                : Money.of(prep.snapshot().price());
        Money subtotal = unitPrice.multiply(prep.quantity());

        ProductDetail detail = productDetailMap.get(prep.snapshot().productId());
        String name = detail != null ? detail.title() : "";
        String image = (detail != null && detail.images() != null && !detail.images().isEmpty())
            ? detail.images().getFirst() : "";
        String description = detail != null && detail.description() != null ? detail.description() : "";
        String conditionLevel = detail != null && detail.conditionLevel() != null ? detail.conditionLevel() : "";

        return OrderItem.builder()
            .id(snowflakeIdGenerator.nextId())
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
     * 获取商品的默认地址（如果未指定）
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
    public record OrderItemRequest(Long productId, int quantity, BigDecimal unitPriceOverride) {
        public OrderItemRequest(Long productId, int quantity) { this(productId, quantity, null); }
    }

    /**
     * 商品准备结果
     */
    public record ItemPreparation(ProductInventoryPort.ProductSnapshot snapshot, int quantity, BigDecimal unitPriceOverride) {}

    /**
     * 准备结果
     */
    public record PreparationResult(Long sellerId, List<OrderItem> orderItems) {}
}