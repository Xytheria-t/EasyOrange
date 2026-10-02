package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminProductAssembler;
import com.cartethyia.easyorange.admin.application.service.AdminProductAppService;
import com.cartethyia.easyorange.admin.domain.model.ProductDetailView;
import com.cartethyia.easyorange.admin.domain.model.ProductListView;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.PartyProfile;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductSummary;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 后台商品接口契约测试 — 锁 JSON 字段名（{@code productId} / 主图取第一张图）。
 */
@WebMvcTest(AdminProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminProductAssembler.class)
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminProductAppService adminProductService;

    private static ProductSummary summary(String id, String name, String status) {
        return new ProductSummary(
                id,
                name,
                BigDecimal.valueOf(100),
                null,
                1,
                status,
                "状态",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void listProducts_shouldReturnPaginatedProducts() throws Exception {
        when(adminProductService.listProducts(any(ProductQueryCondition.class)))
                .thenReturn(new ProductListView(
                        new ProductQueryResult(
                                List.of(summary("1", "Product1", "ONLINE"), summary("2", "Product2", "DRAFT")),
                                2,
                                1,
                                20),
                        Map.of(),
                        Map.of()));

        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].productId").value("1"))
                .andExpect(jsonPath("$.data.records[0].name").value("Product1"))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void listProducts_withStatusFilter_shouldPassFilterToService() throws Exception {
        when(adminProductService.listProducts(any(ProductQueryCondition.class)))
                .thenReturn(new ProductListView(
                        new ProductQueryResult(List.of(summary("1", "Online", "ONLINE")), 1, 1, 20),
                        Map.of(),
                        Map.of()));

        mockMvc.perform(get("/api/admin/products?status=" + "ONLINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].status").value("ONLINE"));
    }

    @Test
    void listProducts_shouldTakeFirstImageAsMainImage() throws Exception {
        when(adminProductService.listProducts(any(ProductQueryCondition.class)))
                .thenReturn(new ProductListView(
                        new ProductQueryResult(List.of(summary("1", "Product1", "ONLINE")), 1, 1, 20),
                        Map.of("1", List.of("first.jpg", "second.jpg")),
                        Map.of()));

        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].mainImage").value("first.jpg"));
    }

    @Test
    void getProductDetail_shouldReturnProduct() throws Exception {
        when(adminProductService.getProductDetail("1"))
                .thenReturn(new ProductDetailView(
                        new ProductDetail(
                                "1",
                                "DetailProduct",
                                "A detailed product",
                                BigDecimal.valueOf(150),
                                null,
                                1,
                                "ONLINE",
                                "上架",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null),
                        List.of(),
                        new PartyProfile("卖家昵称", "avatar.png", "电子数码")));
        mockMvc.perform(get("/api/admin/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.productId").value("1"))
                .andExpect(jsonPath("$.data.name").value("DetailProduct"))
                .andExpect(jsonPath("$.data.description").value("A detailed product"))
                // 卖家与分类档案曾声明在 DTO 里却从不下发：审核列表「资产方」整列、详情「分类」「资产方」两格恒空
                .andExpect(jsonPath("$.data.sellerName").value("卖家昵称"))
                .andExpect(jsonPath("$.data.categoryName").value("电子数码"));
    }

    @Test
    void updateProductStatus_shouldSucceed() throws Exception {
        doNothing().when(adminProductService).updateProductStatus(eq("1"), eq("OFFLINE"));

        mockMvc.perform(put("/api/admin/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"OFFLINE\", \"reason\": \"下架商品\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void updateProductStatus_withoutStatus_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"下架商品\"}"))
                .andExpect(status().isBadRequest());
    }
}
