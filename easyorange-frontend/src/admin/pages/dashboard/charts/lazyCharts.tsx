import { lazy, Suspense } from 'react';
import type { TopProductItem, TrendItem } from '../../../types/admin';

const TrendChart = lazy(() => import('./TrendChart'));
const TopProductsChart = lazy(() => import('./TopProductsChart'));

interface TrendChartProps {
    data: TrendItem[];
    isCompact?: boolean;
    height?: number;
}

interface TopProductsChartProps {
    data: TopProductItem[];
}

const ChartFallback = ({ height = 200 }: { height?: number }) => (
    <div
        role="img"
        aria-label="图表加载中"
        className="flex items-center justify-center rounded-xl bg-white/40"
        style={{ height, color: '#B5AEA8', fontSize: '0.85rem' }}
    >
        <span className="inline-flex items-center gap-2">
            <span
                aria-hidden="true"
                className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-orange-200 border-t-orange-500"
            />
            图表加载中…
        </span>
    </div>
);

export function LazyTrendChart(props: TrendChartProps) {
    return (
        <Suspense fallback={<ChartFallback height={props.height} />}>
            <TrendChart {...props} />
        </Suspense>
    );
}

export function LazyTopProductsChart(props: TopProductsChartProps) {
    return (
        <Suspense fallback={<ChartFallback />}>
            <TopProductsChart {...props} />
        </Suspense>
    );
}

export type { TopProductsChartProps, TrendChartProps };
