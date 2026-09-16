package com.cartethyia.easyorange.order.application.query.assembler;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItemSnapshot;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrderReadModelAssembler {

    public List<OrderVO> toOrderVOs(List<OrderReadModel> orders, Map<String, String> usernames) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        return orders.stream().map(o -> toOrderVO(o, usernames, true)).toList();
    }

    public OrderVO toOrderVO(OrderReadModel order, Map<String, String> usernames, boolean maskSensitive) {
        List<OrderVO.OrderItemVO> itemVOs = order.items().stream()
                .map(item -> OrderVO.OrderItemVO.builder()
                        .itemId(item.itemId())
                        .productId(item.productId())
                        .productName(nameOf(item))
                        .productImage(imageOf(item))
                        .unitPrice(item.unitPrice())
                        .quantity(item.quantity())
                        .subtotal(item.subtotal())
                        .build())
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

    /** 名称缺失回退空串（前端与 TS 类型均按 string 消费）。 */
    private static String nameOf(OrderItemReadModel item) {
        OrderItemSnapshot snapshot = item.snapshot();
        return snapshot == null || snapshot.name() == null ? "" : snapshot.name();
    }

    /** 无主图时给 null，前端按 `productImage ? <img> : 占位` 渲染。 */
    private static String imageOf(OrderItemReadModel item) {
        OrderItemSnapshot snapshot = item.snapshot();
        return snapshot == null || snapshot.image() == null || snapshot.image().isEmpty() ? null : snapshot.image();
    }
}
