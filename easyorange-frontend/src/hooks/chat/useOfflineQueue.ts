import { useCallback, useEffect, useRef } from 'react';
import { useChatStore } from '@/store/chatStore';
import { useStompChat } from './useStompChat';

const QUEUE_KEY = 'chat_offline_queue';
const MAX_QUEUE_SIZE = 100;

interface QueuedMessage {
    id: string;
    conversationId: string;
    payload: Record<string, unknown>;
    timestamp: number;
}

function loadQueue(): QueuedMessage[] {
    try {
        const raw = localStorage.getItem(QUEUE_KEY);
        return raw ? JSON.parse(raw) : [];
    } catch {
        return [];
    }
}

function saveQueue(queue: QueuedMessage[]) {
    try {
        localStorage.setItem(QUEUE_KEY, JSON.stringify(queue.slice(0, MAX_QUEUE_SIZE)));
    } catch {
        // Storage full or unavailable
    }
}

export function useOfflineQueue() {
    const sendMessage = useStompChat().sendMessage;
    const addMessage = useChatStore(s => s.addMessage);
    const updateMessage = useChatStore(s => s.updateMessage);
    const connectionStatus = useChatStore(s => s.connectionStatus);
    const isSendingRef = useRef(false);

    const flushQueue = useCallback(() => {
        if (isSendingRef.current || connectionStatus !== 'connected') {
            return;
        }

        const queue = loadQueue();
        if (queue.length === 0) {
            return;
        }

        isSendingRef.current = true;

        for (const item of queue) {
            try {
                sendMessage(item.payload);
                updateMessage(item.conversationId, item.id, { status: 'SENDING' });
            } catch {
                updateMessage(item.conversationId, item.id, { status: 'FAILED' });
            }
        }

        saveQueue([]);
        isSendingRef.current = false;
    }, [sendMessage, updateMessage, connectionStatus]);

    const enqueue = useCallback(
        (conversationId: string, payload: Record<string, unknown>) => {
            const pendingId = `offline_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
            const now = new Date().toISOString();

            addMessage(conversationId, {
                id: pendingId,
                senderId: '',
                receiverId: String(payload.receiverId || ''),
                content: String(payload.content || ''),
                type: 'TEXT',
                status: 'FAILED',
                createTime: now,
                readTime: null,
                recalledAt: null,
            });

            const queue = loadQueue();
            queue.push({ id: pendingId, conversationId, payload, timestamp: Date.now() });
            saveQueue(queue);

            if (connectionStatus === 'connected') {
                flushQueue();
            }
        },
        [addMessage, connectionStatus, flushQueue]
    );

    useEffect(() => {
        const handleOnline = () => {
            setTimeout(flushQueue, 500);
        };
        window.addEventListener('online', handleOnline);
        return () => window.removeEventListener('online', handleOnline);
    }, [flushQueue]);

    useEffect(() => {
        if (connectionStatus === 'connected') {
            flushQueue();
        }
    }, [connectionStatus, flushQueue]);

    const clearFailedMessages = useCallback(() => {
        const queue = loadQueue();
        for (const item of queue) {
            updateMessage(item.conversationId, item.id, { status: 'FAILED' });
        }
        saveQueue([]);
    }, [updateMessage]);

    const getPendingCount = useCallback(() => {
        return loadQueue().length;
    }, []);

    return { enqueue, flushQueue, clearFailedMessages, getPendingCount };
}
