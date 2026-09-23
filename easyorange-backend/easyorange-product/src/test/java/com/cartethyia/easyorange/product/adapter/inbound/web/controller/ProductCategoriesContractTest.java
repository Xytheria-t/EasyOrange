package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.product.adapter.inbound.web.assembler.CategoryAssembler;
import com.cartethyia.easyorange.product.application.command.ProductCommandHandler;
import com.cartethyia.easyorange.product.application.port.cache.ViewCountPort;
import com.cartethyia.easyorange.product.application.query.CategoryQueryHandler;
import com.cartethyia.easyorange.product.application.query.ProductQueryHandler;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 契约冒烟 — 分类端点（读路径带 Redis 缓存，失败 fail-open 直查 DB）。
 * <p>
 * 这条链路前端缓存与发布表单都依赖，且缓存层（TD-017）出过序列化恒失败：
 * 端点没被 UI 调用不等于可以烂，封套 + 列表形状在这里钉住。
 */
@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CategoryAssembler.class)
@DisplayName("契约冒烟：分类端点")
class ProductCategoriesContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductCommandHandler commandHandler;

    @MockitoBean
    private ViewCountPort viewCountPort;

    @MockitoBean
    private ProductQueryHandler queryHandler;

    @MockitoBean
    private CategoryQueryHandler categoryQueryHandler;

    @Test
    @DisplayName("根分类列表 -> 200 + 封套 A0000 + 数组形状与映射字段")
    void categories_root_returnsListShape() throws Exception {
        when(categoryQueryHandler.getCategories(null))
                .thenReturn(List.of(new CategoryReadModel("c-1", "手机数码", null, 1, null, 10, 1, null, 42)));

        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data[0].id").value("c-1"))
                .andExpect(jsonPath("$.data[0].name").value("手机数码"))
                .andExpect(jsonPath("$.data[0].productCount").value(42));
    }

    @Test
    @DisplayName("parentId 透传给 handler，空列表仍是 A0000 数组")
    void categories_withParentId_passesThrough() throws Exception {
        when(categoryQueryHandler.getCategories("c-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/products/categories").param("parentId", "c-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(categoryQueryHandler).getCategories("c-1");
    }
}
