package com.cartethyia.easyorange.product.domain.port;

import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.List;

public record SearchResult(
    List<ProductReadModel> records,
    long total,
    int pageNum,
    int pageSize,
    List<FacetBucket> categoryFacets,
    List<FacetBucket> conditionFacets,
    List<FacetBucket> priceRangeFacets
) { }