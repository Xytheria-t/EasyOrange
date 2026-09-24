package com.cartethyia.easyorange.ai.application.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.retrieval.AssetSourcingService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalService;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 检索冗余判据测试 —— 「本轮命中的内容此前全部出现过」时观察换成收敛提示。
 * <p>
 * 这条判据同时是换快模型做决策的前置条件：实测决策模型换成 flash 后，在「已经检索过」的歧义场景里
 * 3 次全部选择继续检索（强模型是 3 次里 2 次选 finish），模型自己看不出「换关键词也没新内容」，
 * 必须由工具把这件事作为一条明确观察告诉它。
 */
@DisplayName("Agent 工具面 -> 检索冗余判据测试")
class AgentToolsTest {

    private final KnowledgeRetrievalService retrievalService = mock(KnowledgeRetrievalService.class);
    private final AssetSourcingService assetSourcingService = mock(AssetSourcingService.class);
    private final List<KnowledgeHit> knowledgeHits = new ArrayList<>();
    private final List<AssetHit> assets = new ArrayList<>();

    private AgentTools tools() {
        return new AgentTools(
                knowledgeHits,
                assets,
                new ArrayList<>(),
                retrievalService,
                assetSourcingService,
                mock(AssetDetailPort.class),
                mock(UserPreferenceRepository.class),
                "user-1");
    }

    private static KnowledgeHit doc(String docId, String title) {
        return new KnowledgeHit(docId, title, "正文", 0.5);
    }

    private static AssetHit asset(String id, String title) {
        return new AssetHit(id, title, BigDecimal.valueOf(1000), "相机", "九五新", 0.5);
    }

    @Nested
    @DisplayName("knowledge_search")
    class KnowledgeSearch {

        @Test
        @DisplayName("首次命中 -> 正常观察")
        void firstHit() {
            when(retrievalService.search(anyString(), anyInt())).thenReturn(List.of(doc("kb-1", "退款规则")));

            assertThat(tools().knowledgeSearch("查退款规则", "退款")).contains("命中 1 条").contains("退款规则");
        }

        @Test
        @DisplayName("换关键词召回同一批文档 -> 收敛提示而不是又一条「命中 N 条」")
        void repeatedHitsConverge() {
            // 第一次：kb-1 / kb-2 入累加器
            when(retrievalService.search(anyString(), anyInt()))
                    .thenReturn(List.of(doc("kb-1", "退款规则"), doc("kb-2", "运费规则")));
            tools().knowledgeSearch("查退款规则", "退款");

            // 第二次换个关键词，召回的仍是同一批 —— 对回答零增量
            when(retrievalService.search(anyString(), anyInt()))
                    .thenReturn(List.of(doc("kb-1", "退款规则"), doc("kb-2", "运费规则")));
            String observation = tools().knowledgeSearch("换个词再查", "退钱流程");

            assertThat(observation).contains("无新增信息").contains("finish");
        }

        @Test
        @DisplayName("部分新增 -> 正常观察，且只把新增的计入累加器")
        void partiallyNewHits() {
            when(retrievalService.search(anyString(), anyInt())).thenReturn(List.of(doc("kb-1", "退款规则")));
            tools().knowledgeSearch("查退款规则", "退款");

            // 1 旧 + 4 新 = 重合率 20%，低于阈值，仍算有效增量
            when(retrievalService.search(anyString(), anyInt()))
                    .thenReturn(List.of(
                            doc("kb-1", "退款规则"),
                            doc("kb-9", "评价规则"),
                            doc("kb-10", "运费规则"),
                            doc("kb-11", "禁售品类"),
                            doc("kb-12", "积分规则")));
            String observation = tools().knowledgeSearch("查评价规则", "评价");

            assertThat(observation).contains("命中 5 条");
            // 累加器不因重复召回而膨胀：kb-1 只算一次
            assertThat(knowledgeHits)
                    .extracting(KnowledgeHit::docId)
                    .containsExactly("kb-1", "kb-9", "kb-10", "kb-11", "kb-12");
        }

        @Test
        @DisplayName("换关键词只夹带一两个新条目（重合率 ≥ 60%）-> 收敛提示")
        void highOverlapConverges() {
            // 这是实测里真正发生的形态：topK 截断下换词返回高度重叠的一批，逐条判重会让模型无限换词
            when(retrievalService.search(anyString(), anyInt()))
                    .thenReturn(List.of(
                            doc("kb-1", "退款规则"),
                            doc("kb-2", "运费规则"),
                            doc("kb-3", "评价规则"),
                            doc("kb-4", "物流时效"),
                            doc("kb-5", "平台交易")));
            tools().knowledgeSearch("查退款规则", "退款");

            when(retrievalService.search(anyString(), anyInt()))
                    .thenReturn(List.of(
                            doc("kb-3", "评价规则"),
                            doc("kb-4", "物流时效"),
                            doc("kb-5", "平台交易"),
                            doc("kb-1", "退款规则"),
                            doc("kb-99", "新冒出来的一篇")));
            String observation = tools().knowledgeSearch("换个词再查", "退钱流程");

            assertThat(observation).contains("无新增信息").contains("finish");
        }

        @Test
        @DisplayName("完全没召回到 -> 提示可换关键词（与「无新增」是两回事）")
        void emptyResult() {
            when(retrievalService.search(anyString(), anyInt())).thenReturn(List.of());

            assertThat(tools().knowledgeSearch("查规则", "冷门词")).contains("未命中").contains("重试");
        }
    }

    @Nested
    @DisplayName("product_search")
    class ProductSearch {

        @Test
        @DisplayName("首次召回 -> 正常观察")
        void firstHit() {
            when(assetSourcingService.search(anyString(), anyInt())).thenReturn(List.of(asset("p-1", "索尼 A7M3")));

            assertThat(tools().productSearch("找微单", "微单相机")).contains("召回 1 件").contains("索尼 A7M3");
        }

        @Test
        @DisplayName("重复召回同一批在售资产 -> 收敛提示")
        void repeatedHitsConverge() {
            when(assetSourcingService.search(anyString(), anyInt())).thenReturn(List.of(asset("p-1", "索尼 A7M3")));
            tools().productSearch("找微单", "微单相机");

            when(assetSourcingService.search(anyString(), anyInt())).thenReturn(List.of(asset("p-1", "索尼 A7M3")));
            String observation = tools().productSearch("再找找", "二手微单 夜景");

            assertThat(observation).contains("无新增信息").contains("finish");
            assertThat(assets).hasSize(1);
        }

        @Test
        @DisplayName("没召回到在售资产 -> 提示换更宽泛的关键词")
        void emptyResult() {
            when(assetSourcingService.search(anyString(), anyInt())).thenReturn(List.of());

            assertThat(tools().productSearch("找东西", "不存在的品类")).contains("未召回").contains("重试");
        }
    }
}
