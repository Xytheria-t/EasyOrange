package com.cartethyia.easyorange.order.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.application.port.query.OrderQueryRepository;
import com.cartethyia.easyorange.order.application.query.assembler.OrderReadModelAssembler;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.port.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.domain.port.UserInfoPort;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryHandler {

    private final OrderQueryRepository orderReadRepository;
    private final ProductQueryPort productQueryPort;
    private final OrderCachePort<OrderVO> orderCachePort;
    private final OrderReadModelAssembler readModelAssembler;
    private final UserInfoPort userInfoPort;

    public OrderVO getOrderDetailForOwner(String userId, String orderId) {
        OrderReadModel order = orderReadRepository
                .findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));

        if (!order.buyerId().equals(userId) && !order.sellerId().equals(userId)) {
            throw new OrderDomainException(OrderResultCode.ORDER_NOT_OWNER);
        }

        Map<String, ProductDetail> productMap = loadProductMap(order);
        Map<String, String> usernames = userInfoPort.findUsernames(Set.of(order.buyerId(), order.sellerId()));
        return readModelAssembler.toOrderVO(order, productMap, usernames, false);
    }

    /**
     * 我的订单（认领方视角） — buyerId 自动填充为当前登录用户。
     */
    public PageResult<OrderVO> getMyOrders(String userId, OrderListQuery query) {
        return queryOrdersWithCache(userId, null, query);
    }

    /**
     * 我售出的订单（资产方视角） — sellerId 自动填充为当前登录用户。
     */
    public PageResult<OrderVO> getSoldOrders(String userId, OrderListQuery query) {
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
            return queryPage(buyerId, sellerId, query);
        }

        String statusCode = query.status() != null ? query.status().getCode() : null;
        String cacheKey = orderCachePort.buildOrderListKey(userId, statusCode, query.pageNum(), query.pageSize());
        Optional<PageResult<OrderVO>> cachedResult = orderCachePort.getOrderList(cacheKey);
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        PageResult<OrderVO> result = queryPage(buyerId, sellerId, query);
        orderCachePort.putOrderList(cacheKey, result);
        return result;
    }

    private PageResult<OrderVO> queryPage(String buyerId, String sellerId, OrderListQuery query) {
        OrderQueryCondition condition = new OrderQueryCondition(
                query.orderNo(), query.status(), buyerId, sellerId, query.pageNum(), query.pageSize());
        PageResult<OrderReadModel> orderPage = orderReadRepository.findPage(condition);
        List<OrderVO> voList = assembleOrderVOs(orderPage.records());
        return PageResult.of(voList, orderPage.total(), orderPage.current(), orderPage.size());
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
        Set<String> userIds = orders.stream()
                .flatMap(o -> java.util.stream.Stream.of(o.buyerId(), o.sellerId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, ProductDetail> productMap = loadProducts(productIds);
        Map<String, String> usernames = userInfoPort.findUsernames(userIds);
        return readModelAssembler.toOrderVOs(orders, productMap, usernames);
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
