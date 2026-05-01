package com.cartethyia.easyorange.order.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.infrastructure.cache.OrderCacheService;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.repository.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.interfaces.assembler.OrderVOAssembler;
import com.cartethyia.easyorange.order.interfaces.dto.response.OrderVO;
import com.cartethyia.easyorange.order.enums.OrderResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryHandler {

    private final OrderReadRepository orderReadRepository;
    private final ProductQueryPort productQueryPort;
    private final OrderCacheService orderCacheService;
    private final OrderVOAssembler orderVOAssembler;

    @Transactional(readOnly = true)
    public OrderVO getOrderDetail(Long orderId) {
        OrderReadModel order = orderReadRepository.findById(OrderId.of(orderId)).orElse(null);
        if (order == null) {
            return null;
        }
        Map<Long, ProductDetail> productMap = loadProducts(Set.of(order.productId()));
        return orderVOAssembler.toOrderVO(order, productMap, true);
    }

    @Transactional(readOnly = true)
    public OrderVO getOrderDetailForOwner(Long orderId) {
        OrderReadModel order = orderReadRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));
        BizRequire.notNull(order, OrderResultCode.ORDER_NOT_FOUND);

        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.requireTrue(order.buyerId().equals(userId) || order.sellerId().equals(userId),
                OrderResultCode.ORDER_NOT_OWNER);

        Map<Long, ProductDetail> productMap = loadProducts(Set.of(order.productId()));
        return orderVOAssembler.toOrderVO(order, productMap, false);
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO> handle(QueryOrderRequest request) {
        OrderQueryCondition condition = toCondition(request);
        PageResult<OrderReadModel> orderPage = orderReadRepository.findPage(condition);
        List<OrderVO> voList = assembleOrderVOs(orderPage.records());
        return PageResult.of(voList, orderPage.total(),
                orderPage.current(), orderPage.size());
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO> getMyOrders(QueryOrderRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return queryOrdersWithCache(request, userId, null);
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO> getSoldOrders(QueryOrderRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return queryOrdersWithCache(request, null, userId);
    }

    private PageResult<OrderVO> queryOrdersWithCache(QueryOrderRequest request, Long buyerId, Long sellerId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        String cacheKey = orderCacheService.buildOrderListKey(userId, request.getStatus());
        Optional<PageResult<OrderVO>> cachedResult = orderCacheService.getOrderListCache(cacheKey);
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        PageResult<OrderVO> result = queryOrdersByRole(request, buyerId, sellerId);
        orderCacheService.setOrderListCache(cacheKey, result);
        return result;
    }

    private PageResult<OrderVO> queryOrdersByRole(QueryOrderRequest request,
                                                  Long buyerId,
                                                  Long sellerId) {
        QueryOrderRequest normalized = request.normalized();
        OrderQueryCondition condition = new OrderQueryCondition(
                request.getOrderNo(),
                request.getStatus(),
                buyerId,
                sellerId,
                request.getProductId(),
                normalized.getPageNum(),
                normalized.getPageSize()
        );

        PageResult<OrderReadModel> orderPage = orderReadRepository.findPage(condition);
        List<OrderVO> voList = assembleOrderVOs(orderPage.records());
        return PageResult.of(voList, orderPage.total(),
                orderPage.current(), orderPage.size());
    }

    private List<OrderVO> assembleOrderVOs(List<OrderReadModel> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        Set<Long> productIds = orders.stream()
                .map(OrderReadModel::productId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        Map<Long, ProductDetail> productMap = loadProducts(productIds);
        return orderVOAssembler.toOrderVOs(orders, productMap);
    }

    private Map<Long, ProductDetail> loadProducts(Set<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        List<ProductDetail> products = productQueryPort.getProductsByIds(List.copyOf(productIds));
        return orderVOAssembler.buildProductMap(products);
    }

    private OrderQueryCondition toCondition(QueryOrderRequest request) {
        QueryOrderRequest normalized = request.normalized();
        return new OrderQueryCondition(
                request.getOrderNo(),
                request.getStatus(),
                request.getBuyerId(),
                request.getSellerId(),
                request.getProductId(),
                normalized.getPageNum(),
                normalized.getPageSize()
        );
    }
}
