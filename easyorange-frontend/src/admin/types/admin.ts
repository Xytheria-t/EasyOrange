/**
 * 管理端 API 的 DTO —— 只覆盖 AI 运维两个面（知识库管理 + 检索质量回看）。
 * 业务 CRUD 的 DTO 随前端页面一并裁撤（2026-10-13）；后端 DTO 不受影响。
 */

/** 知识库文档（RAG 摄入管线，管理端）— 列表接口不返回正文（content） */
export interface KnowledgeDoc {
    id: string;
    title: string;
    source: string;
    status: 'PENDING' | 'INDEXED' | 'FAILED';
    chunkCount: number;
    createTime: string;
}

export interface CreateKnowledgeDocRequest {
    title: string;
    content: string;
    source: string;
}

/** 检索评测线（按 case_id 前缀判定）— KNOWLEDGE 真实 embedding 语义空间 / ASSET 合成向量语料 */
export type RetrievalEvalLine = 'KNOWLEDGE' | 'ASSET';

/** 一次评测批次（run_id）的汇总行 — hit@5 与 MRR 的分母都是本批用例数 */
export interface RetrievalEvalRun {
    runId: string;
    line: RetrievalEvalLine;
    lineLabel: string;
    caseCount: number;
    hitCount: number;
    hitRateAt5: number;
    hitRatePct: number;
    mrr: number;
    createdAt: string | null;
}

/** 单条检索用例的采样明细 — hitRank 为首个命中位次，未命中时为 null */
export interface RetrievalEvalCase {
    caseId: string;
    queryText: string | null;
    goldDocIds: string | null;
    hitAt5: boolean;
    reciprocalRank: number;
    hitRank: number | null;
}
