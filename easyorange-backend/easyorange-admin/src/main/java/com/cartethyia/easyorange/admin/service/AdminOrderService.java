package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminOrderQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderDetailResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.OrderStatsResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderItemDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderItemInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderStats;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderSummary;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserInfo;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final AdminOrderPort adminOrderPort;
    private final AdminUserPort adminUserPort;

    public PageResult<AdminOrderResponse> listOrders(AdminOrderQueryRequest request) {
        LocalDateTime startTime = parseStartTime(request.getStartTime());
        LocalDateTime endTime = parseEndTime(request.getEndTime());

        OrderQueryCondition condition = new OrderQueryCondition(
                request.getOrderNo(),
                request.getBuyerId(),
                request.getSellerId(),
                request.getStatus(),
                request.getPaymentStatus(),
                startTime,
                endTime,
                request.getPageNum(),
                request.getPageSize());

        OrderQueryResult result = adminOrderPort.queryOrders(condition);

        Set<String> userIds = new HashSet<>();
        result.records().forEach(o -> {
            if (o.buyerId() != null) userIds.add(o.buyerId());
            if (o.sellerId() != null) userIds.add(o.sellerId());
        });
        Map<String, UserInfo> userMap =
                adminUserPort.getUserInfos(userIds.stream().toList());

        List<String> orderIds = result.records().stream().map(OrderSummary::id).toList();
        Map<String, List<OrderItemInfo>> itemsMap = adminOrderPort.getOrderItems(orderIds);

        Set<String> productIds = itemsMap.values().stream()
                .flatMap(Collection::stream)
                .map(OrderItemInfo::productId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, ProductInfo> productMap =
                adminOrderPort.getProducts(productIds.stream().toList());

        List<AdminOrderResponse> records = result.records().stream()
                .map(order -> toAdminOrderResponse(order, userMap, itemsMap, productMap))
                .collect(Collectors.toList());

        return PageResult.of(records, result.total(), result.pageNum(), result.pageSize());
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrderDetail(String id) {
        OrderDetail model = adminOrderPort.getOrderDetail(id);
        if (model == null) {
            throw BusinessException.of("订单不存在");
        }

        UserInfo buyer = adminUserPort.getUserInfo(model.buyerId());
        UserInfo seller = adminUserPort.getUserInfo(model.sellerId());

        List<String> productIds = model.items().stream()
                .map(OrderItemDetail::productId)
                .distinct()
                .toList();
        Map<String, ProductInfo> productMap = adminOrderPort.getProducts(productIds);

        List<AdminOrderDetailResponse.ProductInfo> productInfos = model.items().stream()
                .map(item -> {
                    ProductInfo p = productMap.get(item.productId());
                    return p != null
                            ? new AdminOrderDetailResponse.ProductInfo(p.id(), p.name(), null, p.price())
                            : new AdminOrderDetailResponse.ProductInfo(item.productId(), null, null, null);
                })
                .toList();

        return AdminOrderDetailResponse.builder()
                .orderId(model.id())
                .orderNo(model.orderNo())
                .buyer(
                        buyer != null
                                ? new AdminOrderDetailResponse.BuyerInfo(
                                        buyer.id(), buyer.nickName(), buyer.avatar(), buyer.phone())
                                : new AdminOrderDetailResponse.BuyerInfo(model.buyerId(), null, null, null))
                .seller(
                        seller != null
                                ? new AdminOrderDetailResponse.SellerInfo(
                                        seller.id(), seller.nickName(), seller.avatar(), seller.phone())
                                : new AdminOrderDetailResponse.SellerInfo(model.sellerId(), null, null, null))
                .products(productInfos)
                .totalAmount(model.totalAmount())
                .status(model.status())
                .statusDesc(model.statusDesc())
                .paymentStatus(model.paymentStatus())
                .remark(model.remark())
                .cancelReason(model.cancelReason())
                .createTime(model.createTime())
                .updateTime(model.updateTime())
                .cancelTime(model.cancelTime())
                .refundReason(model.refundReason())
                .refundTime(model.refundTime())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderStatsResponse getOrderStats() {
        OrderStats stats = adminOrderPort.getOrderStats();

        return OrderStatsResponse.builder()
                .totalOrders(stats.totalOrders())
                .todayOrders(stats.todayOrders())
                .pendingPayment(stats.pendingPayment())
                .toShip(stats.toShip())
                .toReceive(stats.toReceive())
                .completed(stats.completed())
                .cancelled(stats.cancelled())
                .refunded(stats.refunded())
                .totalRevenue(BigDecimal.ZERO)
                .todayRevenue(BigDecimal.ZERO)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String id, String reason) {
        adminOrderPort.cancelOrder(id, reason);
    }

    @Transactional(rollbackFor = Exception.class)
    public void forceComplete(String id, String reason) {
        adminOrderPort.forceComplete(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(String id, String reason) {
        adminOrderPort.refundOrder(id, reason);
    }

    private LocalDateTime parseStartTime(String startTimeStr) {
        if (!StringUtils.hasText(startTimeStr)) {
            return null;
        }
        try {
            return LocalDate.parse(startTimeStr).atStartOfDay();
        } catch (DateTimeParseException e) {
            log.warn("无法解析时间: {}, 格式应为 yyyy-MM-dd", startTimeStr);
            return null;
        }
    }

    private LocalDateTime parseEndTime(String endTimeStr) {
        if (!StringUtils.hasText(endTimeStr)) {
            return null;
        }
        try {
            return LocalDate.parse(endTimeStr).atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            log.warn("无法解析时间: {}, 格式应为 yyyy-MM-dd", endTimeStr);
            return null;
        }
    }

    private AdminOrderResponse toAdminOrderResponse(
            OrderSummary order,
            Map<String, UserInfo> userMap,
            Map<String, List<OrderItemInfo>> itemsMap,
            Map<String, ProductInfo> productMap) {
        UserInfo buyer = userMap.get(order.buyerId());
        UserInfo seller = userMap.get(order.sellerId());

        List<OrderItemInfo> items = itemsMap.getOrDefault(order.id(), List.of());
        List<AdminOrderResponse.ItemInfo> itemInfos = items.stream()
                .map(item -> {
                    ProductInfo product = productMap.get(item.productId());
                    return new AdminOrderResponse.ItemInfo(item.productId(), product != null ? product.name() : null);
                })
                .toList();

        return new AdminOrderResponse(
                order.id(),
                order.orderNo(),
                order.buyerId(),
                buyer != null ? buyer.nickName() : null,
                order.sellerId(),
                seller != null ? seller.nickName() : null,
                itemInfos,
                order.totalAmount(),
                order.status(),
                order.statusDesc(),
                order.paymentStatus(),
                order.paymentStatusDesc(),
                order.createTime());
    }
}
