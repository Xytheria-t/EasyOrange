import type { CSSProperties } from 'react';
import { useMemo } from 'react';
import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { CHART_TEXT, TREND_SERIES } from '../../../chartTheme';
import type { TrendItem } from '../../../types/admin';

interface TrendChartProps {
    data: TrendItem[];
    /** 单个分面的绘图高度，不含横轴 */
    height?: number;
}

/**
 * 月度趋势 —— 分面折线（小多图），不是三条线挤一张图。
 *
 * 三个指标量级差一个数量级：商品数十几个、订单长期个位数。共用一根纵轴时，
 * 订单线会整条压在 x 轴上，读者只会看到"订单 = 0"——上一版就是这个效果，
 * 图表本身没坏，是编码方式让它传达不了信息。分面后每条线各用各的纵轴，
 * 代价是失去交叉点，换来三条线各自的形状都读得出来。
 *
 * 横轴只画在最后一个分面上，但每行都占同样高度留白，三块绘图区因此横向对齐。
 */
export default function TrendChart({ data, height = 104 }: TrendChartProps) {
    const chartData = useMemo(
        () =>
            data.map(item => ({
                ...item,
                monthLabel: item.month ? `${item.month.split('-')[1]}月` : '',
            })),
        [data]
    );

    if (data.length === 0) {
        return (
            <div className="admin-panel-state" style={{ height }}>
                暂无趋势数据
            </div>
        );
    }

    return (
        <div className="admin-small-multiples">
            {TREND_SERIES.map((series, idx) => {
                const values = data.map(item => item[series.key as keyof TrendItem] as number);
                const latest = values[values.length - 1] ?? 0;
                const prev = values.length > 1 ? values[values.length - 2] : null;
                const isLast = idx === TREND_SERIES.length - 1;

                return (
                    <div className="admin-sm-row" key={series.key}>
                        <div className="admin-sm-head">
                            <span className="admin-sm-label">
                                <span
                                    className="admin-sm-dot"
                                    style={{ '--sm-color': series.color } as CSSProperties}
                                />
                                {series.label}
                            </span>
                            <span className="admin-sm-latest">
                                {latest.toLocaleString()}
                                {prev === null ? null : <Delta from={prev} to={latest} />}
                            </span>
                        </div>
                        <ResponsiveContainer width="100%" height={height}>
                            <AreaChart data={chartData} margin={{ top: 4, right: 4, left: 0, bottom: 0 }}>
                                <defs>
                                    <linearGradient id={`sm-fill-${series.key}`} x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="0%" stopColor={series.color} stopOpacity="0.2" />
                                        <stop offset="100%" stopColor={series.color} stopOpacity="0" />
                                    </linearGradient>
                                </defs>
                                <YAxis hide domain={[0, (max: number) => Math.max(max * 1.25, 1)]} />
                                <XAxis
                                    dataKey="monthLabel"
                                    height={18}
                                    interval="preserveStartEnd"
                                    axisLine={false}
                                    tickLine={false}
                                    tick={isLast ? { fontSize: 11, fill: CHART_TEXT.axis } : false}
                                />
                                <Tooltip
                                    cursor={{ stroke: 'var(--admin-hairline)', strokeWidth: 1 }}
                                    contentStyle={{
                                        background: 'var(--admin-surface-solid)',
                                        border: '1px solid var(--admin-control-line)',
                                        borderRadius: 10,
                                        fontSize: '0.8rem',
                                        boxShadow: 'var(--admin-shadow-soft)',
                                    }}
                                    labelStyle={{ color: CHART_TEXT.label, fontWeight: 600, marginBottom: 2 }}
                                    itemStyle={{ color: CHART_TEXT.legend, padding: 0 }}
                                    labelFormatter={label => `${label ?? ''}`}
                                    formatter={value => [Number(value ?? 0).toLocaleString(), series.label]}
                                />
                                <Area
                                    type="monotone"
                                    dataKey={series.key}
                                    name={series.label}
                                    stroke={series.color}
                                    strokeWidth={2}
                                    fill={`url(#sm-fill-${series.key})`}
                                    dot={false}
                                    activeDot={{ r: 3.5, strokeWidth: 0 }}
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                );
            })}
        </div>
    );
}

/** 环比：只有两个月可比，首月不显示——没有"较上月"这个基准 */
function Delta({ from, to }: { from: number; to: number }) {
    if (from === 0 && to === 0) {
        return <span className="admin-sm-delta admin-sm-delta--flat">持平</span>;
    }
    if (from === 0) {
        return <span className="admin-sm-delta admin-sm-delta--up">新增</span>;
    }
    const pct = ((to - from) / from) * 100;
    const tone = pct > 0 ? 'up' : pct < 0 ? 'down' : 'flat';
    return (
        <span className={`admin-sm-delta admin-sm-delta--${tone}`}>
            {pct > 0 ? '↑' : pct < 0 ? '↓' : ''} {Math.abs(Math.round(pct))}%
        </span>
    );
}
