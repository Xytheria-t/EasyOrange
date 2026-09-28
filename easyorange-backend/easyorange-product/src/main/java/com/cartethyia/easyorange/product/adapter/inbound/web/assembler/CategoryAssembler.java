package com.cartethyia.easyorange.product.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CategoryAssembler {

    public List<CategoryResponse> toCategoryResponses(List<CategoryReadModel> readModels) {
        if (readModels == null || readModels.isEmpty()) {
            return List.of();
        }

        return readModels.stream().map(this::toCategoryResponse).toList();
    }

    private CategoryResponse toCategoryResponse(CategoryReadModel model) {
        return new CategoryResponse(
                model.id(),
                model.name(),
                model.parentId(),
                model.level(),
                model.icon(),
                model.sortOrder(),
                statusCode(model.status()),
                model.createTime(),
                model.productCount());
    }

    /** 领域枚举 → 对外契约的 0/1（枚举的 {@code @JsonValue} 会序列化成字符串，两侧契约须一致）。 */
    private static Integer statusCode(CategoryStatus status) {
        return status != null && status.isEnabled() ? 1 : 0;
    }
}
