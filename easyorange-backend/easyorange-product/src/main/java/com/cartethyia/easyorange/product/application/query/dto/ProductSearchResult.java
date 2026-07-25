package com.cartethyia.easyorange.product.application.query.dto;

import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.domain.port.FacetBucket;

import java.util.List;

/**
 * 商品搜索结果 —— 应用层输出，不依赖 adapter DTO。
 *
 * @param page           分页结果（含 records / total / current / size / pages）
 * @param facets         分面聚合桶（来自 ES 搜索）
 * @param aiEnhancement  AI 搜索结果增强（可选）
 */
public record ProductSearchResult(
    PageResult<ProductReadModel> page,
    List<FacetBucket> facets,
    AiEnhancement aiEnhancement
) {}
