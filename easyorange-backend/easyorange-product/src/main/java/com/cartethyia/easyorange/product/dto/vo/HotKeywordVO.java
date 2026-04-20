package com.cartethyia.easyorange.product.dto.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HotKeywordVO {

    private Long id;

    private String keyword;

    private Integer searchCount;

    private Integer hotLevel;
}
