package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;

public record CategoryResponse(
    String categoryId,
    String name,
    String parentId,
    String parentName,
    Integer level,
    Integer sortOrder,
    Integer status,
    Long productCount,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {}