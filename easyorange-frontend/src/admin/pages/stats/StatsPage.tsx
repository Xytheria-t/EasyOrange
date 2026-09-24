import { BarChart3, Bell, Package, ShoppingCart, Tag, TrendingUp, Users } from 'lucide-react';
import { useMemo } from 'react';
import { ACTIVITY_COLORS, CATEGORY_COLORS, ORDER_SUMMARY_COLORS, STAT_CARD_GRADIENTS } from '../../chartTheme';
import { AdminCard, AdminErrorBanner, AdminPage, AdminPageHeader } from '../../components/AdminPage';
import { useAdminCategories, useAdminOrderStats, useDashboardStats, useRecentActivity, useTrend } from '../../hooks';
import { LazyTrendChart } from './charts/lazyCharts';

/** 三态占位：加载 / 失败 / 空，三者不共用「暂无数据」一句话。 */
function PanelState({ kind, height = 140 }: { kind: 'loading' | 'error' | 'empty'; height?: number }) {
    return (
        <div
            role={kind === 'error' ? 'alert' : 'status'}
            aria-busy={kind === 'loading'}
            style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                height,
                color: 'var(--admin-muted)',
                fontSize: '0.87rem',
            }}
        >
            {kind === 'loading' ? '加载中…' : kind === 'error' ? '加载失败，请刷新重试' : '暂无数据'}
        </div>
    );
}

