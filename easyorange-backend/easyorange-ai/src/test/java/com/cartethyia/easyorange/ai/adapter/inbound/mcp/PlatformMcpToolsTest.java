package com.cartethyia.easyorange.ai.adapter.inbound.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.service.AssetSourcingService;
import com.cartethyia.easyorange.ai.application.service.KnowledgeRetrievalService;
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.CategorySummary;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.ai.domain.port.CategoryListPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformMcpTools 测试")
class PlatformMcpToolsTest {

    @Mock
    private AssetSourcingService assetSourcingService;

    @Mock
    private AssetDetailPort assetDetailPort;

    @Mock
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @Mock
    private CategoryListPort categoryListPort;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private PlatformMcpTools tools() {
        return new PlatformMcpTools(
                assetSourcingService, assetDetailPort, knowledgeRetrievalService, categoryListPort, registry);
    }

    @Test
    @DisplayName("search_products：topK 缺省 5、钳制到 [1, 20]")
    void searchProducts_clampsTopK() {
        var tools = tools();
        when(assetSourcingService.search("显卡", 5)).thenReturn(List.of(hit("p1")));
        when(assetSourcingService.search("显卡", 20)).thenReturn(List.of());
        when(assetSourcingService.search("显卡", 1)).thenReturn(List.of());

        assertThat(tools.searchProducts("显卡", null)).hasSize(1);
        assertThat(tools.searchProducts("显卡", 99)).isEmpty();
        assertThat(tools.searchProducts("显卡", 0)).isEmpty();

        verify(assetSourcingService).search("显卡", 5);
        verify(assetSourcingService).search("显卡", 20);
        verify(assetSourcingService).search("显卡", 1);
    }

    @Test
    @DisplayName("get_product_detail：命中包成 found=true")
    void getProductDetail_found() {
        var tools = tools();
        var detail = new AssetDetail("p1", "九成新显卡", "RTX 4070", new BigDecimal("1999"), null, null, null, null, null);
        when(assetDetailPort.findDetail("p1")).thenReturn(Optional.of(detail));

        var result = tools.getProductDetail("p1");

        assertThat(result.found()).isTrue();
        assertThat(result.detail()).isSameAs(detail);
    }

    @Test
    @DisplayName("get_product_detail：未找到与空 ID 都是 found=false，不抛异常")
    void getProductDetail_notFoundOrBlank() {
        var tools = tools();
        when(assetDetailPort.findDetail("missing")).thenReturn(Optional.empty());

        assertThat(tools.getProductDetail("missing").found()).isFalse();
        assertThat(tools.getProductDetail(null).found()).isFalse();
        assertThat(tools.getProductDetail("  ").found()).isFalse();

        verifyNoInteractions(assetSourcingService, knowledgeRetrievalService, categoryListPort);
    }

    @Test
    @DisplayName("list_categories：parentId 原样透传（null = 一级类目）")
    void listCategories_delegatesParentId() {
        var tools = tools();
        var level1 = List.of(new CategorySummary("c1", "数码", 1, 12));
        when(categoryListPort.list(null)).thenReturn(level1);
        when(categoryListPort.list("c1")).thenReturn(List.of());

        assertThat(tools.listCategories(null)).isSameAs(level1);
        assertThat(tools.listCategories("c1")).isEmpty();
    }

    @Test
    @DisplayName("search_platform_knowledge：topK 钳制到 [1, 10]")
    void searchKnowledge_clampsTopK() {
        var tools = tools();
        when(knowledgeRetrievalService.search("退款", 10)).thenReturn(List.of(new KnowledgeHit("d1", "退款规则", "正文", 0.9)));
        when(knowledgeRetrievalService.search("退款", 1)).thenReturn(List.of());

        assertThat(tools.searchPlatformKnowledge("退款", 50)).hasSize(1);
        assertThat(tools.searchPlatformKnowledge("退款", -3)).isEmpty();

        verify(knowledgeRetrievalService).search("退款", 10);
        verify(knowledgeRetrievalService).search("退款", 1);
    }

    @Test
    @DisplayName("每次调用计 easyorange.mcp.tool{name} 指标")
    void recordsToolCallMetrics() {
        var tools = tools();
        when(assetSourcingService.search("显卡", 5)).thenReturn(List.of(hit("p1")));
        tools.searchProducts("显卡", null);
        tools.listCategories(null);

        assertThat(registry.get("easyorange.mcp.tool")
                        .tag("name", "search_products")
                        .counter()
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.get("easyorange.mcp.tool")
                        .tag("name", "list_categories")
                        .counter()
                        .count())
                .isEqualTo(1.0);
    }

    private AssetHit hit(String id) {
        return new AssetHit(id, "九成新显卡", new BigDecimal("1999"), "电脑硬件", "九成新", 1.0);
    }
}
