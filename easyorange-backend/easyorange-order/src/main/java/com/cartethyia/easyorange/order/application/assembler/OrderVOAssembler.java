package com.cartethyia.easyorange.order.application.assembler;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;

import java.util.*;

import org.springframework.stereotype.Component;

@Component
public class OrderVOAssembler {

    public List<OrderVO> toOrderVOs(List<OrderReadModel> orders, Map<Long, ProductDetail> productMap) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        return orders.stream()
                .map(o -> toOrderVO(o, productMap, true))
                .toList();
    }

    public OrderVO toOrderVO(OrderReadModel order, Map<Long, ProductDetail> productMap, boolean maskSensitive) {
        List<OrderVO.OrderItemVO> itemVOs = order.items().stream()
                .map(item -> {
                    ProductDetail product = productMap.get(item.productId());
                    String productName = product != null ? product.title() : "";
                    String productImage = (product != null && product.images() != null && !product.images().isEmpty())
                            ? product.images().getFirst() : null;
                    return new OrderVO.OrderItemVO(
                            item.itemId(),
                            item.productId(),
                            productName,
                            productImage,
                            item.unitPrice(),
                            item.quantity(),
                            item.subtotal()
                    );
                })
                .toList();

        OrderVO.OrderVOBuilder builder = OrderVO.builder()
                .id(order.id())
                .orderNo(order.orderNo())
                .buyerId(order.buyerId())
                .sellerId(order.sellerId())
                .items(itemVOs)
                .totalAmount(order.totalAmount())
                .singleItem(itemVOs.size() == 1 && itemVOs.getFirst().getQuantity() == 1)
                .status(order.status())
                .statusDesc(order.statusDesc())
                .remark(order.remark())
                .createTime(order.createTime())
                .updateTime(order.updateTime());

        if (maskSensitive) {
            builder.address(MaskUtils.maskAddress(order.address(), 6))
                   .phone(MaskUtils.maskPhone(order.phone()));
        } else {
            builder.address(order.address())
                   .phone(MaskUtils.maskPhone(order.phone()));
        }

        return builder.build();
    }

    public Map<Long, ProductDetail> buildProductMap(List<ProductDetail> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }

        Map<Long, ProductDetail> productMap = new HashMap<>();
        products.forEach(p -> productMap.put(p.id(), p));
        return productMap;
    }
}
