import { useCallback, useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { messageApi } from '@/api/messageApi';
import { useChatStore } from '@/store/chatStore';
import type { ChatMessage } from '@/types/message';
import { normalizeChatMessages } from '@/utils/message';

const PAGE_SIZE = 50;
const EMPTY_MESSAGES: ChatMessage[] = [];

export function useChatMessages(targetUserId: string | null, conversationId: string) {
  const [hasMore, setHasMore] = useState(true);
  const queryClient = useQueryClient();

  const storeMessages = useChatStore((s) =>
    conversationId ? (s.messages[conversationId] ?? EMPTY_MESSAGES) : EMPTY_MESSAGES
  );

  const { data: baseMessages = EMPTY_MESSAGES, isLoading, error } = useQuery({
    queryKey: ['chat', 'messages', targetUserId],
    queryFn: async () => {
      if (!targetUserId) {return EMPTY_MESSAGES;}
      const response = await messageApi.getConversation(targetUserId);
      const data = normalizeChatMessages(response.data ?? []);
      return data.slice(-PAGE_SIZE);
    },
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
    if (!targetUserId || !hasMore || !oldestMessageId) {return;}

    try {
      const response = await messageApi.getConversation(targetUserId);
      const allMessages = normalizeChatMessages(response.data ?? []);
      const oldestIndex = allMessages.findIndex((m) => m.id === oldestMessageId);

      if (oldestIndex <= 0) {
        setHasMore(false);
        return;
      }

      const olderBatch = allMessages.slice(Math.max(0, oldestIndex - PAGE_SIZE), oldestIndex);
      if (oldestIndex <= PAGE_SIZE) {
        setHasMore(false);
      }

      queryClient.setQueryData<ChatMessage[]>(['chat', 'messages', targetUserId], (old) => {
        return olderBatch.concat(old ?? []);
      });
    } catch {
      // Failed to load older messages
    }
  }, [targetUserId, oldestMessageId, hasMore, queryClient]);

  return {
    messages,
    isLoading,
    error,
    loadOlder,
    hasMore,
  };
}
