import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useChatStore } from '@/store/chatStore';
import type { ChatMessage } from '@/types/message';
import { useChatMessages } from './useChatMessages';

const mockGetConversation = vi.hoisted(() => vi.fn());

vi.mock('@/api/messageApi', () => ({
    messageApi: {
        getConversation: (...args: unknown[]) => mockGetConversation(...args),
    },
}));

function createWrapper() {
    const testQc = new QueryClient({
        defaultOptions: {
            queries: { retry: false, gcTime: 0 },
            mutations: { retry: false },
        },
    });

    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={testQc}>{children}</QueryClientProvider>;
    };
}

function createRawMessage(id: string, overrides: Partial<ChatMessage> = {}) {
    return {
        id,
        senderId: 'user-2',
        receiverId: 'user-1',
        content: `content-${id}`,
        type: 'TEXT',
        status: 'SENT',
        createTime: '2026-07-28T10:00:00Z',
        readTime: null,
        recalledAt: null,
        ...overrides,
    };
}

beforeEach(() => {
    useChatStore.getState().reset();
    mockGetConversation.mockReset();
});

afterEach(() => {
    vi.clearAllMocks();
});

describe('useChatMessages', () => {
    it('does not fetch when targetUserId is null', () => {
        renderHook(() => useChatMessages(null, 'conv-1'), {
            wrapper: createWrapper(),
        });
        expect(mockGetConversation).not.toHaveBeenCalled();
    });

    it('returns empty messages while loading', () => {
        mockGetConversation.mockReturnValue(new Promise(() => {}));
        const { result } = renderHook(() => useChatMessages('user-2', 'conv-1'), {
            wrapper: createWrapper(),
        });
        expect(result.current.isLoading).toBe(true);
        expect(result.current.messages).toEqual([]);
    });

    it('fetches and normalizes the last page of messages', async () => {
        const rawMessages = Array.from({ length: 55 }, (_, i) => createRawMessage(`msg-${i + 1}`));
        mockGetConversation.mockResolvedValue({ data: rawMessages });

        const { result } = renderHook(() => useChatMessages('user-2', 'conv-1'), {
            wrapper: createWrapper(),
        });

        await waitFor(() => expect(result.current.isLoading).toBe(false));

        expect(mockGetConversation).toHaveBeenCalledWith('user-2');
        expect(result.current.messages).toHaveLength(50);
        expect(result.current.messages[0].id).toBe('msg-6');
        expect(result.current.messages[49].id).toBe('msg-55');
    });

    it('merges base messages with store messages', async () => {
        const rawMessages = [createRawMessage('msg-1')];
        mockGetConversation.mockResolvedValue({ data: rawMessages });

        useChatStore.getState().addMessage('conv-1', {
            id: 'msg-2',
            senderId: 'user-1',
            receiverId: 'user-2',
            content: 'store message',
            type: 'TEXT',
            status: 'SENDING',
            createTime: '2026-07-28T10:01:00Z',
            readTime: null,
            recalledAt: null,
        });

        const { result } = renderHook(() => useChatMessages('user-2', 'conv-1'), {
            wrapper: createWrapper(),
        });

        await waitFor(() => expect(result.current.messages).toHaveLength(2));
        const ids = result.current.messages.map(m => m.id).sort();
        expect(ids).toEqual(['msg-1', 'msg-2']);
    });

    it('overrides base message with newer store message for same id', async () => {
        const rawMessages = [createRawMessage('msg-1', { status: 'SENT', content: 'base' })];
        mockGetConversation.mockResolvedValue({ data: rawMessages });

        useChatStore.getState().addMessage('conv-1', {
            id: 'msg-1',
            senderId: 'user-1',
            receiverId: 'user-2',
            content: 'updated',
            type: 'TEXT',
            status: 'READ',
            createTime: '2026-07-28T10:01:00Z',
            readTime: null,
            recalledAt: null,
        });

        const { result } = renderHook(() => useChatMessages('user-2', 'conv-1'), {
            wrapper: createWrapper(),
        });

        await waitFor(() => expect(result.current.messages).toHaveLength(1));
        expect(result.current.messages[0].status).toBe('READ');
        expect(result.current.messages[0].content).toBe('updated');
    });

    it('loads older messages', async () => {
        const rawMessages = Array.from({ length: 60 }, (_, i) => createRawMessage(`msg-${i + 1}`));
        mockGetConversation.mockResolvedValue({ data: rawMessages });

        const { result } = renderHook(() => useChatMessages('user-2', 'conv-1'), {
            wrapper: createWrapper(),
        });

        await waitFor(() => expect(result.current.messages).toHaveLength(50));

        await act(async () => {
            await result.current.loadOlder();
        });

        await waitFor(() => expect(result.current.messages).toHaveLength(60));
        expect(result.current.messages[0].id).toBe('msg-1');
        expect(result.current.hasMore).toBe(false);
    });

    it('keeps hasMore true when more older messages remain', async () => {
        const rawMessages = Array.from({ length: 110 }, (_, i) => createRawMessage(`msg-${i + 1}`));
        mockGetConversation.mockResolvedValue({ data: rawMessages });

        const { result } = renderHook(() => useChatMessages('user-2', 'conv-1'), {
            wrapper: createWrapper(),
        });

        await waitFor(() => expect(result.current.messages).toHaveLength(50));

        await act(async () => {
            await result.current.loadOlder();
        });

        await waitFor(() => expect(result.current.messages).toHaveLength(100));
        expect(result.current.messages[0].id).toBe('msg-11');
        expect(result.current.hasMore).toBe(true);
    });

    it('sets hasMore to false when no older messages exist', async () => {
        const rawMessages = Array.from({ length: 40 }, (_, i) => createRawMessage(`msg-${i + 1}`));
        mockGetConversation.mockResolvedValue({ data: rawMessages });

        const { result } = renderHook(() => useChatMessages('user-2', 'conv-1'), {
            wrapper: createWrapper(),
        });

        await waitFor(() => expect(result.current.messages).toHaveLength(40));
        expect(result.current.hasMore).toBe(true);

        await act(async () => {
            await result.current.loadOlder();
        });

        await waitFor(() => expect(result.current.hasMore).toBe(false));
        expect(result.current.messages).toHaveLength(40);
    });

    it('exposes query error on initial fetch failure', async () => {
        mockGetConversation.mockRejectedValue(new Error('network error'));

        const { result } = renderHook(() => useChatMessages('user-2', 'conv-1'), {
            wrapper: createWrapper(),
        });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.error).toBeDefined();
    });

    it('does nothing when loadOlder is called without oldest message', async () => {
        mockGetConversation.mockResolvedValue({ data: [] });

        const { result } = renderHook(() => useChatMessages('user-2', 'conv-1'), {
            wrapper: createWrapper(),
        });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        mockGetConversation.mockClear();

        await act(async () => {
            await result.current.loadOlder();
        });

        expect(mockGetConversation).not.toHaveBeenCalled();
    });
});
