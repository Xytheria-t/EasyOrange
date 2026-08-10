package com.cartethyia.easyorange.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryCreateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryUpdateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryTreeResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryQueryPort.CategoryRecord;
import com.cartethyia.easyorange.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminCategoryService 单元测试")
class AdminCategoryServiceTest {

    @Mock
    private AdminCategoryQueryPort adminCategoryQueryPort;

    @InjectMocks
    private AdminCategoryService categoryService;

    private static final String CATEGORY_ID = "1";
    private static final String PARENT_ID = "10";

    private CategoryRecord createCategory(String id, String name, String parentId, Integer level) {
        return new CategoryRecord(id, name, parentId, level, null, 0, 1, LocalDateTime.now());
    }

    @Nested
    @DisplayName("categoryTree")
    class CategoryTreeTests {

        @Test
        @DisplayName("获取分类树结构")
        void categoryTree_returnsTree() {
            CategoryRecord parent = createCategory(CATEGORY_ID, "电子数码", null, 1);
            CategoryRecord child = createCategory(PARENT_ID, "手机", CATEGORY_ID, 2);

            when(adminCategoryQueryPort.listCategories(null)).thenReturn(List.of(parent, child));

            List<CategoryTreeResponse> tree = categoryService.categoryTree();

            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).name()).isEqualTo("电子数码");
            assertThat(tree.get(0).children()).hasSize(1);
            assertThat(tree.get(0).children().get(0).name()).isEqualTo("手机");
        }

        @Test
        @DisplayName("没有分类时返回空列表")
        void categoryTree_empty_returnsEmpty() {
            when(adminCategoryQueryPort.listCategories(null)).thenReturn(List.of());

            List<CategoryTreeResponse> tree = categoryService.categoryTree();

            assertThat(tree).isEmpty();
        }

        @Test
        @DisplayName("禁用的分类不进入树")
        void categoryTree_disabledFiltered() {
            CategoryRecord enabled = createCategory(CATEGORY_ID, "电子数码", null, 1);
            CategoryRecord disabled = new CategoryRecord(PARENT_ID, "已禁用", null, 1, null, 0, 0, LocalDateTime.now());

            when(adminCategoryQueryPort.listCategories(null)).thenReturn(List.of(enabled, disabled));

            List<CategoryTreeResponse> tree = categoryService.categoryTree();

            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).name()).isEqualTo("电子数码");
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategoryTests {

        @Test
        @DisplayName("创建一级分类成功")
        void createCategory_root_success() {
            when(adminCategoryQueryPort.findCategoryByName("新分类")).thenReturn(null);
            when(adminCategoryQueryPort.createCategory("新分类", null, 1, 1))
                    .thenReturn(createCategory("99", "新分类", null, 1));

            CategoryCreateRequest request = new CategoryCreateRequest("新分类", null, 1);

            categoryService.createCategory(request);

            verify(adminCategoryQueryPort).createCategory("新分类", null, 1, 1);
        }

        @Test
        @DisplayName("创建子分类成功")
        void createCategory_child_success() {
            when(adminCategoryQueryPort.getCategory(CATEGORY_ID))
                    .thenReturn(createCategory(CATEGORY_ID, "电子数码", null, 1));
            when(adminCategoryQueryPort.findCategoryByName("手机")).thenReturn(null);
            when(adminCategoryQueryPort.createCategory("手机", CATEGORY_ID, 0, 2))
                    .thenReturn(createCategory("99", "手机", CATEGORY_ID, 2));

            CategoryCreateRequest request = new CategoryCreateRequest("手机", CATEGORY_ID, null);

            categoryService.createCategory(request);

            verify(adminCategoryQueryPort).createCategory("手机", CATEGORY_ID, 0, 2);
        }

        @Test
        @DisplayName("父分类不存在抛出异常")
        void createCategory_parentNotFound_throws() {
            when(adminCategoryQueryPort.getCategory(CATEGORY_ID)).thenReturn(null);

            CategoryCreateRequest request = new CategoryCreateRequest("新分类", CATEGORY_ID, null);

            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("父分类不存在");
        }

        @Test
        @DisplayName("同名一级分类抛出异常")
        void createCategory_duplicateName_throws() {
            when(adminCategoryQueryPort.findCategoryByName("已存在"))
                    .thenReturn(createCategory(CATEGORY_ID, "已存在", null, 1));

            CategoryCreateRequest request = new CategoryCreateRequest("已存在", null, null);

            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("同名");
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategoryTests {

        @Test
        @DisplayName("更新分类成功")
        void updateCategory_success() {
            when(adminCategoryQueryPort.getCategory(CATEGORY_ID))
                    .thenReturn(createCategory(CATEGORY_ID, "旧名称", null, 1));
            when(adminCategoryQueryPort.findCategoryByName("新名称")).thenReturn(null);
            when(adminCategoryQueryPort.countProductsByCategoryIds(anyList())).thenReturn(Map.of(CATEGORY_ID, 0L));

            CategoryUpdateRequest request = new CategoryUpdateRequest("新名称", null, 2);

            categoryService.updateCategory(CATEGORY_ID, request);

            verify(adminCategoryQueryPort).updateCategory(any(CategoryRecord.class));
        }

        @Test
        @DisplayName("更新不存在的分类抛出异常")
        void updateCategory_notFound_throws() {
            when(adminCategoryQueryPort.getCategory(CATEGORY_ID)).thenReturn(null);

            CategoryUpdateRequest request = new CategoryUpdateRequest("新名称", null, null);

            assertThatThrownBy(() -> categoryService.updateCategory(CATEGORY_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("分类不存在");
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTests {

        @Test
        @DisplayName("删除空分类成功")
        void deleteCategory_noChildren_success() {
            when(adminCategoryQueryPort.getCategory(CATEGORY_ID))
                    .thenReturn(createCategory(CATEGORY_ID, "测试分类", null, 1));
            when(adminCategoryQueryPort.countCategoryChildren(CATEGORY_ID)).thenReturn(0L);
            when(adminCategoryQueryPort.countProductsByCategoryIds(anyList())).thenReturn(Map.of(CATEGORY_ID, 0L));

            categoryService.deleteCategory(CATEGORY_ID);

            verify(adminCategoryQueryPort).deleteCategory(CATEGORY_ID);
        }

        @Test
        @DisplayName("有子分类时无法删除")
        void deleteCategory_hasChildren_throws() {
            when(adminCategoryQueryPort.getCategory(CATEGORY_ID))
                    .thenReturn(createCategory(CATEGORY_ID, "测试分类", null, 1));
            when(adminCategoryQueryPort.countCategoryChildren(CATEGORY_ID)).thenReturn(2L);

            assertThatThrownBy(() -> categoryService.deleteCategory(CATEGORY_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("子分类");
        }

        @Test
        @DisplayName("有商品关联时无法删除")
        void deleteCategory_hasProducts_throws() {
            when(adminCategoryQueryPort.getCategory(CATEGORY_ID))
                    .thenReturn(createCategory(CATEGORY_ID, "测试分类", null, 1));
            when(adminCategoryQueryPort.countCategoryChildren(CATEGORY_ID)).thenReturn(0L);
            when(adminCategoryQueryPort.countProductsByCategoryIds(anyList())).thenReturn(Map.of(CATEGORY_ID, 3L));

            assertThatThrownBy(() -> categoryService.deleteCategory(CATEGORY_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("关联商品");
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("更新分类状态成功")
        void updateStatus_success() {
            when(adminCategoryQueryPort.getCategory(CATEGORY_ID))
                    .thenReturn(createCategory(CATEGORY_ID, "测试", null, 1));

            categoryService.updateStatus(CATEGORY_ID, 0);

            verify(adminCategoryQueryPort).updateCategory(any(CategoryRecord.class));
        }

        @Test
        @DisplayName("更新不存在的分类状态抛出异常")
        void updateStatus_notFound_throws() {
            when(adminCategoryQueryPort.getCategory(CATEGORY_ID)).thenReturn(null);

            assertThatThrownBy(() -> categoryService.updateStatus(CATEGORY_ID, 0))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("分类不存在");
        }
    }
}
