import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useChatStore } from '@/store/chatStore';
import { useMessageRecall } from './useMessageRecall';

const mockRecallApi = vi.hoisted(() => vi.fn());

vi.mock('@/api/messageApi', () => ({
    messageApi: {
        recallMessage: (...args: unknown[]) => mockRecallApi(...args),
    },
}));

function createMessage(overrides: Partial<{ createTime: string; status: string; senderId: string }> = {}) {
    return {
        createTime: '2026-07-28T10:00:00Z',
        status: 'SENT',
        senderId: 'user-1',
        ...overrides,
    };
}

beforeEach(() => {
    useChatStore.getState().reset();
    mockRecallApi.mockReset();
});

afterEach(() => {
    vi.restoreAllMocks();
});

describe('useMessageRecall', () => {
    describe('canRecall', () => {
        it('returns true for own message within timeout', () => {
            const { result } = renderHook(() => useMessageRecall('conv-1'));
            const message = createMessage({ senderId: 'user-1', createTime: new Date().toISOString() });
            expect(result.current.canRecall(message, 'user-1')).toBe(true);
        });

        it('returns false for messages sent by others', () => {
            const { result } = renderHook(() => useMessageRecall('conv-1'));
            const message = createMessage({ senderId: 'user-2' });
            expect(result.current.canRecall(message, 'user-1')).toBe(false);
        });

        it('returns false for already recalled messages', () => {
            const { result } = renderHook(() => useMessageRecall('conv-1'));
            const message = createMessage({ status: 'RECALLED' });
            expect(result.current.canRecall(message, 'user-1')).toBe(false);
        });

        it('returns false when recall window has expired', () => {
            const { result } = renderHook(() => useMessageRecall('conv-1'));
            const threeMinutesAgo = new Date(Date.now() - 3 * 60 * 1000).toISOString();
            const message = createMessage({ createTime: threeMinutesAgo });
            expect(result.current.canRecall(message, 'user-1')).toBe(false);
        });

        it('returns false for invalid createTime', () => {
            const { result } = renderHook(() => useMessageRecall('conv-1'));
            const message = createMessage({ createTime: 'invalid-date' });
            expect(result.current.canRecall(message, 'user-1')).toBe(false);
        });
    });

    describe('recallMessage', () => {
        it('updates store message on successful recall', async () => {
            useChatStore.getState().addMessage('conv-1', {
                id: 'msg-1',
                senderId: 'user-1',
                receiverId: 'user-2',
                content: 'original',
                type: 'TEXT',
                status: 'SENT',
                createTime: '2026-07-28T10:00:00Z',
                readTime: null,
                recalledAt: null,
            });

            mockRecallApi.mockResolvedValue({
                data: {
                    messageId: 'msg-1',
                    conversationId: 'conv-1',
                    operatorId: 'user-1',
                    recalledAt: '2026-07-28T10:01:00Z',
                },
            });

            const { result } = renderHook(() => useMessageRecall('conv-1'));

            let success = false;
            await act(async () => {
                success = await result.current.recallMessage('msg-1');
            });

            expect(success).toBe(true);
            expect(mockRecallApi).toHaveBeenCalledWith('msg-1');

            const message = useChatStore.getState().messages['conv-1']?.[0];
            expect(message?.status).toBe('RECALLED');
            expect(message?.content).toBe('[消息已撤回]');
            expect(message?.type).toBe('RECALLED');
            expect(message?.recalledAt).toBeDefined();
        });

        it('returns false and does not update store on failure', async () => {
            mockRecallApi.mockRejectedValue(new Error('recall failed'));

            const { result } = renderHook(() => useMessageRecall('conv-1'));

            let success = true;
            await act(async () => {
                success = await result.current.recallMessage('msg-1');
            });

            expect(success).toBe(false);
            expect(useChatStore.getState().messages['conv-1']).toBeUndefined();
        });
    });

    describe('copyMessage', () => {
        it('copies content to clipboard', async () => {
            const writeText = vi.fn().mockResolvedValue(undefined);
            Object.defineProperty(globalThis.navigator, 'clipboard', {
                value: { writeText },
                configurable: true,
            });

            const { result } = renderHook(() => useMessageRecall('conv-1'));

            await act(async () => {
                await result.current.copyMessage('hello world');
            });

            expect(writeText).toHaveBeenCalledWith('hello world');
        });

        it('silently handles clipboard rejection', async () => {
            const writeText = vi.fn().mockRejectedValue(new Error('denied'));
            Object.defineProperty(globalThis.navigator, 'clipboard', {
                value: { writeText },
                configurable: true,
            });

            const { result } = renderHook(() => useMessageRecall('conv-1'));

            await expect(
                act(async () => {
                    await result.current.copyMessage('hello world');
                })
            ).resolves.not.toThrow();

            expect(writeText).toHaveBeenCalledWith('hello world');
        });
    });
});
