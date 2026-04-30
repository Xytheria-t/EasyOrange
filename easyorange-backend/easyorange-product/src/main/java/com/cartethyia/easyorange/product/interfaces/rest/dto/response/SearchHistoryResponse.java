package com.cartethyia.easyorange.product.interfaces.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchHistoryResponse {

    private Long id;

    private String keyword;

    private Integer searchCount;

    private LocalDateTime createTime;
}
