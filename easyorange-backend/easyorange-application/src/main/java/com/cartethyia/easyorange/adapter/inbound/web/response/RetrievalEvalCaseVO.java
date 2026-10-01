package com.cartethyia.easyorange.adapter.inbound.web.response;

/**
 * 检索评测用例明细视图 — 管理端下钻行。
 *
 * @param hitRank 首个命中位次（1 起）；未命中为 null，前端据此显示「未命中」而不是「第 0 位」
 */
public record RetrievalEvalCaseVO(
        String caseId, String queryText, String goldDocIds, boolean hitAt5, double reciprocalRank, Integer hitRank) {}
