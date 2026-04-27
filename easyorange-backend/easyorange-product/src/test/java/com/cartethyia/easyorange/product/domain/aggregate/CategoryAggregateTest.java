package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CategoryAggregate 聚合根测试")
class CategoryAggregateTest {

    @Nested
    @DisplayName("create 静态工厂方法")
    class CreateTests {

        @Test
        @DisplayName("创建一级分类成功")
        void create_level1Category_success() {
            CategoryAggregate aggregate = CategoryAggregate.create(null, "电子产品", 1, 1);

            assertThat(aggregate.getCategory()).isNotNull();
            assertThat(aggregate.getName()).isEqualTo("电子产品");
            assertThat(aggregate.getLevel()).isEqualTo(1);
            assertThat(aggregate.getSortOrder()).isEqualTo(1);
            assertThat(aggregate.getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("创建二级分类成功")
        void create_level2Category_success() {
            CategoryAggregate parent = CategoryAggregate.create(null, "电子产品", 1, 1);
            CategoryAggregate child = CategoryAggregate.create(parent.getId(), "手机", 2, 1);

            assertThat(child.getCategory()).isNotNull();
            assertThat(child.getParentId()).isEqualTo(parent.getId());
            assertThat(child.getLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("创建三级分类成功")
        void create_level3Category_success() {
            CategoryAggregate grandchild = CategoryAggregate.create(100L, "智能手机", 3, 1);

            assertThat(grandchild.getLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("名称为空抛出异常")
        void create_withNullName_throws() {
            assertThatThrownBy(() -> CategoryAggregate.create(null, null, 1, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类名称不能为空");
        }

        @Test
        @DisplayName("名称为空白抛出异常")
        void create_withBlankName_throws() {
            assertThatThrownBy(() -> CategoryAggregate.create(null, "   ", 1, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类名称不能为空");
        }

        @Test
        @DisplayName("层级小于 1 抛出异常")
        void create_withLevelLessThan1_throws() {
            assertThatThrownBy(() -> CategoryAggregate.create(null, "测试", 0, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类层级必须在 1-3 之间");
        }

        @Test
        @DisplayName("层级大于 3 抛出异常")
        void create_withLevelGreaterThan3_throws() {
            assertThatThrownBy(() -> CategoryAggregate.create(null, "测试", 4, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类层级必须在 1-3 之间");
        }

        @Test
        @DisplayName("sortOrder 为 null 使用默认值 0")
        void create_withNullSortOrder_usesDefault() {
            CategoryAggregate aggregate = CategoryAggregate.create(null, "测试", 1, null);
            assertThat(aggregate.getSortOrder()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("load 静态工厂方法")
    class LoadTests {

        @Test
        @DisplayName("加载 null 返回 null")
        void load_withNullCategory_returnsNull() {
            assertThat(CategoryAggregate.load(null, List.of())).isNull();
        }

        @Test
        @DisplayName("children 为 null 使用空列表")
        void load_withNullChildren_usesEmptyList() {
            Category category = Category.builder()
                    .name("测试分类")
                    .parentId(null)
                    .level(1)
                    .build();

            CategoryAggregate aggregate = CategoryAggregate.load(category, null);

            assertThat(aggregate.hasChildren()).isFalse();
        }
    }

    @Nested
    @DisplayName("toEntity 方法")
    class ToEntityTests {

        @Test
        @DisplayName("转换为实体成功")
        void toEntity_success() {
            CategoryAggregate aggregate = CategoryAggregate.create(null, "测试", 1, 5);

            Category entity = aggregate.toEntity();

            assertThat(entity.getName()).isEqualTo("测试");
            assertThat(entity.getLevel()).isEqualTo(1);
            assertThat(entity.getSortOrder()).isEqualTo(5);
            assertThat(entity.getStatus()).isEqualTo(1);
        }
    }
}
