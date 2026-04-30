package com.cartethyia.easyorange.product.interfaces.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {

    private Long id;

    private String name;

    private Long parentId;

    private Integer level;

    private String icon;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;
}
