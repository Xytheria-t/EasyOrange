import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { RetrievalEvalCase, RetrievalEvalLine, RetrievalEvalRun } from '../types/admin';

/**
 * 检索质量回看 —— 批次列表与用例下钻。批次按时间倒序，下钻按未命中优先（后端口径）。
 * 明细只在选中批次后才请求：runId 为空时后端直接回空页，前端也不必发一次无意义的请求。
 */

export const ADMIN_RETRIEVAL_EVAL_KEYS = {
    all: ['admin', 'retrieval-eval'] as const,
    runs: (line: RetrievalEvalLine) => [...ADMIN_RETRIEVAL_EVAL_KEYS.all, 'runs', line] as const,
    cases: (runId: string, pageNum: number, pageSize: number) =>
        [...ADMIN_RETRIEVAL_EVAL_KEYS.all, 'cases', runId, pageNum, pageSize] as const,
};

export function useAdminRetrievalEvalRuns(line: RetrievalEvalLine) {
    return useQuery({
        queryKey: ADMIN_RETRIEVAL_EVAL_KEYS.runs(line),
        queryFn: async () => (await adminApi.getRetrievalEvalRuns(line)).data,
        placeholderData: keepPreviousData,
        staleTime: 60 * 1000,
        gcTime: 5 * 60 * 1000,
        retry: 1,
    });
}

export function useAdminRetrievalEvalCases(runId: string | null, pageNum: number, pageSize: number) {
    return useQuery({
        queryKey: ADMIN_RETRIEVAL_EVAL_KEYS.cases(runId ?? '', pageNum, pageSize),
        queryFn: async () => (await adminApi.getRetrievalEvalCases(runId as string, pageNum, pageSize)).data,
        // 未选中批次时不发请求：enabled=false 下 data 恒 undefined，页面据此显示「先选一个批次」
        enabled: !!runId,
        select: (data: { records: RetrievalEvalCase[]; total: number; current: number }) => ({
            records: data.records,
            total: data.total,
            current: data.current,
        }),
        placeholderData: keepPreviousData,
        staleTime: 60 * 1000,
        gcTime: 5 * 60 * 1000,
        retry: 1,
    });
}

export type { RetrievalEvalCase, RetrievalEvalLine, RetrievalEvalRun };
