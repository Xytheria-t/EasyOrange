package com.cartethyia.easyorange.product.application.port.query;

import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.List;

public record SearchResult(
        List<ProductReadModel> records,
        long total,
        int current,
        int size,
        List<FacetBucket> categoryFacets,
        List<FacetBucket> conditionFacets,
        List<FacetBucket> priceRangeFacets) {}
