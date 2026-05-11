package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CategoryVO {

    private Long categoryId;

    private String name;

    private Long parentId;

    private String parentName;

    private Integer level;

    private Integer sortOrder;

    private Integer status;

    private Long productCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
