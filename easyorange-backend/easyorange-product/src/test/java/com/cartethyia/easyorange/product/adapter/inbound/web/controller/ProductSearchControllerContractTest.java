package com.cartethyia.easyorange.product.adapter.inbound.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.port.query.FacetBucket;
import com.cartethyia.easyorange.product.application.query.ProductSearchQueryHandler;
import com.cartethyia.easyorange.product.application.query.dto.ProductSearchResult;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 契约冒烟 — 搜索端点（乱码关键词入口）。
 * <p>
 * 前端不再逐字段盯这些端点后，封套/字段形状回归会静默潜伏（TD-018 同类）：
 * UI 不调 ≠ 可以烂。这里钉住 SearchPageResponse 的前端消费形状
 * （records/total/current/size/pages/facets）与封套 A0000。
 */
@WebMvcTest(ProductSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("契约冒烟：商品搜索端点")
class ProductSearchControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductSearchQueryHandler searchQueryHandler;

    private static ProductReadModel product() {
        return ProductReadModel.builder().id("p-1").title("测试商品").build();
    }

    @Test
    @DisplayName("乱码关键词 -> 200 + 封套 A0000 + SearchPageResponse 全字段形状")
    void search_garbledKeyword_fullShape() throws Exception {
        when(searchQueryHandler.search(any(), anyBoolean()))
                .thenReturn(new ProductSearchResult(
                        PageResult.of(List.of(product()), 1L, 1, 20), List.of(new FacetBucket("category", "手机", 3))));

        mockMvc.perform(get("/api/products/search").param("keyword", "乱码%%%查询").param("aiEnhanced", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.records[0].id").value("p-1"))
                .andExpect(jsonPath("$.data.records[0].title").value("测试商品"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andExpect(jsonPath("$.data.facets[0].code").value("category"))
                .andExpect(jsonPath("$.data.facets[0].label").value("手机"))
                .andExpect(jsonPath("$.data.facets[0].count").value(3));

        verify(searchQueryHandler).search(argThat(c -> "乱码%%%查询".equals(c.keyword())), eq(true));
    }

    @Test
    @DisplayName("无结果时封套与空分页形状（分页/空态前端消费契约）")
    void search_noResult_emptyShape() throws Exception {
        when(searchQueryHandler.search(any(), anyBoolean()))
                .thenReturn(new ProductSearchResult(PageResult.empty(1, 20), List.of()));

        mockMvc.perform(get("/api/products/search").param("keyword", "找不到的词").param("aiEnhanced", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.records").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.facets").isEmpty());
    }
}
