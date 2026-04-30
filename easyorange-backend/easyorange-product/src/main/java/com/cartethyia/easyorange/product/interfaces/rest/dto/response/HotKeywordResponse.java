package com.cartethyia.easyorange.product.interfaces.rest.dto.response;

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
public class HotKeywordResponse {

    private Long id;

    private String keyword;

    private Integer searchCount;

    private Integer hotLevel;
}
