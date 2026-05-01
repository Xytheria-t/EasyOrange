package com.cartethyia.easyorange.order.interfaces.assembler;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.interfaces.dto.response.OrderVO;

import java.util.*;

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
        OrderVO.OrderVOBuilder builder = OrderVO.builder()
                .id(order.id())
                .orderNo(order.orderNo())
                .buyerId(order.buyerId())
                .sellerId(order.sellerId())
                .productId(order.productId())
                .amount(order.amount())
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

        ProductDetail product = productMap.get(order.productId());
        if (product != null) {
            builder.productTitle(product.title());
            if (product.images() != null && !product.images().isEmpty()) {
                builder.productImage(product.images().getFirst());
            }
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
