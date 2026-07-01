import { useQuery } from '@tanstack/react-query';
import { Brain, MessageCircle, RefreshCw, Send, Sparkles, Zap } from 'lucide-react';
import { Link } from 'react-router-dom';
import { messageApi } from '@/api/messageApi';
import { Button } from '@/components/ui/button';
import type { ChatSession } from '@/types';
import './messages.css';

function MessagesPage() {
    const {
        data: conversations,
        isLoading,
        error,
        refetch,
    } = useQuery({
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
                </div>
                <div className="messages-topbar">
                    <div className="messages-topbar-left">
                        <div className="messages-kicker">
                            <span className="kicker-dot" />
                            Messages
                        </div>
                        <div className="messages-topbar-title">
                            <h1>消息中心</h1>
                            <p>与资产方实时沟通，快速达成交易</p>
                        </div>
                    </div>
                </div>
                <div className="messages-body">
                    <div className="messages-conversations-panel">
                        <div className="messages-loading">
                            <div className="loading-spinner" />
                            <span>加载中...</span>
                        </div>
                    </div>
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
                </div>
                <div className="messages-topbar">
                    <div className="messages-topbar-left">
                        <div className="messages-kicker">
                            <span className="kicker-dot" />
                            Messages
                        </div>
                        <div className="messages-topbar-title">
                            <h1>消息中心</h1>
                        </div>
                    </div>
                </div>
                <div className="messages-body">
                    <div className="messages-welcome-panel">
                        <div className="messages-error">
                            <p>加载消息失败，请稍后重试</p>
                            <Button
                                type="button"
                                variant="outline"
                                className="error-retry-btn"
                                onClick={() => refetch()}
                            >
                                <RefreshCw size={14} style={{ marginRight: '0.375rem', display: 'inline' }} />
                                重新加载
                            </Button>
                        </div>
                    </div>
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
            </div>

            {/* Topbar */}
            <div className="messages-topbar">
                <div className="messages-topbar-left">
                    <div className="messages-kicker">
                        <span className="kicker-dot" />
                        Messages
                    </div>
                    <div className="messages-topbar-title">
                        <h1>消息中心</h1>
                        <p>与资产方实时沟通，快速达成交易</p>
                    </div>
                </div>
            </div>

            {/* Two-panel body */}
            <div className="messages-body">
                {/* LEFT: Conversation panel */}
                <div className="messages-conversations-panel">
                    {/* AI Smart Card - compact */}
                    <div className="messages-ai-compact">
                        <div className="messages-ai-compact-inner">
                            <div className="messages-ai-compact-icon">
                                <Brain size={16} />
                            </div>
                            <div className="messages-ai-compact-body">
                                <div className="messages-ai-compact-body-header">
                                    <h3>AI智能助手</h3>
                                    <span className="messages-ai-compact-badge">
                                        <Zap size={8} />
                                        智能回复
                                    </span>
                                </div>
                                <p className="messages-ai-compact-desc">
                                    AI可以根据对话内容，为你推荐合适的回复建议，让沟通更高效
                                </p>
                            </div>
                        </div>
                        <div className="messages-ai-compact-features">
                            <span className="ai-compact-feature">
                                <Sparkles />
                                智能回复建议
                            </span>
                            <span className="ai-compact-feature">
                                <Send />
                                一键发送
                            </span>
                            <span className="ai-compact-feature">
                                <MessageCircle />
                                多场景适配
                            </span>
                        </div>
                    </div>

                    {/* Conversation list */}
                    <div className="messages-list-container">
                        <div className="messages-list">
                            {hasConversations ? (
                                conversations.map(conv => (
                                    <Link key={conv.id} to={`/messages/${conv.targetUserId}`} className="message-card">
                                        <div className="message-avatar-wrap">
                                            {conv.targetUserAvatar ? (
                                                <img
                                                    src={conv.targetUserAvatar}
                                                    alt={conv.targetUserName}
                                                    className="message-avatar"
                                                    width="44"
                                                    height="44"
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
                                            <div className="message-header-row">
                                                <h3 className="message-name">{conv.targetUserName}</h3>
                                                <span className="message-time">{conv.lastMessageTime}</span>
                                            </div>
                                            <p className="message-preview">{conv.lastMessage}</p>
                                        </div>
                                    </Link>
                                ))
                            ) : (
                                /* Empty state inside list area - when no conversations */
                                <div className="messages-empty" style={{ padding: '3rem 1rem' }}>
                                    <div className="empty-visual">
                                        <div className="empty-orbit" />
                                        <div className="empty-icon-wrap">
                                            <MessageCircle size={28} />
                                        </div>
                                    </div>
                                    <h3>暂无新消息</h3>
                                    <p>当您收到资产方回复或系统通知时，会在这里显示</p>
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                {/* RIGHT: Welcome panel - features + notification */}
                <div className="messages-welcome-panel">
                    <div className="messages-welcome-content">
                        {hasConversations && (
                            <div className="messages-empty">
                                <div className="empty-visual">
                                    <div className="empty-orbit" />
                                    <div className="empty-icon-wrap">
                                        <MessageCircle size={32} />
                                    </div>
                                </div>
                                <h3>选择对话</h3>
                                <p>从左侧选择一个会话开始聊天</p>
                            </div>
                        )}

                        {/* AI features */}
                        <div className="messages-welcome-features">
                            <div className="welcome-feature-item">
                                <div className="welcome-feature-icon">
                                    <Sparkles size={18} />
                                </div>
                                <div className="welcome-feature-text">
                                    <strong>智能回复建议</strong>
                                    <span>AI 根据对话内容推荐合适的回复</span>
                                </div>
                            </div>
                            <div className="welcome-feature-item">
                                <div className="welcome-feature-icon">
                                    <Send size={18} />
                                </div>
                                <div className="welcome-feature-text">
                                    <strong>一键发送</strong>
                                    <span>快速回复，让沟通更高效</span>
                                </div>
                            </div>
                            <div className="welcome-feature-item">
                                <div className="welcome-feature-icon">
                                    <MessageCircle size={18} />
                                </div>
                                <div className="welcome-feature-text">
                                    <strong>多场景适配</strong>
                                    <span>聊天、咨询、售后全覆盖</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default MessagesPage;
