import { act, fireEvent, screen, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { aiApi } from '@/api/aiApi';
import { server } from '@/testUtils/mocks/server';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { ChatStreamEvent } from '@/types/ai';
import PlaygroundPage from './PlaygroundPage';

vi.mock('@/api/aiApi', () => ({
    aiApi: {
        chatStream: vi.fn(),
        feedback: vi.fn().mockResolvedValue({ code: 'A0000', message: 'ok', data: null, timestamp: 0 }),
    },
}));

// 商品卡的图片走 Image 组件的 WebP 能力探测，jsdom 的 canvas.toDataURL 返回 null 会直接抛。
// 与 ProductCard.test.tsx 同一处理：测试里替掉 Image，组件本身的图片逻辑另有测试覆盖。
vi.mock('@/components/ui/Image', () => ({
    Image: ({ src, alt, className, style, ...props }: React.ImgHTMLAttributes<HTMLImageElement>) => (
        <img src={src} alt={alt} className={className} style={style} data-mocked="true" {...props} />
    ),
    preloadImage: vi.fn(),
    preloadImages: vi.fn(),
    clearImageCache: vi.fn(),
    buildThumbnailUrl: vi.fn(),
    buildResponsiveUrl: vi.fn(),
    buildViewUrl: vi.fn(),
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
    // 默认成功收口；个别用例自行覆盖实现。不重置的话，上一条用例的
    // mockRejectedValue / 挂起实现会漏到下一条，断言互相干扰
    beforeEach(() => {
        mockedChatStream.mockReset();
        mockedChatStream.mockResolvedValue(undefined);
    });

    it('渲染欢迎语与建议问题', () => {
        renderWithProviders(<PlaygroundPage />);

        expect(screen.getByText(/EasyOrange AI 助手/)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '3000 以内适合拍视频的手机有哪些？' })).toBeInTheDocument();
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
            { type: 'sources', data: [{ type: 'knowledge', id: 'kb-0002', title: '退款规则' }] },
            { type: 'token', data: '退款' },
            { type: 'done', data: '可以退款' },
        ]);

        await waitFor(() => {
            expect(screen.getByText('可以退款')).toBeInTheDocument();
            expect(screen.getByText('规则 · 退款规则')).toBeInTheDocument();
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

    it('流中途失败 -> 保留已流出的部分答案，错误提示另起一条', async () => {
        mockedChatStream.mockResolvedValue(undefined);
        renderWithProviders(<PlaygroundPage />);

        fireEvent.change(screen.getByLabelText('问题输入'), { target: { value: '怎么退款？' } });
        fireEvent.click(screen.getByRole('button', { name: '发送' }));
        emit([
            { type: 'token', data: '可以' },
            { type: 'token', data: '申请' },
            { type: 'error', data: 'AI 服务暂时不可用，请稍后重试' },
        ]);

        await waitFor(() => {
            expect(screen.getByText('可以申请')).toBeInTheDocument();
        });
        // 关键：半截答案没被错误文案顶掉
        expect(screen.getByText('AI 服务暂时不可用，请稍后重试')).toBeInTheDocument();
    });

    it('流失败 -> 提供重试按钮，点击后重新发同一问题', async () => {
        mockedChatStream.mockResolvedValue(undefined);
        renderWithProviders(<PlaygroundPage />);

        fireEvent.change(screen.getByLabelText('问题输入'), { target: { value: '怎么退款？' } });
        fireEvent.click(screen.getByRole('button', { name: '发送' }));
        emit([{ type: 'error', data: 'AI 服务暂时不可用' }]);

        const retry = await screen.findByRole('button', { name: /重试/ });
        fireEvent.click(retry);

        await waitFor(() => {
            expect(mockedChatStream).toHaveBeenCalledTimes(2);
        });
        expect(mockedChatStream).toHaveBeenLastCalledWith(
            expect.objectContaining({ question: '怎么退款？' }),
            expect.any(Function),
            expect.any(AbortSignal)
        );
    });

    it('生成中 -> 发送按钮变停止，点击中止后输入框解锁', async () => {
        // 挂起到 signal 触发：真实的 streamChat 收到 abort 会抛 AbortError
        mockedChatStream.mockImplementation(
            (_data, _onEvent, signal) =>
                new Promise((_resolve, reject) => {
                    signal?.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')));
                })
        );
        renderWithProviders(<PlaygroundPage />);

        fireEvent.change(screen.getByLabelText('问题输入'), { target: { value: '怎么退款？' } });
        fireEvent.click(screen.getByRole('button', { name: '发送' }));
        emit([{ type: 'token', data: '正在想' }]);

        const stop = await screen.findByRole('button', { name: '停止生成' });
        fireEvent.click(stop);

        await waitFor(() => {
            expect(screen.getByRole('button', { name: '发送' })).toBeInTheDocument();
        });
        // 中止不是故障：半截答案留着，标「已停止」
        expect(screen.getByText('正在想')).toBeInTheDocument();
        expect(screen.getByText('已停止生成')).toBeInTheDocument();
    });

    it('请求失败 -> 报错并解锁输入，不永久卡在生成中', async () => {
        mockedChatStream.mockRejectedValue(new Error('stream request failed: HTTP 500'));
        renderWithProviders(<PlaygroundPage />);

        fireEvent.change(screen.getByLabelText('问题输入'), { target: { value: '怎么退款？' } });
        fireEvent.click(screen.getByRole('button', { name: '发送' }));

        await waitFor(() => {
            expect(screen.getByText('连接中断，请重试')).toBeInTheDocument();
        });
        expect(screen.getByRole('button', { name: '发送' })).toBeInTheDocument();
    });

    it('资产来源 -> 渲染可点进商品页的真实商品卡', async () => {
        // AI 侧的召回物只有 id / 标题，商品卡要的真实字段走公开 batch 接口补全
        server.use(
            http.post('/api/products/batch', () =>
                HttpResponse.json({
                    code: 'A0000',
                    message: 'ok',
                    data: [
                        {
                            id: 'p-1',
                            title: '二手 iPhone 13',
                            price: 2800,
                            categoryId: 'c-1',
                            categoryName: '手机',
                            condition: 2,
                            sellerId: 'u-9',
                            sellerName: '小李',
                            images: [],
                            status: 'ONLINE',
                        },
                    ],
                    timestamp: 0,
                })
            )
        );
        mockedChatStream.mockResolvedValue(undefined);
        renderWithProviders(<PlaygroundPage />);

        fireEvent.change(screen.getByLabelText('问题输入'), { target: { value: '3000 以内的手机' } });
        fireEvent.click(screen.getByRole('button', { name: '发送' }));
        emit([
            { type: 'sources', data: [{ type: 'asset', id: 'p-1', title: '二手 iPhone 13' }] },
            { type: 'done', data: '为你找到这台' },
        ]);

        const link = await screen.findByRole('link', { name: /二手 iPhone 13/ });
        expect(link).toHaveAttribute('href', '/products/p-1');
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
