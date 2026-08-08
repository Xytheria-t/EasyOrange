package com.cartethyia.easyorange.ai.adapter.outbound.tool;

/** 工具类型：LLM 调用（外部依赖，需限流/预算治理）或本地规则引擎（亚毫秒，零 LLM 成本）。 */
public enum SearchToolKind {
    LLM,
    RULE
}
