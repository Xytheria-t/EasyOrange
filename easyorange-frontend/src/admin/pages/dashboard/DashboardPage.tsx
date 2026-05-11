import { Link } from 'react-router-dom';
import { StatCard } from './StatCard';
import {
  useDashboardStats,
  usePendingItems,
  useRecentUsers,
  useRecentProducts,
} from '../../hooks/useAdminDashboard';

function formatTimeAgo(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);
  if (diffMins < 1) return '刚刚';
  if (diffMins < 60) return `${diffMins}分钟前`;
  if (diffHours < 24) return `${diffHours}小时前`;
  return `${diffDays}天前`;
}

function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour < 6) return '夜深了';
  if (hour < 12) return '上午好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
}

function getCurrentTime(): string {
  const now = new Date();
  const dateStr = `${now.getMonth() + 1}月${now.getDate()}日`;
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  return `${dateStr} ${weekDays[now.getDay()]}`;
}

const AVATAR_GRADIENTS = [
  'linear-gradient(135deg, #F97316, #FB923C)',
  'linear-gradient(135deg, #FB7185, #C39BD3)',
  'linear-gradient(135deg, #34D399, #10B981)',
  'linear-gradient(135deg, #FBBF24, #F97316)',
  'linear-gradient(135deg, #C39BD3, #D8B4FE)',
] as const;

const PRODUCT_ICONS: Record<string, string> = {
  default: '📦', 教材: '📚', 电子: '💻', 手机: '📱', 电脑: '💻',
  自行车: '🚴', 衣服: '👕', 鞋: '👟', 书: '📖',
};

function getProductIcon(name: string): string {
  for (const key of Object.keys(PRODUCT_ICONS)) {
    if (key !== 'default' && name.includes(key)) return PRODUCT_ICONS[key];
  }
  return PRODUCT_ICONS.default;
}

