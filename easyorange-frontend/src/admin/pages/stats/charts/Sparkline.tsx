import { useId } from 'react';

interface SparklineProps {
    values: number[];
    /** 令牌引用，如 'var(--admin-chart-1)'——真实色值只在 admin.css */
    color: string;
}

/**
 * KPI 卡里的迷你走势。
 *
 * 不走 recharts：这里只需要一条折线，引入图表库换来的是几百 KB 依赖，
 * 而这四张卡每刷新一次就重画一次。手写 SVG 二十行，viewBox 固定 + preserveAspectRatio
 * 拉伸，描边用 non-scaling-stroke 保持等宽——横向拉变形、纵向不变形也不会把线拉粗。
 */
export function Sparkline({ values, color }: SparklineProps) {
    const gradientId = useId();

    // 单点或全等值都画不出趋势线，直接不渲染，避免出现一条无意义的水平线
    if (values.length < 2 || new Set(values).size === 1) {
        return null;
    }

    const max = Math.max(...values);
    const min = Math.min(...values);
    const span = max - min || 1;
    const step = 100 / (values.length - 1);
    // 上下各留 4 单位内边距，线不会贴死卡边
    const y = (value: number) => 20 - ((value - min) / span) * 12;

    const line = values.map((value, i) => `${(i * step).toFixed(2)},${y(value).toFixed(2)}`).join(' ');
    const area = `0,20 ${line} 100,20`;

    return (
        <svg
            className="admin-kpi-spark"
            viewBox="0 0 100 20"
            preserveAspectRatio="none"
            role="img"
            aria-hidden="true"
            focusable="false"
        >
            <defs>
                <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={color} stopOpacity="0.22" />
                    <stop offset="100%" stopColor={color} stopOpacity="0" />
                </linearGradient>
            </defs>
            <polygon points={area} fill={`url(#${gradientId})`} />
            <polyline
                points={line}
                fill="none"
                stroke={color}
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                vectorEffect="non-scaling-stroke"
            />
        </svg>
    );
}
