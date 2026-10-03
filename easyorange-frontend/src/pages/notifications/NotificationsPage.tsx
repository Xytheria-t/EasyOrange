import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, ArrowRight, Bell, CheckCheck, Loader2, Package, ShoppingCart, XCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { notificationApi } from '@/api/notificationApi';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/StateDisplay';
import { PaginationBar } from '@/components/PaginationBar';
import { Button } from '@/components/ui/button';
import { usePagination } from '@/hooks/usePagination';
import type { NotificationItem } from '@/types';
import { formatRelativeTime } from '@/utils';
import './notifications.css';

const PAGE_SIZE = 20;

/** 后端 MessageBizType.code —— 决定图标与点击跳转，不再靠标题中文猜 */
const BIZ_TYPE = { NONE: 0, PRODUCT: 1, ORDER: 2 } as const;

function getNotificationIcon(bizType: number, title: string) {
    if (bizType === BIZ_TYPE.ORDER) {
        return { icon: ShoppingCart, color: '#6366F1' };
    }
    if (bizType === BIZ_TYPE.PRODUCT) {
        // 审核结论只在标题里，且同属商品事件，用形状区分成败比再加一个 bizType 更划算
        return title.includes('审核未通过') || title.includes('驳回')
            ? { icon: XCircle, color: '#EF4444' }
            : { icon: Package, color: '#F97316' };
    }
    return { icon: Bell, color: '#8B5CF6' };
}

/**
 * 点击落点 —— businessId 指向哪张页面由 bizType 决定。
 * 返回 null 表示没有可跳转的业务对象（点一下只标已读，不给用户一个必然 404 的地址）。
 */
function resolveTarget(item: NotificationItem): string | null {
    if (!item.businessId) {
        return null;
    }
    if (item.bizType === BIZ_TYPE.PRODUCT) {
        return `/products/${item.businessId}`;
    }
    if (item.bizType === BIZ_TYPE.ORDER) {
        return `/orders/${item.businessId}`;
    }
    return null;
}

export default function NotificationsPage() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const { pageNum: page, goTo } = usePagination();

    // 全局未读数：与顶栏铃铛同一个 query key，读缓存不额外发请求。
    // 此前按当前页过滤，第一页恰好全已读而后面还有未读时，「全部已读」按钮会凭空消失
    const { data: unreadCountData } = useQuery({
        queryKey: ['unread-count'],
        queryFn: async () => {
            const response = await notificationApi.getUnreadCount();
            return response.data;
        },
        staleTime: 15 * 1000,
    });

    const { data, isLoading, error } = useQuery({
        queryKey: ['notifications', page],
        queryFn: async () => {
            const response = await notificationApi.getNotifications(page, PAGE_SIZE);
            return response.data;
        },
    });

    const markAsReadMutation = useMutation({
        mutationFn: (id: string) => notificationApi.markAsRead(id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['notifications'] });
            queryClient.invalidateQueries({ queryKey: ['unread-count'] });
        },
    });

    const markAllReadMutation = useMutation({
        mutationFn: () => notificationApi.markAllSystemAsRead(),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['notifications'] });
            queryClient.invalidateQueries({ queryKey: ['unread-count'] });
        },
    });

    const handleNotificationClick = (item: NotificationItem) => {
        if (item.isRead === 0) {
            markAsReadMutation.mutate(item.id);
        }
        const target = resolveTarget(item);
        if (target) {
            navigate(target);
        }
    };

    const notifications = data?.records ?? [];
    const totalPages = data?.pages ?? 1;
    const systemUnread = unreadCountData?.systemCount ?? 0;

    return (
        <div className="notifications-page">
            <div className="notifications-ambient">
                <div className="notifications-orb notifications-orb-1" />
                <div className="notifications-orb notifications-orb-2" />
                <div className="notifications-orb notifications-orb-3" />
            </div>

            <div className="notifications-container">
                {/* Header */}
                <div className="notifications-header">
                    <div className="notifications-header-left">
                        <Button
                            variant="ghost"
                            size="icon"
                            className="notifications-back-btn"
                            onClick={() => navigate(-1)}
                            aria-label="返回"
                        >
                            <ArrowLeft size={20} />
                        </Button>
                        <div className="notifications-header-info">
                            <h1 className="notifications-title">系统通知</h1>
                            <div className="notifications-header-meta">
                                <span className="notifications-kicker">
                                    <span className="kicker-dot" />
                                    通知中心
                                </span>
                                <span className="notifications-subtitle">审核结果等系统消息</span>
                            </div>
                        </div>
                    </div>
                    {systemUnread > 0 && (
                        <Button
                            className="notifications-mark-all-btn"
                            onClick={() => markAllReadMutation.mutate()}
                            disabled={markAllReadMutation.isPending}
                        >
                            {markAllReadMutation.isPending ? (
                                <Loader2 size={16} className="animate-spin" />
                            ) : (
                                <CheckCheck size={16} />
                            )}
                            全部已读
                            {systemUnread > 0 && <span className="notifications-mark-all-count">{systemUnread}</span>}
                        </Button>
                    )}
                </div>

                <div className="notifications-divider" />

                {/* Content */}
                {isLoading ? (
                    <LoadingState label="正在加载通知" />
                ) : error ? (
                    // 失败必须与空态分开：显示「暂无通知」会被当成本来就没有
                    <ErrorState
                        title="通知加载失败"
                        description="网络或服务暂时不可用，请稍后重试。"
                        onRetry={() => queryClient.invalidateQueries({ queryKey: ['notifications'] })}
                    />
                ) : notifications.length === 0 ? (
                    <EmptyState
                        icon={Bell}
                        title="暂无系统通知"
                        description="商品审核结果、订单状态等系统消息会出现在这里。"
                    />
                ) : (
                    <>
                        <ul className="notifications-list">
                            {notifications.map(item => {
                                const { icon: Icon, color } = getNotificationIcon(item.bizType, item.title);
                                const isUnread = item.isRead === 0;
                                const target = resolveTarget(item);
                                return (
                                    <li key={item.id} className={`notification-card ${isUnread ? 'unread' : ''}`}>
                                        {isUnread && <span className="notification-card-accent" />}
                                        <div
                                            className="notification-card-icon"
                                            style={{
                                                background: `linear-gradient(135deg, ${color}18, ${color}08)`,
                                                borderColor: `${color}15`,
                                                color,
                                            }}
                                        >
                                            <Icon size={18} />
                                        </div>
                                        <div className="notification-card-body">
                                            <div className="notification-card-header">
                                                <h3 className="notification-card-title">
                                                    {item.title}
                                                    {isUnread && <span className="notification-unread-dot" />}
                                                </h3>
                                                <span className="notification-card-time">
                                                    {formatRelativeTime(item.createTime)}
                                                </span>
                                            </div>
                                            <p className="notification-card-content">{item.content}</p>
                                            {target && (
                                                <span className="notification-card-link">
                                                    查看详情
                                                    <ArrowRight size={11} />
                                                </span>
                                            )}
                                        </div>
                                        {/* 覆盖整卡的按钮：键盘可达，标题语义留给 h3 */}
                                        <button
                                            type="button"
                                            className="notification-card-overlay"
                                            onClick={() => handleNotificationClick(item)}
                                            aria-label={`${item.title}${isUnread ? '，未读' : ''}`}
                                        />
                                    </li>
                                );
                            })}
                        </ul>

                        {/* Pagination */}
                        <PaginationBar pageNum={page} totalPages={totalPages} onPageChange={goTo} />
                    </>
                )}
            </div>
        </div>
    );
}
