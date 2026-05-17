import { useEffect, useRef, useCallback } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import { useChatStore } from '@/stores/chatStore';
import type { ChatMessage, TypingPayload, RecallPayload } from '@/types/message';

declare module '@stomp/stompjs' {
  interface Client {
    reconnectDelay: number;
  }
}

const WS_URL = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`;
const HEARTBEAT_OUTGOING = 30000;
const HEARTBEAT_INCOMING = 30000;
const RECONNECT_DELAYS = [1000, 2000, 4000, 8000, 16000, 30000];

export interface UseStompChatReturn {
  sendMessage: (conversationId: string, payload: Record<string, unknown>) => void;
  sendTyping: (conversationId: string, targetUserId: string) => void;
  subscribe: (conversationId: string) => void;
  unsubscribe: (conversationId: string) => void;
  disconnect: () => void;
}

export function useStompChat(): UseStompChatReturn {
  const clientRef = useRef<Client | null>(null);
  const reconnectAttemptRef = useRef(0);
  const subscriptionsRef = useRef<Map<string, () => void>>(new Map());

  const setConnectionStatus = useChatStore((s) => s.setConnectionStatus);
  const addMessage = useChatStore((s) => s.addMessage);
  const updateMessage = useChatStore((s) => s.updateMessage);
  const setTyping = useChatStore((s) => s.setTyping);

  useEffect(() => {
    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: {},
      heartbeatOutgoing: HEARTBEAT_OUTGOING,
      heartbeatIncoming: HEARTBEAT_INCOMING,
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
      onWebSocketError: (evt: Event) => {
        console.error('[STOMP] WebSocket error', evt);
      },
      beforeConnect: () => {
        const delay = RECONNECT_DELAYS[Math.min(reconnectAttemptRef.current, RECONNECT_DELAYS.length - 1)];
        client.reconnectDelay = delay;
        reconnectAttemptRef.current++;
        setConnectionStatus('connecting');
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      subscriptionsRef.current.forEach(( unsub ) => unsub());
      subscriptionsRef.current.clear();
      client.deactivate();
      clientRef.current = null;
    };
  }, [setConnectionStatus]);

  const sendMessage = useCallback(
    (_conversationId: string, payload: Record<string, unknown>) => {
      clientRef.current?.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(payload),
      });
    },
    []
  );

  const sendTyping = useCallback(
    (_conversationId: string, targetUserId: string) => {
      clientRef.current?.publish({
        destination: '/app/chat.typing',
        body: JSON.stringify({ conversationId: _conversationId, targetUserId }),
      });
    },
    []
  );

  const subscribe = useCallback(
    (conversationId: string) => {
      const client = clientRef.current;
      if (!client || !client.connected) {return;}

      const msgSub = client.subscribe(
        `/queue/chat/${conversationId}`,
        (message: IMessage) => {
          try {
            const data: ChatMessage = JSON.parse(message.body);
            addMessage(conversationId, data);
          } catch (e) {
            console.error('[STOMP] Failed to parse message', e);
          }
        }
      );

      const typingSub = client.subscribe(
        `/topic/chat/${conversationId}/typing`,
        (message: IMessage) => {
          try {
            const data: TypingPayload = JSON.parse(message.body);
            setTyping(data.userId, true);
            setTimeout(() => setTyping(data.userId, false), 3000);
          } catch (e) {
            console.error('[STOMP] Failed to parse typing event', e);
          }
        }
      );

      const recallSub = client.subscribe(
        `/topic/chat/${conversationId}/recall`,
        (message: IMessage) => {
          try {
            const data: RecallPayload = JSON.parse(message.body);
            updateMessage(conversationId, data.messageId, {
              status: 'RECALLED',
              content: '[消息已撤回]',
              type: 'RECALLED',
              recalledAt: data.recalledAt,
            });
          } catch (e) {
            console.error('[STOMP] Failed to parse recall event', e);
          }
        }
      );

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

  const disconnect = useCallback(() => {
    clientRef.current?.deactivate();
  }, []);

  return { sendMessage, sendTyping, subscribe, unsubscribe, disconnect };
}
