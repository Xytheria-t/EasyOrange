package com.cartethyia.easyorange.product.domain.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class HotKeyword {
    private Long id;
    private String keyword;
    private Integer searchCount;
    private LocalDateTime lastSearchTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}