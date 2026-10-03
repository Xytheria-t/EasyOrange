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

/** 列表按最后一条消息的时间分三段，长列表滚动时才有方位感 */
const BUCKETS = ['today', 'yesterday', 'earlier'] as const;
type Bucket = (typeof BUCKETS)[number];
const BUCKET_LABEL: Record<Bucket, string> = { today: '今天', yesterday: '昨天', earlier: '更早' };

function bucketOf(time: string): Bucket {
    const at = new Date(time);
    if (Number.isNaN(at.getTime())) {
        return 'earlier';
    }
    const now = new Date();
    const dayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const msgDay = new Date(at.getFullYear(), at.getMonth(), at.getDate()).getTime();
    const days = Math.round((dayStart - msgDay) / 86_400_000);
    if (days <= 0) {
        return 'today';
    }
    if (days === 1) {
        return 'yesterday';
    }
    return 'earlier';
}

function sessionRow(conv: ChatSession, targetUserId: string | undefined) {
    const isActive = conv.targetUserId === targetUserId;
    const isSystem = conv.targetUserId === SYSTEM_TARGET_USER_ID;
    const hasUnread = conv.unreadCount > 0;

    return (
        <li key={conv.targetUserId}>
            <Link
                to={`/messages/${conv.targetUserId}`}
                className={`message-card ${isActive ? 'is-active' : ''} ${hasUnread ? 'has-unread' : ''}`}
                aria-current={isActive ? 'page' : undefined}
                aria-label={hasUnread ? `${conv.targetUserName}，${conv.unreadCount} 条未读` : conv.targetUserName}
            >
                <span className="message-avatar-wrap">
                    {conv.targetUserAvatar ? (
                        <img
                            src={conv.targetUserAvatar}
                            alt=""
                            className="message-avatar"
                            width="42"
                            height="42"
                            loading="lazy"
                            decoding="async"
                        />
                    ) : isSystem ? (
                        // 系统通知不是「某个人」：首字母圆点会让人以为还有同名用户
                        <span className="message-avatar-fallback" aria-hidden="true">
                            <Bell size={17} />
                        </span>
                    ) : (
                        <span className="message-avatar-fallback" aria-hidden="true">
                            {conv.targetUserName?.charAt(0) ?? '?'}
                        </span>
                    )}
                    {hasUnread && (
                        <span className="message-badge">{conv.unreadCount > 99 ? '99+' : conv.unreadCount}</span>
                    )}
                </span>
                <span className="message-content">
                    <span className="message-header-row">
                        <span className="message-name">{conv.targetUserName}</span>
                        <span className="message-time">{formatRelativeTime(conv.lastMessageTime)}</span>
                    </span>
                    <span className="message-preview">
                        {conv.lastMessage || '暂无消息'}
                        {isSystem && <span className="message-card-tag">只读</span>}
                    </span>
                </span>
            </Link>
        </li>
    );
}

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

    const groupedSessions = useMemo(() => {
        const start = (convPage - 1) * CONVERSATION_PAGE_SIZE;
        const page = sessions.slice(start, start + CONVERSATION_PAGE_SIZE);
        const groups = BUCKETS.map(bucket => ({
            bucket,
            items: page.filter(conv => bucketOf(conv.lastMessageTime) === bucket),
        })).filter(group => group.items.length > 0);
        return groups;
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
                <div className="messages-panel-body">
                    {groupedSessions.map(group => (
                        <section key={group.bucket} aria-label={BUCKET_LABEL[group.bucket]}>
                            <h2 className="messages-group-label">{BUCKET_LABEL[group.bucket]}</h2>
                            <ul className="messages-list">{group.items.map(conv => sessionRow(conv, targetUserId))}</ul>
                        </section>
                    ))}
                </div>
                {totalConversationPages > 1 && (
                    <div className="messages-panel-foot">
                        <PaginationBar
                            pageNum={convPage}
                            totalPages={totalConversationPages}
                            onPageChange={setConvPage}
                        />
                    </div>
                )}
            </>
        );
    })();

    return (
        <div className={`messages-page ${targetUserId ? 'has-thread' : ''}`}>
            <main className="messages-body">
                <section className="messages-panel" aria-label="会话列表">
                    <header className="messages-panel-head">
                        <h1 className="messages-panel-title">消息</h1>
                        <p className="messages-panel-subtitle">{subtitle}</p>
                    </header>
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
            </main>
        </div>
    );
}

export default MessagesPage;
