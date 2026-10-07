package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminOrderAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminOrderQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.OrderInterventionRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderDetailResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminOrderResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.OrderStatsResponse;
import com.cartethyia.easyorange.admin.application.service.AdminOrderAppService;
import com.cartethyia.easyorange.admin.domain.model.DayRange;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderQueryCondition;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 后台订单接口 — 查询串的拆解与出参组装都止步于 web 层，服务只见 {@code domain} 记录。
 */
@Tag(name = "管理后台-订单", description = "订单管理")
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderAppService adminOrderService;
    private final AdminOrderAssembler assembler;

    @GetMapping
    @Operation(summary = "订单分页；按当页批量补买家 / 卖家昵称、订单项与商品名")
    public Result<PageResult<AdminOrderResponse>> listOrders(AdminOrderQueryRequest request) {
        return Result.success(assembler.toPageResponses(adminOrderService.listOrders(toCondition(request))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情拼买卖双方与商品；支付字段取自支付单，未发起支付时为 null")
    public Result<AdminOrderDetailResponse> getOrderDetail(@PathVariable String id) {
        return Result.success(assembler.toDetailResponse(adminOrderService.getOrderDetail(id)));
    }

    @GetMapping("/stats")
    @Operation(summary = "各状态订单数与营收；营收口径为支付单 SUCCESS 金额合计")
    public Result<OrderStatsResponse> getOrderStats() {
        return Result.success(assembler.toStatsResponse(adminOrderService.getOrderStats()));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单（PENDING_PAYMENT 取消 / PAID 强制取消），reason 落库")
    public Result<Void> cancelOrder(@PathVariable String id, @Valid @RequestBody OrderInterventionRequest request) {
        adminOrderService.cancelOrder(id, request.reason());
        return Result.success();
    }

    @PutMapping("/{id}/force-complete")
    @Operation(summary = "强制完成订单（确认收货）；reason 只校验不落库，不合法状态由 order 侧拒绝")
    public Result<Void> forceComplete(@PathVariable String id, @Valid @RequestBody OrderInterventionRequest request) {
        // 原因只做入参校验：order 侧没有「强制完成」的原因字段可写，下发了也无处安放
        adminOrderService.forceComplete(id, request.reason());
        return Result.success();
    }

    @PutMapping("/{id}/refund")
    @Operation(summary = "订单退款；reason 落库，合法状态由 order 侧领域异常裁决")
    public Result<Void> refundOrder(@PathVariable String id, @Valid @RequestBody OrderInterventionRequest request) {
        adminOrderService.refundOrder(id, request.reason());
        return Result.success();
    }

    private static OrderQueryCondition toCondition(AdminOrderQueryRequest request) {
        DayRange range = DayRange.of(request.startTime(), request.endTime());
        return new OrderQueryCondition(
                request.orderNo(),
                request.buyerId(),
                request.sellerId(),
                request.status(),
                request.paymentStatus(),
                range.start(),
                range.end(),
                request.pageNum(),
                request.pageSize());
    }
}
