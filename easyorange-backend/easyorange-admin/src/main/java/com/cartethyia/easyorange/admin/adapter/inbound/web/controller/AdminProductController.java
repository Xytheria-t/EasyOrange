package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminProductAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminProductQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminProductResponse;
import com.cartethyia.easyorange.admin.application.service.AdminProductAppService;
import com.cartethyia.easyorange.admin.domain.model.DayRange;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductQueryCondition;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-商品", description = "商品管理")
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductAppService adminProductService;
    private final AdminProductAssembler assembler;

    @GetMapping
    @Operation(summary = "商品分页；按当页批量补主图与卖家 / 分类档案")
    public Result<PageResult<AdminProductResponse>> listProducts(AdminProductQueryRequest request) {
        return Result.success(assembler.toPageResponses(adminProductService.listProducts(toCondition(request))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情，含全部图片与卖家 / 分类档案")
    public Result<AdminProductResponse> getProductDetail(@PathVariable String id) {
        return Result.success(assembler.toDetailResponse(adminProductService.getProductDetail(id)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "改商品状态（ONLINE / OFFLINE / SOLD），不合法转换由 product 聚合拒绝")
    public Result<Void> updateProductStatus(@PathVariable String id, @Valid @RequestBody UpdateStatusRequest request) {
        adminProductService.updateProductStatus(id, request.status());
        return Result.success();
    }

    private static ProductQueryCondition toCondition(AdminProductQueryRequest request) {
        DayRange range = DayRange.of(request.startTime(), request.endTime());
        return new ProductQueryCondition(
                request.keyword(),
                request.categoryId(),
                request.status(),
                request.sellerId(),
                range.start(),
                range.end(),
                request.pageNum(),
                request.pageSize());
    }
}
