package com.cartethyia.easyorange.ai.domain.model;

import java.util.List;

/**
 * 金标准评测用例 — {@code eval/golden-set.yaml} 的一条。
 *
 * @param id              用例 ID（chat-001 / retr-001 …）
 * @param scope           场景（chat / retrieval）
 * @param question        用户问题
 * @param referenceAnswer 参考回答（Judge 对照打分；retrieval 用例可空）
 * @param goldDocIds      期望命中的知识库文档 ID（检索指标 hit@5/MRR 用，可空）
 * @param expectedTools   期望模型走过的工具（路由准确率用；可空 = 该用例不参与路由评估）。
 *                        判据是「期望工具都在实际路径里出现」，不比顺序与次数 —— 多查一步不算走错，
 *                        把顺序也卡死会让指标对合理波动过敏
 */
public record GoldenSetCase(
        String id,
        String scope,
        String question,
        String referenceAnswer,
        List<String> goldDocIds,
        List<String> expectedTools) {}
