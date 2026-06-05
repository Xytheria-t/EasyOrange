package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.util.BatchQueryUtil;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminOrderQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderDetailResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.OrderStatsResponse;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderDO;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderItemDO;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderItemMapper;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderMapper;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderReadRepository orderReadRepository;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final BatchQueryUtil batchQueryUtil;

    public PageResult<AdminOrderResponse> listOrders(AdminOrderQueryRequest request) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        var wrapper = ChainWrappers.lambdaQueryChain(orderMapper)
            .eq(OrderDO::getDelFlag, 0);

        if (StringUtils.hasText(request.getOrderNo())) {
            wrapper.like(OrderDO::getOrderNo, request.getOrderNo());
        }
        if (request.getBuyerId() != null) {
            wrapper.eq(OrderDO::getBuyerId, request.getBuyerId());
        }
        if (request.getSellerId() != null) {
            wrapper.eq(OrderDO::getSellerId, request.getSellerId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(OrderDO::getStatus, request.getStatus());
        }
        if (request.getPaymentStatus() != null) {
            wrapper.eq(OrderDO::getPaymentStatus, request.getPaymentStatus());
        }
        if (StringUtils.hasText(request.getStartTime())) {
            try {
                LocalDateTime startTime = LocalDateTime.parse(request.getStartTime() + " 00:00:00", BatchQueryUtil.DATE_FORMATTER);
                wrapper.ge(OrderDO::getCreateTime, startTime);
            } catch (Exception ignored) {
            }
        }
        if (StringUtils.hasText(request.getEndTime())) {
            try {
                LocalDateTime endTime = LocalDateTime.parse(request.getEndTime() + " 23:59:59", BatchQueryUtil.DATE_FORMATTER);
                wrapper.le(OrderDO::getCreateTime, endTime);
            } catch (Exception ignored) {
            }
        }

        wrapper.orderByDesc(OrderDO::getCreateTime);

        Page<OrderDO> page = wrapper.page(new Page<>(pageNum, pageSize));

        Set<Long> userIds = new HashSet<>();
        page.getRecords().forEach(o -> {
            if (o.getBuyerId() != null) userIds.add(o.getBuyerId());
            if (o.getSellerId() != null) userIds.add(o.getSellerId());
        });
        Map<Long, UserEntity> userMap = batchQueryUtil.batchGetUsers(userIds.stream().toList());
        Map<Long, List<OrderItemDO>> itemsMap = batchGetOrderItems(page);
        Map<Long, ProductDO> productMap = batchGetProductsFromItems(itemsMap);

        List<AdminOrderResponse> records = page.getRecords().stream()
            .map(order -> toAdminOrderResponse(order, userMap, itemsMap, productMap))
            .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrderDetail(Long id) {
        OrderReadModel model = orderReadRepository.findById(new OrderId(id))
            .orElseThrow(() -> BusinessException.of("订单不存在"));

        UserEntity buyer = userMapper.selectById(model.buyerId());
        UserEntity seller = userMapper.selectById(model.sellerId());

        List<Long> productIds = model.items().stream()
            .map(OrderItemReadModel::productId)
            .distinct()
            .toList();
        Map<Long, ProductDO> productMap;
        if (!productIds.isEmpty()) {
            productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(ProductDO::getId, p -> p, (a, b) -> a));
        } else {
            productMap = Map.of();
        }

        List<AdminOrderDetailResponse.ProductInfo> productInfos = model.items().stream()
            .map(item -> {
                ProductDO p = productMap.get(item.productId());
                return p != null
                    ? new AdminOrderDetailResponse.ProductInfo(p.getId(), p.getName(), null, p.getPrice())
                    : new AdminOrderDetailResponse.ProductInfo(item.productId(), null, null, null);
            })
            .toList();

        return AdminOrderDetailResponse.builder()
            .orderId(model.id())
            .orderNo(model.orderNo())
            .buyer(buyer != null ? new AdminOrderDetailResponse.BuyerInfo(
                buyer.getId(), buyer.getNickName(), buyer.getAvatar(), buyer.getPhone()
            ) : new AdminOrderDetailResponse.BuyerInfo(model.buyerId(), null, null, null))
            .seller(seller != null ? new AdminOrderDetailResponse.SellerInfo(
                seller.getId(), seller.getNickName(), seller.getAvatar(), seller.getPhone()
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
        long pendingPayment = orderReadRepository.countByStatus(OrderStatus.PENDING_PAYMENT.getCode());
        long paid = orderReadRepository.countByStatus(OrderStatus.PAID.getCode());
        long shipped = orderReadRepository.countByStatus(OrderStatus.SHIPPED.getCode());
        long toReceive = shipped;
        long completed = orderReadRepository.countByStatus(OrderStatus.COMPLETED.getCode());
        long cancelled = orderReadRepository.countByStatus(OrderStatus.CANCELLED.getCode());
        long refunded = orderReadRepository.countByStatus(OrderStatus.REFUNDED.getCode());

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        long todayOrders = ChainWrappers.lambdaQueryChain(orderMapper)
            .eq(OrderDO::getDelFlag, 0)
            .ge(OrderDO::getCreateTime, todayStart)
            .count();

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
    public void cancelOrder(Long id, String reason) {
        OrderDO order = orderMapper.selectById(id);
        if (order == null || order.getDelFlag() != 0) {
            throw BusinessException.of("订单不存在");
        }
        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        if (currentStatus == null || !(currentStatus == OrderStatus.PENDING_PAYMENT || currentStatus == OrderStatus.PAID)) {
            throw BusinessException.of("当前订单状态不允许取消");
        }

        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelReason(reason);
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void forceComplete(Long id, String reason) {
        OrderDO order = orderMapper.selectById(id);
        if (order == null || order.getDelFlag() != 0) {
            throw BusinessException.of("订单不存在");
        }
        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        if (currentStatus == null || currentStatus != OrderStatus.SHIPPED) {
            throw BusinessException.of("仅已发货的订单可强制完成");
        }

        order.setStatus(OrderStatus.COMPLETED.getCode());
        orderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long id, String reason) {
        OrderDO order = orderMapper.selectById(id);
        if (order == null || order.getDelFlag() != 0) {
            throw BusinessException.of("订单不存在");
        }
        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        if (currentStatus == null || currentStatus == OrderStatus.REFUNDED) {
            throw BusinessException.of("该订单已退款");
        }
        if (currentStatus == OrderStatus.CANCELLED) {
            throw BusinessException.of("已取消的订单无法退款");
        }

        order.setStatus(OrderStatus.REFUNDED.getCode());
        order.setCancelReason(reason);
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private Map<Long, List<OrderItemDO>> batchGetOrderItems(Page<OrderDO> orderPage) {
        List<Long> orderIds = orderPage.getRecords().stream()
            .map(OrderDO::getId)
            .filter(Objects::nonNull)
            .toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        List<OrderItemDO> items = ChainWrappers.lambdaQueryChain(orderItemMapper)
            .in(OrderItemDO::getOrderId, orderIds)
            .list();
        return items.stream().collect(Collectors.groupingBy(OrderItemDO::getOrderId));
    }

    private Map<Long, ProductDO> batchGetProductsFromItems(Map<Long, List<OrderItemDO>> itemsMap) {
        Set<Long> productIds = itemsMap.values().stream()
            .flatMap(Collection::stream)
            .map(OrderItemDO::getProductId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductDO> products = productMapper.selectBatchIds(productIds);
        return products.stream().collect(Collectors.toMap(ProductDO::getId, p -> p, (a, b) -> a));
    }

    private AdminOrderResponse toAdminOrderResponse(OrderDO order, Map<Long, UserEntity> userMap,
                                                     Map<Long, List<OrderItemDO>> itemsMap,
                                                     Map<Long, ProductDO> productMap) {
        UserEntity buyer = userMap.get(order.getBuyerId());
        UserEntity seller = userMap.get(order.getSellerId());
        OrderStatus status = OrderStatus.fromCode(order.getStatus());

        List<OrderItemDO> items = itemsMap.getOrDefault(order.getId(), List.of());
        List<AdminOrderResponse.ItemInfo> itemInfos = items.stream()
            .map(item -> {
                ProductDO product = productMap.get(item.getProductId());
                return new AdminOrderResponse.ItemInfo(
                    item.getProductId(),
                    product != null ? product.getName() : null
                );
            })
            .toList();

        return new AdminOrderResponse(
            order.getId(),
            order.getOrderNo(),
            order.getBuyerId(),
            buyer != null ? buyer.getNickName() : null,
            order.getSellerId(),
            seller != null ? seller.getNickName() : null,
            itemInfos,
            order.getTotalAmount(),
            order.getStatus(),
            status != null ? status.getDesc() : "未知状态",
            order.getPaymentStatus(),
            getPaymentStatusDesc(order.getPaymentStatus()),
            order.getCreateTime()
        );
    }

    private String getPaymentStatusDesc(Integer paymentStatus) {
        if (paymentStatus == null) return "未支付";
        return switch (paymentStatus) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已退款";
            default -> "未知";
        };
    }
}
