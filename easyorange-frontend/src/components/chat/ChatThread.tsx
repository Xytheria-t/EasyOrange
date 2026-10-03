import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo } from 'react';
import { messageApi } from '@/api/messageApi';
import { useChatMessages, useMessageRecall, useStompChat } from '@/hooks/chat';
import { chatMessagesQueryKey } from '@/hooks/chat/chatMessagesQuery';
import { useAuthStore } from '@/store/authStore';
import { useChatStore } from '@/store/chatStore';
import { useUIStore } from '@/store/uiStore';
import type { ChatMessage } from '@/types/message';
import { SYSTEM_TARGET_USER_ID, WS_MESSAGE_TYPE_CHAT } from '@/types/message';
import { errorHandler } from '@/utils/errorHandler';
import { ErrorState } from '../feedback/StateDisplay';
import ChatHeader from './ChatHeader';
import ChatInputBar from './ChatInputBar';
import MessageList from './MessageList';

/** 与后端 markAsReadBatch 的单次上限（50）对齐 */
const MARK_READ_BATCH_SIZE = 50;

export interface ChatCounterpart {
    id: string;
    name: string;
    avatar: string | null;
}

interface ChatThreadProps {
    /** 会话对方。名字与头像由会话列表带下来，深链进来时退化为从消息反查 */
    counterpart: ChatCounterpart;
    /** 收起会话（移动端从列表进入时用）；桌面端双栏常驻列表，不传即不渲染返回键 */
    onBack?: () => void;
}

/**
 * 单个会话的完整视图 —— 头部 + 消息流 + 输入框。
 *
 * <p>消息中心双栏与独立路由共用这一个组件：会话的订阅、标已读、撤回、发送都收敛在此，
 * 页面层只负责「当前选中谁」。
 */
