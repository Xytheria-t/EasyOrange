import { useQueryClient } from '@tanstack/react-query';
import { Package, RefreshCw, ShoppingCart, TrendingUp, UserPlus } from 'lucide-react';
import type { CSSProperties, ReactNode } from 'react';
import { useMemo } from 'react';
import { Button } from '@/components/ui/button';
import { formatRelativeTime } from '@/utils/format';
import { ACTIVITY_COLORS, CATEGORY_RANKS, RANK_VISIBLE_COUNT, TREND_SERIES } from '../../chartTheme';
import { AdminCard, AdminCardHead, AdminErrorBanner, AdminPage, AdminPageHeader } from '../../components/AdminPage';
import { useAdminCategories, useAdminOrderStats, useDashboardStats, useRecentActivity, useTrend } from '../../hooks';
import type { ActivityItem, TrendItem } from '../../types/admin';
import { Sparkline } from './Sparkline';

/** 面板三态：加载 / 失败 / 空，三者不共用一句「暂无数据」。 */
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

function formatMoney(value: number | null | undefined): string {
    if (value == null) {
        return '—';
    }
    return `¥${value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatCount(value: number | null | undefined): string {
    return value == null ? '—' : value.toLocaleString('zh-CN');
}

/** 环比：与上一期比。首期没有上一期，返回 null（不画箭头，也不写「持平」冒充变化）。 */
function trendDelta(values: number[]): { text: string; kind: 'up' | 'down' | 'flat' } | null {
    if (values.length < 2) {
        return null;
    }
    const previous = values[values.length - 2];
    const latest = values[values.length - 1];
    if (previous === 0) {
        return { text: latest > 0 ? '新增' : '持平', kind: 'flat' };
    }
    const pct = ((latest - previous) / previous) * 100;
    const rounded = Math.round(Math.abs(pct));
    if (rounded === 0) {
        return { text: '持平', kind: 'flat' };
    }
    return {
        text: `${pct > 0 ? '+' : '-'}${rounded}%`,
        kind: pct > 0 ? 'up' : 'down',
    };
}

const ACTIVITY_ICONS = {
    user: UserPlus,
    product: Package,
    order: ShoppingCart,
} as const;

const ORDER_STATUS_METRICS = [
    { key: 'pendingPayment', label: '待付款', color: 'var(--admin-faint)' },
    { key: 'toShip', label: '待发货', color: 'var(--admin-flow-ship)' },
    { key: 'toReceive', label: '待收货', color: 'var(--admin-flow-transit)' },
    { key: 'completed', label: '已完成', color: 'var(--admin-flow-done)' },
    { key: 'cancelled', label: '已取消', color: 'var(--admin-danger)' },
    { key: 'refunded', label: '已退款', color: 'var(--admin-chart-4)' },
] as const;

export default function OverviewPage() {
    const queryClient = useQueryClient();
    const { data: stats, isLoading, isError, error, isFetching } = useDashboardStats();
    const { data: orderStats } = useAdminOrderStats();
    const { data: trend } = useTrend();
    const { data: categories } = useAdminCategories();
    const { data: activity } = useRecentActivity();

    const series = useMemo(() => {
        const points: TrendItem[] = trend ?? [];
        return {
            users: points.map(p => Number(p.users ?? 0)),
            products: points.map(p => Number(p.products ?? 0)),
            orders: points.map(p => Number(p.orders ?? 0)),
        };
    }, [trend]);

    /**
     * 分类分布只取一级分类：一级计数已把子分类归并上来（每件商品只计一次），
     * 二级计数是叶子直挂数——两层混排会把同一件商品算两遍，分母虚大一倍。
     */
    const distribution = useMemo(() => {
        const level1 = (categories ?? []).filter(c => c.level === 1 && (c.productCount ?? 0) > 0);
        if (level1.length === 0) {
            return { rows: [], total: 0, categoryCount: 0 };
        }
        const sorted = [...level1].sort((a, b) => (b.productCount ?? 0) - (a.productCount ?? 0));
        const total = sorted.reduce((sum, c) => sum + (c.productCount ?? 0), 0);
        const maxCount = sorted[0]?.productCount ?? 0;
        // 只列前几名却按全部一级分类算占比，百分数加起来对不上 100%，
        // 余下的合并成「其他 N 个分类」一行
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

    const kpis: {
        label: string;
        value: ReactNode;
        foot: ReactNode;
        lead?: boolean;
        spark?: { values: number[]; color: string };
    }[] = [
        {
            label: '累计营收',
            value: formatMoney(stats?.totalRevenue ?? orderStats?.totalRevenue),
            foot: orderStats ? `今日 ${formatMoney(orderStats.todayRevenue)}` : '已支付订单合计',
            lead: true,
        },
        {
            label: '总用户数',
            value: formatCount(stats?.totalUsers),
            foot: stats ? `今日新增 ${formatCount(stats.todayNewUsers)} 人` : '累计注册',
            spark: { values: series.users, color: 'var(--admin-chart-1)' },
        },
        {
            label: '总商品数',
            value: formatCount(stats?.totalProducts),
            foot: stats ? `待审核 ${formatCount(stats.pendingProducts)} 件` : '累计在架',
            spark: { values: series.products, color: 'var(--admin-chart-3)' },
        },
        {
            label: '总订单数',
            value: formatCount(stats?.totalOrders),
            foot: stats ? `今日 ${formatCount(stats.todayOrders)} 笔` : '累计成交',
            spark: { values: series.orders, color: 'var(--admin-chart-5)' },
        },
    ];

    return (
        <AdminPage>
            <AdminPageHeader
                icon={<TrendingUp size={17} aria-hidden="true" />}
                title="数据总览"
                description="平台侧核心指标：用户 / 商品 / 订单 / 营收，以及各口径的分布与走势"
                actions={
                    <Button
                        variant="outline"
                        onClick={() => queryClient.invalidateQueries({ queryKey: ['admin'] })}
                        disabled={isFetching}
                        isLoading={isFetching}
                        loadingText="刷新中"
                    >
                        {!isFetching ? <RefreshCw size={15} aria-hidden="true" /> : null}
                        刷新
                    </Button>
                }
            />

            <AdminErrorBanner
                message={isError ? (error as Error)?.message : null}
                onRetry={() => queryClient.invalidateQueries({ queryKey: ['admin'] })}
                retrying={isFetching}
            />

            {isLoading ? (
                <AdminCard>
                    <PanelState kind="loading" />
                </AdminCard>
            ) : (
                <div className="admin-kpi-band">
                    {kpis.map(kpi => (
                        <div key={kpi.label} className={`admin-kpi-cell${kpi.lead ? ' admin-kpi-cell--lead' : ''}`}>
                            <span className="admin-kpi-label">{kpi.label}</span>
                            <span className={`admin-kpi-figure${kpi.lead ? ' admin-kpi-figure--lead' : ''}`}>
                                {kpi.value}
                            </span>
                            <span className="admin-kpi-foot">
                                <span>{kpi.foot}</span>
                            </span>
                            {kpi.lead ? <p className="admin-kpi-caption">口径：已支付订单金额合计</p> : null}
                            {kpi.spark ? (
                                <Sparkline
                                    values={kpi.spark.values}
                                    color={kpi.spark.color}
                                    label={`${kpi.label}近 ${kpi.spark.values.length} 月走势`}
                                    fill
                                />
                            ) : null}
                        </div>
                    ))}
                </div>
            )}

            {/* 双栏杂志式：左栏是随时间变化的（走势 + 动态），右栏是分布（分类 + 订单状态）。
                两栏各自堆叠卡片，末位卡片撑满余高——两栏高度自动配平，
                不会再出现「一列高、一列空」的洞。 */}
            <div className="admin-split">
                <div className="admin-col">
                    <AdminCard>
                        <AdminCardHead
                            title="新增走势"
                            note="三条序列各用各的纵轴：量级差一个数量级时叠在一张图上小的会被压平"
                        />
                        {!trend || trend.length === 0 ? (
                            <PanelState kind="empty" />
                        ) : (
                            <div className="admin-small-multiples">
                                {TREND_SERIES.map(s => {
                                    const values = series[s.key];
                                    const latest = values[values.length - 1] ?? 0;
                                    const delta = trendDelta(values);
                                    return (
                                        <div
                                            key={s.key}
                                            className="admin-sm-row"
                                            style={{ '--sm-color': s.color } as CSSProperties}
                                        >
                                            <div className="admin-sm-head">
                                                <span className="admin-sm-label">
                                                    <span className="admin-sm-dot" aria-hidden="true" />
                                                    {s.label}新增
                                                </span>
                                                <span className="admin-sm-latest">
                                                    {formatCount(latest)}
                                                    {delta ? (
                                                        <span
                                                            className={`admin-sm-delta admin-sm-delta--${delta.kind}`}
                                                        >
                                                            {delta.text}
                                                        </span>
                                                    ) : null}
                                                </span>
                                            </div>
                                            <Sparkline
                                                values={values}
                                                color={s.color}
                                                label={`${s.label}新增走势`}
                                                fill
                                            />
                                        </div>
                                    );
                                })}
                                {/* 折线本身没有刻度，横轴口径得写出来：每月一个点，首月到末月 */}
                                <p className="admin-muted admin-axis-note">
                                    横轴：{trend[0]?.month} → {trend[trend.length - 1]?.month}，每月一个点（近{' '}
                                    {trend.length} 月新增）
                                </p>
                            </div>
                        )}
                    </AdminCard>

                    <AdminCard grow>
                        <AdminCardHead title="最近动态" note="用户 / 商品 / 订单侧的近期动作" />
                        {!activity || activity.length === 0 ? (
                            <PanelState kind="empty" />
                        ) : (
                            <div className="admin-feed">
                                {activity.map((item: ActivityItem) => {
                                    const Icon = ACTIVITY_ICONS[item.type] ?? TrendingUp;
                                    return (
                                        <div key={`${item.time}-${item.type}-${item.text}`} className="admin-feed-item">
                                            <span
                                                className="admin-feed-icon"
                                                style={{ '--feed-color': ACTIVITY_COLORS[item.type] } as CSSProperties}
                                                aria-hidden="true"
                                            >
                                                <Icon size={15} />
                                            </span>
                                            <span className="admin-feed-text">{item.text}</span>
                                            <span className="admin-feed-time">{formatRelativeTime(item.time)}</span>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </AdminCard>
                </div>

                <div className="admin-col">
                    <AdminCard>
                        <AdminCardHead title="商品分类分布" note="一级分类口径，子分类已归并" />
                        {distribution.rows.length === 0 ? (
                            <PanelState kind="empty" />
                        ) : (
                            <>
                                <div className="admin-rank-list">
                                    {distribution.rows.map((row, idx) => (
                                        <div key={row.key} className="admin-rank-row">
                                            <span className="admin-rank-index">{idx + 1}</span>
                                            <span className="admin-rank-name">{row.name}</span>
                                            <span className="admin-rank-count">
                                                {row.count} 件 · {row.pct.toFixed(1)}%
                                            </span>
                                            <span className="admin-rank-track">
                                                <span
                                                    className="admin-rank-fill"
                                                    style={
                                                        {
                                                            width: `${(row.ratio * 100).toFixed(1)}%`,
                                                            '--rank-color': row.color,
                                                        } as CSSProperties
                                                    }
                                                />
                                            </span>
                                        </div>
                                    ))}
                                </div>
                                <p className="admin-rank-total admin-muted">
                                    共 {distribution.categoryCount} 个一级分类 · {formatCount(distribution.total)}{' '}
                                    件在架
                                </p>
                            </>
                        )}
                    </AdminCard>

                    <AdminCard grow>
                        <AdminCardHead title="订单状态分布" note="按订单当前状态计数，一张订单只落一格" />
                        {!orderStats ? (
                            <PanelState kind="loading" />
                        ) : (
                            <div className="admin-metric-strip admin-metric-strip--fill">
                                {ORDER_STATUS_METRICS.map(metric => (
                                    <div key={metric.key} className="admin-metric">
                                        <span className="admin-metric-label">
                                            <span
                                                className="admin-status-dot"
                                                style={{ background: metric.color }}
                                                aria-hidden="true"
                                            />
                                            {metric.label}
                                        </span>
                                        <span className="admin-metric-value">
                                            {formatCount(orderStats[metric.key])}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </AdminCard>
                </div>
            </div>
        </AdminPage>
    );
}
