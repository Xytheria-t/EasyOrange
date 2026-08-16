package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.QueryOrderRequest;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.application.query.OrderListQuery;
import com.cartethyia.easyorange.order.application.query.OrderQueryHandler;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理", description = "订单查询")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderQueryController {

    private final OrderQueryHandler queryHandler;

    @GetMapping("/owned/{id}")
    public Result<OrderVO> getOrderDetail(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        return Result.success(queryHandler.getOrderDetailForOwner(user.userId(), id));
    }

    @GetMapping("/my")
    public Result<PageResult<OrderVO>> getMyOrders(
            @AuthenticationPrincipal AuthUser user, @Valid QueryOrderRequest request) {
        return Result.success(queryHandler.getMyOrders(user.userId(), toScopedListQuery(request)));
    }

    @GetMapping("/sold")
    public Result<PageResult<OrderVO>> getSoldOrders(
            @AuthenticationPrincipal AuthUser user, @Valid QueryOrderRequest request) {
        return Result.success(queryHandler.getSoldOrders(user.userId(), toScopedListQuery(request)));
    }

    /** my/sold 场景：丢弃请求中的 buyerId/sellerId，用户 scope 由 application 层按当前登录人填充。 */
    private static OrderListQuery toScopedListQuery(QueryOrderRequest request) {
        OrderStatus status = resolveStatus(request.getStatus());
        return new OrderListQuery(
                request.getOrderNo(), status, null, null, request.getPageNum(), request.getPageSize());
    }

    /**
     * 边界层 String code → OrderStatus 转换，blank 视为 null（查询全部状态）。
     * 非法 code 由 {@link OrderStatus#fromCode(String)} 抛 IllegalArgumentException，由全局异常处理器映射为 400。
     */
    private static OrderStatus resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return OrderStatus.fromCode(status);
    }
}