function ChatThread({ counterpart, onBack }: ChatThreadProps) {
    const queryClient = useQueryClient();
    const targetUserId = counterpart.id;

    // 系统通知伪会话（后端 ConversationQueryHandler.SYSTEM_CONVERSATION）：只读——
    // 发出的 WS 帧既不落库也无回执，静默失败比禁用输入更糟
    const isSystemSession = targetUserId === SYSTEM_TARGET_USER_ID;

    const { user } = useAuthStore();
    const currentUserId = user?.userId ?? '';

    const conversationId = useMemo(() => {
        if (!targetUserId || !currentUserId) {
            return '';
        }
        return `conv_${[currentUserId, targetUserId].sort().join('_')}`;
    }, [targetUserId, currentUserId]);

    const connectionStatus = useChatStore(s => s.connectionStatus);
    const typingUsers = useChatStore(s => s.typingUsers);

    const { sendMessage, sendTyping, subscribe, unsubscribe } = useStompChat();
    const {
        messages,
        isLoading,
        isError: isMessagesError,
        error: messagesError,
        refetch: refetchMessages,
        loadOlder,
        hasMore,
    } = useChatMessages(targetUserId, conversationId);
    const { canRecall, recallMessage } = useMessageRecall(conversationId);
    const addToast = useUIStore(s => s.addToast);

    // 翻历史失败由 hook 抛出，这里转成提示；静默吞掉会让用户以为已经到底了
    const handleLoadOlder = useCallback(async () => {
        try {
            await loadOlder();
        } catch (e) {
            addToast({
                type: 'error',
                message: e instanceof Error ? e.message : '加载历史消息失败',
            });
        }
    }, [loadOlder, addToast]);

    // 撤回失败要出声：recallMessage 返回 false 时此前无人判返回值，
    // 用户看到的是「点了撤回、菜单关了、什么都没发生」
    const handleRecall = useCallback(
        async (messageId: string) => {
            const ok = await recallMessage(messageId);
            if (!ok) {
                addToast({ type: 'error', message: '撤回失败，可能已超过 2 分钟可撤回时限' });
            }
            return ok;
        },
        [recallMessage, addToast]
    );

    useEffect(() => {
        if (!conversationId) {
            return;
        }
        subscribe(conversationId);
        return () => unsubscribe(conversationId);
    }, [conversationId, subscribe, unsubscribe]);

    useEffect(() => {
        if (messages.length === 0) {
            return;
        }

        // 判据是「收件人是我」，不是「发送方是对方」：自己发的消息收件人是对方，天然被排除；
        // 而系统通知 senderId 为 null，按发送方筛永远选不中，它那一格的未读徽标会永远卡住
        const unreadIds = messages.filter(m => m.receiverId === currentUserId && m.status !== 'READ').map(m => m.id);

        if (unreadIds.length === 0) {
            return;
        }

        // 后端单次上限 50 条（防 IN 子句膨胀），翻历史/长会话积压超限时按 50 分块
        const chunks: string[][] = [];
        for (let i = 0; i < unreadIds.length; i += MARK_READ_BATCH_SIZE) {
            chunks.push(unreadIds.slice(i, i + MARK_READ_BATCH_SIZE));
        }

        // 成功后本地回写 READ：否则缓存里 status 恒为 SENT，每条新消息到达都会把整页未读重发一遍
        const marked = new Set(unreadIds);
        Promise.all(chunks.map(chunk => messageApi.markAsRead(chunk)))
            .then(() => {
                queryClient.setQueryData<ChatMessage[]>(chatMessagesQueryKey(targetUserId), old =>
                    (old ?? []).map(m => (marked.has(m.id) ? { ...m, status: 'READ' } : m))
                );
            })
            .catch(e => {
                addToast({ type: 'error', message: errorHandler.handle(e) });
            });
    }, [messages, currentUserId, targetUserId, queryClient, addToast]);

    const handleSend = useCallback(
        (content: string) => {
            if (isSystemSession || !content.trim()) {
                return;
            }
            sendMessage({
                receiverId: targetUserId,
                content: content.trim(),
                type: WS_MESSAGE_TYPE_CHAT,
                conversationId,
            });
        },
        [isSystemSession, targetUserId, conversationId, sendMessage]
    );

    const handleTyping = useCallback(() => {
        if (!isSystemSession) {
            sendTyping(conversationId, targetUserId);
        }
    }, [isSystemSession, conversationId, targetUserId, sendTyping]);

    const isTyping = typingUsers.size > 0;

    // 会话列表没这一行时（直接粘深链进来）从已拉到的消息反查头像：两侧档案对称，不必再查用户档案
    const targetAvatar = useMemo(() => {
        if (counterpart.avatar) {
            return counterpart.avatar;
        }
        return messages.find(m => m.senderId !== currentUserId)?.senderAvatar ?? null;
    }, [counterpart.avatar, messages, currentUserId]);

    return (
        <div className="chat-thread">
            <div className={`connection-status status-${connectionStatus}`} />

            <ChatHeader
                onBack={onBack}
                isTyping={isTyping}
                targetUser={{ id: targetUserId, name: counterpart.name, avatar: targetAvatar }}
            />

            <div className="chat-messages-area">
                {isLoading ? (
                    <div className="chat-loading" role="status" aria-busy="true">
                        <div className="chat-loading-spinner" />
                        <span className="text-sm font-medium">加载消息中...</span>
                    </div>
                ) : isMessagesError && messages.length === 0 ? (
                    <ErrorState
                        title="消息加载失败"
                        description={
                            messagesError instanceof Error && messagesError.message ? messagesError.message : undefined
                        }
                        onRetry={() => {
                            refetchMessages();
                        }}
                    />
                ) : (
                    <MessageList
                        messages={messages}
                        currentUserId={currentUserId}
                        targetUserName={counterpart.name}
                        isTyping={isTyping}
                        onLoadMore={handleLoadOlder}
                        hasMore={hasMore}
                        onRecall={handleRecall}
                        canRecallFn={msg => canRecall(msg, currentUserId)}
                    />
                )}
            </div>

            <div className="chat-input-area">
                <ChatInputBar
                    onSend={handleSend}
                    onTyping={handleTyping}
                    isDisabled={isSystemSession}
                    disabledPlaceholder="系统通知不支持回复"
                />
            </div>
        </div>
    );
}

export default ChatThread;
