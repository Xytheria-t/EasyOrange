import { Client, type IMessage } from '@stomp/stompjs';
import { useCallback, useEffect, useRef } from 'react';
import { useAuthStore } from '@/store/authStore';
import { useChatStore } from '@/store/chatStore';
import type { ChatMessage, RecallPayload, TypingPayload } from '@/types/message';

declare module '@stomp/stompjs' {
    interface Client {
        reconnectDelay: number;
    }
}

const WS_URL = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`;
const HEARTBEAT_MS = 30000;
const RECONNECT_DELAYS = [1000, 2000, 4000, 8000, 16000, 30000];

export interface UseStompChatReturn {
    sendMessage: (payload: Record<string, unknown>) => void;
    sendTyping: (conversationId: string, targetUserId: string) => void;
    subscribe: (conversationId: string) => void;
    unsubscribe: (conversationId: string) => void;
}

export function useStompChat(): UseStompChatReturn {
    const clientRef = useRef<Client | null>(null);
    const reconnectAttemptRef = useRef(0);
    const subscriptionsRef = useRef<Map<string, () => void>>(new Map());

    const setConnectionStatus = useChatStore(s => s.setConnectionStatus);
    const addMessage = useChatStore(s => s.addMessage);
    const updateMessage = useChatStore(s => s.updateMessage);
    const setTyping = useChatStore(s => s.setTyping);
    const token = useAuthStore(s => s.token);

    useEffect(() => {
        // 未登录不建立连接；brokerURL 追加 ?token= 供后端 WebSocket 握手拦截器认证
        if (!token) {
            setConnectionStatus('disconnected');
            return;
        }
        const brokerURL = `${WS_URL}?token=${encodeURIComponent(token)}`;

        const client = new Client({
            brokerURL,
            connectHeaders: {},
            heartbeatOutgoing: HEARTBEAT_MS,
            heartbeatIncoming: HEARTBEAT_MS,
            reconnectDelay: RECONNECT_DELAYS[0],
            onConnect: () => {
                reconnectAttemptRef.current = 0;
                setConnectionStatus('connected');
            },
            onDisconnect: () => {
                setConnectionStatus('disconnected');
            },
            onWebSocketClose: () => {
                setConnectionStatus('reconnecting');
            },
            onWebSocketError: (_event: Event) => {
                // WebSocket error occurred
            },
            beforeConnect: () => {
                const delay = RECONNECT_DELAYS[Math.min(reconnectAttemptRef.current, RECONNECT_DELAYS.length - 1)];
                client.reconnectDelay = delay;
                reconnectAttemptRef.current++;
                setConnectionStatus('connecting');
            },
        });

        const subscriptions = subscriptionsRef.current;
        client.activate();
        clientRef.current = client;

        return () => {
            subscriptions.forEach(unsub => {
                unsub();
            });
            subscriptions.clear();
            client.deactivate();
            clientRef.current = null;
        };
    }, [setConnectionStatus, token]);

    const sendMessage = useCallback((payload: Record<string, unknown>) => {
        clientRef.current?.publish({
            destination: '/app/chat.send',
            body: JSON.stringify(payload),
        });
    }, []);

    const sendTyping = useCallback((conversationId: string, targetUserId: string) => {
        clientRef.current?.publish({
            destination: '/app/chat.typing',
            body: JSON.stringify({ conversationId, targetUserId }),
        });
    }, []);

    const subscribe = useCallback(
        (conversationId: string) => {
            const client = clientRef.current;
            if (!client?.connected) {
                return;
            }

            const msgSub = client.subscribe(`/queue/chat/${conversationId}`, (message: IMessage) => {
                try {
                    const data: ChatMessage = JSON.parse(message.body);
                    addMessage(conversationId, data);
                } catch {
                    // Failed to parse message
                }
            });

            const typingSub = client.subscribe(`/topic/chat/${conversationId}/typing`, (message: IMessage) => {
                try {
                    const data: TypingPayload = JSON.parse(message.body);
                    setTyping(data.userId, true);
                    setTimeout(() => setTyping(data.userId, false), 3000);
                } catch {
                    // Failed to parse typing event
                }
            });

            const recallSub = client.subscribe(`/topic/chat/${conversationId}/recall`, (message: IMessage) => {
                try {
                    const data: RecallPayload = JSON.parse(message.body);
                    updateMessage(conversationId, data.messageId, {
                        status: 'RECALLED',
                        content: '[消息已撤回]',
                        type: 'RECALLED',
                        recalledAt: data.recalledAt,
                    });
                } catch {
                    // Failed to parse recall event
                }
            });

            subscriptionsRef.current.set(conversationId, () => {
                msgSub.unsubscribe();
                typingSub.unsubscribe();
                recallSub.unsubscribe();
            });
        },
        [addMessage, updateMessage, setTyping]
    );

    const unsubscribe = useCallback((conversationId: string) => {
        const unsub = subscriptionsRef.current.get(conversationId);
        if (unsub) {
            unsub();
            subscriptionsRef.current.delete(conversationId);
        }
    }, []);

    return { sendMessage, sendTyping, subscribe, unsubscribe };
}
