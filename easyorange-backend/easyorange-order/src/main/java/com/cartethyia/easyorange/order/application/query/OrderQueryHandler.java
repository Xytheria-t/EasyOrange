package com.cartethyia.easyorange.order.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.port.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.application.query.assembler.OrderReadModelAssembler;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
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
    private final ProductQueryPort productQueryPort;
    private final OrderCachePort<OrderVO> orderCachePort;
    private final OrderReadModelAssembler readModelAssembler;

    @Transactional(readOnly = true)
    public OrderVO getOrderDetail(String orderId) {
        OrderReadModel order = orderReadRepository.findById(OrderId.of(orderId)).orElse(null);
        if (order == null) {
            return null;
        }
        Map<String, ProductDetail> productMap = loadProductMap(order);
        return readModelAssembler.toOrderVO(order, productMap, true);
    }

    @Transactional(readOnly = true)
    public OrderVO getOrderDetailForOwner(String orderId) {
        OrderReadModel order = orderReadRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));
        BizRequire.notNull(order, OrderResultCode.ORDER_NOT_FOUND);

        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.requireTrue(order.buyerId().equals(userId) || order.sellerId().equals(userId),
                OrderResultCode.ORDER_NOT_OWNER);

        Map<String, ProductDetail> productMap = loadProductMap(order);
        return readModelAssembler.toOrderVO(order, productMap, false);
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO> handle(String orderNo, Integer status, String buyerId, String sellerId,
                                       Integer pageNum, Integer pageSize) {
        int effectivePageNum = pageNum != null ? pageNum : 1;
        int effectivePageSize = pageSize != null ? pageSize : 20;
        OrderQueryCondition condition = new OrderQueryCondition(orderNo, status, buyerId, sellerId,
                effectivePageNum, effectivePageSize);
        PageResult<OrderReadModel> orderPage = orderReadRepository.findPage(condition);
        List<OrderVO> voList = assembleOrderVOs(orderPage.records());
        return PageResult.of(voList, orderPage.total(),
                orderPage.current(), orderPage.size());
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO> getMyOrders(Integer status, Integer pageNum, Integer pageSize) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return queryOrdersWithCache(userId, null, status, pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO> getSoldOrders(Integer status, Integer pageNum, Integer pageSize) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return queryOrdersWithCache(null, userId, status, pageNum, pageSize);
    }

    private PageResult<OrderVO> queryOrdersWithCache(String buyerId, String sellerId, Integer status,
                                                       Integer pageNum, Integer pageSize) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        String cacheKey = orderCachePort.buildOrderListKey(userId, String.valueOf(status));
        Optional<PageResult<OrderVO>> cachedResult = orderCachePort.getOrderList(cacheKey);
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        PageResult<OrderVO> result = queryOrdersByRole(status, buyerId, sellerId, pageNum, pageSize);
        orderCachePort.putOrderList(cacheKey, result);
        return result;
    }

    private PageResult<OrderVO> queryOrdersByRole(Integer status, String buyerId, String sellerId,
                                                    Integer pageNum, Integer pageSize) {
        int effectivePageNum = pageNum != null ? pageNum : 1;
        int effectivePageSize = pageSize != null ? pageSize : 20;
        OrderQueryCondition condition = new OrderQueryCondition(null, status, buyerId, sellerId,
                effectivePageNum, effectivePageSize);

        PageResult<OrderReadModel> orderPage = orderReadRepository.findPage(condition);
        List<OrderVO> voList = assembleOrderVOs(orderPage.records());
        return PageResult.of(voList, orderPage.total(),
                orderPage.current(), orderPage.size());
    }

    private List<OrderVO> assembleOrderVOs(List<OrderReadModel> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        Set<String> productIds = orders.stream()
                .flatMap(o -> o.items().stream())
                .map(OrderItemReadModel::productId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, ProductDetail> productMap = loadProducts(productIds);
        return readModelAssembler.toOrderVOs(orders, productMap);
    }

    private Map<String, ProductDetail> loadProducts(Set<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        List<ProductDetail> products = productQueryPort.getProductsByIds(List.copyOf(productIds));
        return readModelAssembler.buildProductMap(products);
    }

    private Map<String, ProductDetail> loadProductMap(OrderReadModel order) {
        Set<String> productIds = order.items().stream()
                .map(OrderItemReadModel::productId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return loadProducts(productIds);
    }
}