export default function StatsPage() {
    const { data: stats, isLoading, isError, error, refetch } = useDashboardStats();
    const { data: categories, isLoading: categoriesLoading, isError: categoriesError } = useAdminCategories();
    // 此前三处只取 data，丢掉 loading / error：请求中和请求失败都显示成「没有数据」
    const { data: orderStats, isLoading: orderLoading, isError: orderError } = useAdminOrderStats();
    const { data: trend, isLoading: trendLoading, isError: trendError } = useTrend();
    const { data: recentActivity, isLoading: activityLoading, isError: activityError } = useRecentActivity();

    const statCards = [
        {
            label: '总用户数',
            value: stats?.totalUsers ?? 0,
            gradient: STAT_CARD_GRADIENTS[0],
            Icon: Users,
        },
        {
            label: '总商品数',
            value: stats?.totalProducts ?? 0,
            gradient: STAT_CARD_GRADIENTS[1],
            Icon: Package,
        },
        {
            label: '总订单数',
            value: stats?.totalOrders ?? 0,
            gradient: STAT_CARD_GRADIENTS[2],
            Icon: ShoppingCart,
        },
        {
            label: '今日新增用户',
            value: stats?.todayNewUsers ?? 0,
            gradient: STAT_CARD_GRADIENTS[3],
            Icon: TrendingUp,
        },
    ];

    const categoryDistribution = useMemo(() => {
        if (!categories || categories.length === 0) {
            return [];
        }
        const validCategories = categories.filter(c => (c.productCount ?? 0) > 0);
        if (validCategories.length === 0) {
            return [];
        }
        const maxCount = Math.max(...validCategories.map(c => c.productCount ?? 0));
        const total = validCategories.reduce((sum, c) => sum + (c.productCount ?? 0), 0);
        return [...validCategories]
            .sort((a, b) => (b.productCount ?? 0) - (a.productCount ?? 0))
            .slice(0, CATEGORY_COLORS.length)
            .map(c => ({
                name: c.name,
                count: c.productCount ?? 0,
                pct: total > 0 ? ((c.productCount ?? 0) / total) * 100 : 0,
                ratio: maxCount > 0 ? (c.productCount ?? 0) / maxCount : 0,
            }));
    }, [categories]);

    const orderSummary = [
        {
            label: '今日订单',
            value: orderStats ? `${orderStats.todayOrders} 笔` : null,
            color: ORDER_SUMMARY_COLORS.today,
        },
        { label: '待发货', value: orderStats ? `${orderStats.toShip} 笔` : null, color: ORDER_SUMMARY_COLORS.toShip },
        {
            label: '待收货',
            value: orderStats ? `${orderStats.toReceive} 笔` : null,
            color: ORDER_SUMMARY_COLORS.toReceive,
        },
        {
            label: '已完成',
            value: orderStats ? `${orderStats.completed} 笔` : null,
            color: ORDER_SUMMARY_COLORS.completed,
        },
        {
            label: '今日营收',
            value: orderStats ? `¥${orderStats.todayRevenue.toLocaleString()}` : null,
            color: ORDER_SUMMARY_COLORS.revenue,
        },
    ];

    return (
        <AdminPage>
            <AdminErrorBanner
                message={isError ? error?.message || '统计数据加载失败，请稍后重试' : null}
                onRetry={() => refetch()}
                retrying={isLoading}
            />

            <AdminPageHeader
                icon={<BarChart3 size={17} />}
                title="数据统计"
                description="平台运营数据概览与趋势分析（累计口径，实时读取）"
            />

            <div className="admin-stat-grid">
                {statCards.map(({ label, value, gradient, Icon }) => (
                    <AdminCard key={label}>
                        <div
                            aria-hidden="true"
                            style={{
                                position: 'absolute',
                                top: '-12px',
                                right: '-12px',
                                width: 64,
                                height: 64,
                                borderRadius: '50%',
                                background: gradient,
                                opacity: 0.08,
                            }}
                        />
                        <div
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                marginBottom: '0.75rem',
                                gap: '0.5rem',
                            }}
                        >
                            <span className="admin-label">{label}</span>
                            <Icon size={17} aria-hidden="true" style={{ color: 'var(--admin-faint)' }} />
                        </div>
                        {/* 口径：累计值 vs 今日值在标签里写清楚 */}
                        <span className="admin-accent-number">
                            {isLoading || isError ? '—' : value.toLocaleString()}
                        </span>
                    </AdminCard>
                ))}
            </div>

            <AdminCard>
                <div className="admin-toolbar" style={{ padding: '0.9rem 1.15rem' }}>
                    <strong className="admin-label" style={{ alignSelf: 'center' }}>
                        订单摘要
                    </strong>
                    {orderError ? (
                        <span className="admin-muted">订单数据加载失败，刷新页面重试</span>
                    ) : orderLoading ? (
                        <span className="admin-muted">加载中…</span>
                    ) : (
                        orderSummary.map(
                            item =>
                                item.value && (
                                    <span
                                        key={item.label}
                                        style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
                                    >
                                        <span className="admin-status-dot" style={{ background: item.color }} />
                                        <span className="admin-muted" style={{ fontSize: '0.78rem' }}>
                                            {item.label}
                                        </span>
                                        <span
                                            style={{ fontSize: '0.84rem', fontWeight: 700, color: 'var(--admin-ink)' }}
                                        >
                                            {item.value}
                                        </span>
                                    </span>
                                )
                        )
                    )}
                </div>
            </AdminCard>

            <div className="admin-split">
                <AdminCard>
                    <h2 className="admin-section-title" style={{ marginBottom: '0.35rem' }}>
                        月度趋势
                    </h2>
                    <p className="admin-muted" style={{ marginBottom: '1rem' }}>
                        单位：条 / 笔，近 6 个月
                    </p>
                    {trendError ? (
                        <PanelState kind="error" height={280} />
                    ) : (
                        <LazyTrendChart data={trend ?? []} isCompact={false} height={280} />
                    )}
                    {trendLoading ? <PanelState kind="loading" height={0} /> : null}
                </AdminCard>

                <AdminCard>
                    <h2
                        className="admin-section-title"
                        style={{ marginBottom: '0.35rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
                    >
                        <Tag size={15} aria-hidden="true" style={{ color: 'var(--admin-faint)' }} />
                        商品分类分布
                    </h2>
                    <p className="admin-muted" style={{ marginBottom: '1rem' }}>
                        按在架商品数排序，取前 6 个分类
                    </p>
                    {categoriesLoading ? (
                        <PanelState kind="loading" />
                    ) : categoriesError ? (
                        <PanelState kind="error" />
                    ) : categoryDistribution.length === 0 ? (
                        <PanelState kind="empty" />
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
                            {categoryDistribution.map((cat, idx) => (
                                <div key={cat.name}>
                                    <div
                                        style={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            marginBottom: '0.3rem',
                                        }}
                                    >
                                        <span
                                            style={{
                                                fontSize: '0.84rem',
                                                fontWeight: 600,
                                                color: 'var(--admin-ink-soft)',
                                            }}
                                        >
                                            {cat.name}
                                        </span>
                                        <span className="admin-muted">
                                            {cat.count} 件 ({Math.round(cat.pct)}%)
                                        </span>
                                    </div>
                                    <div
                                        style={{
                                            height: 8,
                                            borderRadius: 4,
                                            background:
                                                'color-mix(in srgb, var(--admin-control-line) 30%, transparent)',
                                            overflow: 'hidden',
                                        }}
                                    >
                                        <div
                                            style={{
                                                height: '100%',
                                                borderRadius: 4,
                                                background: CATEGORY_COLORS[idx % CATEGORY_COLORS.length],
                                                width: `${Math.max(cat.ratio * 100, 4)}%`,
                                                transition: 'width 0.6s var(--ease-out)',
                                            }}
                                        />
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </AdminCard>
            </div>

            <AdminCard>
                <h2
                    className="admin-section-title"
                    style={{ marginBottom: '0.35rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
                >
                    <Bell size={15} aria-hidden="true" style={{ color: 'var(--admin-faint)' }} />
                    最近动态
                </h2>
                <p className="admin-muted" style={{ marginBottom: '0.75rem' }}>
                    用户、商品、订单的最新变更
                </p>
                {activityLoading ? (
                    <PanelState kind="loading" height={100} />
                ) : activityError ? (
                    <PanelState kind="error" height={100} />
                ) : !recentActivity?.length ? (
                    <PanelState kind="empty" height={100} />
                ) : (
                    <div>
                        {recentActivity.map((activity, idx) => (
                            <div
                                // 动态列表会往头部插入新项，下标做 key 会让刷新后复用错位的 DOM
                                key={`${activity.time}-${activity.type}-${activity.text}`}
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '0.85rem',
                                    padding: '0.85rem 0',
                                    borderBottom:
                                        idx < recentActivity.length - 1 ? '1px solid var(--admin-line-soft)' : 'none',
                                }}
                            >
                                <span
                                    className="admin-status-dot"
                                    style={{ background: ACTIVITY_COLORS[activity.type] ?? 'var(--admin-faint)' }}
                                />
                                <span style={{ flex: 1, fontSize: '0.87rem', color: 'var(--admin-ink-soft)' }}>
                                    {activity.text}
                                </span>
                                <span
                                    className="admin-muted"
                                    style={{ fontSize: '0.78rem', flexShrink: 0, whiteSpace: 'nowrap' }}
                                >
                                    {activity.time}
                                </span>
                            </div>
                        ))}
                    </div>
                )}
            </AdminCard>
        </AdminPage>
    );
}
