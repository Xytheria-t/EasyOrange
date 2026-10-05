import { act, renderHook } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { useUIStore } from '@/store/uiStore';
import { server } from '@/testUtils/mocks/server';
import { useAutoListing } from './useAutoListing';

const listingResult = {
    title: '二手 iPhone 14',
    description: '九成新，无划痕',
    price: 4500,
    categoryName: '手机数码',
    conditionLevel: '2',
    location: '上海',
};

/** SSE 桩 — hook 消费的是真流（fetch + ReadableStream），事件按帧序流入 */
function sseResponse(frames: string[]) {
    const encoder = new TextEncoder();
    const body = new ReadableStream<Uint8Array>({
        start(controller) {
            for (const frame of frames) {
                controller.enqueue(encoder.encode(frame));
            }
            controller.close();
        },
    });
    return new HttpResponse(body, { headers: { 'Content-Type': 'text/event-stream' } });
}

const stepFrame = 'event: step\ndata: {"step":1,"tool":"list_categories","thought":"确认品类能不能挂"}\n\n';
const listingFrames = [stepFrame, `event: done\ndata: ${JSON.stringify(listingResult)}\n\n`];

describe('useAutoListing', () => {
    beforeEach(() => {
        useUIStore.setState({ toasts: [] });
    });

    it('识别成功：步骤进 steps，done 解析成结果并提示已填充', async () => {
        server.use(http.post('/api/ai/auto-listing/stream', () => sseResponse(listingFrames)));

        const { result } = renderHook(() => useAutoListing());
        await act(async () => {
            await result.current.analyzeImages(['https://example.com/a.jpg']);
        });

        expect(result.current.steps).toEqual([{ step: 1, tool: 'list_categories', thought: '确认品类能不能挂' }]);
        expect(result.current.result).toEqual(listingResult);
        expect(result.current.isLoading).toBe(false);
        expect(useUIStore.getState().toasts.at(-1)).toMatchObject({ type: 'success' });
    });

    it('后端返回业务错误码：错误提示直接采用后端文案（而不是零反馈或笼统兜底）', async () => {
        server.use(
            http.post('/api/ai/auto-listing/stream', () =>
                HttpResponse.json(
                    {
                        code: 'B8002',
                        message: 'AI 服务暂时不可用，请稍后重试',
                        data: null,
                        timestamp: Date.now(),
                    },
                    { status: 400 }
                )
            )
        );

        const { result } = renderHook(() => useAutoListing());
        await act(async () => {
            await result.current.analyzeImages(['https://example.com/a.jpg']);
        });

        const toasts = useUIStore.getState().toasts;
        expect(toasts).toHaveLength(1);
        expect(toasts[0]).toMatchObject({ type: 'error', message: 'AI 服务暂时不可用，请稍后重试' });
        expect(result.current.result).toBeNull();
        // 失败文案持久挂 hook 上（识别按钮旁的重试入口），不随 toast 消失
        expect(result.current.failure).toBe('AI 服务暂时不可用，请稍后重试');

        // 重试（再次 analyze）成功 -> 失败态清除
        server.use(http.post('/api/ai/auto-listing/stream', () => sseResponse(listingFrames)));
        await act(async () => {
            await result.current.analyzeImages(['https://example.com/a.jpg']);
        });
        expect(result.current.failure).toBeNull();
        expect(result.current.result).toEqual(listingResult);
    });

    it('error 事件：走失败文案且不落结果，重试清空上一轮步骤', async () => {
        server.use(
            http.post('/api/ai/auto-listing/stream', () =>
                sseResponse([stepFrame, 'event: error\ndata: 今日 AI 调用预算已用尽，请明天再试\n\n'])
            )
        );

        const { result } = renderHook(() => useAutoListing());
        await act(async () => {
            await result.current.analyzeImages(['https://example.com/a.jpg']);
        });

        expect(result.current.failure).toBe('今日 AI 调用预算已用尽，请明天再试');
        expect(result.current.result).toBeNull();
        expect(useUIStore.getState().toasts.at(-1)).toMatchObject({ type: 'error' });
        // 失败轮已流出的步骤不作数：下一轮从空面板重新流入
        expect(result.current.steps).toHaveLength(1);
        server.use(
            http.post('/api/ai/auto-listing/stream', () =>
                sseResponse([`event: done\ndata: ${JSON.stringify(listingResult)}\n\n`])
            )
        );
        await act(async () => {
            await result.current.analyzeImages(['https://example.com/a.jpg']);
        });
        expect(result.current.steps).toEqual([]);
    });
});
