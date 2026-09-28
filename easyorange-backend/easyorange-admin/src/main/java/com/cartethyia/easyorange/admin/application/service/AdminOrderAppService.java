package com.cartethyia.easyorange.admin.application.service;

import com.cartethyia.easyorange.admin.domain.exception.AdminDomainException;
import com.cartethyia.easyorange.admin.domain.model.OrderDetailView;
import com.cartethyia.easyorange.admin.domain.model.OrderListView;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderItemDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderItemInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderStats;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderSummary;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserInfo;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台订单查询 / 干预 — 出入参只用 {@code domain} 的端口记录与读模型，HTTP 形状由 web 侧 assembler 收口。
 * <p>
 * <b>取舍</b>：订单本体在 order 模块、昵称在 user 模块、商品名在 product 模块，列表要一次展示就得跨端口补数据。
 * 补齐在这里**整页批量**做（逐条回查就是 N+1），结果包成 {@link OrderListView} 一起返回 ——
 * 「这一页要补哪些关联」是业务事实，交给 web 层自己收集 id 等于把同一段逻辑复制一份。
 * <p>
 * <b>边界</b>：订单不存在抛 {@link AdminDomainException#orderNotFound}；越权与状态非法由 order 模块的
 * 领域异常决定，本类只翻译不吞。强制完成不落库原因，故 {@code reason} 只在 web 层做非空校验，不下发端口。
 */
@Service
@RequiredArgsConstructor
public class AdminOrderAppService {

    private final AdminOrderPort adminOrderPort;
    private final AdminUserPort adminUserPort;

    @Transactional(readOnly = true)
    public OrderListView listOrders(OrderQueryCondition condition) {
        OrderQueryResult page = adminOrderPort.queryOrders(condition);
        Map<String, List<OrderItemInfo>> items = loadOrderItems(page.records());
        return new OrderListView(page, loadUserInfos(page.records()), items, loadProductInfos(items));
    }

    @Transactional(readOnly = true)
    public OrderDetailView getOrderDetail(String id) {
        OrderDetail order = adminOrderPort.getOrderDetail(id);
        if (order == null) {
            throw AdminDomainException.orderNotFound(id);
        }
        List<String> productIds = order.items().stream()
                .map(OrderItemDetail::productId)
                .distinct()
                .toList();
        return new OrderDetailView(
                order,
                adminUserPort.getUserInfo(order.buyerId()),
                adminUserPort.getUserInfo(order.sellerId()),
                adminOrderPort.getProducts(productIds));
    }

    @Transactional(readOnly = true)
    public OrderStats getOrderStats() {
        return adminOrderPort.getOrderStats();
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String id, String reason) {
        adminOrderPort.cancelOrder(id, reason);
    }

    @Transactional(rollbackFor = Exception.class)
    public void forceComplete(String id, String reason) {
        adminOrderPort.forceComplete(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(String id, String reason) {
        adminOrderPort.refundOrder(id, reason);
    }

    private Map<String, UserInfo> loadUserInfos(List<OrderSummary> orders) {
        Set<String> userIds = new HashSet<>();
        orders.forEach(o -> {
            if (o.buyerId() != null) userIds.add(o.buyerId());
            if (o.sellerId() != null) userIds.add(o.sellerId());
        });
        return adminUserPort.getUserInfos(userIds.stream().toList());
    }

    private Map<String, List<OrderItemInfo>> loadOrderItems(List<OrderSummary> orders) {
        return adminOrderPort.getOrderItems(
                orders.stream().map(OrderSummary::id).toList());
    }

    private Map<String, ProductInfo> loadProductInfos(Map<String, List<OrderItemInfo>> items) {
        Set<String> productIds = items.values().stream()
                .flatMap(Collection::stream)
                .map(OrderItemInfo::productId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return adminOrderPort.getProducts(productIds.stream().toList());
    }
}
