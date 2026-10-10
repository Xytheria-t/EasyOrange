package com.cartethyia.easyorange.ai.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * 引用来源 — 前端按 {@link #id()} 决定这条引用能点什么：商品引用拉真实商品卡、规则引用按文档展示，
 * 只下发标题字符串会让商品引用被误标成知识库来源且点不动。id 来自召回结果（docId / productId），
 * 是防幻觉的锚点 —— 模型在正文里写错标题，点开仍会落到真实存在的那一条。
 */
public record ChatSource(Type type, String id, String title) {

    /**
     * 来源类型 —— 与前端 {@code types/ai.ts} 的 {@code ChatSource['type']} 字面量对齐；{@code @JsonValue} 是必需手段：
     * 枚举名默认下发大写，前端 {@code parseSources} 判小写字面量，缺了它商品引用会被全判成知识库来源、渲染成点不动的胶囊。
     */
    public enum Type {
        /** 平台规则知识库片段，可展开看原文。 */
        KNOWLEDGE("knowledge"),
        /** 在售资产，可点进商品详情。 */
        ASSET("asset");

        @JsonValue
        private final String code;

        Type(String code) {
            this.code = code;
        }

        @JsonCreator
        public static Type fromCode(String code) {
            return Arrays.stream(values())
                    .filter(t -> t.code.equals(code))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("未知来源类型: " + code));
        }
    }

    public static ChatSource from(KnowledgeHit hit) {
        return new ChatSource(Type.KNOWLEDGE, hit.docId(), hit.title());
    }

    public static ChatSource from(AssetHit hit) {
        return new ChatSource(Type.ASSET, hit.productId(), hit.title());
    }

    /** 合并两路召回的来源，资产优先（找货是主链路，规则来源不该在数量上挤出它）；去重按 (type, id) 而非标题 —— 同名资产是两条不同记录。 */
    public static List<ChatSource> merge(List<KnowledgeHit> knowledgeHits, List<AssetHit> assetHits, int limit) {
        return Stream.of(
                        assetHits.stream().map(ChatSource::from).toList(),
                        knowledgeHits.stream().map(ChatSource::from).toList())
                .flatMap(List::stream)
                .distinct()
                .limit(limit)
                .toList();
    }
}
