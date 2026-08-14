package com.cartethyia.easyorange.ai.eval;

import java.util.List;

/**
 * 金标准评测用例 — {@code eval/golden-set.yaml} 的一条。
 *
 * @param id              用例 ID（chat-001 / retr-001 …）
 * @param scope           场景（chat）
 * @param question        用户问题
 * @param referenceAnswer 参考回答（Judge 对照打分；检索类用例可空）
 * @param goldDocIds      期望命中的知识库文档 ID（检索指标 hit@5/MRR 用，可空）
 */
public record GoldenSetCase(
        String id, String scope, String question, String referenceAnswer, List<String> goldDocIds) {}
