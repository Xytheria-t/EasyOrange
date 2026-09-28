package com.cartethyia.easyorange.admin.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.admin.domain.model.CategoryView;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryWritePort.CategoryWriteResult;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 分类出参组装 — 服务只给 {@code domain} 视图 / 写侧结果，字段命名与树形递归归 web 边界。
 * <p>
 * 列表与树共用一个 DTO：树只是 {@code children} 非空的列表，单独养一套 {@code CategoryTreeResponse}
 * 会让「同一个分类两种 JSON」变成前端要维护的事实。
 */
@Component
public class AdminCategoryAssembler {

    public List<CategoryResponse> toResponses(List<CategoryView> views) {
        return views.stream().map(this::toResponse).toList();
    }

    public CategoryResponse toResponse(CategoryView view) {
        return new CategoryResponse(
                view.id(),
                view.name(),
                view.parentId(),
                view.parentName(),
                view.level(),
                view.sortOrder(),
                view.status(),
                view.productCount(),
                view.createTime(),
                view.children() == null ? List.of() : toResponses(view.children()));
    }

    /** 写侧不回传父分类名：新建/更新刚提交，为一个展示字段再查一次端口不值。 */
    public CategoryResponse toResponse(CategoryWriteResult result) {
        return new CategoryResponse(
                result.categoryId(),
                result.name(),
                result.parentId(),
                null,
                result.level(),
                result.sortOrder(),
                result.status(),
                result.productCount(),
                result.createTime(),
                List.of());
    }
}
