package com.cartethyia.easyorange.product.domain.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import com.cartethyia.easyorange.product.domain.exception.ProductDomainException;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryName;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 分类聚合的领域规则测试。
 * <p>
 * 重点覆盖三个曾经出过问题的判定：移动的**环检测**、移动的**整棵子树深度**、
 * 以及一级分类的 **parentId = null** 语义。
 */
@DisplayName("Category 聚合领域测试")
class CategoryTest {

    private static final String ROOT_ID = "root-1";
    private static final String MID_ID = "mid-1";
    private static final String LEAF_ID = "leaf-1";

    private static Category root() {
        return Category.builder()
                .id(CategoryId.of(ROOT_ID))
                .name(CategoryName.of("电子数码"))
                .parentId(null)
                .level(1)
                .sortOrder(1)
                .status(CategoryStatus.ENABLED)
                .build();
    }

    private static Category child(Category parent, String id, String name) {
        return Category.builder()
                .id(CategoryId.of(id))
                .name(CategoryName.of(name))
                .parentId(parent.getId())
                .level(parent.getLevel() + 1)
                .sortOrder(1)
                .status(CategoryStatus.ENABLED)
                .build();
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("无父分类 -> 一级，parentId 为 null（不是字符串 '0'）")
        void create_root_hasNullParent() {
            Category created = Category.create(CategoryId.of("new-1"), CategoryName.of("书籍教材"), null, null, 3);

            assertThat(created.getLevel()).isEqualTo(1);
            assertThat(created.getParentId()).isNull();
            assertThat(created.isRoot()).isTrue();
            assertThat(created.getSortOrder()).isEqualTo(3);
            assertThat(created.getStatus()).isEqualTo(CategoryStatus.ENABLED);
        }

        @Test
        @DisplayName("有父分类 -> 层级 = 父层级 + 1")
        void create_child_derivesLevel() {
            Category created = Category.create(CategoryId.of("new-2"), CategoryName.of("手机"), root(), null, 0);

            assertThat(created.getLevel()).isEqualTo(2);
            assertThat(created.getParentId()).isEqualTo(CategoryId.of(ROOT_ID));
        }

        @Test
        @DisplayName("父分类已在第 3 级 -> 新建子分类越界被拒")
        void create_beyondMaxLevel_rejected() {
            Category level2 = child(root(), MID_ID, "手机");
            Category level3 = child(level2, LEAF_ID, "折叠屏");

            assertThatThrownBy(() -> Category.create(CategoryId.of("new-3"), CategoryName.of("更深"), level3, null, 0))
                    .isInstanceOf(ProductDomainException.class)
                    .hasMessageContaining("3");
        }
    }

    @Nested
    @DisplayName("moveTo")
    class MoveTests {

        @Test
        @DisplayName("移到根：parentId 置 null —— 靠的是显式赋值，不是「不赋值」")
        void moveToRoot_clearsParent() {
            Category mid = child(root(), MID_ID, "手机");

            Category moved = mid.moveTo(null, List.of(mid.getId()), 1);

            assertThat(moved.getParentId()).isNull();
            assertThat(moved.getLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("挂到自己身上 -> 拒绝（成环）")
        void moveToSelf_rejected() {
            Category mid = child(root(), MID_ID, "手机");

            assertThatThrownBy(() -> mid.moveTo(mid, List.of(mid.getId()), 1))
                    .isInstanceOf(ProductDomainException.class)
                    .hasMessageContaining("子分类");
        }

        @Test
        @DisplayName("挂到自己的子孙下 -> 拒绝（成环，前端之外的防线）")
        void moveToDescendant_rejected() {
            Category mid = child(root(), MID_ID, "手机");
            Category leaf = child(mid, LEAF_ID, "折叠屏");

            // 子树 = 自身 + 叶子；把 mid 挂到 leaf 下即成环
            assertThatThrownBy(() -> mid.moveTo(leaf, List.of(mid.getId(), leaf.getId()), 2))
                    .isInstanceOf(ProductDomainException.class)
                    .hasMessageContaining("子分类");
        }

        @Test
        @DisplayName("子树高度 2 + 挂到 2 级父下 -> 最深 3 级，放行")
        void moveWithinMaxLevel_allowed() {
            Category otherRoot = Category.create(CategoryId.of("root-2"), CategoryName.of("书籍教材"), null, null, 0);
            Category otherMid = child(otherRoot, "other-mid", "教材");
            Category mid = child(root(), MID_ID, "手机");
            Category leaf = child(mid, LEAF_ID, "折叠屏");

            // 新父在 2 级，自身变 3 级，子孙会变 4 级 -> 应被拒
            assertThatThrownBy(() -> mid.moveTo(otherMid, List.of(mid.getId(), leaf.getId()), 2))
                    .isInstanceOf(ProductDomainException.class)
                    .hasMessageContaining("3");

            // 挂到根下：自身 2 级、子孙 3 级，放行
            Category moved = mid.moveTo(otherRoot, List.of(mid.getId(), leaf.getId()), 2);
            assertThat(moved.getLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("移到根但子树本身就超深 -> 拒绝")
        void moveToRoot_withDeepSubtree_rejected() {
            Category mid = child(root(), MID_ID, "手机");

            assertThatThrownBy(() -> mid.moveTo(null, List.of(mid.getId()), 4))
                    .isInstanceOf(ProductDomainException.class);
        }
    }

    @Nested
    @DisplayName("属性变更")
    class MutationTests {

        @Test
        @DisplayName("改名 / 排序 / 图标 / 状态各自独立生效")
        void mutations_areIndependent() {
            Category renamed = root().rename(CategoryName.of("数码")).changeSortOrder(9).changeStatus(CategoryStatus.DISABLED);

            assertThat(renamed.getName().value()).isEqualTo("数码");
            assertThat(renamed.getSortOrder()).isEqualTo(9);
            assertThat(renamed.getStatus()).isEqualTo(CategoryStatus.DISABLED);
        }

        @Test
        @DisplayName("withLevel 改层级不改其它字段")
        void withLevel_onlyTouchesLevel() {
            Category shifted = root().withLevel(3);

            assertThat(shifted.getLevel()).isEqualTo(3);
            assertThat(shifted.getName()).isEqualTo(root().getName());
            assertThat(shifted.getParentId()).isEqualTo(root().getParentId());
        }
    }
}
