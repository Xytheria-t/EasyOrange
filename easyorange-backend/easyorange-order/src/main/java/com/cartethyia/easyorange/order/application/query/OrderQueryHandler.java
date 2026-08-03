package com.cartethyia.easyorange.order.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    public OrderVO getOrderDetailForOwner(String orderId) {
        OrderReadModel order = orderReadRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));

        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        if (!order.buyerId().equals(userId) && !order.sellerId().equals(userId)) {
            throw new OrderDomainException(OrderResultCode.ORDER_NOT_OWNER);
        }

        Map<String, ProductDetail> productMap = loadProductMap(order);
        return readModelAssembler.toOrderVO(order, productMap, false);
    }

    /**
     * 订单列表查询（管理端 / 通用） — 通过 OrderListQuery 收敛 6 个参数。
     * Controller 通过代理调用本方法，事务注解生效。
     */
    @Transactional(readOnly = true)
    public PageResult<OrderVO> listOrders(OrderListQuery query) {
        return doListOrders(query);
    }

    /** listOrders 的实际实现 — 供同对象内部调用，避免 @Transactional 自调用失效。 */
    private PageResult<OrderVO> doListOrders(OrderListQuery query) {
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

    /**
     * 带缓存的当前用户订单列表查询。
     * <p>
     * 缓存 key 由当前用户 + 状态 + 分页构成，覆盖所有影响结果集的参数；
     * orderNo 为精确过滤、无法纳入 key，直接绕过缓存避免跨查询污染。
     */
    private PageResult<OrderVO> queryOrdersWithCache(String buyerId, String sellerId, OrderListQuery query) {
        String userId = buyerId != null ? buyerId : sellerId;

        if (query.orderNo() != null && !query.orderNo().isBlank()) {
            return doListOrders(withUserScope(query, buyerId, sellerId));
        }

        String statusCode = query.status() != null ? query.status().getCode() : null;
        String cacheKey = orderCachePort.buildOrderListKey(userId, statusCode, query.pageNum(), query.pageSize());
        Optional<PageResult<OrderVO>> cachedResult = orderCachePort.getOrderList(cacheKey);
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        PageResult<OrderVO> result = doListOrders(withUserScope(query, buyerId, sellerId));
        orderCachePort.putOrderList(cacheKey, result);
        return result;
    }

    /** 注入当前用户视角的 buyerId/sellerId 到查询条件。 */
    private OrderListQuery withUserScope(OrderListQuery query, String buyerId, String sellerId) {
        return new OrderListQuery(
                query.orderNo(), query.status(), buyerId, sellerId,
                query.pageNum(), query.pageSize());
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
