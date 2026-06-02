package com.cartethyia.easyorange.product.domain.port;

import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.List;
import java.util.Optional;

public interface AiSearchEnhancerPort {
    Optional<AiEnhancement> tryEnhance(String keyword, List<ProductReadModel> topProducts);
}