export default function DashboardPage() {
  const { data: stats, isLoading: statsLoading } = useDashboardStats();
  const { data: pendingItems, isLoading: pendingLoading } = usePendingItems();
  const { data: recentUsers, isLoading: usersLoading } = useRecentUsers(5);
  const { data: recentProducts, isLoading: productsLoading } = useRecentProducts(5);

  const hasPendingItems =
    pendingItems &&
    (pendingItems.pendingProducts > 0 ||
      pendingItems.pendingReports > 0 ||
      pendingItems.pendingRefunds > 0);

  const totalPending =
    (pendingItems?.pendingProducts ?? 0) +
    (pendingItems?.pendingReports ?? 0) +
    (pendingItems?.pendingRefunds ?? 0);

  const sectionCard: React.CSSProperties = {
    background: 'rgba(255,255,255,0.72)',
    backdropFilter: 'blur(20px)',
    WebkitBackdropFilter: 'blur(20px)',
    border: '1px solid rgba(255,255,255,0.65)',
    borderRadius: 20,
    overflow: 'hidden',
    transition: 'all 0.35s ease',
  };

  const sectionHeader: React.CSSProperties = {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '1.15rem 1.5rem',
    borderBottom: '1px solid rgba(229,224,219,0.4)',
    position: 'relative',
  };

  const sectionTitle: React.CSSProperties = {
    fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
    fontSize: '1rem', fontWeight: 700, color: '#2A2520',
    letterSpacing: '-0.02em',
    display: 'flex', alignItems: 'center', gap: '0.5rem',
  };

  return (
    <div style={{ position: 'relative', animation: 'dashPageIn 0.5s ease-out both' }}>
      {/* Background atmosphere */}
      <div style={{
        position: 'fixed', inset: 0, zIndex: 0, pointerEvents: 'none',
        background: `
          radial-gradient(ellipse 60% 40% at 10% 15%, rgba(249,115,22,0.04) 0%, transparent 55%),
          radial-gradient(ellipse 45% 55% at 90% 80%, rgba(195,155,211,0.035) 0%, transparent 50%),
          radial-gradient(ellipse 35% 35% at 50% 50%, rgba(251,113,133,0.025) 0%, transparent 55%),
          linear-gradient(180deg, #FAF8F5 0%, #FDF9F6 40%, #FAF8F5 100%)
        `,
      }} />

      <div style={{ position: 'relative', zIndex: 1 }}>
        {/* Header */}
        <header style={{ marginBottom: '2rem', animation: 'dashHeaderIn 0.7s ease-out both' }}>
          <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
            <div>
              <h1 style={{
                fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                fontSize: 'clamp(1.5rem, 3vw, 2rem)', fontWeight: 700,
                color: '#2A2520', letterSpacing: '-0.03em', lineHeight: 1.2,
              }}>
                <span>{getGreeting()}，</span>
                <span style={{
                  background: 'linear-gradient(135deg, #F97316, #FB7185)',
                  WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text',
                }}>管理员</span>
              </h1>
              <p style={{ marginTop: '0.375rem', fontSize: '0.9375rem', color: '#9B9590', fontWeight: 400, letterSpacing: '0.01em' }}>
                平台运营数据一览，掌控全局动态
              </p>
            </div>
            <div style={{
              display: 'flex', alignItems: 'center', gap: '0.5rem',
              padding: '0.5rem 1rem', borderRadius: 14,
              background: 'rgba(255,255,255,0.6)', backdropFilter: 'blur(12px)',
              WebkitBackdropFilter: 'blur(12px)',
              border: '1px solid rgba(255,255,255,0.5)',
              fontSize: '0.8125rem', color: '#6B6460', fontWeight: 500,
            }}>
              <span style={{
                width: 8, height: 8, borderRadius: '50%',
                background: '#10B981',
                boxShadow: '0 0 8px rgba(16,185,129,0.4)',
                animation: 'dashPulse 2s ease-in-out infinite',
              }} />
              <span>{getCurrentTime()}</span>
            </div>
          </div>
        </header>

        {/* Stats Grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(5, 1fr)',
          gap: '1.25rem',
          marginBottom: '2rem',
          animation: 'dashGridIn 0.6s ease-out 0.1s both',
        }}>
          {statsLoading ? (
            Array.from({ length: 5 }).map((_, i) => (
              <div key={i} style={{
                background: 'rgba(255,255,255,0.72)',
                backdropFilter: 'blur(20px)',
                WebkitBackdropFilter: 'blur(20px)',
                border: '1px solid rgba(255,255,255,0.7)',
                borderRadius: 20, padding: '1.5rem',
                animation: `dashCardIn 0.5s ease-out ${0.05 + i * 0.05}s both`,
              }}>
                <div style={{ width: '40%', height: 12, background: 'rgba(229,224,219,0.4)', borderRadius: 6, marginBottom: '0.75rem' }} />
                <div style={{ width: '60%', height: 28, background: 'rgba(229,224,219,0.3)', borderRadius: 8 }} />
              </div>
            ))
          ) : (
            <>
              <div style={{ animation: 'dashCardIn 0.5s ease-out 0.05s both' }}>
                <StatCard
                  title="总用户数"
                  value={stats?.totalUsers ?? 0}
                  growth={stats?.userGrowth}
                  accent="orange"
                  icon={<svg fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="1.8" width="22" height="22"><path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" /></svg>}
                />
              </div>
              <div style={{ animation: 'dashCardIn 0.5s ease-out 0.1s both' }}>
                <StatCard
                  title="今日新增"
                  value={stats?.newUsersToday ?? 0}
                  growth={stats?.userGrowth}
                  accent="rose"
                  icon={<svg fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="1.8" width="22" height="22"><path strokeLinecap="round" strokeLinejoin="round" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" /></svg>}
                />
              </div>
              <div style={{ animation: 'dashCardIn 0.5s ease-out 0.15s both' }}>
                <StatCard
                  title="商品总数"
                  value={stats?.totalProducts ?? 0}
                  growth={stats?.productGrowth}
                  accent="purple"
                  icon={<svg fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="1.8" width="22" height="22"><path strokeLinecap="round" strokeLinejoin="round" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" /></svg>}
                />
              </div>
              <div style={{ animation: 'dashCardIn 0.5s ease-out 0.2s both' }}>
                <StatCard
                  title="待审核"
                  value={stats?.pendingReview ?? 0}
                  accent="gold"
                  icon={<svg fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="1.8" width="22" height="22"><path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>}
                />
              </div>
              <div style={{ animation: 'dashCardIn 0.5s ease-out 0.25s both' }}>
                <StatCard
                  title="订单数"
                  value={stats?.totalOrders ?? 0}
                  growth={stats?.orderGrowth}
                  accent="emerald"
                  icon={<svg fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="1.8" width="22" height="22"><path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" /></svg>}
                />
              </div>
            </>
          )}
        </div>

        {/* Pending Items Alert */}
        {hasPendingItems && (
          <section style={{
            ...sectionCard,
            border: '1px solid rgba(249,115,22,0.15)',
            marginBottom: '1.5rem',
            animation: 'dashCardIn 0.5s ease-out 0.3s both',
          }}>
            <div style={sectionHeader}>
              <h2 style={sectionTitle}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#F97316" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                待处理事项
                <span style={{
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                  minWidth: 22, height: 22, padding: '0 6px', borderRadius: 9999,
                  background: 'linear-gradient(135deg, #F97316, #EA580C)',
                  color: '#fff', fontSize: '0.7rem', fontWeight: 700, letterSpacing: 0,
                }}>
                  {totalPending}
                </span>
              </h2>
            </div>
            <div style={{ padding: '1.25rem 1.5rem' }}>
              {pendingLoading ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {Array.from({ length: 3 }).map((_, i) => (
                    <div key={i} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <div style={{ width: 192, height: 16, background: 'rgba(229,224,219,0.4)', borderRadius: 6 }} />
                      <div style={{ width: 80, height: 32, background: 'rgba(229,224,219,0.3)', borderRadius: 8 }} />
                    </div>
                  ))}
                </div>
              ) : (
                <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '0.25rem', margin: 0, padding: 0 }}>
                  {pendingItems?.pendingProducts > 0 && (
                    <li style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.875rem 1rem', borderRadius: 12, transition: 'background 0.15s ease' }}
                      onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(249,115,22,0.04)'; }}
                      onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#F97316', flexShrink: 0, boxShadow: '0 0 8px rgba(249,115,22,0.4)', animation: 'dashPulse 2s ease-in-out infinite' }} />
                        <span style={{ fontSize: '0.9rem', color: '#4A4540', fontWeight: 500 }}>
                          <strong style={{ fontWeight: 700, color: '#EA580C' }}>{pendingItems.pendingProducts}</strong> 个商品待审核
                        </span>
                      </div>
                      <Link to="/admin/products?status=0" style={{
                        display: 'inline-flex', alignItems: 'center', gap: '0.375rem',
                        padding: '0.4rem 0.875rem', borderRadius: 9999,
                        fontSize: '0.8125rem', fontWeight: 600, color: '#fff',
                        background: 'linear-gradient(135deg, #F97316, #EA580C)',
                        textDecoration: 'none', transition: 'all 0.2s ease',
                        letterSpacing: '0.01em', border: 'none', cursor: 'pointer',
                      }}
                        onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateX(3px)'; e.currentTarget.style.boxShadow = '0 4px 12px rgba(249,115,22,0.3)'; }}
                        onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateX(0)'; e.currentTarget.style.boxShadow = 'none'; }}
                      >
                        立即处理
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14M12 5l7 7-7 7" /></svg>
                      </Link>
                    </li>
                  )}
                  {pendingItems?.pendingReports > 0 && (
                    <li style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.875rem 1rem', borderRadius: 12, transition: 'background 0.15s ease' }}
                      onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(249,115,22,0.04)'; }}
                      onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#F43F5E', flexShrink: 0, boxShadow: '0 0 8px rgba(244,63,94,0.4)', animation: 'dashPulse 2s ease-in-out infinite' }} />
                        <span style={{ fontSize: '0.9rem', color: '#4A4540', fontWeight: 500 }}>
                          <strong style={{ fontWeight: 700, color: '#E11D48' }}>{pendingItems.pendingReports}</strong> 条举报待处理
                        </span>
                      </div>
                      <Link to="/admin/reports" style={{
                        display: 'inline-flex', alignItems: 'center', gap: '0.375rem',
                        padding: '0.4rem 0.875rem', borderRadius: 9999,
                        fontSize: '0.8125rem', fontWeight: 600, color: '#fff',
                        background: 'linear-gradient(135deg, #F43F5E, #E11D48)',
                        textDecoration: 'none', transition: 'all 0.2s ease',
                        letterSpacing: '0.01em', border: 'none', cursor: 'pointer',
                      }}
                        onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateX(3px)'; e.currentTarget.style.boxShadow = '0 4px 12px rgba(244,63,94,0.3)'; }}
                        onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateX(0)'; e.currentTarget.style.boxShadow = 'none'; }}
                      >
                        立即处理
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14M12 5l7 7-7 7" /></svg>
                      </Link>
                    </li>
                  )}
                  {pendingItems?.pendingRefunds > 0 && (
                    <li style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.875rem 1rem', borderRadius: 12, transition: 'background 0.15s ease' }}
                      onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(249,115,22,0.04)'; }}
                      onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#FBBF24', flexShrink: 0, boxShadow: '0 0 8px rgba(251,191,36,0.4)', animation: 'dashPulse 2s ease-in-out infinite' }} />
                        <span style={{ fontSize: '0.9rem', color: '#4A4540', fontWeight: 500 }}>
                          <strong style={{ fontWeight: 700, color: '#D97706' }}>{pendingItems.pendingRefunds}</strong> 笔退款申请
                        </span>
                      </div>
                      <Link to="/admin/orders" style={{
                        display: 'inline-flex', alignItems: 'center', gap: '0.375rem',
                        padding: '0.4rem 0.875rem', borderRadius: 9999,
                        fontSize: '0.8125rem', fontWeight: 600, color: '#fff',
                        background: 'linear-gradient(135deg, #FBBF24, #D97706)',
                        textDecoration: 'none', transition: 'all 0.2s ease',
                        letterSpacing: '0.01em', border: 'none', cursor: 'pointer',
                      }}
                        onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateX(3px)'; e.currentTarget.style.boxShadow = '0 4px 12px rgba(251,191,36,0.3)'; }}
                        onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateX(0)'; e.currentTarget.style.boxShadow = 'none'; }}
                      >
                        立即处理
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14M12 5l7 7-7 7" /></svg>
                      </Link>
                    </li>
                  )}
                </ul>
              )}
            </div>
          </section>
        )}

        {/* Two-column grid for recent items */}
        <div style={{
          display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)',
          gap: '1.5rem', animation: 'dashCardIn 0.5s ease-out 0.35s both',
        }}>
          {/* Recent Users */}
          <section style={sectionCard}>
            <div style={sectionHeader}>
              <h2 style={sectionTitle}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#F97316" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 00-3-3.87" /><path d="M16 3.13a4 4 0 010 7.75" />
                </svg>
                最近注册用户
              </h2>
            </div>
            <div style={{ padding: '1.25rem 1.5rem' }}>
              {usersLoading ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {Array.from({ length: 3 }).map((_, i) => (
                    <div key={i} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <div style={{ width: 36, height: 36, borderRadius: 10, background: 'rgba(229,224,219,0.4)' }} />
                        <div style={{ width: 80, height: 14, background: 'rgba(229,224,219,0.4)', borderRadius: 6 }} />
                      </div>
                      <div style={{ width: 64, height: 14, background: 'rgba(229,224,219,0.3)', borderRadius: 6 }} />
                    </div>
                  ))}
                </div>
              ) : recentUsers && recentUsers.length > 0 ? (
                <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', margin: 0, padding: 0 }}>
                  {recentUsers.map((user, idx) => (
                    <li key={user.userId} style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      padding: '0.75rem 0', gap: '1rem',
                      borderBottom: idx < recentUsers.length - 1 ? '1px solid rgba(229,224,219,0.35)' : 'none',
                      transition: 'padding 0.15s ease',
                    }}
                      onMouseEnter={(e) => { e.currentTarget.style.paddingLeft = '0.5rem'; }}
                      onMouseLeave={(e) => { e.currentTarget.style.paddingLeft = '0'; }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', minWidth: 0 }}>
                        <span style={{
                          width: 36, height: 36, borderRadius: 10,
                          display: 'flex', alignItems: 'center', justifyContent: 'center',
                          fontSize: '0.8125rem', fontWeight: 700, color: '#fff',
                          fontFamily: "'Playfair Display', serif",
                          background: AVATAR_GRADIENTS[idx % AVATAR_GRADIENTS.length],
                          flexShrink: 0,
                        }}>
                          {(user.nickname || user.username || '?').charAt(0).toUpperCase()}
                        </span>
                        <span style={{ fontSize: '0.9rem', fontWeight: 500, color: '#4A4540', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {user.nickname || user.username}
                        </span>
                      </div>
                      <span style={{ fontSize: '0.8125rem', color: '#9B9590', whiteSpace: 'nowrap', flexShrink: 0 }}>
                        {formatTimeAgo(user.createTime)}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <div style={{ textAlign: 'center', padding: '2.5rem 1rem' }}>
                  <div style={{ fontSize: '2rem', marginBottom: '0.5rem', opacity: 0.4 }}>📭</div>
                  <div style={{ fontSize: '0.9rem', color: '#9B9590' }}>暂无数据</div>
                </div>
              )}
            </div>
          </section>

          {/* Recent Products */}
          <section style={sectionCard}>
            <div style={sectionHeader}>
              <h2 style={sectionTitle}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#C39BD3" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" />
                  <polyline points="3.27 6.96 12 12.01 20.73 6.96" /><line x1="12" y1="22.08" x2="12" y2="12" />
                </svg>
                最近上架商品
              </h2>
            </div>
            <div style={{ padding: '1.25rem 1.5rem' }}>
              {productsLoading ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {Array.from({ length: 3 }).map((_, i) => (
                    <div key={i} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <div style={{ width: 36, height: 36, borderRadius: 10, background: 'rgba(229,224,219,0.4)' }} />
                        <div style={{ width: 96, height: 14, background: 'rgba(229,224,219,0.4)', borderRadius: 6 }} />
                      </div>
                      <div style={{ width: 64, height: 14, background: 'rgba(229,224,219,0.3)', borderRadius: 6 }} />
                    </div>
                  ))}
                </div>
              ) : recentProducts && recentProducts.length > 0 ? (
                <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', margin: 0, padding: 0 }}>
                  {recentProducts.map((product, idx) => (
                    <li key={product.id} style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      padding: '0.75rem 0', gap: '1rem',
                      borderBottom: idx < recentProducts.length - 1 ? '1px solid rgba(229,224,219,0.35)' : 'none',
                      transition: 'padding 0.15s ease',
                    }}
                      onMouseEnter={(e) => { e.currentTarget.style.paddingLeft = '0.5rem'; }}
                      onMouseLeave={(e) => { e.currentTarget.style.paddingLeft = '0'; }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', minWidth: 0 }}>
                        <span style={{
                          width: 36, height: 36, borderRadius: 10,
                          background: 'linear-gradient(135deg, rgba(249,115,22,0.08), rgba(195,155,211,0.06))',
                          display: 'flex', alignItems: 'center', justifyContent: 'center',
                          fontSize: '1rem', flexShrink: 0,
                        }}>
                          {getProductIcon(product.name)}
                        </span>
                        <span style={{ fontSize: '0.9rem', fontWeight: 500, color: '#4A4540', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {product.name}
                        </span>
                      </div>
                      <span style={{ fontSize: '0.8125rem', color: '#9B9590', whiteSpace: 'nowrap', flexShrink: 0 }}>
                        {formatTimeAgo(product.createTime)}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <div style={{ textAlign: 'center', padding: '2.5rem 1rem' }}>
                  <div style={{ fontSize: '2rem', marginBottom: '0.5rem', opacity: 0.4 }}>📭</div>
                  <div style={{ fontSize: '0.9rem', color: '#9B9590' }}>暂无数据</div>
                </div>
              )}
            </div>
          </section>
        </div>
      </div>

      <style>{`
        @keyframes dashPageIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes dashHeaderIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes dashGridIn { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes dashCardIn { from { opacity: 0; transform: translateY(20px) scale(0.97); } to { opacity: 1; transform: translateY(0) scale(1); } }
        @keyframes dashPulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.6; transform: scale(0.85); } }
        @media (max-width: 1280px) { }
        @media (max-width: 900px) { }
        @media (max-width: 520px) { }
      `}</style>
    </div>
  );
}
