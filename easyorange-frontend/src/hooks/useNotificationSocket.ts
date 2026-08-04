import { Client, type IMessage } from '@stomp/stompjs';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { useAuthStore } from '@/store/authStore';

const WS_URL = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`;
const HEARTBEAT_MS = 30000;
const RECONNECT_DELAYS = [1000, 2000, 4000, 8000, 16000, 30000];

export function useNotificationSocket(): void {
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
                client.subscribe('/user/queue/notification', (message: IMessage) => {
                    try {
                        JSON.parse(message.body);
                        queryClient.invalidateQueries({ queryKey: ['unread-count'] });
                    } catch {
                        // ignore parse errors
                    }
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
