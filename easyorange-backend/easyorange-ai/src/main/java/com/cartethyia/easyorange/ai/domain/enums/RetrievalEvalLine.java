package com.cartethyia.easyorange.ai.domain.enums;

/**
 * 检索评测线 — 金标准集检索用例（{@code retr-*}）与 asset 找货消融（{@code asset-*}）共用同一张指标表，
 * 靠 case_id 前缀区分。两条线的语料与查询空间不同（真实 embedding vs 合成向量），混在一起算均值会得出
 * 无法解释的数，故分列。
 */
public enum RetrievalEvalLine {

    /** 金标准集知识库检索（retr-*）——真实 embedding 语义空间。 */
    KNOWLEDGE("知识库检索", "retr-"),

    /** asset 找货检索评测（asset-*）——合成向量语料那一层。 */
    ASSET("找货检索", "asset-");

    private final String label;

    private final String caseIdPrefix;

    RetrievalEvalLine(String label, String caseIdPrefix) {
        this.label = label;
        this.caseIdPrefix = caseIdPrefix;
    }

    public String label() {
        return label;
    }

    public String caseIdPrefix() {
        return caseIdPrefix;
    }
}
