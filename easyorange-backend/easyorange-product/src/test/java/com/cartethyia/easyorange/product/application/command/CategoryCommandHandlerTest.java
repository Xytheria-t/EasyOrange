package com.cartethyia.easyorange.product.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.product.application.port.cache.CategoryCachePort;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.domain.aggregate.Category;
import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import com.cartethyia.easyorange.product.domain.exception.ProductDomainException;
import com.cartethyia.easyorange.product.domain.repository.CategoryRepository;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryName;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 分类命令处理器测试 — 覆盖聚合之外、需要查库才能判定的那些规则。
 * <p>
 * 纯算术（层级 / 环 / 深度）在 {@code CategoryTest} 里测；这里测的是「取数 + 编排」：
 * 同级重名怎么查、删除前置条件用哪个口径、移动时子孙层级怎么平移。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CategoryCommandHandler 单元测试")
class CategoryCommandHandlerTest {

    private static final String ROOT_ID = "018bcfe5-6800-752e-89a7-834df2a74de4";
    private static final String MID_ID = "018bcfe5-7f70-76f0-8478-82e36b0d549b";
    private static final String LEAF_ID = "018bcfe5-8740-76ca-b4eb-252b0f21ddb6";

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryQueryRepository categoryQueryRepository;

    @Mock
    private CategoryCachePort categoryCachePort;

    @Mock
    private IdGenerator idGenerator;

