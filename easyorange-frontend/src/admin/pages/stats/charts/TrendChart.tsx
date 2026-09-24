import { useMemo } from 'react';
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { CHART_TEXT, TREND_SERIES_COLORS } from '../../../chartTheme';
import type { TrendItem } from '../../../types/admin';

interface TrendChartProps {
    data: TrendItem[];
    isCompact?: boolean;
    height?: number;
}

export default function TrendChart({ data, isCompact = false, height = 200 }: TrendChartProps) {
    const chartData = useMemo(() => {
        return data.map(item => ({
            ...item,
            monthLabel: item.month ? `${item.month.split('-')[1]}月` : '',
        }));
    }, [data]);

    if (data.length === 0) {
        return (
            <div
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    height,
                    color: CHART_TEXT.axis,
                    fontSize: '0.87rem',
                }}
            >
                暂无趋势数据
            </div>
        );
    }

    return (
        <ResponsiveContainer width="100%" height={height}>
            <LineChart data={chartData} margin={{ top: 5, right: 5, left: isCompact ? 0 : 10, bottom: 5 }}>
                {!isCompact && <CartesianGrid strokeDasharray="3 3" stroke="var(--admin-hairline)" />}
                <XAxis
                    dataKey="monthLabel"
                    tick={{ fontSize: isCompact ? 11 : 12, fill: CHART_TEXT.axis }}
                    axisLine={{ stroke: 'var(--admin-line)' }}
                    tickLine={false}
                />
                {!isCompact && (
                    <YAxis
                        tick={{ fontSize: 12, fill: CHART_TEXT.axis }}
                        axisLine={false}
                        tickLine={false}
                        allowDecimals={false}
                    />
                )}
                {!isCompact && (
                    <Tooltip
                        contentStyle={{
                            background: 'color-mix(in srgb, var(--admin-surface-solid) 90%, transparent)',
                            backdropFilter: 'blur(12px)',
                            border: '1.5px solid var(--admin-control-line)',
                            borderRadius: 12,
                            fontSize: '0.82rem',
                            boxShadow: 'var(--admin-shadow-soft)',
                        }}
                        labelStyle={{ fontWeight: 600, color: CHART_TEXT.label, marginBottom: '0.25rem' }}
                    />
                )}
                {!isCompact && (
                    <Legend wrapperStyle={{ fontSize: '0.78rem', color: CHART_TEXT.legend, paddingTop: '0.5rem' }} />
                )}
                <Line
                    type="monotone"
                    dataKey="users"
                    name="用户"
                    stroke={TREND_SERIES_COLORS.users}
                    strokeWidth={isCompact ? 2 : 2.5}
                    dot={isCompact ? false : { fill: TREND_SERIES_COLORS.users, r: 3 }}
                    activeDot={{ r: isCompact ? 4 : 5 }}
                />
                <Line
                    type="monotone"
                    dataKey="products"
                    name="商品"
                    stroke={TREND_SERIES_COLORS.products}
                    strokeWidth={isCompact ? 2 : 2.5}
                    dot={isCompact ? false : { fill: TREND_SERIES_COLORS.products, r: 3 }}
                    activeDot={{ r: isCompact ? 4 : 5 }}
                />
                <Line
                    type="monotone"
                    dataKey="orders"
                    name="订单"
                    stroke={TREND_SERIES_COLORS.orders}
                    strokeWidth={isCompact ? 2 : 2.5}
                    dot={isCompact ? false : { fill: TREND_SERIES_COLORS.orders, r: 3 }}
                    activeDot={{ r: isCompact ? 4 : 5 }}
                />
            </LineChart>
        </ResponsiveContainer>
    );
}
