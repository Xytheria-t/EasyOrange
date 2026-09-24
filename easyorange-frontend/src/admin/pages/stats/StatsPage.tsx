import { BarChart3, Bell, Package, ShoppingCart, Tag, TrendingUp, Users } from 'lucide-react';
import { useMemo } from 'react';
import { AdminCard, AdminErrorBanner, AdminPage, AdminPageHeader } from '../../components/AdminPage';
import { accentNumberText, labelText, mutedText, sectionTitleText, statusDot } from '../../components/admin-theme';
import { useAdminCategories, useAdminOrderStats, useDashboardStats, useRecentActivity, useTrend } from '../../hooks';
import { LazyTrendChart } from './charts/lazyCharts';

const CATEGORY_COLORS = ['#F97316', '#FB7185', '#C39BD3', '#FBBF24', '#10B981', '#6E6862'];

const ACTIVITY_COLORS: Record<string, string> = {
    user: '#F97316',
    product: '#C39BD3',
    order: '#10B981',
};

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
            gradient: 'linear-gradient(135deg, #F97316, #FB923C)',
            Icon: Users,
        },
        {
            label: '总商品数',
            value: stats?.totalProducts ?? 0,
            gradient: 'linear-gradient(135deg, #C39BD3, #D8B4FE)',
            Icon: Package,
        },
        {
            label: '总订单数',
            value: stats?.totalOrders ?? 0,
            gradient: 'linear-gradient(135deg, #10B981, #34D399)',
            Icon: ShoppingCart,
        },
        {
            label: '今日新增用户',
            value: stats?.todayNewUsers ?? 0,
            gradient: 'linear-gradient(135deg, #FBBF24, #F97316)',
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
        { label: '今日订单', value: orderStats ? `${orderStats.todayOrders} 笔` : null, color: '#F97316' },
        { label: '待发货', value: orderStats ? `${orderStats.toShip} 笔` : null, color: '#FBBF24' },
        { label: '待收货', value: orderStats ? `${orderStats.toReceive} 笔` : null, color: '#10B981' },
        { label: '已完成', value: orderStats ? `${orderStats.completed} 笔` : null, color: '#C39BD3' },
        {
            label: '今日营收',
            value: orderStats ? `¥${orderStats.todayRevenue.toLocaleString()}` : null,
            color: '#FB7185',
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
                            <span style={labelText}>{label}</span>
                            <Icon size={17} aria-hidden="true" style={{ color: 'var(--admin-faint)' }} />
                        </div>
                        {/* 口径：累计值 vs 今日值在标签里写清楚 */}
                        <span style={accentNumberText}>{isLoading || isError ? '—' : value.toLocaleString()}</span>
                    </AdminCard>
                ))}
            </div>

            <AdminCard>
                <div className="admin-toolbar" style={{ padding: '0.9rem 1.15rem' }}>
                    <strong style={{ ...labelText, alignSelf: 'center' }}>订单摘要</strong>
                    {orderError ? (
                        <span style={mutedText}>订单数据加载失败，刷新页面重试</span>
                    ) : orderLoading ? (
                        <span style={mutedText}>加载中…</span>
                    ) : (
                        orderSummary.map(
                            item =>
                                item.value && (
                                    <span
                                        key={item.label}
                                        style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
                                    >
                                        <span style={statusDot(item.color)} />
                                        <span style={{ ...mutedText, fontSize: '0.78rem' }}>{item.label}</span>
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
                    <h2 style={{ ...sectionTitleText, marginBottom: '0.35rem' }}>月度趋势</h2>
                    <p style={{ ...mutedText, marginBottom: '1rem' }}>单位：条 / 笔，近 6 个月</p>
                    {trendError ? (
                        <PanelState kind="error" height={280} />
                    ) : (
                        <LazyTrendChart data={trend ?? []} isCompact={false} height={280} />
                    )}
                    {trendLoading ? <PanelState kind="loading" height={0} /> : null}
                </AdminCard>

                <AdminCard>
                    <h2
                        style={{
                            ...sectionTitleText,
                            marginBottom: '0.35rem',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '0.5rem',
                        }}
                    >
                        <Tag size={15} aria-hidden="true" style={{ color: 'var(--admin-faint)' }} />
                        商品分类分布
                    </h2>
                    <p style={{ ...mutedText, marginBottom: '1rem' }}>按在架商品数排序，取前 6 个分类</p>
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
                                        <span style={mutedText}>
                                            {cat.count} 件 ({Math.round(cat.pct)}%)
                                        </span>
                                    </div>
                                    <div
                                        style={{
                                            height: 8,
                                            borderRadius: 4,
                                            background: 'rgba(229,224,219,0.3)',
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
                    style={{
                        ...sectionTitleText,
                        marginBottom: '0.35rem',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.5rem',
                    }}
                >
                    <Bell size={15} aria-hidden="true" style={{ color: 'var(--admin-faint)' }} />
                    最近动态
                </h2>
                <p style={{ ...mutedText, marginBottom: '0.75rem' }}>用户、商品、订单的最新变更</p>
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
                                // biome-ignore lint/suspicious/noArrayIndexKey: stable list
                                key={idx}
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '0.85rem',
                                    padding: '0.85rem 0',
                                    borderBottom:
                                        idx < recentActivity.length - 1 ? '1px solid var(--admin-line-soft)' : 'none',
                                }}
                            >
                                <span style={statusDot(ACTIVITY_COLORS[activity.type] ?? '#9B9590')} />
                                <span style={{ flex: 1, fontSize: '0.87rem', color: 'var(--admin-ink-soft)' }}>
                                    {activity.text}
                                </span>
                                <span
                                    style={{ ...mutedText, fontSize: '0.78rem', flexShrink: 0, whiteSpace: 'nowrap' }}
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
