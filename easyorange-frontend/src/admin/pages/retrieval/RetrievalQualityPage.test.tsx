import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { adminApi } from '../../api/adminApi';
import type { RetrievalEvalCase, RetrievalEvalRun } from '../../types/admin';
import RetrievalQualityPage from './RetrievalQualityPage';

vi.mock('../../api/adminApi', () => ({
    adminApi: {
        getRetrievalEvalRuns: vi.fn(),
        getRetrievalEvalCases: vi.fn(),
    },
}));

const runs: RetrievalEvalRun[] = [
    {
        runId: '0198abcd-1234-7aaa-8000-000000000001',
        line: 'KNOWLEDGE',
        lineLabel: '知识库检索',
        caseCount: 30,
        hitCount: 27,
        hitRateAt5: 0.9,
        hitRatePct: 90,
        mrr: 0.84,
        createdAt: '2026-10-02T00:30:00',
    },
];

const cases: RetrievalEvalCase[] = [
    {
        caseId: 'retr-028',
        queryText: '垫的那笔钱什么时候能拿回来',
        goldDocIds: 'kb-0019',
        hitAt5: false,
        reciprocalRank: 0,
        hitRank: null,
    },
    {
        caseId: 'retr-001',
        queryText: '交易流程',
        goldDocIds: 'kb-0001',
        hitAt5: true,
        reciprocalRank: 1,
        hitRank: 1,
    },
];

function ok<T>(data: T) {
    return { code: 'A0000', message: 'ok', data, timestamp: 0 };
}

describe('RetrievalQualityPage (检索质量回看)', () => {
    beforeEach(() => {
        // 用例间清调用记录：上一条用例点过批次就会调明细，不清的话「未点批次不发请求」这条必假失败
        vi.clearAllMocks();
    });

    it('渲染批次列表的 hit@5 与 MRR（不回显成裸比例）', async () => {
        vi.mocked(adminApi.getRetrievalEvalRuns).mockResolvedValue(ok(runs));

        renderWithProviders(<RetrievalQualityPage />);

        await waitFor(() => {
            expect(screen.getByText('90.0%')).toBeInTheDocument();
        });
        expect(screen.getByText('0.8400')).toBeInTheDocument();
    });

    it('空批次时给出「去跑评测」的提示，而不是显示 0% 命中率', async () => {
        vi.mocked(adminApi.getRetrievalEvalRuns).mockResolvedValue(ok([]));

        renderWithProviders(<RetrievalQualityPage />);

        await waitFor(() => {
            expect(screen.getByText(/暂无评测批次/)).toBeInTheDocument();
        });
    });

    it('选中批次后下钻出用例明细，未命中显示为「未命中」而非「第 0 位」', async () => {
        vi.mocked(adminApi.getRetrievalEvalRuns).mockResolvedValue(ok(runs));
        vi.mocked(adminApi.getRetrievalEvalCases).mockResolvedValue(
            ok({ records: cases, total: 2, current: 1, size: 10, pages: 1 })
        );

        renderWithProviders(<RetrievalQualityPage />);

        await waitFor(() => {
            expect(screen.getByText('知识库检索')).toBeInTheDocument();
        });
        fireEvent.click(screen.getByText('0198abcd'));

        await waitFor(() => {
            expect(screen.getByText('垫的那笔钱什么时候能拿回来')).toBeInTheDocument();
        });
        expect(screen.getByText('未命中')).toBeInTheDocument();
        expect(screen.getByText('第 1 位')).toBeInTheDocument();
        expect(adminApi.getRetrievalEvalCases).toHaveBeenCalledWith(runs[0].runId, 1, 10);
    });

    it('未选中批次时不请求明细，明细区提示先选批次', async () => {
        vi.mocked(adminApi.getRetrievalEvalRuns).mockResolvedValue(ok(runs));

        renderWithProviders(<RetrievalQualityPage />);

        await waitFor(() => {
            expect(screen.getByText(/先在上方选择一个批次/)).toBeInTheDocument();
        });
        expect(adminApi.getRetrievalEvalCases).not.toHaveBeenCalled();
    });
});
