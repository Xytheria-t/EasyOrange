package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryCreateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryUpdateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryTreeResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.CategoryMapper;
import com.cartethyia.easyorange.product.domain.repository.query.CategoryQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminCategoryService 单元测试")
class AdminCategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CategoryQueryRepository categoryQueryRepository;

    @InjectMocks
    private AdminCategoryService categoryService;

    private static final String CATEGORY_ID = "1";
    private static final String PARENT_ID = "10";

    private CategoryDO createCategory(String id, String name, String parentId, Integer level) {
        CategoryDO cat = new CategoryDO(name, parentId, level, null, 0, 1);
        cat.setId(id);
        cat.setDelFlag(0);
        cat.setCreateTime(LocalDateTime.now());
        return cat;
    }

    @Nested
    @DisplayName("categoryTree")
    class CategoryTreeTests {

        @Test
        @DisplayName("获取分类树结构")
        void categoryTree_returnsTree() {
            CategoryDO parent = createCategory(CATEGORY_ID, "电子数码", null, 1);
            CategoryDO child = createCategory(PARENT_ID, "手机", CATEGORY_ID, 2);

            when(categoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(parent, child));

            List<CategoryTreeResponse> tree = categoryService.categoryTree();

            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).name()).isEqualTo("电子数码");
            assertThat(tree.get(0).children()).hasSize(1);
            assertThat(tree.get(0).children().get(0).name()).isEqualTo("手机");
        }

        @Test
        @DisplayName("没有分类时返回空列表")
        void categoryTree_empty_returnsEmpty() {
            when(categoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            List<CategoryTreeResponse> tree = categoryService.categoryTree();

            assertThat(tree).isEmpty();
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategoryTests {

        @Test
        @DisplayName("创建一级分类成功")
        void createCategory_root_success() {
            when(categoryQueryRepository.findByName("新分类")).thenReturn(null);
            // Set ID on insert to avoid NPE in toCategoryResponse (entity.id would be null after mocked insert)
            doAnswer(invocation -> {
                CategoryDO entity = invocation.getArgument(0);
                entity.setId("99");
                return 1;
            }).when(categoryMapper).insert(any(CategoryDO.class));

            CategoryCreateRequest request = new CategoryCreateRequest("新分类", null, 1);

            categoryService.createCategory(request);

            verify(categoryMapper).insert(any(CategoryDO.class));
        }

        @Test
        @DisplayName("创建子分类成功")
        void createCategory_child_success() {
            CategoryDO parent = createCategory(CATEGORY_ID, "电子数码", null, 1);
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(parent);
            when(categoryQueryRepository.findByName("手机")).thenReturn(null);
            doAnswer(invocation -> {
                CategoryDO entity = invocation.getArgument(0);
                entity.setId("99");
                return 1;
            }).when(categoryMapper).insert(any(CategoryDO.class));

            CategoryCreateRequest request = new CategoryCreateRequest("手机", CATEGORY_ID, null);

            categoryService.createCategory(request);

            verify(categoryMapper).insert(any(CategoryDO.class));
        }

        @Test
        @DisplayName("父分类不存在抛出异常")
        void createCategory_parentNotFound_throws() {
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(null);

            CategoryCreateRequest request = new CategoryCreateRequest("新分类", CATEGORY_ID, null);

            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("父分类不存在");
        }

        @Test
        @DisplayName("同名一级分类抛出异常")
        void createCategory_duplicateName_throws() {
            CategoryDO existing = createCategory(CATEGORY_ID, "已存在", null, 1);
            when(categoryQueryRepository.findByName("已存在")).thenReturn(existing);

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
            CategoryDO existing = createCategory(CATEGORY_ID, "旧名称", null, 1);
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(existing);
            when(categoryQueryRepository.countProductsByCategoryIds(anyList())).thenReturn(Map.of(CATEGORY_ID, 0L));

            CategoryUpdateRequest request = new CategoryUpdateRequest("新名称", null, 2);

            categoryService.updateCategory(CATEGORY_ID, request);

            verify(categoryMapper).updateById(any(CategoryDO.class));
        }

        @Test
        @DisplayName("更新不存在的分类抛出异常")
        void updateCategory_notFound_throws() {
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(null);

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
            CategoryDO cat = createCategory(CATEGORY_ID, "测试分类", null, 1);
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(cat);
            when(categoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(categoryQueryRepository.countProductsByCategoryIds(anyList())).thenReturn(Map.of(CATEGORY_ID, 0L));

            categoryService.deleteCategory(CATEGORY_ID);

            verify(categoryMapper).updateById(any(CategoryDO.class));
        }

        @Test
        @DisplayName("有子分类时无法删除")
        void deleteCategory_hasChildren_throws() {
            CategoryDO cat = createCategory(CATEGORY_ID, "测试分类", null, 1);
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(cat);
            when(categoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

            assertThatThrownBy(() -> categoryService.deleteCategory(CATEGORY_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("子分类");
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("更新分类状态成功")
        void updateStatus_success() {
            CategoryDO cat = createCategory(CATEGORY_ID, "测试", null, 1);
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(cat);

            categoryService.updateStatus(CATEGORY_ID, 0);

            verify(categoryMapper).updateById(any(CategoryDO.class));
        }

        @Test
        @DisplayName("更新不存在的分类状态抛出异常")
        void updateStatus_notFound_throws() {
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(null);

            assertThatThrownBy(() -> categoryService.updateStatus(CATEGORY_ID, 0))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("分类不存在");
        }
    }
}
