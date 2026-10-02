import { messageApi } from '@/api/messageApi';
import type { ChatMessage } from '@/types/message';
import { normalizeChatMessages } from '@/utils/message';

export const CHAT_PAGE_SIZE = 50;

/**
 * 聊天消息的 query key 与取数函数 —— 唯一来源。
 *
 * <p>此前 {@code ProductDetailPage} 的 prefetch 与 {@code useChatMessages} 各写一份同 key 的
 * queryFn，prefetch 那份没过 {@link normalizeChatMessages}。两者 {@code staleTime} 都是 Infinity，
 * 缓存新鲜时页面的 queryFn 不执行，原始 {@code ConversationVO} 直接进组件：
 * status 为 undefined（自己发的消息一个勾都不显示）、type 是数字而非 'TEXT'。
 */
export const chatMessagesQueryKey = (targetUserId: string) => ['chat', 'messages', targetUserId] as const;

export async function fetchChatMessages(targetUserId: string | null): Promise<ChatMessage[]> {
    if (!targetUserId) {
        return [];
    }
    const response = await messageApi.getConversation(targetUserId);
    return normalizeChatMessages(response.data ?? []).slice(-CHAT_PAGE_SIZE);
}
