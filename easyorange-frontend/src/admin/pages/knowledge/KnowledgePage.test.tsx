import { fireEvent, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { PageResult } from '@/types';
import { adminApi } from '../../api/adminApi';
import type { KnowledgeDoc } from '../../types/admin';
import KnowledgePage from './KnowledgePage';

vi.mock('../../api/adminApi', () => ({
    adminApi: {
        getKnowledgeDocs: vi.fn(),
        createKnowledgeDoc: vi.fn(),
        deleteKnowledgeDoc: vi.fn(),
        reindexKnowledge: vi.fn(),
    },
}));

const docs: KnowledgeDoc[] = [
    {
        id: 'kb-0001',
        title: '平台交易流程',
        source: '平台规则',
        status: 'INDEXED',
        chunkCount: 2,
        createTime: '2026-08-14 10:00:00',
    },
    {
        id: 'kb-0002',
        title: '退款规则',
        source: '平台规则',
        status: 'PENDING',
        chunkCount: 0,
        createTime: '2026-08-14 11:00:00',
    },
];

const pageResult: PageResult<KnowledgeDoc> = {
    records: docs,
    total: 2,
    current: 1,
    size: 10,
    pages: 1,
};

describe('KnowledgePage (知识库管理)', () => {
    it('渲染文档列表与状态标签', async () => {
        vi.mocked(adminApi.getKnowledgeDocs).mockResolvedValue({
            code: 'A0000',
            message: 'ok',
            data: pageResult,
            timestamp: 0,
        });
        renderWithProviders(<KnowledgePage />);

        await waitFor(() => {
            expect(screen.getByText('平台交易流程')).toBeInTheDocument();
            expect(screen.getByText('退款规则')).toBeInTheDocument();
            expect(screen.getByText('已索引')).toBeInTheDocument();
            expect(screen.getByText('待索引')).toBeInTheDocument();
        });
    });

    it('删除 -> 确认弹窗 -> 调用删除接口', async () => {
        vi.mocked(adminApi.getKnowledgeDocs).mockResolvedValue({
            code: 'A0000',
            message: 'ok',
            data: pageResult,
            timestamp: 0,
        });
        vi.mocked(adminApi.deleteKnowledgeDoc).mockResolvedValue({
            code: 'A0000',
            message: 'ok',
            data: undefined,
            timestamp: 0,
        });
        renderWithProviders(<KnowledgePage />);

        await waitFor(() => {
            expect(screen.getByText('平台交易流程')).toBeInTheDocument();
        });
        fireEvent.click(screen.getAllByRole('button', { name: '删除' })[0]);

        await waitFor(() => {
            expect(screen.getByText(/确认删除「平台交易流程」/)).toBeInTheDocument();
        });
        const confirmButtons = screen.getAllByRole('button', { name: '删除' });
        fireEvent.click(confirmButtons[confirmButtons.length - 1]);

        await waitFor(() => {
            expect(adminApi.deleteKnowledgeDoc).toHaveBeenCalledWith('kb-0001');
        });
    });

    it('补索引按钮 -> 调用 reindex 接口', async () => {
        vi.mocked(adminApi.getKnowledgeDocs).mockResolvedValue({
            code: 'A0000',
            message: 'ok',
            data: pageResult,
            timestamp: 0,
        });
        vi.mocked(adminApi.reindexKnowledge).mockResolvedValue({
            code: 'A0000',
            message: 'ok',
            data: 2,
            timestamp: 0,
        });
        renderWithProviders(<KnowledgePage />);

        fireEvent.click(screen.getByRole('button', { name: '补索引' }));

        await waitFor(() => {
            expect(adminApi.reindexKnowledge).toHaveBeenCalled();
        });
    });
});
