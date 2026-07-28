import type { IMessage } from '@stomp/stompjs';
import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useChatStore } from '@/store/chatStore';
import { useStompChat } from './useStompChat';

const messageCallbacks: Array<(message: IMessage) => void> = [];
const mockUnsubscribe = vi.hoisted(() => vi.fn());

interface StompClientConfig {
    beforeConnect?: () => void;
    onConnect?: () => void;
    onDisconnect?: () => void;
    onWebSocketClose?: () => void;
    onWebSocketError?: (event: Event) => void;
    [key: string]: unknown;
}

const mockUtils = vi.hoisted(() => {
    let capturedConfig: StompClientConfig | null = null;
    let currentClient: ReturnType<typeof createMockClient> | null = null;

    function createMockClient() {
        const client = {
            reconnectDelay: 0,
            connected: false,
            activate: vi.fn(() => {
                capturedConfig?.beforeConnect?.();
                client.connected = true;
                capturedConfig?.onConnect?.();
            }),
            deactivate: vi.fn(() => {
                capturedConfig?.onDisconnect?.();
            }),
            publish: vi.fn(),
            subscribe: vi.fn(),
        };
        return client;
    }

    return {
        createMockClient,
        getCapturedConfig: () => capturedConfig,
        setCapturedConfig: (config: StompClientConfig) => {
            capturedConfig = config;
        },
        getCurrentClient: () => currentClient,
        setCurrentClient: (client: ReturnType<typeof createMockClient>) => {
            currentClient = client;
        },
    };
});

vi.mock('@stomp/stompjs', () => ({
    Client: function (this: unknown, config: Record<string, unknown>) {
        mockUtils.setCapturedConfig(config);
        const client = mockUtils.createMockClient();
        mockUtils.setCurrentClient(client);
        return client;
    },
}));

function createMessage(body: unknown): IMessage {
    return { body: JSON.stringify(body) } as IMessage;
}

beforeEach(() => {
    useChatStore.getState().reset();
    messageCallbacks.length = 0;
    vi.useFakeTimers({ shouldAdvanceTime: true });
});

afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
});

