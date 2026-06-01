package com.cartethyia.easyorange.product.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryAssembler {

    public List<CategoryResponse> toCategoryResponses(List<CategoryReadModel> readModels) {
        if (readModels == null || readModels.isEmpty()) {
            return List.of();
        }

        return readModels.stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    private CategoryResponse toCategoryResponse(CategoryReadModel model) {
        return CategoryResponse.builder()
                .id(model.id())
                .name(model.name())
                .parentId(model.parentId())
                .level(model.level())
                .icon(model.icon())
                .sortOrder(model.sortOrder())
                .status(model.status())
                .createTime(model.createTime())
                .productCount(model.productCount())
                .build();
    }
}