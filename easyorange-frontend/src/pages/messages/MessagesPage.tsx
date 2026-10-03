import { useQuery } from '@tanstack/react-query';
import { Bell, MessageCircle } from 'lucide-react';
import { useMemo } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { messageApi } from '@/api/messageApi';
import { ChatThread } from '@/components/chat';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/StateDisplay';
import { PaginationBar } from '@/components/PaginationBar';
import { Button } from '@/components/ui/button';
import { usePagination } from '@/hooks/usePagination';
import type { ChatSession } from '@/types';
import { SYSTEM_TARGET_USER_ID } from '@/types/message';
import { formatRelativeTime } from '@/utils';
import './messages.css';

/** 从商品页「联系卖家」带过来的对方昵称：会话列表里还没有这条会话时，头部得有个能认出来的人名 */
interface ChatNavState {
    counterpartName?: string;
}

const CONVERSATION_PAGE_SIZE = 20;

/**
 * 消息中心 —— 左侧会话列表 + 右侧当前会话。
 *
 * <p>选中会话走路由参数（`/messages/:targetUserId`）而非组件内state：
 * 这样深链可分享、浏览器后退能回到上一个会话，移动端也能只显示会话视图。
 */
function MessagesPage() {
    const { targetUserId } = useParams<{ targetUserId: string }>();
    const location = useLocation();
    const navigate = useNavigate();

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

    const { pageNum: convPage, goTo: setConvPage } = usePagination({ pageSize: CONVERSATION_PAGE_SIZE });

    // 系统通知会话保留在列表里：后端把 senderId 为 null 的消息（系统/订单/支付各类通知）
    // 都归并到它，通知页只列 type=1，滤掉这行等于让订单类通知在界面上无处可见。
    // 它只读不可回，头部与输入框据此禁用输入。
    const sessions = conversations ?? [];

    const totalConversationPages = Math.max(1, Math.ceil(sessions.length / CONVERSATION_PAGE_SIZE));
    const paginatedSessions = useMemo(() => {
        const start = (convPage - 1) * CONVERSATION_PAGE_SIZE;
        return sessions.slice(start, start + CONVERSATION_PAGE_SIZE);
    }, [sessions, convPage]);

    const totalUnread = useMemo(
        () => sessions.reduce((sum, c) => sum + (c.unreadCount > 0 ? c.unreadCount : 0), 0),
        [sessions]
    );

    const activeSession = useMemo(() => sessions.find(c => c.targetUserId === targetUserId), [sessions, targetUserId]);

    const navState = location.state as ChatNavState | null;
    const counterpartName = activeSession?.targetUserName ?? navState?.counterpartName ?? '私聊';

    const closeThread = () => navigate('/messages');

    const subtitle =
        totalUnread > 0
            ? `${sessions.length} 个会话 · ${totalUnread} 条未读`
            : `${sessions.length} 个会话 · 与资产方实时沟通`;

    const listPanel = (() => {
        if (isLoading) {
            return <LoadingState label="正在加载会话" className="messages-panel-state" />;
        }
        if (error) {
            return (
                <ErrorState
                    title="会话加载失败"
                    description="网络或服务暂时不可用，请稍后重试。"
                    onRetry={() => refetch()}
                    className="messages-panel-state"
                />
            );
        }
        if (sessions.length === 0) {
            return (
                <EmptyState
                    icon={MessageCircle}
                    title="暂无会话"
                    description="在商品详情页点「联系卖家」即可开始第一段对话。"
                    action={
                        <Button variant="outline" asChild>
                            <Link to="/products">去逛逛商品</Link>
                        </Button>
                    }
                    className="messages-panel-state"
                />
            );
        }
        return (
            <>
                <ul className="messages-list">
                    {paginatedSessions.map(conv => {
                        const isActive = conv.targetUserId === targetUserId;
                        const isSystem = conv.targetUserId === SYSTEM_TARGET_USER_ID;
                        return (
                            <li key={conv.targetUserId}>
                                <Link
                                    to={`/messages/${conv.targetUserId}`}
                                    className={`message-card ${isActive ? 'is-active' : ''}`}
                                    aria-current={isActive ? 'page' : undefined}
                                    aria-label={
                                        conv.unreadCount > 0
                                            ? `${conv.targetUserName}，${conv.unreadCount} 条未读`
                                            : conv.targetUserName
                                    }
                                >
                                    <span className="message-avatar-wrap">
                                        {conv.targetUserAvatar ? (
                                            <img
                                                src={conv.targetUserAvatar}
                                                alt=""
                                                className="message-avatar"
                                                width="44"
                                                height="44"
                                                loading="lazy"
                                                decoding="async"
                                            />
                                        ) : isSystem ? (
                                            // 系统通知不是「某个人」：首字母圆点会让人以为还有同名用户
                                            <span className="message-avatar-fallback" aria-hidden="true">
                                                <Bell size={18} />
                                            </span>
                                        ) : (
                                            <span className="message-avatar-fallback" aria-hidden="true">
                                                {conv.targetUserName?.charAt(0) ?? '?'}
                                            </span>
                                        )}
                                        {conv.unreadCount > 0 && (
                                            <span className="message-badge">
                                                {conv.unreadCount > 99 ? '99+' : conv.unreadCount}
                                            </span>
                                        )}
                                    </span>
                                    <span className="message-content">
                                        <span className="message-header-row">
                                            <span className="message-name">{conv.targetUserName}</span>
                                            <span className="message-time">
                                                {formatRelativeTime(conv.lastMessageTime)}
                                            </span>
                                        </span>
                                        <span className="message-preview">{conv.lastMessage || '暂无消息'}</span>
                                        {isSystem && <span className="message-card-tag">只读</span>}
                                    </span>
                                </Link>
                            </li>
                        );
                    })}
                </ul>
                <PaginationBar pageNum={convPage} totalPages={totalConversationPages} onPageChange={setConvPage} />
            </>
        );
    })();

    return (
        <div className={`messages-page ${targetUserId ? 'has-thread' : ''}`}>
            <div className="messages-ambient" aria-hidden="true">
                <div className="messages-orb messages-orb-1" />
                <div className="messages-orb messages-orb-2" />
            </div>

            <div className="messages-topbar">
                <div className="messages-kicker">
                    <span className="kicker-dot" />
                    Messages
                </div>
                <div className="messages-topbar-title">
                    <h1>消息中心</h1>
                    <p>{subtitle}</p>
                </div>
            </div>

            <div className="messages-body">
                <section className="messages-conversations-panel" aria-label="会话列表">
                    {listPanel}
                </section>

                <section className="messages-thread-panel" aria-label="当前会话">
                    {targetUserId ? (
                        <ChatThread
                            counterpart={{
                                id: targetUserId,
                                name: counterpartName,
                                avatar: activeSession?.targetUserAvatar ?? null,
                            }}
                            onBack={closeThread}
                        />
                    ) : (
                        <EmptyState
                            icon={MessageCircle}
                            title="选择会话"
                            description="从左侧选一个会话开始聊天，或从商品详情页联系卖家。"
                            className="messages-thread-empty"
                        />
                    )}
                </section>
            </div>
        </div>
    );
}

export default MessagesPage;
