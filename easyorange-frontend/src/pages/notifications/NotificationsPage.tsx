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

/** 图标配色交给 CSS 的 [data-biz]，色值统一在 tokens 里 */
type BizKey = 'order' | 'product' | 'reject' | 'system';

function getNotificationIcon(bizType: number, title: string): { icon: typeof Bell; biz: BizKey } {
    if (bizType === BIZ_TYPE.ORDER) {
        return { icon: ShoppingCart, biz: 'order' };
    }
    if (bizType === BIZ_TYPE.PRODUCT) {
        // 审核结论只在标题里，且同属商品事件，用形状区分成败比再加一个 bizType 更划算
        return title.includes('审核未通过') || title.includes('驳回')
            ? { icon: XCircle, biz: 'reject' }
            : { icon: Package, biz: 'product' };
    }
    return { icon: Bell, biz: 'system' };
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
            <div className="notifications-container">
                <header className="notifications-header">
                    <div className="notifications-header-left">
                        <Button
                            variant="ghost"
                            size="icon"
                            className="notifications-back-btn"
                            onClick={() => navigate(-1)}
                            aria-label="返回"
                        >
                            <ArrowLeft size={19} />
                        </Button>
                        <div>
                            <h1 className="notifications-title">系统通知</h1>
                            <p className="notifications-subtitle">
                                <span>通知中心</span>
                                <span className="notifications-subtitle-tail">审核结果与订单状态</span>
                            </p>
                        </div>
                    </div>

                    {systemUnread > 0 && (
                        <Button
                            variant="outline"
                            size="sm"
                            className="notifications-mark-all-btn"
                            onClick={() => markAllReadMutation.mutate()}
                            disabled={markAllReadMutation.isPending}
                        >
                            {markAllReadMutation.isPending ? (
                                <Loader2 size={15} className="animate-spin" />
                            ) : (
                                <CheckCheck size={15} />
                            )}
                            全部已读
                            <span className="notifications-mark-all-count">{systemUnread}</span>
                        </Button>
                    )}
                </header>

                <div className="notifications-card">
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
                        <ul className="notifications-list">
                            {notifications.map(item => {
                                const { icon: Icon, biz } = getNotificationIcon(item.bizType, item.title);
                                const isUnread = item.isRead === 0;
                                const target = resolveTarget(item);
                                return (
                                    <li key={item.id} className={`notification-row ${isUnread ? 'unread' : ''}`}>
                                        <span className="notification-icon" data-biz={biz} aria-hidden="true">
                                            <Icon size={17} />
                                        </span>
                                        <div className="notification-body">
                                            <div className="notification-row-head">
                                                <h3 className="notification-title">
                                                    {item.title}
                                                    {isUnread && <span className="notification-unread-dot" />}
                                                </h3>
                                                <span className="notification-time">
                                                    {formatRelativeTime(item.createTime)}
                                                </span>
                                            </div>
                                            <p className="notification-content">{item.content}</p>
                                            {target && (
                                                <span className="notification-link">
                                                    查看详情
                                                    <ArrowRight size={11} />
                                                </span>
                                            )}
                                        </div>
                                        {/* 覆盖整行的按钮：键盘可达，标题语义留给 h3 */}
                                        <button
                                            type="button"
                                            className="notification-row-overlay"
                                            onClick={() => handleNotificationClick(item)}
                                            aria-label={`${item.title}${isUnread ? '，未读' : ''}`}
                                        />
                                    </li>
                                );
                            })}
                        </ul>
                    )}
                </div>

                {totalPages > 1 && !isLoading && !error && notifications.length > 0 && (
                    <div className="notifications-foot">
                        <PaginationBar pageNum={page} totalPages={totalPages} onPageChange={goTo} />
                    </div>
                )}
            </div>
        </div>
    );
}
