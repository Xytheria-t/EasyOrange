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

    // 后端 AgentTools 的工具面（单一来源：easyorange-backend 的 AgentTools.TOOL_* 常量）。
    // 后端加工具必须同步 PlaygroundPage 的 STEP_LABELS，否则该步渲染成裸工具名 —— 本用例就是这条同步的断言。
    const BACKEND_TOOLS: Record<string, string> = {
        knowledge_search: '查规则',
        product_search: '找资产',
        product_detail: '看详情',
        market_price_stats: '看行情',
        compare_assets: '比候选',
        remember_preference: '记偏好',
        finish: '生成回答',
    };

    it('每个后端工具在步骤区都有中文文案，不出现裸工具名', async () => {
        mockedChatStream.mockResolvedValue(undefined);
        renderWithProviders(<PlaygroundPage />);

        fireEvent.change(screen.getByLabelText('问题输入'), { target: { value: '预算 5000 想买笔记本，帮我挑一台' } });
        fireEvent.click(screen.getByRole('button', { name: '发送' }));

        // 不带 thought：走标签兜底分支（带 thought 时展示的是模型给出的理由，不是标签）
        emit(
            Object.keys(BACKEND_TOOLS).map((tool, index) => ({
                type: 'step' as const,
                data: { step: index + 1, tool },
            }))
        );

        await waitFor(() => {
            for (const [tool, label] of Object.entries(BACKEND_TOOLS)) {
                expect(screen.getByText(label)).toBeInTheDocument();
                expect(screen.queryByText(tool)).not.toBeInTheDocument();
            }
        });
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
            expect(screen.getByText('来源 · 退款规则')).toBeInTheDocument();
        });
    });

    it('assistant 回答按 Markdown 渲染(列表/加粗),用户消息保持纯文本', async () => {
        mockedChatStream.mockResolvedValue(undefined);
        renderWithProviders(<PlaygroundPage />);

        fireEvent.change(screen.getByLabelText('问题输入'), { target: { value: '怎么退款？' } });
        fireEvent.click(screen.getByRole('button', { name: '发送' }));
        emit([
            {
                type: 'done',
                data: '退款步骤：\n\n1. **打开订单**\n2. 申请退款\n\n| 项目 | 说明 |\n|---|---|\n| 时效 | 3 天 |',
            },
        ]);

        await waitFor(() => {
            expect(screen.getByRole('list')).toBeInTheDocument();
        });
        expect(screen.getByText('打开订单').tagName).toBe('STRONG');
        expect(screen.getByRole('table')).toBeInTheDocument();
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

    it('赞反馈 -> 调用 feedback 接口', async () => {
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
