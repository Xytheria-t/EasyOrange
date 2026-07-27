package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.application.query.OrderListQuery;
import com.cartethyia.easyorange.order.application.query.OrderQueryHandler;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.QueryOrderRequest;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理", description = "订单查询")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderQueryController {

    private final OrderQueryHandler queryHandler;

    @GetMapping("/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable String id) {
        return Result.success(queryHandler.getOrderDetailForOwner(id));
    }

    @GetMapping("/my")
    public Result<PageResult<OrderVO>> getMyOrders(@Valid QueryOrderRequest request) {
        return Result.success(queryHandler.getMyOrders(toListQuery(request, true)));
    }

    @GetMapping("/sold")
    public Result<PageResult<OrderVO>> getSoldOrders(@Valid QueryOrderRequest request) {
        return Result.success(queryHandler.getSoldOrders(toListQuery(request, false)));
    }

    @GetMapping("/list")
    public Result<PageResult<OrderVO>> queryOrders(@Valid QueryOrderRequest request) {
        return Result.success(queryHandler.listOrders(toListQuery(request, false)));
    }

    /**
     * 将 HTTP 入参 QueryOrderRequest 转换为 application 层 OrderListQuery。
     * <p>
     * String status code 在此边界转换为 {@link OrderStatus} 枚举，类型安全下沉到 application/domain 层。
     * 非法 code 由 {@link OrderStatus#fromCode(String)} 抛出 IllegalArgumentException，
     * 由全局异常处理器映射为 400 响应。
     *
     * @param skipUserScope true 表示 buyerId/sellerId 由 service 层根据登录用户填充（my/sold 场景），
     *                      false 表示直接透传 request 中的 buyerId/sellerId（list 场景）
     */
    private static OrderListQuery toListQuery(QueryOrderRequest request, boolean skipUserScope) {
        OrderStatus status = resolveStatus(request.getStatus());
        String buyerId = skipUserScope ? null : request.getBuyerId();
        String sellerId = skipUserScope ? null : request.getSellerId();
        return new OrderListQuery(
                request.getOrderNo(), status, buyerId, sellerId,
                request.getPageNum(), request.getPageSize());
    }

    /** 边界层 String code → OrderStatus 转换，blank 视为 null（查询全部状态）。 */
    private static OrderStatus resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return OrderStatus.fromCode(status);
    }
}
