import { Client, type IMessage } from '@stomp/stompjs';
import { useCallback, useEffect, useRef } from 'react';
import { useAuthStore } from '@/store/authStore';
import { useChatStore } from '@/store/chatStore';
import { useUIStore } from '@/store/uiStore';
import type { RecallPayload, TypingPayload } from '@/types/message';
import type { RawChatMessage } from '@/types/raw';
import { normalizeChatMessage } from '@/utils/message';

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
    const disposedRef = useRef(false);
    // 订阅意图跨 client 重建保留（token 刷新会换实例）：onConnect 统一补订阅。
    // 进页时 WS 尚未握手完成，旧逻辑直接 return 静默丢订阅，消息发出后收不到回显。
    const wantedRef = useRef<Set<string>>(new Set());

    const setConnectionStatus = useChatStore(s => s.setConnectionStatus);
    const addMessage = useChatStore(s => s.addMessage);
    const updateMessage = useChatStore(s => s.updateMessage);
    const setTyping = useChatStore(s => s.setTyping);
    const addToast = useUIStore(s => s.addToast);
    const token = useAuthStore(s => s.token);

    const doSubscribe = useCallback(
        (client: Client, conversationId: string) => {
            subscriptionsRef.current.get(conversationId)?.();

            const msgSub = client.subscribe(`/queue/chat/${conversationId}`, (message: IMessage) => {
                try {
                    // 走 normalize：后端 chatFrame 的 type 是数字码且没有 senderAvatar/readTime，
                    // 直接当 ChatMessage 断言会让实时消息与 REST 拉回的同一条渲染不一致
                    addMessage(conversationId, normalizeChatMessage(JSON.parse(message.body) as RawChatMessage));
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

    useEffect(() => {
        // 未登录不建立连接；brokerURL 追加 ?token= 供后端 WebSocket 握手拦截器认证
        if (!token) {
            setConnectionStatus('disconnected');
            return;
        }
        // 主动 deactivate（离开聊天页的 cleanup）也会触发 onWebSocketClose：
        // 不加此标记，正常退出会把连接状态残留成 reconnecting
        disposedRef.current = false;
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
                const c = clientRef.current;
                if (c) {
                    wantedRef.current.forEach(id => {
                        doSubscribe(c, id);
                    });
                }
                // 发送失败回执（限流/校验/技术异常）：此前只回帧无人订阅，用户点发送毫无反馈
                client.subscribe('/user/queue/error', (message: IMessage) => {
                    try {
                        const data: { type: string; message: string } = JSON.parse(message.body);
                        addToast({ type: 'error', message: data.message });
                    } catch {
                        // Failed to parse error frame
                    }
                });
            },
            onDisconnect: () => {
                setConnectionStatus('disconnected');
            },
            onWebSocketClose: () => {
                setConnectionStatus(disposedRef.current ? 'disconnected' : 'reconnecting');
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
            disposedRef.current = true;
            subscriptions.forEach(unsub => {
                unsub();
            });
            subscriptions.clear();
            client.deactivate();
            clientRef.current = null;
        };
    }, [setConnectionStatus, token, doSubscribe, addToast]);

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
            wantedRef.current.add(conversationId);
            const client = clientRef.current;
            if (client?.connected) {
                doSubscribe(client, conversationId);
            }
        },
        [doSubscribe]
    );

    const unsubscribe = useCallback((conversationId: string) => {
        wantedRef.current.delete(conversationId);
        const unsub = subscriptionsRef.current.get(conversationId);
        if (unsub) {
            unsub();
            subscriptionsRef.current.delete(conversationId);
        }
    }, []);

    return { sendMessage, sendTyping, subscribe, unsubscribe };
}
