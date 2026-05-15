import { useMemo } from 'react';
import { useDashboardStats, useAdminCategories, useAdminOrderStats, useTrend, useRecentActivity } from '../../hooks';

const CATEGORY_COLORS = ['#F97316', '#FB7185', '#C39BD3', '#FBBF24', '#10B981', '#8B857E'];

const ACTIVITY_COLORS: Record<string, string> = {
  user: '#F97316',
  product: '#C39BD3',
  order: '#10B981',
  report: '#F43F5E',
};

export default function StatsPage() {
  const { data: stats, isLoading } = useDashboardStats();
  const { data: categories, isLoading: categoriesLoading } = useAdminCategories();
  const { data: orderStats } = useAdminOrderStats();
  const { data: trend = [] } = useTrend();
  const { data: recentActivity = [] } = useRecentActivity();

  const statCards = [
    { label: '总用户数', value: stats?.totalUsers ?? 0, color: '#F97316', gradient: 'linear-gradient(135deg, #F97316, #FB923C)', emoji: '👥' },
    { label: '总商品数', value: stats?.totalProducts ?? 0, color: '#C39BD3', gradient: 'linear-gradient(135deg, #C39BD3, #D8B4FE)', emoji: '📦' },
    { label: '总订单数', value: stats?.totalOrders ?? 0, color: '#10B981', gradient: 'linear-gradient(135deg, #10B981, #34D399)', emoji: '🛒' },
    { label: '今日新增用户', value: stats?.todayNewUsers ?? 0, color: '#FBBF24', gradient: 'linear-gradient(135deg, #FBBF24, #F97316)', emoji: '✨' },
  ];

  const maxTrendValue = useMemo(() => {
    if (trend.length === 0) {return 1;}
    return Math.max(...trend.flatMap((t) => [t.users, t.orders, t.products]));
  }, [trend]);

  const categoryDistribution = useMemo(() => {
    if (!categories || categories.length === 0) {return [];}
    const validCategories = categories.filter((c) => (c.productCount ?? 0) > 0);
    if (validCategories.length === 0) {return [];}
    const maxCount = Math.max(...validCategories.map((c) => c.productCount ?? 0));
    const total = validCategories.reduce((sum, c) => sum + (c.productCount ?? 0), 0);
    return validCategories
      .sort((a, b) => (b.productCount ?? 0) - (a.productCount ?? 0))
      .slice(0, CATEGORY_COLORS.length)
      .map((c) => ({
        name: c.name,
        count: c.productCount ?? 0,
        pct: total > 0 ? ((c.productCount ?? 0) / total * 100) : 0,
        ratio: maxCount > 0 ? ((c.productCount ?? 0) / maxCount) : 0,
      }));
  }, [categories]);

  const monthLabel = (month: string) => {
    const parts = month.split('-');
    if (parts.length === 2) {return parseInt(parts[1]) + '月';}
    return month;
  };

  return (
    <div style={{ position: 'relative', minHeight: 'calc(100vh - 80px)', animation: 'pageIn 0.5s ease-out both' }}>
      {/* Background atmosphere */}
      <div style={{
        position: 'absolute', inset: 0, zIndex: 0, pointerEvents: 'none',
        borderRadius: 20,
        background: `
          radial-gradient(ellipse 55% 35% at 8% 12%, rgba(249,115,22,0.035) 0%, transparent 50%),
          radial-gradient(ellipse 40% 45% at 92% 85%, rgba(195,155,211,0.03) 0%, transparent 48%),
          linear-gradient(180deg, #FAF8F5 0%, #FDF9F6 40%, #FAF8F5 100%)
        `,
      }} />

      <div style={{ position: 'relative', zIndex: 1, display: 'flex', flexDirection: 'column' }}>
        {/* Header */}
        <header style={{ marginBottom: '1.75rem', animation: 'headerSlide 0.6s ease-out both' }}>
          <h1 style={{
            fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
            fontSize: 'clamp(1.5rem, 2.5vw, 1.875rem)', fontWeight: 700,
            color: '#2A2520', letterSpacing: '-0.03em', lineHeight: 1.2,
            display: 'flex', alignItems: 'center', gap: '0.65rem',
          }}>
            <span style={{
              width: 30, height: 30, borderRadius: 10,
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
              background: 'linear-gradient(135deg, #C39BD3, #D8B4FE)',
              color: '#fff', flexShrink: 0,
            }}>
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="20" x2="18" y2="10" /><line x1="12" y1="20" x2="12" y2="4" /><line x1="6" y1="20" x2="6" y2="14" />
              </svg>
            </span>
            数据统计
          </h1>
          <p style={{ fontSize: '0.88rem', color: '#9B9590', marginTop: '0.3rem', paddingLeft: '36px' }}>
            平台运营数据概览与趋势分析
          </p>
        </header>

        {/* Stat cards */}
        <div style={{
          display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1rem', marginBottom: '1.5rem', animation: 'toolbarIn 0.5s ease-out 0.08s both',
        }}>
          {statCards.map((card, idx) => (
            <div
              key={card.label}
              style={{
                padding: '1.25rem',
                background: 'rgba(255,255,255,0.72)', backdropFilter: 'blur(16px)',
                WebkitBackdropFilter: 'blur(16px)',
                border: '1px solid rgba(255,255,255,0.6)', borderRadius: 20,
                position: 'relative', overflow: 'hidden',
                animation: `cardIn 0.5s ease-out ${0.08 + idx * 0.05}s both`,
              }}
            >
              <div style={{
                position: 'absolute', top: '-12px', right: '-12px',
                width: 64, height: 64, borderRadius: '50%',
                background: card.gradient, opacity: 0.08,
              }} />
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                <span style={{ fontSize: '0.82rem', fontWeight: 500, color: '#9B9590' }}>{card.label}</span>
                <span style={{ fontSize: '1.1rem' }}>{card.emoji}</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem' }}>
                <span style={{
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: '1.75rem', fontWeight: 700, color: '#2A2520',
                  lineHeight: 1,
                }}>
                  {isLoading ? '—' : card.value.toLocaleString()}
                </span>
              </div>
            </div>
          ))}
        </div>

        {/* Order stats quick summary */}
        {orderStats && (
          <div style={{
            display: 'flex', flexWrap: 'wrap', gap: '0.75rem',
            marginBottom: '1.5rem', animation: 'toolbarIn 0.5s ease-out 0.2s both',
            padding: '1rem 1.25rem',
            background: 'rgba(255,255,255,0.6)', backdropFilter: 'blur(12px)',
            WebkitBackdropFilter: 'blur(12px)',
            border: '1px solid rgba(255,255,255,0.55)', borderRadius: 16,
            alignItems: 'center',
          }}>
            {[
              { label: '今日订单', value: orderStats.todayOrders, color: '#F97316' },
              { label: '待发货', value: orderStats.toShip, color: '#FBBF24' },
              { label: '待收货', value: orderStats.toReceive, color: '#10B981' },
              { label: '已完成', value: orderStats.completed, color: '#C39BD3' },
              { label: '今日营收', value: `¥${orderStats.todayRevenue.toLocaleString()}`, color: '#FB7185' },
            ].map((item) => (
              <div key={item.label} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <span style={{ width: 7, height: 7, borderRadius: '50%', background: item.color, flexShrink: 0 }} />
                <span style={{ fontSize: '0.78rem', color: '#9B9590' }}>{item.label}:</span>
                <span style={{ fontSize: '0.84rem', fontWeight: 700, color: '#2A2520' }}>{item.value}</span>
              </div>
            ))}
          </div>
        )}

        {/* Two-column layout */}
        <div style={{
          display: 'grid', gridTemplateColumns: '1fr 1fr',
          gap: '1.25rem', marginBottom: '1.25rem',
          animation: 'cardIn 0.5s ease-out 0.25s both',
        }}>
          {/* Trend chart */}
          <div style={{
            background: 'rgba(255,255,255,0.72)', backdropFilter: 'blur(20px)',
            WebkitBackdropFilter: 'blur(20px)',
            border: '1px solid rgba(255,255,255,0.65)', borderRadius: 20,
            padding: '1.5rem', overflow: 'hidden',
          }}>
            <h3 style={{
              fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
              fontSize: '1rem', fontWeight: 700, color: '#2A2520',
              marginBottom: '1.25rem',
            }}>
              📈 月度趋势
            </h3>
            {trend.length === 0 ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 160, color: '#B5AEA8', fontSize: '0.87rem' }}>
                加载中…
              </div>
            ) : (
              <div style={{ display: 'flex', alignItems: 'flex-end', gap: '0.75rem', height: 160 }}>
                {trend.map((item) => (
                  <div key={item.month} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.35rem' }}>
                    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 3, height: 120, width: '100%' }}>
                      <div style={{
                        flex: 1, borderRadius: '6px 6px 2px 2px',
                        background: 'linear-gradient(180deg, #F97316, rgba(249,115,22,0.5))',
                        height: `${(item.users / maxTrendValue) * 100}%`,
                        minHeight: 4, transition: 'height 0.5s ease',
                      }} />
                      <div style={{
                        flex: 1, borderRadius: '6px 6px 2px 2px',
                        background: 'linear-gradient(180deg, #C39BD3, rgba(195,155,211,0.5))',
                        height: `${(item.products / maxTrendValue) * 100}%`,
                        minHeight: 4, transition: 'height 0.5s ease',
                      }} />
                      <div style={{
                        flex: 1, borderRadius: '6px 6px 2px 2px',
                        background: 'linear-gradient(180deg, #10B981, rgba(16,185,129,0.5))',
                        height: `${(item.orders / maxTrendValue) * 100}%`,
                        minHeight: 4, transition: 'height 0.5s ease',
                      }} />
                    </div>
                    <span style={{ fontSize: '0.72rem', color: '#9B9590', fontWeight: 500 }}>{monthLabel(item.month)}</span>
                  </div>
                ))}
              </div>
            )}
            {/* Legend */}
            <div style={{ display: 'flex', justifyContent: 'center', gap: '1.25rem', marginTop: '1rem' }}>
              {[
                { label: '用户', color: '#F97316' },
                { label: '商品', color: '#C39BD3' },
                { label: '订单', color: '#10B981' },
              ].map((l) => (
                <div key={l.label} style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                  <span style={{ width: 10, height: 10, borderRadius: 3, background: l.color }} />
                  <span style={{ fontSize: '0.76rem', color: '#8B857E', fontWeight: 500 }}>{l.label}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Category distribution — powered by useAdminCategories() */}
          <div style={{
            background: 'rgba(255,255,255,0.72)', backdropFilter: 'blur(20px)',
            WebkitBackdropFilter: 'blur(20px)',
            border: '1px solid rgba(255,255,255,0.65)', borderRadius: 20,
            padding: '1.5rem', overflow: 'hidden',
          }}>
            <h3 style={{
              fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
              fontSize: '1rem', fontWeight: 700, color: '#2A2520',
              marginBottom: '1.25rem',
            }}>
              🏷️ 商品分类分布
            </h3>
            {categoriesLoading ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 140, color: '#B5AEA8', fontSize: '0.87rem' }}>
                加载中…
              </div>
            ) : categoryDistribution.length === 0 ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 140, color: '#B5AEA8', fontSize: '0.87rem' }}>
                暂无分类数据
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
                {categoryDistribution.map((cat, idx) => (
                  <div key={cat.name}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.3rem' }}>
                      <span style={{ fontSize: '0.84rem', fontWeight: 600, color: '#4A4540' }}>{cat.name}</span>
                      <span style={{ fontSize: '0.78rem', color: '#9B9590' }}>{cat.count} 件 ({Math.round(cat.pct)}%)</span>
                    </div>
                    <div style={{
                      height: 8, borderRadius: 4,
                      background: 'rgba(229,224,219,0.3)',
                      overflow: 'hidden',
                    }}>
                      <div style={{
                        height: '100%', borderRadius: 4,
                        background: CATEGORY_COLORS[idx % CATEGORY_COLORS.length],
                        width: `${Math.max(cat.ratio * 100, 4)}%`,
                        transition: 'width 0.6s ease',
                      }} />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Recent activity */}
        <div style={{
          background: 'rgba(255,255,255,0.72)', backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          border: '1px solid rgba(255,255,255,0.65)', borderRadius: 20,
          padding: '1.5rem', overflow: 'hidden',
          animation: 'cardIn 0.5s ease-out 0.35s both',
        }}>
          <h3 style={{
            fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
            fontSize: '1rem', fontWeight: 700, color: '#2A2520',
            marginBottom: '1.25rem',
          }}>
            🔔 最近动态
          </h3>
          {recentActivity.length === 0 ? (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 100, color: '#B5AEA8', fontSize: '0.87rem' }}>
              暂无动态
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
              {recentActivity.map((activity, idx) => (
                <div
                  key={idx}
                  style={{
                    display: 'flex', alignItems: 'center', gap: '0.85rem',
                    padding: '0.85rem 0',
                    borderBottom: idx < recentActivity.length - 1 ? '1px solid rgba(229,224,219,0.35)' : 'none',
                  }}
                >
                  <div style={{
                    width: 8, height: 8, borderRadius: '50%',
                    background: ACTIVITY_COLORS[activity.type] ?? '#9B9590',
                    flexShrink: 0,
                    boxShadow: `0 0 8px ${ACTIVITY_COLORS[activity.type] ?? '#9B9590'}40`,
                  }} />
                  <span style={{ flex: 1, fontSize: '0.87rem', color: '#4A4540' }}>{activity.text}</span>
                  <span style={{ fontSize: '0.78rem', color: '#B5AEA8', flexShrink: 0, whiteSpace: 'nowrap' }}>{activity.time}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <style>{`
        @keyframes pageIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes headerSlide { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes toolbarIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes cardIn { from { opacity: 0; transform: translateY(16px) scale(0.99); } to { opacity: 1; transform: translateY(0) scale(1); } }
      `}</style>
    </div>
  );
}
