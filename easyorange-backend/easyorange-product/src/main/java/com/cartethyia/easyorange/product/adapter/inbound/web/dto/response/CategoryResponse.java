package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CategoryResponse {

    private String id;

    private String name;

    private String parentId;

    private Integer level;

    private String icon;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;

    private Integer productCount;

    private List<CategoryResponse> children;
}
