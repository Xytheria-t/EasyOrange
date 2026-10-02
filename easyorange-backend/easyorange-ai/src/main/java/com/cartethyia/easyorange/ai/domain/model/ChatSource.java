package com.cartethyia.easyorange.ai.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * 引用来源 — 前端据此决定「这条引用能点什么」：商品引用按 {@link #id()} 拉真实商品卡，
 * 规则引用按文档维度展示，两条引用链各走各的（只下发标题字符串时，商品引用会被误标成
 * 知识库来源且点不动）。
 * <p>
 * <b>id 是防幻觉的锚点，不是装饰</b>：它直接来自召回结果（docId / productId），前端拿它去查的
 * 都是真实记录 —— 模型在正文里写错标题，点开仍会落到真实存在的那一条。
 */
public record ChatSource(Type type, String id, String title) {

    /**
     * 来源类型 —— 与前端 {@code types/ai.ts} 的 {@code ChatSource['type']} 字面量对齐。
     * <p>
     * {@code @JsonValue} 是必需的对齐手段而非装饰：枚举名默认按 {@code name()} 下发大写，
     * 前端 {@code parseSources} 判的是小写字面量，缺了它商品引用会被全判成知识库来源、
     * 渲染成点不动的胶囊。
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
