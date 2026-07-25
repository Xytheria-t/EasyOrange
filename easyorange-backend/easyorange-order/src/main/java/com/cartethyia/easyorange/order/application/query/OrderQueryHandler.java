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
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.application.query.assembler.OrderReadModelAssembler;
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

        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.requireTrue(
                order.buyerId().equals(userId) || order.sellerId().equals(userId),
                OrderResultCode.ORDER_NOT_OWNER);

        Map<String, ProductDetail> productMap = loadProductMap(order);
        return readModelAssembler.toOrderVO(order, productMap, false);
    }

    /**
     * 订单列表查询（管理端 / 通用） — 通过 OrderListQuery 收敛 6 个参数。
     */
    @Transactional(readOnly = true)
    public PageResult<OrderVO> listOrders(OrderListQuery query) {
        OrderQueryCondition condition = new OrderQueryCondition(
                query.orderNo(), query.status(), query.buyerId(), query.sellerId(),
                query.pageNum(), query.pageSize());
        PageResult<OrderReadModel> orderPage = orderReadRepository.findPage(condition);
        List<OrderVO> voList = assembleOrderVOs(orderPage.records());
        return PageResult.of(voList, orderPage.total(),
                orderPage.current(), orderPage.size());
    }

    /**
     * 我的订单（认领方视角） — 通过 OrderListQuery 收敛参数，与 listOrders 入口统一。
     * buyerId 自动填充为当前登录用户。
     */
    @Transactional(readOnly = true)
    public PageResult<OrderVO> getMyOrders(OrderListQuery query) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return queryOrdersWithCache(userId, null, query);
    }

    /**
     * 我售出的订单（资产方视角） — 通过 OrderListQuery 收敛参数，与 listOrders 入口统一。
     * sellerId 自动填充为当前登录用户。
     */
    @Transactional(readOnly = true)
    public PageResult<OrderVO> getSoldOrders(OrderListQuery query) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return queryOrdersWithCache(null, userId, query);
    }

    private PageResult<OrderVO> queryOrdersWithCache(String buyerId, String sellerId, OrderListQuery query) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String statusCode = query.status() != null ? query.status().getCode() : null;
        String cacheKey = orderCachePort.buildOrderListKey(userId, statusCode);

        Optional<PageResult<OrderVO>> cachedResult = orderCachePort.getOrderList(cacheKey);
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        OrderListQuery effectiveQuery = new OrderListQuery(
                query.orderNo(), query.status(), buyerId, sellerId,
                query.pageNum(), query.pageSize());
        PageResult<OrderVO> result = listOrders(effectiveQuery);
        orderCachePort.putOrderList(cacheKey, result);
        return result;
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
