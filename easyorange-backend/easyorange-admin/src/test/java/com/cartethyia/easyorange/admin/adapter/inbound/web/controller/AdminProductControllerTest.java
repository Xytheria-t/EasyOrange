package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminProductResponse;
import com.cartethyia.easyorange.admin.service.AdminProductService;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminProductService adminProductService;

    @Test
    void listProducts_shouldReturnPaginatedProducts() throws Exception {
        var products = List.of(
            AdminProductResponse.builder().productId("1").name("Product1").price(BigDecimal.valueOf(100))
                .status(ProductStatus.ONLINE.getCode()).statusDesc("上架").build(),
            AdminProductResponse.builder().productId("2").name("Product2").price(BigDecimal.valueOf(200))
                .status(ProductStatus.DRAFT.getCode()).statusDesc("草稿").build()
        );
        var pageResult = PageResult.of(products, 2L, 1, 20);
        when(adminProductService.listProducts(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.records.length()").value(2))
            .andExpect(jsonPath("$.data.records[0].productId").value("1"))
            .andExpect(jsonPath("$.data.records[0].name").value("Product1"))
            .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void listProducts_withStatusFilter_shouldFilterByStatus() throws Exception {
        var products = List.of(
            AdminProductResponse.builder().productId("1").name("Online").status(ProductStatus.ONLINE.getCode()).build()
        );
        var pageResult = PageResult.of(products, 1L, 1, 20);
        when(adminProductService.listProducts(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/products?status=" + ProductStatus.ONLINE.getCode()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records[0].status").value(ProductStatus.ONLINE.getCode()));
    }

    @Test
    void getProductDetail_shouldReturnProduct() throws Exception {
        var product = AdminProductResponse.builder()
            .productId("1").name("DetailProduct").description("A detailed product")
            .price(BigDecimal.valueOf(150)).status(ProductStatus.ONLINE.getCode()).statusDesc("上架").build();
        when(adminProductService.getProductDetail("1")).thenReturn(product);

        mockMvc.perform(get("/api/admin/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.productId").value("1"))
            .andExpect(jsonPath("$.data.name").value("DetailProduct"))
            .andExpect(jsonPath("$.data.description").value("A detailed product"));
    }

    @Test
    void updateProductStatus_shouldSucceed() throws Exception {
        doNothing().when(adminProductService).updateProductStatus("1", null);

        mockMvc.perform(put("/api/admin/products/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"" + ProductStatus.OFFLINE.getCode() + "\", \"reason\": \"下架商品\"}"))
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