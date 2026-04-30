package com.cartethyia.easyorange.product.domain.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SearchHistory {
    private Long id;
    private Long userId;
    private String keyword;
    private LocalDateTime searchTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}