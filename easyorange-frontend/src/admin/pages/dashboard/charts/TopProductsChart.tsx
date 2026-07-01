import { Bar, BarChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { TopProductItem } from '../../../types/admin';

interface TopProductsChartProps {
    data: TopProductItem[];
}

const BAR_COLORS = [
    '#F97316',
    '#FB7185',
    '#C39BD3',
    '#FBBF24',
    '#10B981',
    '#F97316',
    '#FB7185',
    '#C39BD3',
    '#FBBF24',
    '#10B981',
];

export default function TopProductsChart({ data }: TopProductsChartProps) {
    if (data.length === 0) {
        return (
            <div
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    height: 260,
                    color: '#B5AEA8',
                    fontSize: '0.87rem',
                }}
            >
                暂无商品数据
            </div>
        );
    }

    const chartData = [...data]
        .map(item => ({
            name: item.name.length > 10 ? `${item.name.slice(0, 10)}…` : item.name,
            views: item.viewCount,
            price: item.price,
            fullName: item.name,
        }))
        .reverse(); // reverse so top item appears at top

    return (
        <ResponsiveContainer width="100%" height={Math.max(200, data.length * 32)}>
            <BarChart data={chartData} layout="vertical" margin={{ top: 5, right: 30, left: 0, bottom: 5 }}>
                <XAxis type="number" tick={{ fontSize: 11, fill: '#9B9590' }} axisLine={false} tickLine={false} />
                <YAxis
                    type="category"
                    dataKey="name"
                    tick={{ fontSize: 11, fill: '#6B6460', fontFamily: "'LXGW WenKai', sans-serif" }}
                    axisLine={false}
                    tickLine={false}
                    width={80}
                />
                <Tooltip
                    contentStyle={{
                        background: 'rgba(255,255,255,0.9)',
                        backdropFilter: 'blur(12px)',
                        border: '1px solid rgba(229,224,219,0.3)',
                        borderRadius: 12,
                        fontSize: '0.82rem',
                        boxShadow: '0 8px 24px rgba(42,37,32,0.08)',
                    }}
                    formatter={(
                        value: number,
                        _name: string,
                        item: { payload?: { fullName?: string; price?: number } }
                    ) => {
                        const payload = item?.payload;
                        return [
                            `浏览量: ${value.toLocaleString()} | ¥${payload?.price?.toFixed(2) ?? '-'}`,
                            payload?.fullName ?? '',
                        ];
                    }}
                    labelFormatter={() => ''}
                />
                <Bar dataKey="views" radius={[0, 6, 6, 0]} maxBarSize={20}>
                    {chartData.map((_entry, index) => (
                        <Cell key={`cell-${index}`} fill={BAR_COLORS[index % BAR_COLORS.length]} fillOpacity={0.8} />
                    ))}
                </Bar>
            </BarChart>
        </ResponsiveContainer>
    );
}
