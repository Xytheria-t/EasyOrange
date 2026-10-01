import { act, renderHook } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { useUIStore } from '@/store/uiStore';
import { server } from '@/testUtils/mocks/server';
import { useAutoListing } from './useAutoListing';

const successBody = {
    code: 'A0000',
    message: 'success',
    data: {
        title: '二手 iPhone 14',
        description: '九成新，无划痕',
        price: 4500,
        categoryName: '手机数码',
        conditionLevel: '2',
        location: '上海',
    },
    timestamp: Date.now(),
};

describe('useAutoListing', () => {
    beforeEach(() => {
        useUIStore.setState({ toasts: [] });
    });

    it('识别成功：返回结构化结果并提示已填充', async () => {
        server.use(http.post('/api/ai/auto-listing', () => HttpResponse.json(successBody)));

        const { result } = renderHook(() => useAutoListing());
        await act(async () => {
            await result.current.analyzeImages(['https://example.com/a.jpg']);
        });

        expect(result.current.result).toEqual(successBody.data);
        expect(result.current.isLoading).toBe(false);
        expect(useUIStore.getState().toasts.at(-1)).toMatchObject({ type: 'success' });
    });

    it('后端返回业务错误码：错误提示直接采用后端文案（而不是零反馈或笼统兜底）', async () => {
        server.use(
            http.post('/api/ai/auto-listing', () =>
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
        server.use(http.post('/api/ai/auto-listing', () => HttpResponse.json(successBody)));
        await act(async () => {
            await result.current.analyzeImages(['https://example.com/a.jpg']);
        });
        expect(result.current.failure).toBeNull();
        expect(result.current.result).toEqual(successBody.data);
    });
});
