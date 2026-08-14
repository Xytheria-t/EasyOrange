import { act, fireEvent, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { aiApi } from '@/api/aiApi';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { ChatStreamEvent } from '@/types/ai';
import PlaygroundPage from './PlaygroundPage';

vi.mock('@/api/aiApi', () => ({
    aiApi: {
        chatStream: vi.fn(),
        feedback: vi.fn().mockResolvedValue({ code: 'A0000', message: 'ok', data: null, timestamp: 0 }),
        chat: vi.fn(),
        knowledgeSearch: vi.fn(),
        suggestPrice: vi.fn(),
        autoListing: vi.fn(),
        semanticSearch: vi.fn(),
        answerQuestion: vi.fn(),
        generateCopy: vi.fn(),
    },
}));

const mockedChatStream = vi.mocked(aiApi.chatStream);

function emit(events: ChatStreamEvent[]) {
    act(() => {
        for (const event of events) {
            const callback = mockedChatStream.mock.calls.at(-1)?.[1];
            callback?.(event);
        }
    });
}

describe('PlaygroundPage (AI 智能助手)', () => {
    it('渲染欢迎语与建议问题', () => {
        renderWithProviders(<PlaygroundPage />);

        expect(screen.getByText(/EasyOrange AI 助手/)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '平台交易流程是什么？' })).toBeInTheDocument();
    });

    it('发送问题 -> 流式 token 逐字渲染 + 知识库来源 + done 收口', async () => {
        mockedChatStream.mockResolvedValue(undefined);
        renderWithProviders(<PlaygroundPage />);

        const input = screen.getByLabelText('问题输入');
        fireEvent.change(input, { target: { value: '怎么退款？' } });
        fireEvent.click(screen.getByRole('button', { name: '发送' }));

        expect(mockedChatStream).toHaveBeenCalledWith(
            expect.objectContaining({ question: '怎么退款？', sessionId: expect.stringContaining('sess-') }),
            expect.any(Function),
            expect.any(AbortSignal)
        );

        emit([
            { type: 'token', data: '可以' },
            { type: 'sources', data: ['退款规则'] },
            { type: 'token', data: '退款' },
            { type: 'done', data: '可以退款' },
        ]);

        await waitFor(() => {
            expect(screen.getByText('可以退款')).toBeInTheDocument();
            expect(screen.getByText('[来源:退款规则]')).toBeInTheDocument();
        });
    });

    it('error 事件 -> 展示降级文案', async () => {
        mockedChatStream.mockResolvedValue(undefined);
        renderWithProviders(<PlaygroundPage />);

        fireEvent.change(screen.getByLabelText('问题输入'), { target: { value: '你好' } });
        fireEvent.click(screen.getByRole('button', { name: '发送' }));
        emit([{ type: 'error', data: '今日 AI 调用预算已用尽，请明天再试' }]);

        await waitFor(() => {
            expect(screen.getByText('今日 AI 调用预算已用尽，请明天再试')).toBeInTheDocument();
        });
    });

    it('👍 反馈 -> 调用 feedback 接口', async () => {
        mockedChatStream.mockResolvedValue(undefined);
        renderWithProviders(<PlaygroundPage />);

        fireEvent.change(screen.getByLabelText('问题输入'), { target: { value: '怎么退款？' } });
        fireEvent.click(screen.getByRole('button', { name: '发送' }));
        emit([
            { type: 'token', data: '可以退款' },
            { type: 'done', data: '可以退款' },
        ]);

        await waitFor(() => {
            expect(screen.getByText('可以退款')).toBeInTheDocument();
        });
        fireEvent.click(screen.getAllByRole('button', { name: '有帮助' })[0]);

        await waitFor(() => {
            expect(aiApi.feedback).toHaveBeenCalledWith(
                expect.objectContaining({ scope: 'chat', answer: '可以退款', helpful: true })
            );
        });
    });
});
