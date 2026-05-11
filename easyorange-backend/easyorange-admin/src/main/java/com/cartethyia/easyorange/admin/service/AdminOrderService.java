package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.admin.dto.request.AdminOrderQueryRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminOrderDetailVO;
import com.cartethyia.easyorange.admin.dto.response.AdminOrderVO;
import com.cartethyia.easyorange.admin.dto.response.OrderStatsVO;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderDO;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderMapper;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.output.OrderReadRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderMapper orderMapper;
    private final OrderReadRepository orderReadRepository;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PageResult<AdminOrderVO> listOrders(AdminOrderQueryRequest request) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<OrderDO>()
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
                LocalDateTime startTime = LocalDateTime.parse(request.getStartTime() + " 00:00:00", DATE_FORMATTER);
                wrapper.ge(OrderDO::getCreateTime, startTime);
            } catch (Exception ignored) {
            }
        }
        if (StringUtils.hasText(request.getEndTime())) {
            try {
                LocalDateTime endTime = LocalDateTime.parse(request.getEndTime() + " 23:59:59", DATE_FORMATTER);
                wrapper.le(OrderDO::getCreateTime, endTime);
            } catch (Exception ignored) {
            }
        }

        wrapper.orderByDesc(OrderDO::getCreateTime);

        Page<OrderDO> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        Map<Long, UserEntity> userMap = batchGetUsers(page);
        Map<Long, ProductDO> productMap = batchGetProducts(page);

        List<AdminOrderVO> records = page.getRecords().stream()
            .map(order -> toAdminOrderVO(order, userMap, productMap))
            .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailVO getOrderDetail(Long id) {
        OrderReadModel model = orderReadRepository.findById(new OrderId(id))
            .orElseThrow(() -> BusinessException.of("订单不存在"));

        UserEntity buyer = userMapper.selectById(model.buyerId());
        UserEntity seller = userMapper.selectById(model.sellerId());
        ProductDO product = productMapper.selectById(model.productId());

        return AdminOrderDetailVO.builder()
            .orderId(model.id())
            .orderNo(model.orderNo())
            .buyer(buyer != null ? new AdminOrderDetailVO.BuyerInfo(
                buyer.getId(), buyer.getNickName(), buyer.getAvatar(), buyer.getPhone()
            ) : new AdminOrderDetailVO.BuyerInfo(model.buyerId(), null, null, null))
            .seller(seller != null ? new AdminOrderDetailVO.SellerInfo(
                seller.getId(), seller.getNickName(), seller.getAvatar(), seller.getPhone()
            ) : new AdminOrderDetailVO.SellerInfo(model.sellerId(), null, null, null))
            .product(product != null ? new AdminOrderDetailVO.ProductInfo(
                product.getId(), product.getName(), null, product.getPrice()
            ) : new AdminOrderDetailVO.ProductInfo(model.productId(), null, null, null))
            .amount(model.amount())
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
    public OrderStatsVO getOrderStats() {
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
        long todayOrders = orderMapper.selectCount(
            new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getDelFlag, 0)
                .ge(OrderDO::getCreateTime, todayStart)
        );

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal todayRevenue = BigDecimal.ZERO;

        return OrderStatsVO.builder()
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

    private Map<Long, UserEntity> batchGetUsers(Page<OrderDO> orderPage) {
        Set<Long> userIds = new HashSet<>();
        orderPage.getRecords().forEach(o -> {
            userIds.add(o.getBuyerId());
            userIds.add(o.getSellerId());
        });
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(UserEntity::getId, u -> u, (a, b) -> a));
    }

    private Map<Long, ProductDO> batchGetProducts(Page<OrderDO> orderPage) {
        List<Long> productIds = orderPage.getRecords().stream()
            .map(OrderDO::getProductId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductDO> products = productMapper.selectBatchIds(productIds);
        return products.stream().collect(Collectors.toMap(ProductDO::getId, p -> p, (a, b) -> a));
    }

    private AdminOrderVO toAdminOrderVO(OrderDO order, Map<Long, UserEntity> userMap, Map<Long, ProductDO> productMap) {
        UserEntity buyer = userMap.get(order.getBuyerId());
        UserEntity seller = userMap.get(order.getSellerId());
        ProductDO product = productMap.get(order.getProductId());
        OrderStatus status = OrderStatus.fromCode(order.getStatus());

        return new AdminOrderVO(
            order.getId(),
            order.getOrderNo(),
            order.getBuyerId(),
            buyer != null ? buyer.getNickName() : null,
            order.getSellerId(),
            seller != null ? seller.getNickName() : null,
            order.getProductId(),
            product != null ? product.getName() : null,
            order.getAmount(),
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
