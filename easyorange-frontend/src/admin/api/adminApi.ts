import { request } from '@/api/core/request';
import type { PageResult, Result } from '@/types';
import type {
    CreateKnowledgeDocRequest,
    KnowledgeDoc,
    RetrievalEvalCase,
    RetrievalEvalLine,
    RetrievalEvalRun,
} from '../types/admin';

const ADMIN_API_PREFIX = '/admin';

/**
 * 后端 `JacksonConfig` 给 `Long` 全局注册了 `ToStringSerializer`：DTO 里声明成包装类型
 * `Long` 的计数字段，线上到的是 `"24"` 而不是 `24`，但前端类型标注是 `number`。
 * 不收敛就会在算术处悄悄退化成字符串拼接——统计页分类占比恒显示 0% 就是这么来的
 * （`0 + "40" + "24"` 得到天文数字，再拿 40 去除，四舍五入后还是 0）。
 * 声明为数字的计数字段，一律在 API 层收敛。
 */
function coerceCounts<T>(row: T, keys: readonly (keyof T)[]): T {
    const next = { ...row };
    for (const key of keys) {
        if (next[key] != null) {
            next[key] = Number(next[key]) as T[keyof T];
        }
    }
    return next;
}

/** `PageResult.total` 是 `long`，同样是字符串；收敛后大数才有千位分隔。 */
function coercePageTotal<T>(res: Result<PageResult<T>>): Result<PageResult<T>> {
    return { ...res, data: { ...res.data, total: Number(res.data.total ?? 0) } };
}

/**
 * 管理端 API —— 只覆盖 AI 运维两个面（知识库管理 + 检索质量回看）。
 * 业务 CRUD（用户 / 商品审核 / 订单 / 分类 / 仪表盘）的端点由后端 REST 承载，
 * 前端不再平行维护一层调用与 DTO（2026-10-13 裁剪，路由同 rationale 见 AdminRoutes）。
 */
export const adminApi = {
    async getKnowledgeDocs(pageNum = 1, pageSize = 10) {
        return coercePageTotal(
            await request<PageResult<KnowledgeDoc>>(`${ADMIN_API_PREFIX}/knowledge`, {
                params: { pageNum, pageSize },
            })
        );
    },

    /** 检索评测批次列表（hit@5 / MRR 趋势）—— 空数组即「还没跑过评测」，与「跑了全没命中」分得开 */
    async getRetrievalEvalRuns(line: RetrievalEvalLine, limit = 20) {
        const response = await request<RetrievalEvalRun[]>(`${ADMIN_API_PREFIX}/ai/retrieval-eval/runs`, {
            params: { line, limit },
        });
        // reciprocalRank 来自 DECIMAL 列、caseCount 等计数在后端是包装类型 Long —— 一律收敛成 number
        return {
            ...response,
            data: (response.data ?? []).map(row =>
                coerceCounts(row, ['caseCount', 'hitCount', 'hitRateAt5', 'hitRatePct', 'mrr'])
            ),
        };
    },

    async getRetrievalEvalCases(runId: string, pageNum = 1, pageSize = 10) {
        const page = await coercePageTotal(
            await request<PageResult<RetrievalEvalCase>>(`${ADMIN_API_PREFIX}/ai/retrieval-eval/cases`, {
                params: { runId, pageNum, pageSize },
            })
        );
        return {
            ...page,
            data: {
                ...page.data,
                records: (page.data?.records ?? []).map(row => coerceCounts(row, ['reciprocalRank'])),
            },
        };
    },

    createKnowledgeDoc(data: CreateKnowledgeDocRequest) {
        return request<string>(`${ADMIN_API_PREFIX}/knowledge`, {
            method: 'POST',
            body: data,
        });
    },

    deleteKnowledgeDoc(id: string) {
        return request<void>(`${ADMIN_API_PREFIX}/knowledge/${id}`, {
            method: 'DELETE',
        });
    },

    reindexKnowledge() {
        return request<number>(`${ADMIN_API_PREFIX}/knowledge/reindex`, {
            method: 'POST',
        });
    },
};
