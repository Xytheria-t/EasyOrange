package com.cartethyia.easyorange.order.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.dto.request.QueryOrderRequest;
import com.cartethyia.easyorange.order.dto.vo.OrderVO;
import com.cartethyia.easyorange.order.entity.Order;
import com.cartethyia.easyorange.order.enums.OrderResultCode;
import com.cartethyia.easyorange.order.enums.OrderStatus;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import com.cartethyia.easyorange.product.application.handler.ProductQueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryHandler {

    private final OrderReadRepository orderReadRepository;
    private final ProductQueryHandler productQueryHandler;
    private final com.cartethyia.easyorange.order.application.cache.OrderCacheService orderCacheService;

    @Transactional(readOnly = true)
    public OrderVO getOrderDetail(Long orderId) {
        Order order = orderReadRepository.findById(orderId).orElse(null);
        if (order == null) {
            return null;
        }
        List<OrderVO> voList = batchToOrderVOs(List.of(order));
        return voList.isEmpty() ? null : voList.getFirst();
    }

    @Transactional(readOnly = true)
    public OrderVO getOrderDetailForOwner(Long orderId) {
        Order order = orderReadRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        BizRequire.notNull(order, OrderResultCode.ORDER_NOT_FOUND);

        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.isTrue(order.getBuyerId().equals(userId) || order.getSellerId().equals(userId),
                OrderResultCode.ORDER_NOT_OWNER);

        return toOrderVOWithMask(order);
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO> handle(QueryOrderRequest request) {
        PageResult<Order> orderPage = orderReadRepository.findPage(request);
        List<OrderVO> voList = batchToOrderVOs(orderPage.records());
        return PageResult.of(voList, orderPage.total(),
                orderPage.current(), orderPage.size());
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO> getMyOrders(QueryOrderRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        
        String cacheKey = orderCacheService.buildOrderListKey(userId, request.getStatus());
        PageResult<OrderVO> cachedResult = orderCacheService.getOrderListCache(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        PageResult<OrderVO> result = queryOrdersByRole(request, Order::getBuyerId, userId);
        orderCacheService.setOrderListCache(cacheKey, result);
        return result;
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO> getSoldOrders(QueryOrderRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        
        String cacheKey = orderCacheService.buildOrderListKey(userId, request.getStatus());
        PageResult<OrderVO> cachedResult = orderCacheService.getOrderListCache(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        PageResult<OrderVO> result = queryOrdersByRole(request, Order::getSellerId, userId);
        orderCacheService.setOrderListCache(cacheKey, result);
        return result;
    }

    private PageResult<OrderVO> queryOrdersByRole(QueryOrderRequest request,
                                                  com.baomidou.mybatisplus.core.toolkit.support.SFunction<Order, Long> roleField,
                                                  Long userId) {
        QueryOrderRequest roleRequest = QueryOrderRequest.builder()
                .orderNo(request.getOrderNo())
                .status(request.getStatus())
                .productId(request.getProductId())
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .build();

        PageResult<Order> orderPage = orderReadRepository.findPage(roleRequest);
        List<OrderVO> voList = batchToOrderVOs(orderPage.records());
        return PageResult.of(voList, orderPage.total(),
                orderPage.current(), orderPage.size());
    }

    private List<OrderVO> batchToOrderVOs(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        Set<Long> productIds = orders.stream()
                .map(Order::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, ProductVO> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<ProductVO> products = productQueryHandler.getProductsByIds(List.copyOf(productIds));
            if (products != null) {
                products.forEach(p -> productMap.put(p.getId(), p));
            }
        }

        return orders.stream()
                .map(o -> toOrderVO(o, productMap))
                .collect(Collectors.toList());
    }

    private OrderVO toOrderVO(Order order, Map<Long, ProductVO> productMap) {
        OrderVO.OrderVOBuilder builder = OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .buyerId(order.getBuyerId())
                .sellerId(order.getSellerId())
                .productId(order.getProductId())
                .amount(order.getAmount())
                .status(order.getStatus())
                .statusDesc(OrderStatus.getDescByCode(order.getStatus()))
                .address(order.getAddress())
                .phone(order.getPhone())
                .remark(order.getRemark())
                .createTime(order.getCreateTime())
                .updateTime(order.getUpdateTime());

        ProductVO product = productMap.get(order.getProductId());
        if (product != null) {
            builder.productTitle(product.getTitle());
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                builder.productImage(product.getImages().getFirst());
            }
        }

        return builder.build();
    }

    private OrderVO toOrderVOWithMask(Order order) {
        Map<Long, ProductVO> productMap = new HashMap<>();
        if (order.getProductId() != null) {
            try {
                ProductVO product = productQueryHandler.getProductById(order.getProductId());
                if (product != null) {
                    productMap.put(order.getProductId(), product);
                }
            } catch (Exception e) {
                log.warn("action=get_product_failed productId={}", order.getProductId(), e);
            }
        }

        OrderVO.OrderVOBuilder builder = OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .buyerId(order.getBuyerId())
                .sellerId(order.getSellerId())
                .productId(order.getProductId())
                .amount(order.getAmount())
                .status(order.getStatus())
                .statusDesc(OrderStatus.getDescByCode(order.getStatus()))
                .address(order.getAddress())
                .phone(MaskUtils.maskPhone(order.getPhone()))
                .remark(order.getRemark())
                .createTime(order.getCreateTime())
                .updateTime(order.getUpdateTime());

        ProductVO product = productMap.get(order.getProductId());
        if (product != null) {
            builder.productTitle(product.getTitle());
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                builder.productImage(product.getImages().getFirst());
            }
        }

        return builder.build();
    }
}
