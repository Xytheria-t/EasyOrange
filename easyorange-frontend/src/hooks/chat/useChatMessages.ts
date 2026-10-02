import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useRef, useState } from 'react';
import { messageApi } from '@/api/messageApi';
import { useChatStore } from '@/store/chatStore';
import type { ChatMessage } from '@/types/message';
import { normalizeChatMessages } from '@/utils/message';
import { CHAT_PAGE_SIZE, chatMessagesQueryKey, fetchChatMessages } from './chatMessagesQuery';

const EMPTY_MESSAGES: ChatMessage[] = [];

export function useChatMessages(targetUserId: string | null, conversationId: string) {
    const [hasMore, setHasMore] = useState(true);
    const loadingOlderRef = useRef(false);
    const queryClient = useQueryClient();

    const storeMessages = useChatStore(s =>
        conversationId ? (s.messages[conversationId] ?? EMPTY_MESSAGES) : EMPTY_MESSAGES
    );

    const {
        data: baseMessages = EMPTY_MESSAGES,
        isLoading,
        error,
        refetch,
    } = useQuery({
        queryKey: chatMessagesQueryKey(targetUserId ?? ''),
        queryFn: () => fetchChatMessages(targetUserId),
        enabled: !!targetUserId,
        staleTime: Infinity,
        refetchOnWindowFocus: false,
    });

    const messages = useMemo(() => {
        const map = new Map<string, ChatMessage>();
        for (const msg of baseMessages) {
            map.set(msg.id, msg);
        }
        for (const msg of storeMessages) {
            map.set(msg.id, msg);
        }
        return Array.from(map.values());
    }, [baseMessages, storeMessages]);

    const oldestMessageId = messages[0]?.id;

    const loadOlder = useCallback(async () => {
        // ref 互斥而非 state：await 期间 hasMore 仍是旧值，顶部连续滚动会并发进入，
        // 两次都算出同一个 oldestMessageId，把同一批历史 concat 两遍（消息重复）
        if (!targetUserId || !hasMore || !oldestMessageId || loadingOlderRef.current) {
            return;
        }
        loadingOlderRef.current = true;

        try {
            const response = await messageApi.getConversation(targetUserId);
            const allMessages = normalizeChatMessages(response.data ?? []);
            const oldestIndex = allMessages.findIndex(m => m.id === oldestMessageId);

            if (oldestIndex <= 0) {
                setHasMore(false);
                return;
            }

            const olderBatch = allMessages.slice(Math.max(0, oldestIndex - CHAT_PAGE_SIZE), oldestIndex);
            if (oldestIndex <= CHAT_PAGE_SIZE) {
                setHasMore(false);
            }

            queryClient.setQueryData<ChatMessage[]>(chatMessagesQueryKey(targetUserId), old => {
                return olderBatch.concat(old ?? []);
            });
        } catch (e) {
            // 向上抛：翻历史失败要让用户看到提示，不能静默吞掉
            throw e instanceof Error ? e : new Error('加载历史消息失败');
        } finally {
            loadingOlderRef.current = false;
        }
    }, [targetUserId, oldestMessageId, hasMore, queryClient]);

    return {
        messages,
        isLoading,
        isError: !!error,
        error,
        refetch,
        loadOlder,
        hasMore,
    };
}
