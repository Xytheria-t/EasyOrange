package com.cartethyia.easyorange.admin.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderDetailResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderDetailResponse.Address;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderDetailResponse.Participant;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderResponse.ItemInfo;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.OrderStatsResponse;
import com.cartethyia.easyorange.admin.domain.model.OrderDetailView;
import com.cartethyia.easyorange.admin.domain.model.OrderListView;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderItemDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderItemInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderStats;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderSummary;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserInfo;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 订单出参组装 — 服务的 {@code domain} 读模型在这里翻成后台订单 DTO 并分页包装。
 * <p>
 * 关联数据（昵称 / 商品名）由服务批量取回，这里只做「查不到就留 null」的展示兜底：
 * 订单必须照常显示，少个昵称不该让整页消失。
 */
@Component
public class AdminOrderAssembler {

    public PageResult<AdminOrderResponse> toPageResponses(OrderListView view) {
        List<AdminOrderResponse> records = view.page().records().stream()
                .map(order -> toResponse(order, view))
                .toList();
        OrderQueryResult page = view.page();
        return PageResult.of(records, page.total(), page.pageNum(), page.pageSize());
    }

    public AdminOrderDetailResponse toDetailResponse(OrderDetailView view) {
        OrderDetail order = view.order();
        return AdminOrderDetailResponse.builder()
                .orderId(order.id())
                .orderNo(order.orderNo())
                .buyer(toParticipant(view.buyer(), order.buyerId()))
                .seller(toParticipant(view.seller(), order.sellerId()))
                .items(toItems(order.items(), view.products()))
                .totalAmount(order.totalAmount())
                .status(order.status())
                .statusDesc(order.statusDesc())
                .paymentStatus(order.paymentStatus())
                .paymentNo(order.paymentNo())
                .paidAmount(order.paidAmount())
                .refundedAmount(order.refundedAmount())
                .shippingAddress(toAddress(view.buyer(), order))
                .remark(order.remark())
                .cancelReason(order.cancelReason())
                .createTime(order.createTime())
                .payTime(order.payTime())
                .updateTime(order.updateTime())
                .cancelTime(order.cancelTime())
                .refundReason(order.refundReason())
                .refundTime(order.refundTime())
                .build();
    }

    /** 收件人名取买家昵称：eo_order 只存 address / phone 两列，没有收件人姓名单列。 */
    private static Address toAddress(UserInfo buyer, OrderDetail order) {
        if (order.address() == null && order.phone() == null) {
            return null;
        }
        String receiverName = buyer != null ? buyer.nickName() : null;
        return new Address(receiverName, order.phone(), order.address());
    }

    public OrderStatsResponse toStatsResponse(OrderStats stats) {
        return OrderStatsResponse.builder()
                .totalOrders(stats.totalOrders())
                .todayOrders(stats.todayOrders())
                .pendingPayment(stats.pendingPayment())
                .toShip(stats.toShip())
                .toReceive(stats.toReceive())
                .completed(stats.completed())
                .cancelled(stats.cancelled())
                .refunded(stats.refunded())
                .totalRevenue(stats.totalRevenue())
                .todayRevenue(stats.todayRevenue())
                .build();
    }

    private AdminOrderResponse toResponse(OrderSummary order, OrderListView view) {
        UserInfo buyer = view.users().get(order.buyerId());
        UserInfo seller = view.users().get(order.sellerId());
        List<ItemInfo> items = itemInfos(view.items().getOrDefault(order.id(), List.of()), view.products());

        return new AdminOrderResponse(
                order.id(),
                order.orderNo(),
                order.buyerId(),
                buyer != null ? buyer.nickName() : null,
                order.sellerId(),
                seller != null ? seller.nickName() : null,
                items,
                order.totalAmount(),
                order.status(),
                order.statusDesc(),
                order.paymentStatus(),
                order.paymentStatusDesc(),
                order.createTime());
    }

    private List<ItemInfo> itemInfos(List<OrderItemInfo> items, Map<String, AdminOrderPort.ProductInfo> products) {
        return items.stream()
                .map(item -> {
                    AdminOrderPort.ProductInfo product = products.get(item.productId());
                    return new ItemInfo(item.productId(), product != null ? product.name() : null);
                })
                .toList();
    }

    private List<AdminOrderDetailResponse.ItemInfo> toItems(
            List<OrderItemDetail> items, Map<String, AdminOrderPort.ProductInfo> products) {
        return items.stream()
                .map(item -> {
                    AdminOrderPort.ProductInfo product = products.get(item.productId());
                    return new AdminOrderDetailResponse.ItemInfo(
                            item.itemId(),
                            item.productId(),
                            product != null ? product.name() : null,
                            null,
                            item.price(),
                            item.quantity(),
                            item.subtotal());
                })
                .toList();
    }

    /** 查不到档案时保留 id，让前端能显示「用户已不可见」而不是把整块信息抹成空白。 */
    private static Participant toParticipant(UserInfo user, String fallbackId) {
        return user != null
                ? new Participant(user.id(), user.nickName(), user.avatar(), user.phone())
                : new Participant(fallbackId, null, null, null);
    }
}
