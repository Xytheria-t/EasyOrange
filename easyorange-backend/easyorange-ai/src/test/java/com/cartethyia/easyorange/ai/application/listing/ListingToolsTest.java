package com.cartethyia.easyorange.ai.application.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.chat.ChatTools;
import com.cartethyia.easyorange.ai.application.retrieval.AssetSourcingAppService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalAppService;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.port.CategoryCatalogPort;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 发布链路工具面测试 — 检索观察 / 冗余收敛与买家侧同一份机制（{@code RetrievalObservations}），这里验
 * listing 侧的接线与工具名契约：与 ChatTools 的四个同名工具必须逐字相等，漂移会让 golden-set 的
 * expected_tools、前端 STEP_LABELS 两处一起失锚。
 */
@DisplayName("ListingTools (发布链路工具面) -> 测试")
class ListingToolsTest {

    private final KnowledgeRetrievalAppService retrievalService = mock(KnowledgeRetrievalAppService.class);
    private final AssetSourcingAppService assetSourcingService = mock(AssetSourcingAppService.class);
    private final CategoryCatalogPort categoryCatalogPort = mock(CategoryCatalogPort.class);

    private ListingTools tools;

    @BeforeEach
    void setUp() {
        tools = new ListingTools(retrievalService, assetSourcingService, categoryCatalogPort);
    }

    @Test
    @DisplayName("四个共享工具名与 ChatTools 逐字相等 —— golden-set 与 STEP_LABELS 只认一份词汇")
    void toolNamesMatchChatFacet() {
        assertThat(ListingTools.TOOL_KNOWLEDGE_SEARCH).isEqualTo(ChatTools.TOOL_KNOWLEDGE_SEARCH);
        assertThat(ListingTools.TOOL_PRODUCT_SEARCH).isEqualTo(ChatTools.TOOL_PRODUCT_SEARCH);
        assertThat(ListingTools.TOOL_MARKET_PRICE_STATS).isEqualTo(ChatTools.TOOL_MARKET_PRICE_STATS);
        assertThat(ListingTools.TOOL_FINISH).isEqualTo(ChatTools.TOOL_FINISH);
    }

    @Test
    @DisplayName("knowledge_search 首次命中 -> 正常观察并进累加器")
    void knowledgeSearch_firstHit() {
        when(retrievalService.search(anyString(), anyInt()))
                .thenReturn(List.of(new KnowledgeHit("kb-0005", "禁售品类", "烟酒禁售", 0.9)));

        String observation = tools.searchKnowledge("查禁售", "相机 禁售");

        assertThat(observation).contains("命中 1 条").contains("禁售品类");
        assertThat(tools.knowledgeHits()).hasSize(1);
    }

    @Test
    @DisplayName("换关键词召回同一批 -> 收敛提示（与买家侧同一判据单一来源）")
    void knowledgeSearch_convergesOnRedundantHits() {
        when(retrievalService.search(anyString(), anyInt()))
                .thenReturn(List.of(new KnowledgeHit("kb-0005", "禁售品类", "烟酒禁售", 0.9)));
        tools.searchKnowledge("查禁售", "相机 禁售");

        String observation = tools.searchKnowledge("换个词", "能不能卖相机");

        assertThat(observation).contains("无新增信息");
        assertThat(tools.knowledgeHits()).hasSize(1);
    }

    @Test
    @DisplayName("product_search 召回带价格观察，重复召回不重复计入累加器")
    void productSearch_accumulatesWithDedup() {
        AssetHit hit = new AssetHit("p-1", "索尼 A7M3", BigDecimal.valueOf(8000), "相机", "九五新", 0.9);
        when(assetSourcingService.search(anyString(), anyInt())).thenReturn(List.of(hit));

        assertThat(tools.searchProducts("召回同类", "索尼 A7M3")).contains("召回 1 件").contains("¥8000");
        // 同一件再召回：观察收敛成「无新增」（与买家侧同一判据），累加器不重复计入
        assertThat(tools.searchProducts("再召回", "A7M3 二手")).contains("无新增信息");
        assertThat(tools.assetHits()).hasSize(1);
    }

    @Test
    @DisplayName("market_price_stats：召回前统计提示空集，召回后出行情")
    void marketPriceStats_requiresRecallFirst() {
        assertThat(tools.summarizeMarketPrice("看行情")).contains("暂无可统计的在售资产");

        when(assetSourcingService.search(anyString(), anyInt()))
                .thenReturn(List.of(
                        new AssetHit("p-1", "索尼 A7M3", BigDecimal.valueOf(8000), "相机", "九五新", 0.9),
                        new AssetHit("p-2", "索尼 A7M3 套机", BigDecimal.valueOf(9000), "相机", "九五新", 0.8)));
        tools.searchProducts("召回同类", "索尼 A7M3");

        assertThat(tools.summarizeMarketPrice("看行情")).isEqualTo("当前 2 件在售，均价 ¥8500，价格区间 ¥8000-¥9000");
    }

    @Test
    @DisplayName("list_categories 返回平台清单（categoryName 取值锚点），空清单有明确观察")
    void listCategories_returnsCatalog() {
        when(categoryCatalogPort.listAvailableCategoryNames()).thenReturn(List.of("手机数码", "图书教材"));
        assertThat(tools.listCategories("看清单")).isEqualTo("可用分类：手机数码、图书教材");

        when(categoryCatalogPort.listAvailableCategoryNames()).thenReturn(List.of());
        assertThat(tools.listCategories("看清单")).isEqualTo("平台暂无可用分类");
    }
}
