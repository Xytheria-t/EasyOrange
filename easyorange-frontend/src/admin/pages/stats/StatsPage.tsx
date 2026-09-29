import { BarChart3, Bell, Package, RefreshCw, ShoppingCart, Tag, TrendingUp, UserPlus, Users } from 'lucide-react';
import { type CSSProperties, useMemo } from 'react';
import { Button } from '@/components/ui/button';
import { ACTIVITY_COLORS, CATEGORY_RANKS, ORDER_SUMMARY_COLORS, RANK_VISIBLE_COUNT } from '../../chartTheme';
import { AdminCard, AdminCardHead, AdminErrorBanner, AdminPage, AdminPageHeader } from '../../components/AdminPage';
import { useAdminCategories, useAdminOrderStats, useDashboardStats, useRecentActivity, useTrend } from '../../hooks';
import type { ActivityItem, TrendItem } from '../../types/admin';
import { LazyTrendChart } from './charts/lazyCharts';
import { Sparkline } from './charts/Sparkline';

/** 三态占位：加载 / 失败 / 空，三者不共用「暂无数据」一句话。 */
function PanelState({
    kind,
    height = 140,
    message,
}: {
    kind: 'loading' | 'error' | 'empty';
    height?: number;
    message?: string;
}) {
    return (
        <div
            role={kind === 'error' ? 'alert' : 'status'}
            aria-busy={kind === 'loading'}
            className={`admin-panel-state${kind === 'error' ? ' admin-panel-state--error' : ''}`}
            style={{ height }}
        >
            {message ?? (kind === 'loading' ? '加载中…' : kind === 'error' ? '加载失败，请刷新重试' : '暂无数据')}
        </div>
    );
}

const ACTIVITY_ICONS = {
    user: UserPlus,
    product: Package,
    order: ShoppingCart,
} as const;

const EMPTY_DISTRIBUTION = { rows: [], total: 0, categoryCount: 0 };

