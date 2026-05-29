import { useMemo } from 'react';
import type { UserActivityItem } from '../../../types/admin';

interface ActivityHeatmapProps {
  data: UserActivityItem[];
}

const DAY_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];

// Map MySQL DAYOFWEEK (1=Sunday) to Mon-Sun index (0=Monday)
function toDayIndex(dayOfWeek: number): number {
  return dayOfWeek === 1 ? 6 : dayOfWeek - 2;
}

export default function ActivityHeatmap({ data }: ActivityHeatmapProps) {
  const { maxCount, grid, isEmpty } = useMemo(() => {
    const grid = new Array(7).fill(null).map(() => new Array(24).fill(0));
    let max = 0;
    for (const item of data) {
      const dayIdx = toDayIndex(item.dayOfWeek);
      if (dayIdx >= 0 && dayIdx < 7 && item.hour >= 0 && item.hour < 24) {
        grid[dayIdx][item.hour] = item.count;
        max = Math.max(max, item.count);
      }
    }
    return { maxCount: max || 1, grid, isEmpty: max === 0 };
  }, [data]);

  if (isEmpty) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 200, color: '#B5AEA8', fontSize: '0.87rem' }}>
        暂无活跃数据
      </div>
    );
  }

  const cellSize = 16;
  const cellGap = 2;
  const padding = { top: 8, right: 8, bottom: 8, left: 36 };
  const chartWidth = padding.left + 24 * (cellSize + cellGap) + padding.right;
  const chartHeight = padding.top + 7 * (cellSize + cellGap) + padding.bottom + 18;

  const getColor = (count: number) => {
    if (count === 0) {return 'rgba(229,224,219,0.15)';}
    const intensity = Math.min(count / maxCount, 1);
    const r = Math.round(249 - (249 - 234) * intensity);
    const g = Math.round(115 + (115 - 88) * (1 - intensity));
    const b = Math.round(22 + (22 + 20) * (1 - intensity));
    const alpha = 0.15 + 0.75 * intensity;
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  };

  return (
    <div style={{ overflowX: 'auto', padding: '0.5rem 0' }}>
      <svg width={chartWidth} height={chartHeight} style={{ display: 'block' }}>
        {/* Day labels */}
        {DAY_LABELS.map((label, i) => (
          <text
            key={label}
            x={padding.left - 6}
            y={padding.top + i * (cellSize + cellGap) + cellSize / 2}
            textAnchor="end"
            dominantBaseline="central"
            style={{ fontSize: 11, fill: '#9B9590', fontFamily: "'LXGW WenKai', sans-serif" }}
          >
            {label}
          </text>
        ))}
        {/* Hour labels — show every 3 hours */}
        {Array.from({ length: 8 }, (_, i) => {
          const hour = i * 3;
          return (
            <text
              key={hour}
              x={padding.left + hour * (cellSize + cellGap) + cellSize / 2}
              y={padding.top + 7 * (cellSize + cellGap) + 12}
              textAnchor="middle"
              style={{ fontSize: 10, fill: '#B5AEA8', fontFamily: "'LXGW WenKai', sans-serif" }}
            >
              {hour}时
            </text>
          );
        })}
        {/* Heatmap cells */}
        {grid.map((row, dayIdx) =>
          row.map((count, hour) => (
            <rect
              key={`${dayIdx}-${hour}`}
              x={padding.left + hour * (cellSize + cellGap)}
              y={padding.top + dayIdx * (cellSize + cellGap)}
              width={cellSize}
              height={cellSize}
              rx={3}
              fill={getColor(count)}
              style={{ transition: 'fill 0.2s ease' }}
            >
              <title>{`${DAY_LABELS[dayIdx]} ${hour}:00 — ${count}次操作`}</title>
            </rect>
          ))
        )}
      </svg>
      {/* Legend */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.35rem', marginTop: '0.35rem' }}>
        <span style={{ fontSize: '0.72rem', color: '#B5AEA8' }}>低</span>
        {[0, 0.25, 0.5, 0.75, 1].map((level) => (
          <div
            key={level}
            style={{
              width: 12, height: 12, borderRadius: 3,
              background: level === 0 ? 'rgba(229,224,219,0.15)' : getColor(maxCount * level),
            }}
          />
        ))}
        <span style={{ fontSize: '0.72rem', color: '#B5AEA8' }}>高</span>
      </div>
    </div>
  );
}
