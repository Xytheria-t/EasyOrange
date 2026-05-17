package com.cartethyia.easyorange.admin.controller;

import com.cartethyia.easyorange.admin.dto.request.CategoryCreateRequest;
import com.cartethyia.easyorange.admin.dto.request.CategoryUpdateRequest;
import com.cartethyia.easyorange.admin.dto.response.CategoryTreeVO;
import com.cartethyia.easyorange.admin.dto.response.CategoryVO;
import com.cartethyia.easyorange.admin.service.AdminCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCategoryService adminCategoryService;

    @Test
    void listCategories_shouldReturnAll() throws Exception {
        var categories = List.of(
            CategoryVO.builder().categoryId(1L).name("电子产品").level(1).sortOrder(1).status(1).productCount(10L).build(),
            CategoryVO.builder().categoryId(2L).name("服装配饰").level(1).sortOrder(2).status(1).productCount(5L).build()
        );
        when(adminCategoryService.listCategories(isNull())).thenReturn(categories);

        mockMvc.perform(get("/api/admin/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].categoryId").value(1))
            .andExpect(jsonPath("$.data[0].name").value("电子产品"));
    }

    @Test
    void listCategories_withParentId_shouldFilterByParent() throws Exception {
        var categories = List.of(
            CategoryVO.builder().categoryId(3L).name("手机").parentId(1L).level(2).build()
        );
        when(adminCategoryService.listCategories(1L)).thenReturn(categories);

        mockMvc.perform(get("/api/admin/categories?parentId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].parentId").value(1))
            .andExpect(jsonPath("$.data[0].name").value("手机"));
    }

    @Test
    void categoryTree_shouldReturnTree() throws Exception {
        var tree = List.of(
            CategoryTreeVO.builder()
                .categoryId(1L).name("电子产品").level(1).sortOrder(1).status(1)
                .children(List.of(
                    CategoryTreeVO.builder()
                        .categoryId(3L).name("手机").level(2).sortOrder(1).status(1)
                        .children(List.of()).build()
                )).build()
        );
        when(adminCategoryService.categoryTree()).thenReturn(tree);

        mockMvc.perform(get("/api/admin/categories/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data[0].categoryId").value(1))
            .andExpect(jsonPath("$.data[0].children[0].name").value("手机"))
            .andExpect(jsonPath("$.data[0].children[0].children").isArray());
    }

    @Test
    void createCategory_shouldReturnCreated() throws Exception {
        var created = CategoryVO.builder().categoryId(1L).name("新分类").level(1).sortOrder(0).status(1).build();
        when(adminCategoryService.createCategory(any(CategoryCreateRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"新分类\", \"sortOrder\": 1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.categoryId").value(1))
            .andExpect(jsonPath("$.data.name").value("新分类"));
    }

    @Test
    void createCategory_withoutName_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sortOrder\": 1}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_withParentId_shouldSucceed() throws Exception {
        var created = CategoryVO.builder().categoryId(4L).name("子分类").parentId(1L).level(2).build();
        when(adminCategoryService.createCategory(any(CategoryCreateRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"子分类\", \"parentId\": 1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.parentId").value(1))
            .andExpect(jsonPath("$.data.level").value(2));
    }

    @Test
    void updateCategory_shouldReturnUpdated() throws Exception {
        var updated = CategoryVO.builder().categoryId(1L).name("更新名称").level(1).build();
        when(adminCategoryService.updateCategory(eq(1L), any(CategoryUpdateRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/admin/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"更新名称\", \"sortOrder\": 2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.name").value("更新名称"));
    }

    @Test
    void updateCategory_withoutName_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sortOrder\": 2}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_shouldSucceed() throws Exception {
        doNothing().when(adminCategoryService).updateStatus(1L, 1);

        mockMvc.perform(put("/api/admin/categories/1/status?status=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void deleteCategory_shouldSucceed() throws Exception {
        doNothing().when(adminCategoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/admin/categories/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"));
    }
}
