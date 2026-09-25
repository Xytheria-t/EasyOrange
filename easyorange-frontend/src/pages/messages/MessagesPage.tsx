import { useQuery } from '@tanstack/react-query';
import { MessageCircle, RefreshCw } from 'lucide-react';
import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { messageApi } from '@/api/messageApi';
import { PaginationBar } from '@/components/PaginationBar';
import { Button } from '@/components/ui/button';
import { usePagination } from '@/hooks/usePagination';
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

    const { pageNum: convPage, pageSize: convPageSize, goTo: setConvPage } = usePagination({ pageSize: 10 });

    const totalConversationPages = Math.max(1, Math.ceil((conversations?.length ?? 0) / convPageSize));
    const paginatedConversations = useMemo(() => {
        if (!conversations) {
            return [];
        }
        return conversations.slice((convPage - 1) * convPageSize, convPage * convPageSize);
    }, [conversations, convPage, convPageSize]);

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
                    {/* Conversation list */}
                    <div className="messages-list-container">
                        <div className="messages-list">
                            {hasConversations ? (
                                paginatedConversations.map(conv => (
                                    <Link
                                        key={conv.targetUserId}
                                        to={`/messages/${conv.targetUserId}`}
                                        className="message-card"
                                    >
                                        <div className="message-avatar-wrap">
                                            {conv.targetUserAvatar ? (
                                                <img
                                                    src={conv.targetUserAvatar}
                                                    alt={conv.targetUserName}
                                                    className="message-avatar"
                                                    width="44"
                                                    height="44"
                                                    loading="lazy"
                                                    decoding="async"
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

                        {/* Pagination for conversation list */}
                        <PaginationBar
                            pageNum={convPage}
                            totalPages={totalConversationPages}
                            onPageChange={setConvPage}
                        />
                    </div>
                </div>

                {/* RIGHT: Welcome panel */}
                <div className="messages-welcome-panel">
                    <div className="messages-welcome-content">
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
                    </div>
                </div>
            </div>
        </div>
    );
}

export default MessagesPage;
