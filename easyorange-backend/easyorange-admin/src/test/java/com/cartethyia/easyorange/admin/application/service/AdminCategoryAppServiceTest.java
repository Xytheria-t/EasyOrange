package com.cartethyia.easyorange.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.domain.exception.AdminDomainException;
import com.cartethyia.easyorange.admin.domain.model.CategoryUpdateCommand;
import com.cartethyia.easyorange.admin.domain.model.CategoryView;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryWritePort;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryWritePort.CategoryWriteResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 后台分类服务测试 — 只测**编排**（何时走移动路径、存在性裁决），出入参全是 {@code domain} 记录。
 * <p>
 * 分类的业务规则（层级 / 环 / 重名 / 删除前置条件）已下沉到 product 模块的
 * {@code Category} 聚合与 {@code CategoryCommandHandler}，其测试在 product 模块；
 * 这里再测一遍规则等于测两遍。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminCategoryAppService 单元测试")
class AdminCategoryAppServiceTest {

    @Mock
    private AdminCategoryPort adminCategoryPort;

    @Mock
    private AdminCategoryWritePort categoryWritePort;

    @InjectMocks
    private AdminCategoryAppService categoryService;

    private static CategoryView view(String id, String name, String parentId, Integer level) {
        return new CategoryView(id, name, parentId, null, level, 0, 1, 0L, null, List.of());
    }

    private static CategoryWriteResult writeResult(String id, String name, String parentId, Integer level) {
        return new CategoryWriteResult(id, name, parentId, level, 0, 1, 0L, null);
    }

    private static CategoryUpdateCommand command(String name, String parentId) {
        return new CategoryUpdateCommand(name, parentId, null, 1, null);
    }

    @Nested
    @DisplayName("查询")
    class QueryTests {

        @Test
        @DisplayName("列表含禁用：后台要能看到并恢复禁用项")
        void listCategories_includesDisabled() {
            when(adminCategoryPort.listCategories(null, true)).thenReturn(List.of(view("1", "电子数码", null, 1)));

            var result = categoryService.listCategories(null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo("1");
            verify(adminCategoryPort).listCategories(null, true);
        }

        @Test
        @DisplayName("树：原样透传端口的层级 children")
        void categoryTree_returnsPortTree() {
            CategoryView child = new CategoryView("2", "手机", "1", "电子数码", 2, 1, 1, 3L, null, List.of());
            CategoryView root = new CategoryView("1", "电子数码", null, null, 1, 1, 1, 3L, null, List.of(child));
            when(adminCategoryPort.categoryTree()).thenReturn(List.of(root));

            var result = categoryService.categoryTree();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).children()).hasSize(1);
            assertThat(result.get(0).children().get(0).name()).isEqualTo("手机");
            assertThat(result.get(0).children().get(0).parentName()).isEqualTo("电子数码");
        }
    }

    @Nested
    @DisplayName("更新：挂载点变化的判定")
    class UpdateRoutingTests {

        @Test
        @DisplayName("parentId 与当前相同 -> 只走属性更新，不触发移动")
        void sameParent_skipsMove() {
            when(adminCategoryPort.getCategory("2")).thenReturn(Optional.of(view("2", "手机", "1", 2)));
            when(categoryWritePort.updateCategory(eq("2"), any(), any(), any(), any()))
                    .thenReturn(writeResult("2", "手机", "1", 2));

            categoryService.updateCategory("2", command("手机", "1"));

            verify(categoryWritePort, never()).moveCategory(any(), any());
            verify(categoryWritePort).updateCategory("2", "手机", 1, null, null);
        }

        @Test
        @DisplayName("parentId 变成另一个值 -> 先移动再更新属性")
        void changedParent_movesFirst() {
            when(adminCategoryPort.getCategory("2")).thenReturn(Optional.of(view("2", "手机", "1", 2)));
            when(categoryWritePort.moveCategory("2", "3")).thenReturn(writeResult("2", "手机", "3", 2));
            when(categoryWritePort.updateCategory(eq("2"), any(), any(), any(), any()))
                    .thenReturn(writeResult("2", "手机", "3", 2));

            categoryService.updateCategory("2", command("手机", "3"));

            verify(categoryWritePort).moveCategory("2", "3");
            verify(categoryWritePort).updateCategory("2", "手机", 1, null, null);
        }

        @Test
        @DisplayName("当前是二级、请求不传 parentId -> 视为移到一级，触发移动")
        void nullParentOnChild_movesToRoot() {
            when(adminCategoryPort.getCategory("2")).thenReturn(Optional.of(view("2", "手机", "1", 2)));
            when(categoryWritePort.moveCategory("2", null)).thenReturn(writeResult("2", "手机", null, 1));
            when(categoryWritePort.updateCategory(eq("2"), any(), any(), any(), any()))
                    .thenReturn(writeResult("2", "手机", null, 1));

            categoryService.updateCategory("2", command("手机", null));

            verify(categoryWritePort).moveCategory("2", null);
        }

        @Test
        @DisplayName("当前是一级、请求不传 parentId -> 位置没变，不触发移动")
        void nullParentOnRoot_skipsMove() {
            when(adminCategoryPort.getCategory("1")).thenReturn(Optional.of(view("1", "电子数码", null, 1)));
            when(categoryWritePort.updateCategory(eq("1"), any(), any(), any(), any()))
                    .thenReturn(writeResult("1", "电子数码", null, 1));

            categoryService.updateCategory("1", command("电子数码", null));

            verify(categoryWritePort, never()).moveCategory(any(), any());
        }

        @Test
        @DisplayName("分类不存在 -> 拒绝，不下发任何写命令")
        void missingCategory_rejected() {
            when(adminCategoryPort.getCategory("404")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.updateCategory("404", command("x", "1")))
                    .isInstanceOf(AdminDomainException.class);

            verify(categoryWritePort, never()).updateCategory(any(), any(), any(), any(), any());
            verify(categoryWritePort, never()).moveCategory(any(), any());
        }
    }

    @Nested
    @DisplayName("创建 / 状态 / 删除")
    class WriteTests {

        @Test
        @DisplayName("创建：透传名称与父分类")
        void create_passesThrough() {
            when(categoryWritePort.createCategory("手机", "1", null, 0)).thenReturn(writeResult("9", "手机", "1", 2));

            var result = categoryService.createCategory("手机", "1", null, 0);

            assertThat(result.categoryId()).isEqualTo("9");
            assertThat(result.level()).isEqualTo(2);
        }

        @Test
        @DisplayName("更新状态：走 status 专用端口")
        void updateStatus_delegates() {
            categoryService.updateStatus("1", 0);

            verify(categoryWritePort).updateCategoryStatus("1", 0);
        }

        @Test
        @DisplayName("删除：走写端口，前置校验在 product 侧")
        void delete_delegates() {
            categoryService.deleteCategory("1");

            verify(categoryWritePort).deleteCategory("1");
        }
    }
}