describe('useStompChat', () => {
    function setupSubscribe() {
        const client = mockUtils.getCurrentClient();
        if (!client) {
            throw new Error('mock client not created');
        }
        client.subscribe.mockImplementation((_destination: string, callback: (message: IMessage) => void) => {
            messageCallbacks.push(callback);
            return { unsubscribe: mockUnsubscribe };
        });
        return client;
    }

    it('returns chat control functions', () => {
        const { result } = renderHook(() => useStompChat());
        expect(typeof result.current.sendMessage).toBe('function');
        expect(typeof result.current.sendTyping).toBe('function');
        expect(typeof result.current.subscribe).toBe('function');
        expect(typeof result.current.unsubscribe).toBe('function');
    });

    it('activates client on mount and updates connection status to connected', async () => {
        renderHook(() => useStompChat());
        const client = mockUtils.getCurrentClient();
        expect(client?.activate).toHaveBeenCalledTimes(1);
        await waitFor(() => expect(useChatStore.getState().connectionStatus).toBe('connected'));
    });

    it('applies increasing reconnect delays through beforeConnect', async () => {
        renderHook(() => useStompChat());
        const client = mockUtils.getCurrentClient();

        await waitFor(() => expect(client?.connected).toBe(true));

        act(() => {
            mockUtils.getCapturedConfig()?.beforeConnect?.();
        });
        expect(client?.reconnectDelay).toBe(1000);

        act(() => {
            mockUtils.getCapturedConfig()?.beforeConnect?.();
        });
        expect(client?.reconnectDelay).toBe(2000);

        act(() => {
            mockUtils.getCapturedConfig()?.beforeConnect?.();
        });
        expect(client?.reconnectDelay).toBe(4000);
    });

    it('caps reconnect delay at the largest configured value', async () => {
        renderHook(() => useStompChat());
        const client = mockUtils.getCurrentClient();

        await waitFor(() => expect(client?.connected).toBe(true));

        for (let i = 0; i < 10; i++) {
            act(() => {
                mockUtils.getCapturedConfig()?.beforeConnect?.();
            });
        }

        expect(client?.reconnectDelay).toBe(30000);
    });

    it('sets status to reconnecting when websocket closes', async () => {
        renderHook(() => useStompChat());
        const client = mockUtils.getCurrentClient();

        await waitFor(() => expect(client?.connected).toBe(true));

        act(() => {
            mockUtils.getCapturedConfig()?.onWebSocketClose?.();
        });

        expect(useChatStore.getState().connectionStatus).toBe('reconnecting');
    });

    it('sets status to disconnected on disconnect', async () => {
        renderHook(() => useStompChat());
        const client = mockUtils.getCurrentClient();

        await waitFor(() => expect(client?.connected).toBe(true));

        act(() => {
            mockUtils.getCapturedConfig()?.onDisconnect?.();
        });

        expect(useChatStore.getState().connectionStatus).toBe('disconnected');
    });

    it('does not throw on websocket error', async () => {
        renderHook(() => useStompChat());
        const client = mockUtils.getCurrentClient();

        await waitFor(() => expect(client?.connected).toBe(true));

        expect(() => {
            act(() => {
                mockUtils.getCapturedConfig()?.onWebSocketError?.(new Event('error'));
            });
        }).not.toThrow();
    });

    it('sends a chat message via publish', async () => {
        const { result } = renderHook(() => useStompChat());
        const client = mockUtils.getCurrentClient();

        act(() => {
            result.current.sendMessage({ content: 'hello', receiverId: 'user-2' });
        });

        expect(client?.publish).toHaveBeenCalledWith({
            destination: '/app/chat.send',
            body: JSON.stringify({ content: 'hello', receiverId: 'user-2' }),
        });
    });

    it('sends typing indicator via publish', async () => {
        const { result } = renderHook(() => useStompChat());
        const client = mockUtils.getCurrentClient();

        act(() => {
            result.current.sendTyping('conv-1', 'user-2');
        });

        expect(client?.publish).toHaveBeenCalledWith({
            destination: '/app/chat.typing',
            body: JSON.stringify({ conversationId: 'conv-1', targetUserId: 'user-2' }),
        });
    });

    it('subscribes to conversation topics when connected', async () => {
        const { result } = renderHook(() => useStompChat());
        const client = setupSubscribe();

        await waitFor(() => expect(client.connected).toBe(true));

        act(() => {
            result.current.subscribe('conv-1');
        });

        expect(client.subscribe).toHaveBeenCalledWith('/queue/chat/conv-1', expect.any(Function));
        expect(client.subscribe).toHaveBeenCalledWith('/topic/chat/conv-1/typing', expect.any(Function));
        expect(client.subscribe).toHaveBeenCalledWith('/topic/chat/conv-1/recall', expect.any(Function));
    });

    it('does not subscribe when client is not connected', async () => {
        const { result } = renderHook(() => useStompChat());
        const client = mockUtils.getCurrentClient();
        if (client) {
            client.connected = false;
        }

        act(() => {
            result.current.subscribe('conv-1');
        });

        expect(client?.subscribe).not.toHaveBeenCalled();
    });

    it('adds incoming chat messages to the store', async () => {
        const { result } = renderHook(() => useStompChat());
        const client = setupSubscribe();

        await waitFor(() => expect(client.connected).toBe(true));

        act(() => {
            result.current.subscribe('conv-1');
        });

        act(() => {
            messageCallbacks[0]?.(
                createMessage({
                    id: 'msg-1',
                    senderId: 'user-2',
                    receiverId: 'user-1',
                    content: 'hi',
                    type: 'TEXT',
                    status: 'SENT',
                    createTime: '2026-07-28T10:00:00Z',
                })
            );
        });

        const messages = useChatStore.getState().messages['conv-1'];
        expect(messages).toHaveLength(1);
        expect(messages?.[0].content).toBe('hi');
    });

    it('handles typing events with a 3 second timeout', async () => {
        const { result } = renderHook(() => useStompChat());
        const client = setupSubscribe();

        await waitFor(() => expect(client.connected).toBe(true));

        act(() => {
            result.current.subscribe('conv-1');
        });

        act(() => {
            messageCallbacks[1]?.(createMessage({ userId: 'user-2', userName: 'Bob' }));
        });

        expect(useChatStore.getState().typingUsers.has('user-2')).toBe(true);

        act(() => {
            vi.advanceTimersByTime(3000);
        });

        expect(useChatStore.getState().typingUsers.has('user-2')).toBe(false);
    });

    it('updates message status on recall events', async () => {
        useChatStore.getState().addMessage('conv-1', {
            id: 'msg-1',
            senderId: 'user-2',
            receiverId: 'user-1',
            content: 'original',
            type: 'TEXT',
            status: 'SENT',
            createTime: '2026-07-28T10:00:00Z',
            readTime: null,
            recalledAt: null,
        });

        const { result } = renderHook(() => useStompChat());
        const client = setupSubscribe();

        await waitFor(() => expect(client.connected).toBe(true));

        act(() => {
            result.current.subscribe('conv-1');
        });

        act(() => {
            messageCallbacks[2]?.(
                createMessage({
                    messageId: 'msg-1',
                    conversationId: 'conv-1',
                    operatorId: 'user-2',
                    recalledAt: '2026-07-28T10:01:00Z',
                })
            );
        });

        const message = useChatStore.getState().messages['conv-1']?.[0];
        expect(message?.status).toBe('RECALLED');
        expect(message?.content).toBe('[消息已撤回]');
        expect(message?.type).toBe('RECALLED');
        expect(message?.recalledAt).toBe('2026-07-28T10:01:00Z');
    });

    it('ignores malformed incoming messages', async () => {
        const { result } = renderHook(() => useStompChat());
        const client = setupSubscribe();

        await waitFor(() => expect(client.connected).toBe(true));

        act(() => {
            result.current.subscribe('conv-1');
        });

        act(() => {
            messageCallbacks[0]?.({ body: 'not-json' } as IMessage);
            messageCallbacks[1]?.({ body: 'not-json' } as IMessage);
            messageCallbacks[2]?.({ body: 'not-json' } as IMessage);
        });

        expect(useChatStore.getState().messages['conv-1']).toBeUndefined();
        expect(useChatStore.getState().typingUsers.size).toBe(0);
    });

    it('unsubscribes from a specific conversation', async () => {
        const { result } = renderHook(() => useStompChat());
        const client = setupSubscribe();

        await waitFor(() => expect(client.connected).toBe(true));

        act(() => {
            result.current.subscribe('conv-1');
        });

        const unsubscribeCallsBefore = mockUnsubscribe.mock.calls.length;

        act(() => {
            result.current.unsubscribe('conv-1');
        });

        expect(mockUnsubscribe.mock.calls.length - unsubscribeCallsBefore).toBe(3);
    });

    it('does nothing when unsubscribing from an unknown conversation', async () => {
        const { result } = renderHook(() => useStompChat());
        const client = setupSubscribe();

        await waitFor(() => expect(client.connected).toBe(true));

        act(() => {
            result.current.unsubscribe('conv-unknown');
        });

        expect(mockUnsubscribe).not.toHaveBeenCalled();
    });

    it('cleans up subscriptions and deactivates client on unmount', async () => {
        const { result, unmount } = renderHook(() => useStompChat());
        const client = setupSubscribe();

        await waitFor(() => expect(client.connected).toBe(true));

        act(() => {
            result.current.subscribe('conv-1');
        });

        unmount();

        expect(mockUnsubscribe).toHaveBeenCalled();
        expect(client.deactivate).toHaveBeenCalled();
    });
});
