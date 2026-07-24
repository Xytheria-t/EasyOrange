package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderItemInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.OrderSummary;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserInfo;
import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminOrderQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderDetailResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.OrderStatsResponse;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(CommonConstant.DATETIME_FORMAT);

    private final AdminOrderQueryPort adminOrderQueryPort;
    private final AdminUserQueryPort adminUserQueryPort;
    private final OrderReadRepository orderReadRepository;
    private final OrderRepository orderRepository;

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
            request.getPageSize()
        );

        OrderQueryResult result = adminOrderQueryPort.queryOrders(condition);

        Set<String> userIds = new HashSet<>();
        result.records().forEach(o -> {
            if (o.buyerId() != null) userIds.add(o.buyerId());
            if (o.sellerId() != null) userIds.add(o.sellerId());
        });
        Map<String, UserInfo> userMap = adminUserQueryPort.getUserInfos(userIds.stream().toList());

        List<String> orderIds = result.records().stream()
            .map(OrderSummary::id)
            .toList();
        Map<String, List<OrderItemInfo>> itemsMap = adminOrderQueryPort.getOrderItems(orderIds);

        Set<String> productIds = itemsMap.values().stream()
            .flatMap(Collection::stream)
            .map(OrderItemInfo::productId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<String, ProductInfo> productMap = adminOrderQueryPort.getProducts(productIds.stream().toList());

        List<AdminOrderResponse> records = result.records().stream()
            .map(order -> toAdminOrderResponse(order, userMap, itemsMap, productMap))
            .collect(Collectors.toList());

        return PageResult.of(records, result.total(), result.pageNum(), result.pageSize());
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrderDetail(String id) {
        OrderReadModel model = orderReadRepository.findById(new OrderId(id))
            .orElseThrow(() -> BusinessException.of("订单不存在"));

        UserInfo buyer = adminUserQueryPort.getUserInfo(model.buyerId());
        UserInfo seller = adminUserQueryPort.getUserInfo(model.sellerId());

        List<String> productIds = model.items().stream()
            .map(OrderItemReadModel::productId)
            .distinct()
            .toList();
        Map<String, ProductInfo> productMap = adminOrderQueryPort.getProducts(productIds);

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
            .buyer(buyer != null ? new AdminOrderDetailResponse.BuyerInfo(
                buyer.id(), buyer.nickName(), buyer.avatar(), buyer.phone()
            ) : new AdminOrderDetailResponse.BuyerInfo(model.buyerId(), null, null, null))
            .seller(seller != null ? new AdminOrderDetailResponse.SellerInfo(
                seller.id(), seller.nickName(), seller.avatar(), seller.phone()
            ) : new AdminOrderDetailResponse.SellerInfo(model.sellerId(), null, null, null))
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
            .build();
    }

    @Transactional(readOnly = true)
    public OrderStatsResponse getOrderStats() {
        long totalOrders = orderReadRepository.countByStatus(null);
        long pendingPayment = orderReadRepository.countByStatus(Integer.valueOf(OrderStatus.PENDING_PAYMENT.getCode()));
        long paid = orderReadRepository.countByStatus(Integer.valueOf(OrderStatus.PAID.getCode()));
        long shipped = orderReadRepository.countByStatus(Integer.valueOf(OrderStatus.SHIPPED.getCode()));
        long toReceive = shipped;
        long completed = orderReadRepository.countByStatus(Integer.valueOf(OrderStatus.COMPLETED.getCode()));
        long cancelled = orderReadRepository.countByStatus(Integer.valueOf(OrderStatus.CANCELLED.getCode()));
        long refunded = orderReadRepository.countByStatus(Integer.valueOf(OrderStatus.REFUNDED.getCode()));

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();

        OrderQueryCondition todayCondition = new OrderQueryCondition(
            null, null, null, null, null,
            todayStart, null, null, null
        );
        long todayOrders = adminOrderQueryPort.queryOrders(todayCondition).total();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal todayRevenue = BigDecimal.ZERO;

        return OrderStatsResponse.builder()
            .totalOrders(totalOrders)
            .todayOrders(todayOrders)
            .pendingPayment(pendingPayment)
            .toShip(paid)
            .toReceive(toReceive)
            .completed(completed)
            .cancelled(cancelled)
            .refunded(refunded)
            .totalRevenue(totalRevenue)
            .todayRevenue(todayRevenue)
            .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String id, String reason) {
        var aggregate = orderRepository.findById(OrderId.of(id))
                .orElseThrow(() -> BusinessException.of("订单不存在"));

        if (aggregate.status() == OrderStatus.PENDING_PAYMENT) {
            var result = aggregate.cancel(reason);
            orderRepository.update(result.aggregate());
        } else if (aggregate.status() == OrderStatus.PAID) {
            var result = aggregate.forceCancel(reason);
            orderRepository.update(result.aggregate());
        } else {
            throw BusinessException.of("当前订单状态不允许取消");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void forceComplete(String id, String reason) {
        var aggregate = orderRepository.findById(OrderId.of(id))
                .orElseThrow(() -> BusinessException.of("订单不存在"));

        var result = aggregate.confirmReceipt();
        orderRepository.update(result.aggregate());
    }

    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(String id, String reason) {
        var aggregate = orderRepository.findById(OrderId.of(id))
                .orElseThrow(() -> BusinessException.of("订单不存在"));

        var result = aggregate.refund(reason);
        orderRepository.update(result.aggregate());
    }

    private LocalDateTime parseStartTime(String startTimeStr) {
        if (!StringUtils.hasText(startTimeStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(startTimeStr + " 00:00:00", DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("无法解析开始时间: {}, 格式应为 yyyy-MM-dd", startTimeStr);
            return null;
        }
    }

    private LocalDateTime parseEndTime(String endTimeStr) {
        if (!StringUtils.hasText(endTimeStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(endTimeStr + " 23:59:59", DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("无法解析结束时间: {}, 格式应为 yyyy-MM-dd", endTimeStr);
            return null;
        }
    }

    private AdminOrderResponse toAdminOrderResponse(OrderSummary order, Map<String, UserInfo> userMap,
                                                      Map<String, List<OrderItemInfo>> itemsMap,
                                                      Map<String, ProductInfo> productMap) {
        UserInfo buyer = userMap.get(order.buyerId());
        UserInfo seller = userMap.get(order.sellerId());

        List<OrderItemInfo> items = itemsMap.getOrDefault(order.id(), List.of());
        List<AdminOrderResponse.ItemInfo> itemInfos = items.stream()
            .map(item -> {
                ProductInfo product = productMap.get(item.productId());
                return new AdminOrderResponse.ItemInfo(
                    item.productId(),
                    product != null ? product.name() : null
                );
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
            order.createTime()
        );
    }
}
