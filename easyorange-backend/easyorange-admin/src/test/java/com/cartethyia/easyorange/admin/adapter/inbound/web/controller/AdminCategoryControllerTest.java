package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryCreateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryUpdateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryTreeResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryResponse;
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
            new CategoryResponse(1L, "电子产品", null, null, 1, 1, 1, 10L, null, null),
            new CategoryResponse(2L, "服装配饰", null, null, 1, 2, 1, 5L, null, null)
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
            new CategoryResponse(3L, "手机", 1L, null, 2, null, null, null, null, null)
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
            new CategoryTreeResponse(1L, "电子产品", 1, 1, 1,
                List.of(
                    new CategoryTreeResponse(3L, "手机", 2, 1, 1, List.of())
                )
            )
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
        var created = new CategoryResponse(1L, "新分类", null, null, 1, 0, 1, null, null, null);
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
        var created = new CategoryResponse(4L, "子分类", 1L, null, 2, null, null, null, null, null);
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
        var updated = new CategoryResponse(1L, "更新名称", null, null, 1, null, null, null, null, null);
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