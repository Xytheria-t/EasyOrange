package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminProductQueryRequest(
    @Min(value = 1, message = "页码最小为1") Integer pageNum,
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100") Integer pageSize,
    String keyword,
    String categoryId,
    Integer status,
    String sellerId,
    String startTime,
    String endTime
) {}