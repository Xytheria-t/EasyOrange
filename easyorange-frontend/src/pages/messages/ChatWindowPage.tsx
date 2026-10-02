import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { messageApi } from '@/api/messageApi';
import { ChatHeader, ChatInputBar, MessageList } from '@/components/chat';
import { ErrorState } from '@/components/feedback/StateDisplay';
import { useChatMessages, useMessageRecall, useStompChat } from '@/hooks/chat';
import { chatMessagesQueryKey } from '@/hooks/chat/chatMessagesQuery';
import { useAuthStore } from '@/store/authStore';
import { useChatStore } from '@/store/chatStore';
import { useUIStore } from '@/store/uiStore';
import type { ChatMessage } from '@/types/message';
import { WS_MESSAGE_TYPE_CHAT } from '@/types/message';
import { errorHandler } from '@/utils/errorHandler';
import './chat-window.css';

function ChatWindowPage() {
    const { targetUserId } = useParams<{ targetUserId: string }>();
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    // 系统通知伪会话（后端 ConversationQueryHandler.SYSTEM_CONVERSATION）：只读——
    // 发出的 WS 帧既不落库也无回执，静默失败比禁用输入更糟
    const isSystemSession = targetUserId === 'system';

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
    } = useChatMessages(targetUserId ?? null, conversationId);
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
        if (messages.length === 0 || !targetUserId) {
            return;
        }

        const unreadIds = messages.filter(m => m.senderId !== targetUserId && m.status !== 'READ').map(m => m.id);

        if (unreadIds.length === 0) {
            return;
        }

        // 成功后本地回写 READ：此前只发请求不回写，缓存里 status 恒为 SENT，
        // 每条新消息到达都会把整页未读 ID 重新 PUT 一遍
        const marked = new Set(unreadIds);
        messageApi
            .markAsRead(unreadIds)
            .then(() => {
                queryClient.setQueryData<ChatMessage[]>(chatMessagesQueryKey(targetUserId), old =>
                    (old ?? []).map(m => (marked.has(m.id) ? { ...m, status: 'READ' } : m))
                );
            })
            .catch(e => {
                addToast({ type: 'error', message: errorHandler.handle(e) });
            });
    }, [messages, targetUserId, queryClient, addToast]);

    const handleSend = useCallback(
        (content: string) => {
            if (!targetUserId || isSystemSession || !content.trim()) {
                return;
            }
            sendMessage({
                receiverId: targetUserId,
                content: content.trim(),
                type: WS_MESSAGE_TYPE_CHAT,
                conversationId,
            });
        },
        [targetUserId, isSystemSession, conversationId, sendMessage]
    );

    const handleTyping = useCallback(() => {
        if (targetUserId && !isSystemSession) {
            sendTyping(conversationId, targetUserId);
        }
    }, [targetUserId, isSystemSession, conversationId, sendTyping]);

    const handleBack = useCallback(() => navigate(-1), [navigate]);

    const isTyping = typingUsers.size > 0;
    const targetUserName = isSystemSession ? '系统通知' : (targetUserId ?? '用户');

    // 对方头像从已拉到的消息里取：会话列表能显示、点进聊天页却退回灰色首字母圆点，
    // 就是因为这里此前写死 null。取任一条对方发的消息即可，两侧档案对称。
    const targetAvatar = useMemo(() => {
        if (isSystemSession) {
            return null;
        }
        const counterpart = messages.find(m => m.senderId !== currentUserId);
        return counterpart?.senderAvatar ?? null;
    }, [messages, isSystemSession, currentUserId]);

    return (
        <div className="chat-window-page">
            <div className={`connection-status status-${connectionStatus}`} />

            <ChatHeader
                onBack={handleBack}
                targetUser={targetUserId ? { id: targetUserId, name: targetUserName, avatar: targetAvatar } : null}
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
                        targetUserName={targetUserName}
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
                    isDisabled={!targetUserId || isSystemSession}
                    disabledPlaceholder={isSystemSession ? '系统通知不支持回复' : undefined}
                />
            </div>
        </div>
    );
}

export default ChatWindowPage;
