package com.cartethyia.easyorange.ai.domain.model;

import java.util.List;
import java.util.stream.Stream;

/**
 * 引用来源 — 前端据此决定「这条引用能点什么」。
 * <p>
 * 此前只下发标题字符串，前端只能渲染成不可点的灰色胶囊：规则引用和商品引用长得一样，
 * 后者更糟 —— 明明是件在售资产却标成「知识库来源」，点不动也进不去商品页。
 * 带上 {@link #type()} 与 {@link #id()} 后，商品引用可以按 id 拉出真实商品卡，
 * 规则引用可以按文档维度展示，两条引用链各走各的。
 * <p>
 * <b>id 是防幻觉的锚点，不是装饰</b>：它直接来自召回结果（docId / productId），
 * 前端拿它去查的都是真实记录 —— 模型在正文里写错标题，点开仍会落到真实存在的那一条。
 *
 * @param type  来源类型（{@link Type}），前端按它分流渲染
 * @param id    命中文档 ID 或资产 ID
 * @param title 来源标题（展示用）
 */
public record ChatSource(Type type, String id, String title) {

    /** 来源类型 —— 与前端 {@code types/ai.ts} 的 {@code ChatSource['type']} 字面量对齐。 */
    public enum Type {
        /** 平台规则知识库片段，可展开看原文。 */
        KNOWLEDGE,
        /** 在售资产，可点进商品详情。 */
        ASSET
    }

    public static ChatSource from(KnowledgeHit hit) {
        return new ChatSource(Type.KNOWLEDGE, hit.docId(), hit.title());
    }

    public static ChatSource from(AssetHit hit) {
        return new ChatSource(Type.ASSET, hit.productId(), hit.title());
    }

    /**
     * 合并两路召回的来源，资产优先。
     * <p>
     * 资产排在前面而非按召回顺序混排：找货是主链路，规则来源不该在数量上把它挤出可见范围。
     * 去重按 {@code (type, id)} 而非标题 —— 同名资产是两条不同记录，按标题去重会误删。
     */
    public static List<ChatSource> merge(List<KnowledgeHit> hits, List<AssetHit> assets, int limit) {
        return Stream.of(
                        assets.stream().map(ChatSource::from).toList(),
                        hits.stream().map(ChatSource::from).toList())
                .flatMap(List::stream)
                .distinct()
                .limit(limit)
                .toList();
    }
}
