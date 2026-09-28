package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminCategoryAssembler;
import com.cartethyia.easyorange.admin.application.service.AdminCategoryAppService;
import com.cartethyia.easyorange.admin.domain.model.CategoryUpdateCommand;
import com.cartethyia.easyorange.admin.domain.model.CategoryView;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryWritePort.CategoryWriteResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 后台分类接口契约测试 — 锁 JSON 字段名。
 * <p>
 * 主键字段从 {@code categoryId} 改名为 {@code id}（与 C 端对齐），所以这里的断言同时是
 * 「改名后前端契约确实是 id」的证据。assembler 用真身入上下文：请求拆解与出参映射
 * 合起来才是这个接口的完整行为。
 */
@WebMvcTest(AdminCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminCategoryAssembler.class)
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCategoryAppService adminCategoryService;

    @Test
    void listCategories_shouldReturnAll() throws Exception {
        when(adminCategoryService.listCategories(isNull()))
                .thenReturn(List.of(
                        new CategoryView("1", "电子产品", null, null, 1, 1, 1, 10L, null, List.of()),
                        new CategoryView("2", "服装配饰", null, null, 1, 2, 1, 5L, null, List.of())));

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("1"))
                .andExpect(jsonPath("$.data[0].name").value("电子产品"));
    }

    @Test
    void listCategories_withParentId_shouldFilterByParent() throws Exception {
        when(adminCategoryService.listCategories("1"))
                .thenReturn(List.of(new CategoryView("3", "手机", "1", "电子产品", 2, null, null, null, null, List.of())));

        mockMvc.perform(get("/api/admin/categories?parentId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].parentId").value("1"))
                .andExpect(jsonPath("$.data[0].parentName").value("电子产品"))
                .andExpect(jsonPath("$.data[0].name").value("手机"));
    }

    @Test
    void categoryTree_shouldNestChildren() throws Exception {
        when(adminCategoryService.categoryTree())
                .thenReturn(List.of(new CategoryView(
                        "1",
                        "电子产品",
                        null,
                        null,
                        1,
                        1,
                        1,
                        null,
                        null,
                        List.of(new CategoryView("3", "手机", "1", null, 2, 1, 1, null, null, List.of())))));

        mockMvc.perform(get("/api/admin/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data[0].id").value("1"))
                .andExpect(jsonPath("$.data[0].children[0].name").value("手机"))
                .andExpect(jsonPath("$.data[0].children[0].children").isArray());
    }

    @Test
    void createCategory_shouldReturnCreated() throws Exception {
        when(adminCategoryService.createCategory("新分类", null, null, 1))
                .thenReturn(new CategoryWriteResult("1", "新分类", null, 1, 1, 1, 0L, null));

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"新分类\", \"sortOrder\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.id").value("1"))
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
        when(adminCategoryService.createCategory("子分类", "1", null, 0))
                .thenReturn(new CategoryWriteResult("4", "子分类", "1", 2, 0, 1, 0L, null));

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"子分类\", \"parentId\": \"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value("1"))
                .andExpect(jsonPath("$.data.level").value(2));
    }

    @Test
    void updateCategory_shouldReturnUpdated() throws Exception {
        when(adminCategoryService.updateCategory(eq("1"), any(CategoryUpdateCommand.class)))
                .thenReturn(new CategoryWriteResult("1", "更新名称", null, 1, 2, 1, 0L, null));

        mockMvc.perform(put("/api/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"更新名称\", \"sortOrder\": 2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.name").value("更新名称"));
    }

    @Test
    void updateCategory_shouldPassParentIdIntoCommand() throws Exception {
        when(adminCategoryService.updateCategory(eq("3"), eq(new CategoryUpdateCommand("手机", "2", null, null, null))))
                .thenReturn(new CategoryWriteResult("3", "手机", "2", 2, 0, 1, 0L, null));

        mockMvc.perform(put("/api/admin/categories/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"手机\", \"parentId\": \"2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value("2"));
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
        doNothing().when(adminCategoryService).updateStatus("1", 1);

        mockMvc.perform(put("/api/admin/categories/1/status?status=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void deleteCategory_shouldSucceed() throws Exception {
        doNothing().when(adminCategoryService).deleteCategory("1");

        mockMvc.perform(delete("/api/admin/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));
    }
}