export default function StatsPage() {
    const { data: stats, isLoading, isError, error, refetch } = useDashboardStats();
    const { data: categories, isLoading: categoriesLoading, isError: categoriesError } = useAdminCategories();
    // 此前三处只取 data，丢掉 loading / error：请求中和请求失败都显示成「没有数据」
    const { data: orderStats, isLoading: orderLoading, isError: orderError } = useAdminOrderStats();
    const { data: trend, isLoading: trendLoading, isError: trendError } = useTrend();
    const { data: recentActivity, isLoading: activityLoading, isError: activityError } = useRecentActivity();

    const sparkSeries = useMemo(() => {
        const points: TrendItem[] = trend ?? [];
        return {
            users: points.map(p => p.users),
            products: points.map(p => p.products),
            orders: points.map(p => p.orders),
        };
    }, [trend]);

    /**
     * 累计值与当日值并排放在同一张卡里。
     * 接口早就返回了待审核商品数、今日订单数、累计营收，这三个字段此前一个都没上屏——
     * 单看「总商品数 500」不知道该做什么动作，看「待审核 10」才知道。副指标不是装饰，
     * 它是这一屏唯一指向下一步操作的信息。
     */
    const statCards = [
        {
            label: '总用户数',
            value: stats?.totalUsers,
            Icon: Users,
            accent: 'var(--admin-chart-1)',
            foot: stats ? `今日新增 ${stats.todayNewUsers} 人` : '累计注册',
            spark: sparkSeries.users,
        },
        {
            label: '总商品数',
            value: stats?.totalProducts,
            Icon: Package,
            accent: 'var(--admin-chart-3)',
            foot: stats ? `待审核 ${stats.pendingProducts} 件` : '累计在架',
            spark: sparkSeries.products,
        },
        {
            label: '总订单数',
            value: stats?.totalOrders,
            Icon: ShoppingCart,
            accent: 'var(--admin-chart-5)',
            foot: stats ? `今日 ${stats.todayOrders} 笔` : '累计成交',
            spark: sparkSeries.orders,
        },
        {
            label: '今日新增用户',
            value: stats?.todayNewUsers,
            Icon: TrendingUp,
            accent: 'var(--admin-chart-2)',
            foot: stats ? `占总用户 ${newUserShare(stats.todayNewUsers, stats.totalUsers)}` : '当日注册',
            // 走势线的口径是「每月新增」，标题是「今日」，两个粒度。
            // 试过干脆不给线：四张并排的卡里空出一块，看着像渲染坏了，比留着更糟。
            // 折中是把粒度写进标签（近 N 月新增），让读者自己分得清这两行数在讲什么。
            spark: sparkSeries.users,
        },
    ];

    const distribution = useMemo(() => {
        if (!categories || categories.length === 0) {
            return EMPTY_DISTRIBUTION;
        }
        // 只取一级分类：后端一级计数已把子分类归并上来（每个商品只计一次），
        // 二级计数是叶子直挂数——两层混排会把同一件商品算两遍，
        // 分母虚大一倍（线上 106 变 212），所有占比正好减半，还会把
        // 叶子分类排进「前 6」挤掉真实的一级分类。
        const level1 = categories.filter(c => c.level === 1 && (c.productCount ?? 0) > 0);
        if (level1.length === 0) {
            return EMPTY_DISTRIBUTION;
        }
        const sorted = [...level1].sort((a, b) => (b.productCount ?? 0) - (a.productCount ?? 0));
        const total = sorted.reduce((sum, c) => sum + (c.productCount ?? 0), 0);
        const maxCount = sorted[0]?.productCount ?? 0;
        // 超出展示上限的分类合并成一行「其他」：只列前 6 却按全部一级分类算占比，
        // 六个百分数加起来对不上 100%，读者会以为数据错了
        const rest = sorted.slice(RANK_VISIBLE_COUNT);
        const rows = sorted.slice(0, RANK_VISIBLE_COUNT).map((c, idx) => ({
            key: c.id,
            name: c.name,
            count: c.productCount ?? 0,
            pct: total > 0 ? ((c.productCount ?? 0) / total) * 100 : 0,
            ratio: maxCount > 0 ? (c.productCount ?? 0) / maxCount : 0,
            color: CATEGORY_RANKS[idx],
        }));
        if (rest.length > 0) {
            const restCount = rest.reduce((sum, c) => sum + (c.productCount ?? 0), 0);
            rows.push({
                key: '__rest__',
                name: `其他 ${rest.length} 个分类`,
                count: restCount,
                pct: total > 0 ? (restCount / total) * 100 : 0,
                ratio: maxCount > 0 ? restCount / maxCount : 0,
                color: CATEGORY_RANKS[CATEGORY_RANKS.length - 1],
            });
        }
        return { rows, total, categoryCount: level1.length };
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
            money: true,
        },
    ];

    const hasStats = !isLoading && !isError;

    return (
        <AdminPage>
            <AdminErrorBanner
                message={isError ? error?.message || '统计数据加载失败，请稍后重试' : null}
                onRetry={() => refetch()}
                retrying={isLoading}
            />

            <AdminPageHeader
                icon={<BarChart3 size={18} />}
                title="数据统计"
                description="平台运营数据概览与趋势分析（累计口径，实时读取）"
                actions={
                    <Button variant="outline" size="sm" onClick={() => refetch()} disabled={isLoading}>
                        <RefreshCw size={14} aria-hidden="true" />
                        {isLoading ? '刷新中' : '刷新'}
                    </Button>
                }
            />

            <div className="admin-kpi-grid">
                {statCards.map(({ label, value, Icon, accent, foot, spark }) => {
                    const showSpark = hasStats && spark !== null && spark.length > 1;
                    return (
                        <AdminCard
                            key={label}
                            className="admin-kpi"
                            // 顶部色条按指标给：走 CSS 变量，让颜色定义留在 admin.css
                            style={{ '--kpi-accent': accent } as CSSProperties}
                        >
                            <div className="admin-kpi-head">
                                <span className="admin-kpi-label">{label}</span>
                                <span className="admin-kpi-icon" aria-hidden="true">
                                    <Icon size={15} />
                                </span>
                            </div>
                            <span className="admin-kpi-value">
                                {hasStats && value !== undefined ? value.toLocaleString() : '—'}
                            </span>
                            {/* 走势线单独一行时读者不知道它是什么，右上角给一句口径：
                                「近 7 月新增」比一条无名的斜线多花几个字，少一次误读。 */}
                            <span className="admin-kpi-foot">
                                <span>{hasStats ? foot : '—'}</span>
                                {showSpark ? <span className="admin-kpi-trend">近 {spark.length} 月新增</span> : null}
                            </span>
                            {showSpark ? <Sparkline values={spark} color={accent} /> : null}
                        </AdminCard>
                    );
                })}
            </div>

            <AdminCard>
                <AdminCardHead title="订单摘要" note="当日成交与在途单据；营收只计今日，不含历史累计" />
                {/* 指标条满幅铺到卡片边缘：它自己带一圈描边，再缩进卡片里就成了
                    「卡片里又搁了张卡片」。满幅后靠卡片外框收边，层级少一层。 */}
                <div className={orderError || orderLoading || !orderStats ? 'admin-card-pad' : 'admin-card-flush'}>
                    {orderError ? (
                        <PanelState kind="error" height={72} message="订单数据加载失败，刷新页面重试" />
                    ) : orderLoading ? (
                        <PanelState kind="loading" height={72} />
                    ) : !orderStats ? (
                        <PanelState kind="empty" height={72} />
                    ) : (
                        <div className="admin-metric-strip admin-metric-strip--flush">
                            {orderSummary.map(
                                item =>
                                    item.value && (
                                        <div className="admin-metric" key={item.label}>
                                            <span className="admin-metric-label">
                                                <span className="admin-status-dot" style={{ background: item.color }} />
                                                {item.label}
                                            </span>
                                            <span
                                                className={`admin-metric-value${item.money ? ' admin-metric-value--money' : ''}`}
                                            >
                                                {item.value}
                                            </span>
                                        </div>
                                    )
                            )}
                        </div>
                    )}
                </div>
            </AdminCard>

            <div className="admin-split">
                <AdminCard>
                    <AdminCardHead
                        title="月度趋势"
                        icon={<BarChart3 size={15} />}
                        note={
                            trend && trend.length > 0
                                ? `单位：条 / 笔，近 ${trend.length} 个月。三项量级不同，分面各用各的纵轴`
                                : '单位：条 / 笔'
                        }
                    />
                    <div className="admin-card-pad">
                        {trendLoading ? (
                            <PanelState kind="loading" height={240} />
                        ) : trendError ? (
                            <PanelState kind="error" height={240} />
                        ) : (
                            <LazyTrendChart data={trend ?? []} height={104} />
                        )}
                    </div>
                </AdminCard>

                <AdminCard>
                    <AdminCardHead
                        title="商品分类分布"
                        icon={<Tag size={15} />}
                        note={
                            distribution.categoryCount > 0
                                ? `一级分类的在架商品数（含子分类），共 ${distribution.categoryCount} 个`
                                : '一级分类的在架商品数（含子分类）'
                        }
                    />
                    <div className="admin-card-pad">
                        {categoriesLoading ? (
                            <PanelState kind="loading" height={240} />
                        ) : categoriesError ? (
                            <PanelState kind="error" height={240} />
                        ) : distribution.rows.length === 0 ? (
                            <PanelState kind="empty" height={240} />
                        ) : (
                            <>
                                <div className="admin-rank-list">
                                    {distribution.rows.map((cat, idx) => (
                                        <div
                                            className="admin-rank-row"
                                            key={cat.key}
                                            style={{ '--rank-color': cat.color } as CSSProperties}
                                        >
                                            <span className="admin-rank-index">{idx + 1}</span>
                                            <span className="admin-rank-name">{cat.name}</span>
                                            <span className="admin-rank-count">
                                                {cat.count} 件 ({Math.round(cat.pct)}%)
                                            </span>
                                            <span className="admin-rank-track">
                                                <span
                                                    className="admin-rank-fill"
                                                    style={{ width: `${Math.max(cat.ratio * 100, 3)}%` }}
                                                />
                                            </span>
                                        </div>
                                    ))}
                                </div>
                                <p className="admin-muted admin-rank-total">合计 {distribution.total} 件</p>
                            </>
                        )}
                    </div>
                </AdminCard>
            </div>

            <AdminCard>
                <AdminCardHead title="最近动态" icon={<Bell size={15} />} note="用户、商品、订单的最新变更" />
                <div className="admin-card-pad">
                    {activityLoading ? (
                        <PanelState kind="loading" height={100} />
                    ) : activityError ? (
                        <PanelState kind="error" height={100} />
                    ) : !recentActivity?.length ? (
                        <PanelState kind="empty" height={100} />
                    ) : (
                        <div className="admin-feed">
                            {recentActivity.map(activity => (
                                <ActivityRow key={activityKey(activity)} activity={activity} />
                            ))}
                        </div>
                    )}
                </div>
            </AdminCard>
        </AdminPage>
    );
}

function newUserShare(today: number, total: number) {
    return `${total > 0 ? ((today / total) * 100).toFixed(1) : '0.0'}%`;
}

/** 动态列表会往头部插入新项，用下标做 key 会让刷新后复用错位的 DOM。 */
function activityKey(activity: ActivityItem) {
    return `${activity.time}-${activity.type}-${activity.text}`;
}

function ActivityRow({ activity }: { activity: ActivityItem }) {
    const Icon = ACTIVITY_ICONS[activity.type] ?? Bell;
    return (
        <div className="admin-feed-item" style={{ '--feed-color': ACTIVITY_COLORS[activity.type] } as CSSProperties}>
            <span className="admin-feed-icon" aria-hidden="true">
                <Icon size={15} />
            </span>
            <span className="admin-feed-text">{activity.text}</span>
            <span className="admin-feed-time">{activity.time}</span>
        </div>
    );
}
