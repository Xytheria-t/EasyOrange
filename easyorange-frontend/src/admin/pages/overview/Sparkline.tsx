interface SparklineProps {
    values: number[];
    /** 折线色，走 admin.css 令牌（CSS 变量在 SVG stroke 上直接可用） */
    color: string;
    /** 无障碍名：折线本身没有文字，屏幕阅读器读这个 */
    label: string;
}

const VIEW_W = 100;
const VIEW_H = 32;

/**
 * 迷你折线 —— 手写 SVG，不引图表库。
 *
 * 统计页的走势只有「一条线 + 几个点」，recharts 这类库为此要打进几十 KB；
 * 管理端样式令牌里已有分面小图的类（.admin-sm-*），这里补的是那条线本身。
 */
export function Sparkline({ values, color, label }: SparklineProps) {
    // 少于两个点画不出线（分母为 0），返回空
    if (values.length < 2) {
        return null;
    }

    const max = Math.max(...values);
    const min = Math.min(...values);
    // 全平的序列：span 取 1，避免除零；此时线落在中位
    const span = max - min || 1;
    const points = values
        .map((value, index) => {
            const x = (index / (values.length - 1)) * VIEW_W;
            const y = VIEW_H - ((value - min) / span) * (VIEW_H - 4) - 2;
            return `${x.toFixed(2)},${y.toFixed(2)}`;
        })
        .join(' ');

    return (
        <svg
            className="admin-spark"
            viewBox={`0 0 ${VIEW_W} ${VIEW_H}`}
            preserveAspectRatio="none"
            role="img"
            aria-label={label}
        >
            <polyline
                points={points}
                fill="none"
                stroke={color}
                strokeWidth={1.5}
                strokeLinejoin="round"
                strokeLinecap="round"
                vectorEffect="non-scaling-stroke"
            />
        </svg>
    );
}
