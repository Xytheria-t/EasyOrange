import { useMemo } from 'react';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
} from 'recharts';
import type { TrendItem } from '../../../types/admin';

interface TrendChartProps {
  data: TrendItem[];
  compact?: boolean;
  height?: number;
}

const COLORS = {
  users: '#F97316',
  products: '#C39BD3',
  orders: '#10B981',
};

export default function TrendChart({ data, compact = false, height = 200 }: TrendChartProps) {
  const chartData = useMemo(() => {
    return data.map((item) => ({
      ...item,
      monthLabel: item.month ? item.month.split('-')[1] + '月' : '',
    }));
  }, [data]);

  if (data.length === 0) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height, color: '#B5AEA8', fontSize: '0.87rem' }}>
        暂无趋势数据
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={chartData} margin={{ top: 5, right: 5, left: compact ? 0 : 10, bottom: 5 }}>
        {!compact && <CartesianGrid strokeDasharray="3 3" stroke="rgba(229,224,219,0.4)" />}
        <XAxis
          dataKey="monthLabel"
          tick={{ fontSize: compact ? 11 : 12, fill: '#9B9590' }}
          axisLine={{ stroke: 'rgba(229,224,219,0.3)' }}
          tickLine={false}
        />
        {!compact && (
          <YAxis
            tick={{ fontSize: 12, fill: '#9B9590' }}
            axisLine={false}
            tickLine={false}
            allowDecimals={false}
          />
        )}
        {!compact && (
          <Tooltip
            contentStyle={{
              background: 'rgba(255,255,255,0.9)',
              backdropFilter: 'blur(12px)',
              border: '1px solid rgba(229,224,219,0.3)',
              borderRadius: 12,
              fontSize: '0.82rem',
              boxShadow: '0 8px 24px rgba(42,37,32,0.08)',
            }}
            labelStyle={{ fontWeight: 600, color: '#2A2520', marginBottom: '0.25rem' }}
          />
        )}
        {!compact && (
          <Legend
            wrapperStyle={{ fontSize: '0.78rem', color: '#8B857E', paddingTop: '0.5rem' }}
          />
        )}
        <Line
          type="monotone"
          dataKey="users"
          name="用户"
          stroke={COLORS.users}
          strokeWidth={compact ? 2 : 2.5}
          dot={compact ? false : { fill: COLORS.users, r: 3 }}
          activeDot={{ r: compact ? 4 : 5 }}
        />
        <Line
          type="monotone"
          dataKey="products"
          name="商品"
          stroke={COLORS.products}
          strokeWidth={compact ? 2 : 2.5}
          dot={compact ? false : { fill: COLORS.products, r: 3 }}
          activeDot={{ r: compact ? 4 : 5 }}
        />
        <Line
          type="monotone"
          dataKey="orders"
          name="订单"
          stroke={COLORS.orders}
          strokeWidth={compact ? 2 : 2.5}
          dot={compact ? false : { fill: COLORS.orders, r: 3 }}
          activeDot={{ r: compact ? 4 : 5 }}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}
