import { Client } from '@stomp/stompjs';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { useAuthStore } from '@/store/authStore';

const WS_URL = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`;
const HEARTBEAT_MS = 30000;
const RECONNECT_DELAYS = [1000, 2000, 4000, 8000, 16000, 30000];

/**
 * 聊天未读角标的 WS 触发器 —— 订阅 /user/queue/unread-count，收到即失效会话列表查询。
 *
 * <p>载荷只当触发器用，REST unread-count / 会话列表是唯一真相源；此前该帧后端每条消息都发、
 * 前端无人订阅，顶栏角标只能等 staleTime 过期。挂在 Layout（登录态常驻），
 * 不挂在单个会话页——退出消息中心后回来看，角标也应该是新的。
 */
export function useChatUnreadSocket(): void {
    const queryClient = useQueryClient();
    const clientRef = useRef<Client | null>(null);
    const reconnectAttemptRef = useRef(0);
    const token = useAuthStore(s => s.token);

    useEffect(() => {
        // 未登录不建立连接；brokerURL 追加 ?token= 供后端 WebSocket 握手拦截器认证
        if (!token) {
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
                client.subscribe('/user/queue/unread-count', () => {
                    // 只失效会话列表：聊天未读由各会话的 unreadCount 求和得出
                    void queryClient.invalidateQueries({ queryKey: ['messages', 'conversations'] });
                });
            },
            beforeConnect: () => {
                const delay = RECONNECT_DELAYS[Math.min(reconnectAttemptRef.current, RECONNECT_DELAYS.length - 1)];
                client.reconnectDelay = delay;
                reconnectAttemptRef.current++;
            },
        });

        client.activate();
        clientRef.current = client;

        return () => {
            client.deactivate();
            clientRef.current = null;
        };
    }, [queryClient, token]);
}
