import { useCallback, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { messageApi } from '@/api/messageApi';
import { useChatStore } from '@/stores/chatStore';
import type { ChatMessage } from '@/types/message';

const PAGE_SIZE = 50;

export function useChatMessages(conversationId: string | null) {
  const [hasMore, setHasMore] = useState(true);
  const queryClient = useQueryClient();

  const storeMessages = useChatStore((s) =>
    conversationId ? (s.messages[conversationId] ?? []) : []
  );

  const { isLoading, error } = useQuery({
    queryKey: ['chat', 'messages', conversationId],
    queryFn: async () => {
      if (!conversationId) {return [] as ChatMessage[];}
      const response = await messageApi.getConversation(conversationId);
      const data = (response.data ?? []) as unknown as ChatMessage[];
      return data.slice(-PAGE_SIZE);
    },
    enabled: !!conversationId,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });

  const baseMessages = queryClient.getQueryData<ChatMessage[]>(['chat', 'messages', conversationId]) ?? [];

  const storeMessageMap = new Map(storeMessages.map((m) => [m.id, m]));

  const messages: ChatMessage[] = baseMessages.map((base) => {
    const stored = storeMessageMap.get(base.id);
    if (!stored) {return base;}
    return stored;
  });

  storeMessages.forEach((stored) => {
    if (!messages.some((m) => m.id === stored.id)) {
      messages.push(stored);
    }
  });

  const loadOlder = useCallback(async () => {
    if (!conversationId || !hasMore) {return;}
    const oldestMessage = messages[0];
    if (!oldestMessage) {return;}

    try {
      const response = await messageApi.getConversation(conversationId);
      const allMessages = (response.data ?? []) as unknown as ChatMessage[];
      const oldestIndex = allMessages.findIndex((m) => m.id === oldestMessage.id);

      if (oldestIndex <= 0) {
        setHasMore(false);
        return;
      }

      const olderBatch = allMessages.slice(Math.max(0, oldestIndex - PAGE_SIZE), oldestIndex);
      if (oldestIndex <= PAGE_SIZE) {
        setHasMore(false);
      }
      return olderBatch;
    } catch (e) {
      console.error('[useChatMessages] Failed to load older messages', e);
    }
  }, [conversationId, messages, hasMore]);

  return {
    messages,
    isLoading,
    error,
    loadOlder,
    hasMore,
  };
}
