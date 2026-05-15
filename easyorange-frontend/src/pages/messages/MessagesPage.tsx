import { Link } from 'react-router-dom';
import { MessageCircle, Bell, Sparkles, Zap, Send, Brain } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { messageApi } from '@/api/messageApi';
import type { ChatSession } from '@/types';
import './messages.css';

function MessagesPage() {
  const { data: conversations, isLoading, error } = useQuery({
    queryKey: ['messages', 'conversations'],
    queryFn: async () => {
      const response = await messageApi.getConversations();
      return (response.data ?? []) as unknown as ChatSession[];
    },
    staleTime: 15 * 1000,
  });

  if (isLoading) {
    return (
      <div className="messages-page">
        <div className="messages-ambient">
          <div className="messages-orb messages-orb-1" />
          <div className="messages-orb messages-orb-2" />
          <div className="messages-orb messages-orb-3" />
        </div>
        <div className="messages-header">
          <div className="messages-header-left">
            <div className="messages-kicker">
              <span className="kicker-dot" />
              Messages
            </div>
            <h1 className="messages-title">消息中心</h1>
            <p className="messages-subtitle">与卖家实时沟通，快速达成交易</p>
          </div>
        </div>
        <div className="messages-loading">
          <div className="loading-spinner" />
          <span>加载中...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="messages-page">
        <div className="messages-ambient">
          <div className="messages-orb messages-orb-1" />
          <div className="messages-orb messages-orb-2" />
          <div className="messages-orb messages-orb-3" />
        </div>
        <div className="messages-header">
          <div className="messages-header-left">
            <div className="messages-kicker">
              <span className="kicker-dot" />
              Messages
            </div>
            <h1 className="messages-title">消息中心</h1>
          </div>
        </div>
        <div className="messages-empty">
          <p>加载消息失败，请稍后重试</p>
        </div>
      </div>
    );
  }

  const hasConversations = conversations && conversations.length > 0;

  return (
    <div className="messages-page">
      <div className="messages-ambient">
        <div className="messages-orb messages-orb-1" />
        <div className="messages-orb messages-orb-2" />
        <div className="messages-orb messages-orb-3" />
      </div>

      <div className="messages-header">
        <div className="messages-header-left">
          <div className="messages-kicker">
            <span className="kicker-dot" />
            Messages
          </div>
          <h1 className="messages-title">消息中心</h1>
          <p className="messages-subtitle">与卖家实时沟通，快速达成交易</p>
        </div>
      </div>

      <div className="messages-ai-section">
        <div className="messages-ai-card">
          <div className="messages-ai-header">
            <div className="messages-ai-icon">
              <Brain size={18} />
            </div>
            <div className="messages-ai-title">
              <h3>AI智能助手</h3>
              <span className="messages-ai-badge">
                <Zap size={10} />
                智能回复
              </span>
            </div>
          </div>
          <p className="messages-ai-desc">AI可以根据对话内容，为你推荐合适的回复建议，让沟通更高效</p>
          <div className="messages-ai-features">
            <div className="ai-feature-item">
              <Sparkles size={14} />
              <span>智能回复建议</span>
            </div>
            <div className="ai-feature-item">
              <Send size={14} />
              <span>一键发送</span>
            </div>
            <div className="ai-feature-item">
              <MessageCircle size={14} />
              <span>多场景适配</span>
            </div>
          </div>
        </div>
      </div>

      {!hasConversations ? (
        <div className="messages-empty">
          <div className="empty-visual">
            <div className="empty-orbit" />
            <div className="empty-icon-wrap">
              <MessageCircle size={36} />
            </div>
          </div>
          <h3>暂无新消息</h3>
          <p>当您收到卖家回复或系统通知时，会在这里显示</p>
        </div>
      ) : (
        <div className="messages-list">
          {conversations.map((conv) => (
            <Link
              key={conv.id}
              to={`/messages/${conv.targetUserId}`}
              className="message-card"
            >
              <div className="message-avatar-wrap">
                {conv.targetUserAvatar ? (
                  <img
                    src={conv.targetUserAvatar}
                    alt={conv.targetUserName}
                    className="message-avatar"
                    width="48"
                    height="48"
                  />
                ) : (
                  <div className="message-avatar-fallback">
                    <span>{conv.targetUserName?.charAt(0) ?? '?'}</span>
                  </div>
                )}
                {conv.unreadCount > 0 && (
                  <span className="message-badge">
                    {conv.unreadCount > 9 ? '9+' : conv.unreadCount}
                  </span>
                )}
              </div>
              <div className="message-content">
                <div className="message-header">
                  <h3 className="message-name">{conv.targetUserName}</h3>
                  <span className="message-time">{conv.lastMessageTime}</span>
                </div>
                <p className="message-preview">{conv.lastMessage}</p>
              </div>
            </Link>
          ))}
        </div>
      )}

      <div className="messages-notification">
        <div className="notification-icon">
          <Bell size={20} />
        </div>
        <div className="notification-content">
          <p className="notification-title">消息提醒</p>
          <p className="notification-desc">开启消息推送，及时获取最新动态</p>
        </div>
        <button type="button" className="notification-btn">开启</button>
      </div>
    </div>
  );
}

export default MessagesPage;