    private CategoryCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new CategoryCommandHandler(categoryRepository, categoryQueryRepository, categoryCachePort, idGenerator);
        when(idGenerator.generateId()).thenReturn("generated-id");
    }

    private static Category category(String id, String name, String parentId, int level) {
        return Category.builder()
                .id(CategoryId.of(id))
                .name(CategoryName.of(name))
                .parentId(parentId != null ? CategoryId.of(parentId) : null)
                .level(level)
                .sortOrder(1)
                .status(CategoryStatus.ENABLED)
                .build();
    }

    @Nested
    @DisplayName("createCategory")
    class CreateTests {

        @Test
        @DisplayName("主键由 IdGenerator 分配（BaseDO 是 IdType.INPUT，漏了就是 NOT NULL 插入失败）")
        void create_assignsGeneratedId() {
            when(categoryRepository.findSiblings(isNull(), isNull())).thenReturn(List.of());
            when(categoryRepository.insert(any(Category.class))).thenAnswer(call -> call.getArgument(0));

            handler.createCategory("书籍教材", null, null, 1);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).insert(captor.capture());
            assertThat(captor.getValue().getId().value()).isEqualTo("generated-id");
        }

        @Test
        @DisplayName("一级分类重名被拒（parentId 用 null 判定根，不是字符串 '0'）")
        void create_duplicateRootName_rejected() {
            when(categoryRepository.findSiblings(isNull(), isNull()))
                    .thenReturn(List.of(category(ROOT_ID, "书籍教材", null, 1)));

            assertThatThrownBy(() -> handler.createCategory("书籍教材", null, null, 1))
                    .isInstanceOf(ProductDomainException.class)
                    .hasMessageContaining("同级");

            verify(categoryRepository, never()).insert(any());
        }

        @Test
        @DisplayName("父分类不存在 -> 拒绝，不落库")
        void create_missingParent_rejected() {
            when(categoryRepository.findById(CategoryId.of("nope"))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler.createCategory("子类", "nope", null, 0))
                    .isInstanceOf(ProductDomainException.class)
                    .hasMessageContaining("父分类不存在");

            verify(categoryRepository, never()).insert(any());
        }

        @Test
        @DisplayName("创建后整份失效分类缓存")
        void create_evictsCache() {
            when(categoryRepository.findSiblings(any(), any())).thenReturn(List.of());
            when(categoryRepository.insert(any(Category.class))).thenAnswer(call -> call.getArgument(0));

            handler.createCategory("新分类", null, null, 0);

            verify(categoryCachePort).evictAll();
        }
    }

    @Nested
    @DisplayName("moveCategory")
    class MoveTests {

        @Test
        @DisplayName("移到根：显式下发 parentId=null，且子孙层级整体下移")
        void moveToRoot_shiftsSubtreeLevels() {
            Category root = category(ROOT_ID, "电子数码", null, 1);
            Category mid = category(MID_ID, "手机", ROOT_ID, 2);
            Category leaf = category(LEAF_ID, "折叠屏", MID_ID, 3);

            when(categoryRepository.findById(CategoryId.of(MID_ID))).thenReturn(Optional.of(mid));
            when(categoryRepository.findChildren(CategoryId.of(MID_ID))).thenReturn(List.of(leaf));
            when(categoryRepository.findChildren(CategoryId.of(LEAF_ID))).thenReturn(List.of());
            when(categoryRepository.findChildren(CategoryId.of(ROOT_ID))).thenReturn(List.of(mid));
            when(categoryRepository.update(any(Category.class))).thenAnswer(call -> call.getArgument(0));

            handler.moveCategory(MID_ID, null);

            // 自身：2 -> 1；子孙：3 -> 2（差值 -1 整体下移）
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository, org.mockito.Mockito.atLeast(2)).update(captor.capture());
            List<Category> updates = captor.getAllValues();
            assertThat(updates)
                    .filteredOn(c -> c.getId().value().equals(MID_ID))
                    .singleElement()
                    .satisfies(c -> {
                        assertThat(c.getParentId()).isNull();
                        assertThat(c.getLevel()).isEqualTo(1);
                    });
            assertThat(updates)
                    .filteredOn(c -> c.getId().value().equals(LEAF_ID))
                    .singleElement()
                    .satisfies(c -> assertThat(c.getLevel()).isEqualTo(2));
            verify(categoryCachePort).evictAll();
            assertThat(root.getLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("挂到自己的子分类下 -> 拒绝（后端环检测，不依赖前端下拉过滤）")
        void moveToDescendant_rejected() {
            Category root = category(ROOT_ID, "电子数码", null, 1);
            Category mid = category(MID_ID, "手机", ROOT_ID, 2);

            when(categoryRepository.findById(CategoryId.of(ROOT_ID))).thenReturn(Optional.of(root));
            when(categoryRepository.findById(CategoryId.of(MID_ID))).thenReturn(Optional.of(mid));
            when(categoryRepository.findChildren(CategoryId.of(ROOT_ID))).thenReturn(List.of(mid));
            when(categoryRepository.findChildren(CategoryId.of(MID_ID))).thenReturn(List.of());

            assertThatThrownBy(() -> handler.moveCategory(ROOT_ID, MID_ID))
                    .isInstanceOf(ProductDomainException.class)
                    .hasMessageContaining("子分类");

            verify(categoryRepository, never()).update(any());
        }

        @Test
        @DisplayName("挂到另一个一级分类下：自身升一级，子孙跟着升（最深层随之校验）")
        void moveToOtherRoot_shiftsSubtreeDown() {
            Category rootA = category(ROOT_ID, "电子数码", null, 1);
            Category midA = category(MID_ID, "手机", ROOT_ID, 2);
            Category leafA = category(LEAF_ID, "折叠屏", MID_ID, 3);
            Category rootB = category("other-root", "书籍教材", null, 1);

            when(categoryRepository.findById(CategoryId.of(ROOT_ID))).thenReturn(Optional.of(rootA));
            when(categoryRepository.findById(CategoryId.of(MID_ID))).thenReturn(Optional.of(midA));
            when(categoryRepository.findById(CategoryId.of(LEAF_ID))).thenReturn(Optional.of(leafA));
            when(categoryRepository.findById(CategoryId.of("other-root"))).thenReturn(Optional.of(rootB));
            when(categoryRepository.findChildren(CategoryId.of(ROOT_ID))).thenReturn(List.of(midA));
            when(categoryRepository.findChildren(CategoryId.of(MID_ID))).thenReturn(List.of(leafA));
            when(categoryRepository.findChildren(CategoryId.of(LEAF_ID))).thenReturn(List.of());
            when(categoryRepository.update(any(Category.class))).thenAnswer(call -> call.getArgument(0));

            // 整棵 3 级子树挂到另一个 1 级下 -> 最深会变 4 级，按不变量应被拒
            assertThatThrownBy(() -> handler.moveCategory(ROOT_ID, "other-root"))
                    .isInstanceOf(ProductDomainException.class)
                    .hasMessageContaining("3");

            verify(categoryRepository, never()).update(any());
        }

        @Test
        @DisplayName("挂到同深度的另一个父下：层级差为 0，不产生多余的子孙更新")
        void moveToSameDepth_skipsDescendantWrites() {
            Category rootA = category(ROOT_ID, "电子数码", null, 1);
            Category midA = category(MID_ID, "手机", ROOT_ID, 2);
            Category leafA = category(LEAF_ID, "折叠屏", MID_ID, 3);
            Category otherRoot = category("other-root", "书籍教材", null, 1);

            when(categoryRepository.findById(CategoryId.of(MID_ID))).thenReturn(Optional.of(midA));
            when(categoryRepository.findById(CategoryId.of(LEAF_ID))).thenReturn(Optional.of(leafA));
            when(categoryRepository.findById(CategoryId.of("other-root"))).thenReturn(Optional.of(otherRoot));
            when(categoryRepository.findChildren(CategoryId.of(MID_ID))).thenReturn(List.of(leafA));
            when(categoryRepository.findChildren(CategoryId.of(LEAF_ID))).thenReturn(List.of());
            when(categoryRepository.update(any(Category.class))).thenAnswer(call -> call.getArgument(0));

            handler.moveCategory(MID_ID, "other-root");

            // 层级差为 0 -> 子孙无需改写，只更新自身一次
            verify(categoryRepository, org.mockito.Mockito.times(1)).update(any(Category.class));
            verify(categoryCachePort).evictAll();
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteTests {

        @Test
        @DisplayName("有子分类 -> 拒绝")
        void delete_withChildren_rejected() {
            when(categoryRepository.findById(CategoryId.of(ROOT_ID)))
                    .thenReturn(Optional.of(category(ROOT_ID, "电子数码", null, 1)));
            when(categoryRepository.findChildren(CategoryId.of(ROOT_ID)))
                    .thenReturn(List.of(category(MID_ID, "手机", ROOT_ID, 2)));

            assertThatThrownBy(() -> handler.deleteCategory(ROOT_ID))
                    .isInstanceOf(ProductDomainException.class)
                    .hasMessageContaining("子分类");

            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("只挂下架商品的分类也拒绝删除（用不限 status 的计数口径）")
        void delete_withOffshelfProducts_rejected() {
            when(categoryRepository.findById(CategoryId.of(LEAF_ID)))
                    .thenReturn(Optional.of(category(LEAF_ID, "折叠屏", MID_ID, 3)));
            when(categoryRepository.findChildren(CategoryId.of(LEAF_ID))).thenReturn(List.of());
            // 「不限上下架状态」的口径：下架商品也计入，因此这里非空
            when(categoryQueryRepository.countAllProductsByCategoryIds(List.of(LEAF_ID)))
                    .thenReturn(Map.of(LEAF_ID, 3L));

            assertThatThrownBy(() -> handler.deleteCategory(LEAF_ID))
                    .isInstanceOf(ProductDomainException.class)
                    .hasMessageContaining("关联商品");

            verify(categoryRepository, never()).delete(any());
            // 明确不走「在售」口径
            verify(categoryQueryRepository, never()).countOnlineProductsByCategoryIdsWithChildren(anyList());
        }

        @Test
        @DisplayName("空分类 -> 逻辑删除并失效缓存")
        void delete_empty_deletes() {
            when(categoryRepository.findById(CategoryId.of(LEAF_ID)))
                    .thenReturn(Optional.of(category(LEAF_ID, "折叠屏", MID_ID, 3)));
            when(categoryRepository.findChildren(CategoryId.of(LEAF_ID))).thenReturn(List.of());
            when(categoryQueryRepository.countAllProductsByCategoryIds(List.of(LEAF_ID)))
                    .thenReturn(Map.of());

            handler.deleteCategory(LEAF_ID);

            verify(categoryRepository).delete(CategoryId.of(LEAF_ID));
            verify(categoryCachePort).evictAll();
        }
    }

    @Nested
    @DisplayName("updateCategory / updateCategoryStatus")
    class UpdateTests {

        @Test
        @DisplayName("改名时同级重名 -> 拒绝（排除自身）")
        void rename_toExistingSibling_rejected() {
            when(categoryRepository.findById(CategoryId.of(MID_ID)))
                    .thenReturn(Optional.of(category(MID_ID, "手机", ROOT_ID, 2)));
            when(categoryRepository.findSiblings(eq(CategoryId.of(ROOT_ID)), eq(MID_ID)))
                    .thenReturn(List.of(category("sibling", "电脑", ROOT_ID, 2)));

            assertThatThrownBy(() -> handler.updateCategory(MID_ID, "电脑", null, null, null))
                    .isInstanceOf(ProductDomainException.class)
                    .hasMessageContaining("同级");

            verify(categoryRepository, never()).update(any());
        }

        @Test
        @DisplayName("状态切换走独立方法并失效缓存")
        void updateStatus_evictsCache() {
            when(categoryRepository.findById(CategoryId.of(MID_ID)))
                    .thenReturn(Optional.of(category(MID_ID, "手机", ROOT_ID, 2)));
            when(categoryRepository.update(any(Category.class))).thenAnswer(call -> call.getArgument(0));

            handler.updateCategoryStatus(MID_ID, CategoryStatus.DISABLED);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).update(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CategoryStatus.DISABLED);
            verify(categoryCachePort).evictAll();
        }
    }
}
