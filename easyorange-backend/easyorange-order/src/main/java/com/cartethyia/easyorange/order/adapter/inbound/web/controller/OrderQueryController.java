package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.QueryOrderRequest;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.application.query.OrderListQuery;
import com.cartethyia.easyorange.order.application.query.OrderQueryHandler;
import com.cartethyia.easyorange.order.domain.enums.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "订单详情，仅买家或卖家本人可见（他人按越权拒绝）")
    public Result<OrderVO> getOrderDetail(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        return Result.success(queryHandler.getOrderDetailForOwner(user.userId(), id));
    }

    @GetMapping("/my")
    @Operation(summary = "我买到的订单列表（buyerId 取登录人，orderNo 过滤绕过缓存）")
    public Result<PageResult<OrderVO>> getMyOrders(
            @AuthenticationPrincipal AuthUser user, @Valid QueryOrderRequest request) {
        return Result.success(queryHandler.getMyOrders(user.userId(), toScopedListQuery(request)));
    }

    @GetMapping("/sold")
    @Operation(summary = "我售出的订单列表（sellerId 取登录人，忽略入参买卖家）")
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
