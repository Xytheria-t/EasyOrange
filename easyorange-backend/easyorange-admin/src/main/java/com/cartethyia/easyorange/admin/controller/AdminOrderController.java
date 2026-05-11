package com.cartethyia.easyorange.admin.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.admin.dto.request.AdminOrderQueryRequest;
import com.cartethyia.easyorange.admin.dto.request.OrderInterventionRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminOrderDetailVO;
import com.cartethyia.easyorange.admin.dto.response.AdminOrderVO;
import com.cartethyia.easyorange.admin.dto.response.OrderStatsVO;
import com.cartethyia.easyorange.admin.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public Result<PageResult<AdminOrderVO>> listOrders(AdminOrderQueryRequest request) {
        return Result.success(adminOrderService.listOrders(request));
    }

    @GetMapping("/{id}")
    public Result<AdminOrderDetailVO> getOrderDetail(@PathVariable Long id) {
        return Result.success(adminOrderService.getOrderDetail(id));
    }

    @GetMapping("/stats")
    public Result<OrderStatsVO> getOrderStats() {
        return Result.success(adminOrderService.getOrderStats());
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(
        @PathVariable Long id,
        @Valid @RequestBody OrderInterventionRequest request
    ) {
        adminOrderService.cancelOrder(id, request.getReason());
        return Result.success();
    }

    @PutMapping("/{id}/force-complete")
    public Result<Void> forceComplete(
        @PathVariable Long id,
        @Valid @RequestBody OrderInterventionRequest request
    ) {
        adminOrderService.forceComplete(id, request.getReason());
        return Result.success();
    }

    @PutMapping("/{id}/refund")
    public Result<Void> refundOrder(
        @PathVariable Long id,
        @Valid @RequestBody OrderInterventionRequest request
    ) {
        adminOrderService.refundOrder(id, request.getReason());
        return Result.success();
    }
}
