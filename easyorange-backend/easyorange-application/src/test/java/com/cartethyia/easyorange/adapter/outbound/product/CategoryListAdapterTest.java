package com.cartethyia.easyorange.adapter.outbound.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.domain.model.CategorySummary;
import com.cartethyia.easyorange.product.application.query.CategoryQueryHandler;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryListAdapter 测试")
class CategoryListAdapterTest {

    @Mock
    private CategoryQueryHandler categoryQueryHandler;

    @Test
    @DisplayName("收敛成公开字段：只留 id/名称/层级/在售数，内部管理字段不带出")
    void list_mapsToPublicSummary() {
        when(categoryQueryHandler.getCategories(null)).thenReturn(List.of(readModel("c1", "数码", 1, 12)));

        var result = new CategoryListAdapter(categoryQueryHandler).list(null);

        assertThat(result).containsExactly(new CategorySummary("c1", "数码", 1, 12));
    }

    @Test
    @DisplayName("level 缺失按 0 兜底，不因拆箱抛 NPE")
    void list_nullLevelDefaultsToZero() {
        when(categoryQueryHandler.getCategories("c1")).thenReturn(List.of(readModel("c2", "显卡", null, 3)));

        var result = new CategoryListAdapter(categoryQueryHandler).list("c1");

        assertThat(result).containsExactly(new CategorySummary("c2", "显卡", 0, 3));
    }

    private CategoryReadModel readModel(String id, String name, Integer level, int productCount) {
        return new CategoryReadModel(
                id, name, null, level, "icon", 1, 1, LocalDateTime.of(2026, 1, 1, 0, 0), productCount);
    }
}
