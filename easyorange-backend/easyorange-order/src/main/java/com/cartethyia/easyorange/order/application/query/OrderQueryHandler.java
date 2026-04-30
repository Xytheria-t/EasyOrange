package com.cartethyia.easyorange.order.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.infrastructure.cache.OrderCacheService;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.repository.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.interfaces.dto.response.OrderVO;
import com.cartethyia.easyorange.order.enums.OrderResultCode;
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
    private final OrderCacheService orderCacheService;

    @Transactional(readOnly = true)
    public OrderVO getOrderDetail(Long orderId) {
        OrderReadModel order = orderReadRepository.findById(OrderId.of(orderId)).orElse(null);
        if (order == null) {
            return null;
        }
        List<OrderVO> voList = batchToOrderVOs(List.of(order));
        return voList.isEmpty() ? null : voList.getFirst();
    }

    @Transactional(readOnly = true)
    public OrderVO getOrderDetailForOwner(Long orderId) {
        OrderReadModel order = orderReadRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderDomainException(OrderResultCode.ORDER_NOT_FOUND));
        BizRequire.notNull(order, OrderResultCode.ORDER_NOT_FOUND);

        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.requireTrue(order.buyerId().equals(userId) || order.sellerId().equals(userId),
                OrderResultCode.ORDER_NOT_OWNER);

        return toOrderVOWithMask(order);
    }

    @Transactional(readOnly = true)
    public PageResult<OrderVO> handle(QueryOrderRequest request) {
        OrderQueryCondition condition = toCondition(request);
        PageResult<OrderReadModel> orderPage = orderReadRepository.findPage(condition);
        List<OrderVO> voList = batchToOrderVOs(orderPage.records());
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
        PageResult<OrderVO> cachedResult = orderCacheService.getOrderListCache(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
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
        List<OrderVO> voList = batchToOrderVOs(orderPage.records());
        return PageResult.of(voList, orderPage.total(),
                orderPage.current(), orderPage.size());
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

    private List<OrderVO> batchToOrderVOs(List<OrderReadModel> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        Set<Long> productIds = orders.stream()
                .map(OrderReadModel::productId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, ProductDetail> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<ProductDetail> products = productQueryPort.getProductsByIds(List.copyOf(productIds));
            if (products != null) {
                products.forEach(p -> productMap.put(p.id(), p));
            }
        }

        return orders.stream()
                .map(o -> toOrderVO(o, productMap))
                .collect(Collectors.toList());
    }

    private OrderVO toOrderVO(OrderReadModel order, Map<Long, ProductDetail> productMap) {
        OrderVO.OrderVOBuilder builder = OrderVO.builder()
                .id(order.id())
                .orderNo(order.orderNo())
                .buyerId(order.buyerId())
                .sellerId(order.sellerId())
                .productId(order.productId())
                .amount(order.amount())
                .status(order.status())
                .statusDesc(order.statusDesc())
                .address(MaskUtils.maskAddress(order.address(), 6))
                .phone(MaskUtils.maskPhone(order.phone()))
                .remark(order.remark())
                .createTime(order.createTime())
                .updateTime(order.updateTime());

        ProductDetail product = productMap.get(order.productId());
        if (product != null) {
            builder.productTitle(product.title());
            if (product.images() != null && !product.images().isEmpty()) {
                builder.productImage(product.images().getFirst());
            }
        }

        return builder.build();
    }

    private OrderVO toOrderVOWithMask(OrderReadModel order) {
        Map<Long, ProductDetail> productMap = new HashMap<>();
        if (order.productId() != null) {
            try {
                productQueryPort.getProductById(order.productId())
                        .ifPresent(p -> productMap.put(order.productId(), p));
            } catch (Exception e) {
                log.warn("action=get_product_failed productId={}", order.productId(), e);
            }
        }

        OrderVO.OrderVOBuilder builder = OrderVO.builder()
                .id(order.id())
                .orderNo(order.orderNo())
                .buyerId(order.buyerId())
                .sellerId(order.sellerId())
                .productId(order.productId())
                .amount(order.amount())
                .status(order.status())
                .statusDesc(order.statusDesc())
                .address(order.address())
                .phone(MaskUtils.maskPhone(order.phone()))
                .remark(order.remark())
                .createTime(order.createTime())
                .updateTime(order.updateTime());

        ProductDetail product = productMap.get(order.productId());
        if (product != null) {
            builder.productTitle(product.title());
            if (product.images() != null && !product.images().isEmpty()) {
                builder.productImage(product.images().getFirst());
            }
        }

        return builder.build();
    }
}
