import { useParams, useNavigate } from 'react-router-dom';
import { useEffect, useCallback, useMemo } from 'react';
import { ChatHeader, MessageList, ChatInputBar } from '@/components/chat';
import { useStompChat, useMessageRecall, useChatMessages } from '@/hooks/chat';
import { useChatStore } from '@/store/chatStore';
import { useAuthStore } from '@/store/authStore';
import { messageApi } from '@/api/messageApi';
import './chat-window.css';

function ChatWindowPage() {
  const { targetUserId } = useParams<{ targetUserId: string }>();
  const navigate = useNavigate();

  const { user } = useAuthStore();
  const currentUserId = user?.userId ?? '';

  const conversationId = useMemo(() => {
    if (!targetUserId || !currentUserId) return '';
    return `conv_${[currentUserId, targetUserId].sort().join('_')}`;
  }, [targetUserId, currentUserId]);

  const connectionStatus = useChatStore((s) => s.connectionStatus);
  const typingUsers = useChatStore((s) => s.typingUsers);

  const { sendMessage, sendTyping, subscribe, unsubscribe } = useStompChat();
  const { messages, isLoading, loadOlder, hasMore } = useChatMessages(targetUserId ?? null, conversationId);
  const { canRecall, recallMessage } = useMessageRecall(conversationId);

  useEffect(() => {
    if (!conversationId) return;
    subscribe(conversationId);
    return () => unsubscribe(conversationId);
  }, [conversationId, subscribe, unsubscribe]);

  useEffect(() => {
    if (messages.length === 0 || !targetUserId) return;

    const unreadIds = messages
      .filter((m) => m.senderId !== targetUserId && m.status !== 'READ')
      .map((m) => m.id);

    if (unreadIds.length > 0) {
      messageApi.markAsRead(unreadIds.map((id) => Number(id))).catch(console.error);
    }
  }, [messages.length, targetUserId]);

  const handleSend = useCallback(
    (content: string) => {
      if (!targetUserId || !content.trim()) return;
      sendMessage({
        receiverId: targetUserId,
        content: content.trim(),
        type: 0,
        conversationId,
      });
    },
    [targetUserId, conversationId, sendMessage]
  );

  const handleTyping = useCallback(() => {
    if (targetUserId) {
      sendTyping(conversationId, targetUserId);
    }
  }, [targetUserId, conversationId, sendTyping]);

  const handleBack = useCallback(() => navigate(-1), [navigate]);

  const isTyping = typingUsers.size > 0;
  const targetUserName = targetUserId ?? '用户';

  return (
    <div className="chat-window-page">
      <div className={`connection-status status-${connectionStatus}`} />

      <ChatHeader onBack={handleBack} targetUser={targetUserId ? { id: targetUserId, name: targetUserName, avatar: null } : null} />

      <div className="chat-messages-area">
        {isLoading ? (
          <div className="chat-loading">
            <div className="chat-loading-spinner" />
            <span className="text-sm font-medium">加载消息中...</span>
          </div>
        ) : (
          <MessageList
            messages={messages}
            currentUserId={currentUserId}
            targetUserName={targetUserName}
            isTyping={isTyping}
            onLoadMore={loadOlder}
            hasMore={hasMore}
            onRecall={recallMessage}
            canRecallFn={(msg) => canRecall(msg, currentUserId)}
          />
        )}
      </div>

      <div className="chat-input-area">
        <ChatInputBar
          onSend={handleSend}
          onTyping={handleTyping}
          isDisabled={!targetUserId}
        />
      </div>
    </div>
  );
}

export default ChatWindowPage;
