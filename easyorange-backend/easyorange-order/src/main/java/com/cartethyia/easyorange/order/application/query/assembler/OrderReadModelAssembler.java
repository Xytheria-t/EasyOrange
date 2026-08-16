package com.cartethyia.easyorange.order.application.query.assembler;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort.ProductDetail;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OrderReadModelAssembler {

    public List<OrderVO> toOrderVOs(
            List<OrderReadModel> orders, Map<String, ProductDetail> productMap, Map<String, String> usernames) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        return orders.stream().map(o -> toOrderVO(o, productMap, usernames, true)).toList();
    }

    public OrderVO toOrderVO(
            OrderReadModel order,
            Map<String, ProductDetail> productMap,
            Map<String, String> usernames,
            boolean maskSensitive) {
        List<OrderVO.OrderItemVO> itemVOs = order.items().stream()
                .map(item -> {
                    ProductDetail product = productMap.get(item.productId());
                    String productName = product != null ? product.title() : "";
                    String productImage = (product != null
                                    && product.images() != null
                                    && !product.images().isEmpty())
                            ? product.images().getFirst()
                            : null;
                    return OrderVO.OrderItemVO.builder()
                            .itemId(item.itemId())
                            .productId(item.productId())
                            .productName(productName)
                            .productImage(productImage)
                            .unitPrice(item.unitPrice())
                            .quantity(item.quantity())
                            .subtotal(item.subtotal())
                            .build();
                })
                .toList();

        OrderVO.OrderVOBuilder builder = OrderVO.builder()
                .id(order.id())
                .orderNo(order.orderNo())
                .buyerId(order.buyerId())
                .buyerUsername(usernames.get(order.buyerId()))
                .sellerId(order.sellerId())
                .sellerUsername(usernames.get(order.sellerId()))
                .items(itemVOs)
                .totalAmount(order.totalAmount())
                .singleItem(itemVOs.size() == 1 && itemVOs.getFirst().getQuantity() == 1)
                .status(order.status())
                .statusDesc(order.statusDesc())
                .remark(order.remark())
                .createTime(order.createTime())
                .updateTime(order.updateTime());

        return builder.address(maskSensitive ? MaskUtils.maskAddress(order.address(), 6) : order.address())
                .phone(MaskUtils.maskPhone(order.phone()))
                .build();
    }

    /**
     * 构建产品 Map — 用 Collectors.toMap 替代手动 HashMap 构造。
     * 重复 key 时保留第一个（与原逻辑一致）。
     */
    public Map<String, ProductDetail> buildProductMap(List<ProductDetail> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }
        return products.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ProductDetail::id, p -> p, (a, _) -> a));
    }
}
