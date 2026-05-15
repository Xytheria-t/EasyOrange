import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { StatCard } from './StatCard';
import TrendChart from './charts/TrendChart';
import ActivityHeatmap from './charts/ActivityHeatmap';
import TopProductsChart from './charts/TopProductsChart';
import {
  useDashboardStats,
  usePendingItems,
  useRecentUsers,
  useRecentProducts,
  useTrend,
  useUserActivityHeatmap,
  useTopProducts,
} from '../../hooks/useAdminDashboard';

function formatTimeAgo(dateString: string | null | undefined): string {
  if (!dateString) {return '未知';}
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);
  if (diffMins < 1) {return '刚刚';}
  if (diffMins < 60) {return `${diffMins}分钟前`;}
  if (diffHours < 24) {return `${diffHours}小时前`;}
  return `${diffDays}天前`;
}

function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour < 6) {return '夜深了';}
  if (hour < 12) {return '上午好';}
  if (hour < 14) {return '中午好';}
  if (hour < 18) {return '下午好';}
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
  default: '\u{1F4E6}', 教材: '\u{1F4DA}', 电子: '\u{1F4BB}', 手机: '\u{1F4F1}', 电脑: '\u{1F4BB}',
  自行车: '\u{1F6B4}', 衣服: '\u{1F455}', 鞋: '\u{1F45F}', 书: '\u{1F4D6}',
};

function getProductIcon(name: string): string {
  for (const key of Object.keys(PRODUCT_ICONS)) {
    if (key !== 'default' && name.includes(key)) {return PRODUCT_ICONS[key];}
  }
  return PRODUCT_ICONS.default;
}

const QUICK_ACTIONS = [
  {
    label: '商品审核',
    path: '/admin/products?status=0',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" />
        <polyline points="3.27 6.96 12 12.01 20.73 6.96" /><line x1="12" y1="22.08" x2="12" y2="12" />
      </svg>
    ),
    accent: '#F97316',
    glowColor: 'rgba(249,115,22,0.25)',
    pendingKey: 'pendingProducts' as const,
  },
  {
    label: '处理举报',
    path: '/admin/reports',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
        <line x1="12" y1="9" x2="12" y2="13" /><line x1="12" y1="17" x2="12.01" y2="17" />
      </svg>
    ),
    accent: '#F43F5E',
    glowColor: 'rgba(244,63,94,0.25)',
    pendingKey: 'pendingReports' as const,
  },
  {
    label: '订单管理',
    path: '/admin/orders',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
        <path d="M9 12h6m-6 4h6" />
      </svg>
    ),
    accent: '#FBBF24',
    glowColor: 'rgba(251,191,36,0.25)',
    pendingKey: 'pendingOrders' as const,
  },
  {
    label: '用户管理',
    path: '/admin/users',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" />
        <path d="M23 21v-2a4 4 0 00-3-3.87" /><path d="M16 3.13a4 4 0 010 7.75" />
      </svg>
    ),
    accent: '#C39BD3',
    glowColor: 'rgba(195,155,211,0.35)',
    pendingKey: undefined,
  },
  {
    label: '分类管理',
    path: '/admin/categories',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <line x1="8" y1="6" x2="21" y2="6" /><line x1="8" y1="12" x2="21" y2="12" />
        <line x1="8" y1="18" x2="21" y2="18" /><line x1="3" y1="6" x2="3.01" y2="6" />
        <line x1="3" y1="12" x2="3.01" y2="12" /><line x1="3" y1="18" x2="3.01" y2="18" />
      </svg>
    ),
    accent: '#10B981',
    glowColor: 'rgba(16,185,129,0.25)',
    pendingKey: undefined,
  },
  {
    label: '数据统计',
    path: '/admin/stats',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <line x1="18" y1="20" x2="18" y2="10" /><line x1="12" y1="20" x2="12" y2="4" />
        <line x1="6" y1="20" x2="6" y2="14" /><line x1="2" y1="20" x2="22" y2="20" />
      </svg>
    ),
    accent: '#6366F1',
    glowColor: 'rgba(99,102,241,0.25)',
    pendingKey: undefined,
  },
];

export default function DashboardPage() {
  const { data: stats, isLoading: statsLoading, isError: statsError, error: statsErr } = useDashboardStats();
  const { data: pendingItems, isLoading: pendingLoading, isError: pendingError, error: pendingErr } = usePendingItems();
  const { data: recentUsers, isLoading: usersLoading, isError: usersError, error: usersErr } = useRecentUsers(5);
  const { data: recentProducts, isLoading: productsLoading, isError: productsError, error: productsErr } = useRecentProducts(5);
  const { data: trend = [] } = useTrend();
  const { data: heatmap = [] } = useUserActivityHeatmap();
  const { data: topProducts = [] } = useTopProducts(10);

  const hasAnyError = statsError || pendingError || usersError || productsError;

  const hasPendingItems =
    pendingItems &&
    (pendingItems.pendingProducts > 0 ||
      pendingItems.pendingReports > 0 ||
      pendingItems.pendingOrders > 0);

  const totalPending =
    (pendingItems?.pendingProducts ?? 0) +
    (pendingItems?.pendingReports ?? 0) +
    (pendingItems?.pendingOrders ?? 0);

  const [currentTime, setCurrentTime] = useState(getCurrentTime());

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(getCurrentTime()), 30000);
    return () => clearInterval(timer);
  }, []);

  const sectionCard: React.CSSProperties = {
    background: 'rgba(255,255,255,0.65)',
    backdropFilter: 'blur(24px)',
    WebkitBackdropFilter: 'blur(24px)',
    border: '1px solid rgba(255,255,255,0.55)',
    borderRadius: 24,
    overflow: 'hidden',
    transition: 'all 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
    position: 'relative',
  };

  const sectionHeader: React.CSSProperties = {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '1.25rem 1.65rem',
    borderBottom: '1px solid rgba(229,224,219,0.3)',
    position: 'relative',
  };

  const sectionTitle: React.CSSProperties = {
    fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
    fontSize: '1.05rem', fontWeight: 700, color: '#2A2520',
    letterSpacing: '-0.02em',
    display: 'flex', alignItems: 'center', gap: '0.55rem',
  };

  return (
    <div style={{ position: 'relative', minHeight: '100%' }}>
      {/* ═══════════ ATMOSPHERIC BACKGROUND LAYER ═══════════ */}
      <div style={{
        position: 'fixed', inset: 0, zIndex: 0, pointerEvents: 'none', overflow: 'hidden',
        background: 'linear-gradient(175deg, #FFF9F5 0%, #FAF6F1 30%, #FDF8F4 60%, #FAF8F5 100%)',
      }}>
        {/* Blob 1 — warm orange top-left */}
        <div style={{
          position: 'absolute', top: '-8%', left: '-6%',
          width: '55vw', height: '55vw', maxWidth: '700px', maxHeight: '700px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(249,115,22,0.09) 0%, rgba(249,115,22,0.04) 40%, transparent 70%)',
          filter: 'blur(40px)', animation: 'dashBlobFloat1 20s ease-in-out infinite',
        }} />
        {/* Blob 2 — rose mid-right */}
        <div style={{
          position: 'absolute', top: '20%', right: '-10%',
          width: '50vw', height: '50vw', maxWidth: '650px', maxHeight: '650px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(251,113,133,0.07) 0%, rgba(251,113,133,0.03) 45%, transparent 70%)',
          filter: 'blur(45px)', animation: 'dashBlobFloat2 25s ease-in-out infinite',
        }} />
        {/* Blob 3 — purple bottom-left */}
        <div style={{
          position: 'absolute', bottom: '-5%', left: '15%',
          width: '45vw', height: '45vw', maxWidth: '580px', maxHeight: '580px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(195,155,211,0.08) 0%, rgba(195,155,211,0.03) 45%, transparent 70%)',
          filter: 'blur(50px)', animation: 'dashBlobFloat3 22s ease-in-out infinite reverse',
        }} />
        {/* Blob 4 — gold accent center-right */}
        <div style={{
          position: 'absolute', top: '45%', right: '25%',
          width: '30vw', height: '30vw', maxWidth: '400px', maxHeight: '400px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(251,191,36,0.05) 0%, transparent 65%)',
          filter: 'blur(35px)', animation: 'dashBlobFloat4 18s ease-in-out infinite',
        }} />
        {/* Aurora conic-gradient */}
        <div style={{
          position: 'absolute', inset: 0,
          background: 'conic-gradient(from 180deg at 50% 50%, rgba(249,115,22,0.04), rgba(251,113,133,0.03), rgba(195,155,211,0.04), rgba(251,191,36,0.02), rgba(249,115,22,0.04))',
          opacity: 0.6, mixBlendMode: 'screen',
        }} />
        {/* Noise texture overlay */}
        <div style={{
          position: 'absolute', inset: 0,
          backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E")`,
          opacity: 0.025,
        }} />
        {/* Subtle grid pattern */}
        <div style={{
          position: 'absolute', inset: 0,
          backgroundImage: 'linear-gradient(rgba(249,115,22,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(249,115,22,0.03) 1px, transparent 1px)',
          backgroundSize: '64px 64px',
          maskImage: 'radial-gradient(ellipse 80% 60% at 50% 40%, black 20%, transparent 70%)',
          WebkitMaskImage: 'radial-gradient(ellipse 80% 60% at 50% 40%, black 20%, transparent 70%)',
        }} />
        {/* Vignette */}
        <div style={{
          position: 'absolute', inset: 0,
          background: 'radial-gradient(ellipse 120% 100% at 50% 0%, transparent 50%, rgba(26,22,18,0.04) 100%)',
        }} />
      </div>

      {/* ═══════════ MAIN CONTENT ═══════════ */}
      <div style={{ position: 'relative', zIndex: 1 }}>
        {/* ─── HEADER SECTION ─── */}
        <header style={{ marginBottom: '2.5rem', animation: 'dashHeaderReveal 0.8s cubic-bezier(0.16, 1, 0.3, 1) both' }}>
          <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: '1.5rem', flexWrap: 'wrap' }}>
            <div>
              {/* Decorative accent line above title */}
              <div style={{
                display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.6rem',
                animation: 'dashAccentLineIn 0.6s ease-out 0.3s both',
              }}>
                <div style={{ width: 28, height: 3, borderRadius: 2, background: 'linear-gradient(90deg, #F97316, #FB7185)' }} />
                <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#F97316', opacity: 0.5 }} />
              </div>

              <h1 style={{
                fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                fontSize: 'clamp(2rem, 3.5vw, 3rem)', fontWeight: 700,
                color: '#1A1612', letterSpacing: '-0.03em', lineHeight: 1.15,
                display: 'flex', alignItems: 'baseline', flexWrap: 'wrap', gap: '0.35em',
              }}>
                <span>{getGreeting()}，</span>
                <span style={{
                  background: 'linear-gradient(135deg, #EA580C 0%, #F97316 30%, #FB7185 60%, #C39BD3 100%)',
                  WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text',
                }}>管理员</span>
              </h1>

              <p style={{
                marginTop: '0.6rem', fontSize: 'clamp(0.9rem, 1.1vw, 1.05rem)', color: '#9B9590',
                fontWeight: 400, letterSpacing: '0.02em', lineHeight: 1.5,
                maxWidth: '480px',
                fontFamily: "'LXGW WenKai', 'Noto Sans SC', sans-serif",
              }}>
                平台运营数据一览，掌控全局动态
              </p>

              {/* Decorative underline element */}
              <div style={{
                marginTop: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.5rem',
                animation: 'dashUnderlineExpand 0.8s ease-out 0.5s both',
              }}>
                <div style={{ width: 48, height: 2, borderRadius: 1, background: 'linear-gradient(90deg, #F97316, transparent)' }} />
                <div style={{ width: 6, height: 6, borderRadius: '50%', border: '1.5px solid #F97316', opacity: 0.35 }} />
              </div>
            </div>

            {/* Live clock pill */}
            <div style={{
              display: 'flex', alignItems: 'center', gap: '0.6rem',
              padding: '0.6rem 1.25rem', borderRadius: 9999,
              background: 'rgba(255,255,255,0.55)', backdropFilter: 'blur(16px)',
              WebkitBackdropFilter: 'blur(16px)',
              border: '1px solid rgba(255,255,255,0.6)',
              boxShadow: '0 4px 24px rgba(42,37,32,0.05), inset 0 1px 0 rgba(255,255,255,0.7)',
              fontSize: '0.85rem', color: '#6B6460', fontWeight: 500,
              fontFamily: "'LXGW WenKai', sans-serif",
              alignSelf: 'flex-end',
              animation: 'dashClockPop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) 0.4s both',
            }}>
              <span style={{
                width: 9, height: 9, borderRadius: '50%',
                background: '#10B981',
                boxShadow: '0 0 10px rgba(16,185,129,0.5), 0 0 20px rgba(16,185,129,0.2)',
                animation: 'dashPulse 2.5s ease-in-out infinite',
              }} />
              <span>{currentTime}</span>
            </div>
          </div>
        </header>

        {/* ─── ERROR BANNER ─── */}
        {hasAnyError && (
          <section style={{
            background: 'linear-gradient(135deg, rgba(244,63,94,0.06), rgba(244,63,94,0.02))',
            border: '1px solid rgba(244,63,94,0.12)',
            borderRadius: 20,
            padding: '1.1rem 1.4rem',
            marginBottom: '1.75rem',
            display: 'flex', alignItems: 'center', gap: '0.85rem',
            animation: 'dashSlideDown 0.5s cubic-bezier(0.16, 1, 0.3, 1) both',
            backdropFilter: 'blur(12px)', WebkitBackdropFilter: 'blur(12px)',
          }}>
            <div style={{
              width: 38, height: 38, borderRadius: 12,
              background: 'linear-gradient(135deg, rgba(244,63,94,0.12), rgba(244,63,94,0.05))',
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#E11D48" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: '0.88rem', fontWeight: 600, color: '#E11D48', marginBottom: '0.15rem' }}>数据加载失败</div>
              <div style={{ fontSize: '0.81rem', color: '#9B9590' }}>
                无法连接到服务器，请检查后端服务是否启动
                {(statsErr ?? pendingErr ?? usersErr ?? productsErr)?.message && ` · ${(statsErr ?? pendingErr ?? usersErr ?? productsErr)?.message}`}
              </div>
            </div>
            <button
              onClick={() => window.location.reload()}
              style={{
                padding: '0.5rem 1.15rem', borderRadius: 12,
                background: 'linear-gradient(135deg, #F43F5E, #E11D48)',
                color: '#fff', fontSize: '0.82rem', fontWeight: 600,
                border: 'none', cursor: 'pointer', whiteSpace: 'nowrap',
                boxShadow: '0 3px 12px rgba(244,63,94,0.28)',
                transition: 'all 0.25s ease',
                fontFamily: "'LXGW WenKai', sans-serif",
              }}
              onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-1px)'; e.currentTarget.style.boxShadow = '0 6px 20px rgba(244,63,94,0.35)'; }}
              onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 3px 12px rgba(244,63,94,0.28)'; }}
            >
              刷新页面
            </button>
          </section>
        )}

        {/* ─── STATS GRID ─── */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(5, 1fr)',
          gap: '1.4rem',
          marginBottom: '2.25rem',
          animation: 'dashGridReveal 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both',
        }}>
          {statsLoading ? (
            Array.from({ length: 5 }).map((_, i) => (
              <div key={i} style={{
                background: 'rgba(255,255,255,0.65)',
                backdropFilter: 'blur(24px)',
                WebkitBackdropFilter: 'blur(24px)',
                border: '1px solid rgba(255,255,255,0.55)',
                borderRadius: 24, padding: '1.85rem 1.65rem',
                animation: `dashStatCardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) ${0.08 + i * 0.06}s both`,
              }}>
                <div style={{ width: '42%', height: 13, background: 'rgba(229,224,219,0.4)', borderRadius: 6, marginBottom: '0.85rem' }} />
                <div style={{ width: '58%', height: 34, background: 'rgba(229,224,219,0.3)', borderRadius: 8 }} />
              </div>
            ))
          ) : (
            <>
              <div style={{ animation: `dashStatCardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.05s both` }}>
                <StatCard title="用户总数" value={stats?.totalUsers ?? 0} accent="orange"
                  icon={<svg fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="1.8" width="26" height="26"><path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" /></svg>}
                />
              </div>
              <div style={{ animation: `dashStatCardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.11s both` }}>
                <StatCard title="今日新增" value={stats?.todayNewUsers ?? 0} accent="rose"
                  icon={<svg fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="1.8" width="26" height="26"><path strokeLinecap="round" strokeLinejoin="round" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" /></svg>}
                />
              </div>
              <div style={{ animation: `dashStatCardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.17s both` }}>
                <StatCard title="商品总数" value={stats?.totalProducts ?? 0} accent="purple"
                  icon={<svg fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="1.8" width="26" height="26"><path strokeLinecap="round" strokeLinejoin="round" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" /></svg>}
                />
              </div>
              <div style={{ animation: `dashStatCardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.23s both` }}>
                <StatCard title="待审商品" value={stats?.pendingProducts ?? 0} accent="gold"
                  icon={<svg fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="1.8" width="26" height="26"><path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>}
                />
              </div>
              <div style={{ animation: `dashStatCardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.29s both` }}>
                <StatCard title="订单总量" value={stats?.totalOrders ?? 0} accent="emerald"
                  icon={<svg fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="1.8" width="26" height="26"><path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" /></svg>}
                />
              </div>
            </>
          )}
        </div>

        {/* ─── QUICK ACTIONS PANEL ─── */}
        <section style={{
          marginBottom: hasPendingItems ? '1.5rem' : '2rem',
          animation: 'dashActionsReveal 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) 0.25s both',
        }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: '0.65rem', marginBottom: '0.85rem',
            animation: 'dashFadeUp 0.5s ease-out 0.3s both',
          }}>
            <div style={{ width: 4, height: 18, borderRadius: 2, background: 'linear-gradient(180deg, #F97316, #FB7185)' }} />
            <span style={{
              fontSize: '0.82rem', fontWeight: 600, color: '#8B857E',
              letterSpacing: '0.06em', textTransform: 'uppercase',
              fontFamily: "'LXGW WenKai', sans-serif",
            }}>快捷操作</span>
          </div>

          <div style={{
            display: 'flex', flexWrap: 'wrap', gap: '0.7rem',
            padding: '1.1rem 1.35rem',
            background: 'rgba(255,255,255,0.5)',
            backdropFilter: 'blur(20px)',
            WebkitBackdropFilter: 'blur(20px)',
            border: '1px solid rgba(255,255,255,0.5)',
            borderRadius: 20,
            boxShadow: '0 2px 20px rgba(42,37,32,0.03)',
            position: 'relative', overflow: 'hidden',
          }}>
            {/* Subtle inner glow line */}
            <div style={{
              position: 'absolute', top: 0, left: '15%', right: '15%', height: 1,
              background: 'linear-gradient(90deg, transparent, rgba(249,115,22,0.12), transparent)',
            }} />

            {QUICK_ACTIONS.map((action, i) => {
              const pendingCount = action.pendingKey ? (pendingItems?.[action.pendingKey] ?? 0) : 0;
              return (
                <Link key={action.label} to={action.path}
                  style={{
                    display: 'inline-flex', alignItems: 'center', gap: '0.55rem',
                    padding: '0.6rem 1.15rem', borderRadius: 14,
                    background: 'rgba(255,255,255,0.7)',
                    border: '1px solid rgba(229,224,219,0.4)',
                    textDecoration: 'none', color: '#4A4540',
                    fontSize: '0.85rem', fontWeight: 500,
                    fontFamily: "'LXGW WenKai', sans-serif",
                    transition: 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
                    position: 'relative', overflow: 'hidden',
                    animation: `dashActionPop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) ${0.3 + i * 0.05}s both`,
                  }}
                  onMouseEnter={(e) => {
                    const el = e.currentTarget;
                    el.style.transform = 'translateY(-2px)';
                    el.style.borderColor = action.accent;
                    el.style.boxShadow = `0 8px 24px ${action.glowColor}, 0 2px 8px rgba(42,37,32,0.06)`;
                    el.style.background = 'rgba(255,255,255,0.9)';
                  }}
                  onMouseLeave={(e) => {
                    const el = e.currentTarget;
                    el.style.transform = 'translateY(0)';
                    el.style.borderColor = 'rgba(229,224,219,0.4)';
                    el.style.boxShadow = 'none';
                    el.style.background = 'rgba(255,255,255,0.7)';
                  }}
                >
                  <span style={{ color: action.accent, display: 'flex', flexShrink: 0 }}>{action.icon}</span>
                  <span>{action.label}</span>
                  {pendingCount > 0 && (
                    <span style={{
                      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                      minWidth: 20, height: 20, padding: '0 5px', borderRadius: 9999,
                      background: `linear-gradient(135deg, ${action.accent}, ${action.accent}dd)`,
                      color: '#fff', fontSize: '0.68rem', fontWeight: 700,
                      lineHeight: 1,
                    }}>{pendingCount}</span>
                  )}
                </Link>
              );
            })}
          </div>
        </section>

        {/* ─── PENDING ITEMS ALERT BAR ─── */}
        {hasPendingItems && (
          <section style={{
            ...sectionCard,
            border: '1px solid rgba(249,115,22,0.12)',
            marginBottom: '2rem',
            animation: 'dashAlertReveal 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.35s both',
            overflow: 'hidden',
          }}>
            {/* Animated border shimmer */}
            <div style={{
              position: 'absolute', top: 0, left: 0, right: 0, height: 2,
              background: 'linear-gradient(90deg, transparent, rgba(249,115,22,0.4), rgba(251,113,133,0.3), rgba(249,115,22,0.4), transparent)',
              backgroundSize: '200% 100%',
              animation: 'dashBorderShimmer 3s ease-in-out infinite',
            }} />

            <div style={sectionHeader}>
              <h2 style={sectionTitle}>
                <span style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  width: 28, height: 28, borderRadius: 8,
                  background: 'linear-gradient(135deg, rgba(249,115,22,0.12), rgba(249,115,22,0.05))',
                  marginRight: '0.2rem',
                }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#F97316" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                </span>
                待处理事项
                <span style={{
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                  minWidth: 24, height: 24, padding: '0 7px', borderRadius: 9999,
                  background: 'linear-gradient(135deg, #F97316, #EA580C)',
                  color: '#fff', fontSize: '0.72rem', fontWeight: 700, letterSpacing: 0,
                  boxShadow: '0 2px 8px rgba(249,115,22,0.3)',
                }}>
                  {totalPending}
                </span>
              </h2>
              <span style={{
                fontSize: '0.78rem', color: '#B5AEA8', fontWeight: 400,
                fontFamily: "'LXGW WenKai', sans-serif",
              }}>需要您的关注</span>
            </div>
            <div style={{ padding: '1.1rem 1.5rem' }}>
              {pendingLoading ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
                  {Array.from({ length: 3 }).map((_, i) => (
                    <div key={i} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <div style={{ width: 200, height: 16, background: 'rgba(229,224,219,0.35)', borderRadius: 8 }} />
                      <div style={{ width: 84, height: 32, background: 'rgba(229,224,219,0.25)', borderRadius: 10 }} />
                    </div>
                  ))}
                </div>
              ) : (
                <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '0.3rem', margin: 0, padding: 0 }}>
                  {pendingItems?.pendingProducts > 0 && (
                    <li style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      padding: '0.85rem 1.1rem', borderRadius: 14,
                      background: 'rgba(249,115,22,0.02)',
                      border: '1px solid rgba(249,115,22,0.06)',
                      transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
                    }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.background = 'rgba(249,115,22,0.05)';
                        e.currentTarget.style.paddingLeft = '1.3rem';
                        e.currentTarget.style.borderColor = 'rgba(249,115,22,0.12)';
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.background = 'rgba(249,115,22,0.02)';
                        e.currentTarget.style.paddingLeft = '0.85rem';
                        e.currentTarget.style.borderColor = 'rgba(249,115,22,0.06)';
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem' }}>
                        <span style={{
                          width: 9, height: 9, borderRadius: '50%', background: '#F97316', flexShrink: 0,
                          boxShadow: '0 0 10px rgba(249,115,22,0.45)',
                          animation: 'dashPulse 2s ease-in-out infinite',
                        }} />
                        <span style={{ fontSize: '0.9rem', color: '#4A4540', fontWeight: 500, fontFamily: "'LXGW WenKai', sans-serif" }}>
                          <strong style={{ fontWeight: 700, color: '#EA580C' }}>{pendingItems.pendingProducts}</strong> 个商品待审核
                        </span>
                      </div>
                      <Link to="/admin/products?status=0" style={{
                        display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
                        padding: '0.42rem 0.95rem', borderRadius: 9999,
                        fontSize: '0.8rem', fontWeight: 600, color: '#fff',
                        background: 'linear-gradient(135deg, #F97316, #EA580C)',
                        textDecoration: 'none', transition: 'all 0.25s ease',
                        letterSpacing: '0.01em', border: 'none', cursor: 'pointer',
                        fontFamily: "'LXGW WenKai', sans-serif",
                        boxShadow: '0 2px 8px rgba(249,115,22,0.2)',
                      }}
                        onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateX(3px)'; e.currentTarget.style.boxShadow = '0 4px 14px rgba(249,115,22,0.35)'; }}
                        onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateX(0)'; e.currentTarget.style.boxShadow = '0 2px 8px rgba(249,115,22,0.2)'; }}
                      >
                        立即处理
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14M12 5l7 7-7 7" /></svg>
                      </Link>
                    </li>
                  )}
                  {pendingItems?.pendingReports > 0 && (
                    <li style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      padding: '0.85rem 1.1rem', borderRadius: 14,
                      background: 'rgba(244,63,94,0.02)',
                      border: '1px solid rgba(244,63,94,0.06)',
                      transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
                    }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.background = 'rgba(244,63,94,0.05)';
                        e.currentTarget.style.paddingLeft = '1.3rem';
                        e.currentTarget.style.borderColor = 'rgba(244,63,94,0.12)';
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.background = 'rgba(244,63,94,0.02)';
                        e.currentTarget.style.paddingLeft = '0.85rem';
                        e.currentTarget.style.borderColor = 'rgba(244,63,94,0.06)';
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem' }}>
                        <span style={{
                          width: 9, height: 9, borderRadius: '50%', background: '#F43F5E', flexShrink: 0,
                          boxShadow: '0 0 10px rgba(244,63,94,0.45)',
                          animation: 'dashPulse 2s ease-in-out infinite',
                        }} />
                        <span style={{ fontSize: '0.9rem', color: '#4A4540', fontWeight: 500, fontFamily: "'LXGW WenKai', sans-serif" }}>
                          <strong style={{ fontWeight: 700, color: '#E11D48' }}>{pendingItems.pendingReports}</strong> 条举报待处理
                        </span>
                      </div>
                      <Link to="/admin/reports" style={{
                        display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
                        padding: '0.42rem 0.95rem', borderRadius: 9999,
                        fontSize: '0.8rem', fontWeight: 600, color: '#fff',
                        background: 'linear-gradient(135deg, #F43F5E, #E11D48)',
                        textDecoration: 'none', transition: 'all 0.25s ease',
                        letterSpacing: '0.01em', border: 'none', cursor: 'pointer',
                        fontFamily: "'LXGW WenKai', sans-serif",
                        boxShadow: '0 2px 8px rgba(244,63,94,0.2)',
                      }}
                        onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateX(3px)'; e.currentTarget.style.boxShadow = '0 4px 14px rgba(244,63,94,0.35)'; }}
                        onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateX(0)'; e.currentTarget.style.boxShadow = '0 2px 8px rgba(244,63,94,0.2)'; }}
                      >
                        立即处理
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14M12 5l7 7-7 7" /></svg>
                      </Link>
                    </li>
                  )}
                  {pendingItems?.pendingOrders > 0 && (
                    <li style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      padding: '0.85rem 1.1rem', borderRadius: 14,
                      background: 'rgba(251,191,36,0.02)',
                      border: '1px solid rgba(251,191,36,0.06)',
                      transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
                    }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.background = 'rgba(251,191,36,0.05)';
                        e.currentTarget.style.paddingLeft = '1.3rem';
                        e.currentTarget.style.borderColor = 'rgba(251,191,36,0.12)';
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.background = 'rgba(251,191,36,0.02)';
                        e.currentTarget.style.paddingLeft = '0.85rem';
                        e.currentTarget.style.borderColor = 'rgba(251,191,36,0.06)';
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem' }}>
                        <span style={{
                          width: 9, height: 9, borderRadius: '50%', background: '#FBBF24', flexShrink: 0,
                          boxShadow: '0 0 10px rgba(251,191,36,0.45)',
                          animation: 'dashPulse 2s ease-in-out infinite',
                        }} />
                        <span style={{ fontSize: '0.9rem', color: '#4A4540', fontWeight: 500, fontFamily: "'LXGW WenKai', sans-serif" }}>
                          <strong style={{ fontWeight: 700, color: '#D97706' }}>{pendingItems.pendingOrders}</strong> 笔待处理订单
                        </span>
                      </div>
                      <Link to="/admin/orders" style={{
                        display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
                        padding: '0.42rem 0.95rem', borderRadius: 9999,
                        fontSize: '0.8rem', fontWeight: 600, color: '#fff',
                        background: 'linear-gradient(135deg, #FBBF24, #D97706)',
                        textDecoration: 'none', transition: 'all 0.25s ease',
                        letterSpacing: '0.01em', border: 'none', cursor: 'pointer',
                        fontFamily: "'LXGW WenKai', sans-serif",
                        boxShadow: '0 2px 8px rgba(251,191,36,0.2)',
                      }}
                        onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateX(3px)'; e.currentTarget.style.boxShadow = '0 4px 14px rgba(251,191,36,0.35)'; }}
                        onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateX(0)'; e.currentTarget.style.boxShadow = '0 2px 8px rgba(251,191,36,0.2)'; }}
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

        {/* ─── TWO-COLUMN CONTENT AREA ─── */}
        <div style={{
          display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)',
          gap: '1.5rem', animation: 'dashContentReveal 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.4s both',
        }}>
          {/* Recent Users */}
          <section style={sectionCard}>
            <div style={sectionHeader}>
              <h2 style={sectionTitle}>
                <span style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  width: 28, height: 28, borderRadius: 8,
                  background: 'linear-gradient(135deg, rgba(249,115,22,0.1), rgba(249,115,22,0.03))',
                  marginRight: '0.15rem',
                }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#F97316" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" />
                    <path d="M23 21v-2a4 4 0 00-3-3.87" /><path d="M16 3.13a4 4 0 010 7.75" />
                  </svg>
                </span>
                最近注册用户
              </h2>
              <Link to="/admin/users" style={{
                fontSize: '0.78rem', color: '#F97316', fontWeight: 500,
                textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.25rem',
                transition: 'gap 0.2s ease', fontFamily: "'LXGW WenKai', sans-serif",
              }}
                onMouseEnter={(e) => { e.currentTarget.style.gap = '0.5rem'; }}
                onMouseLeave={(e) => { e.currentTarget.style.gap = '0.25rem'; }}
              >
                查看全部
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14M12 5l7 7-7 7" /></svg>
              </Link>
            </div>
            <div style={{ padding: '1.15rem 1.5rem' }}>
              {usersLoading ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.8rem' }}>
                  {Array.from({ length: 3 }).map((_, i) => (
                    <div key={i} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <div style={{ width: 38, height: 38, borderRadius: 12, background: 'rgba(229,224,219,0.35)' }} />
                        <div style={{ width: 88, height: 14, background: 'rgba(229,224,219,0.3)', borderRadius: 6 }} />
                      </div>
                      <div style={{ width: 66, height: 14, background: 'rgba(229,224,219,0.25)', borderRadius: 6 }} />
                    </div>
                  ))}
                </div>
              ) : recentUsers && recentUsers.length > 0 ? (
                <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', margin: 0, padding: 0 }}>
                  {recentUsers.map((user, idx) => (
                    <li key={user.userId} style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      padding: '0.72rem 0.4rem', gap: '1rem',
                      borderBottom: idx < recentUsers.length - 1 ? '1px solid rgba(229,224,219,0.28)' : 'none',
                      transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
                      borderRadius: 10,
                    }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.paddingLeft = '0.85rem';
                        e.currentTarget.style.background = 'rgba(249,115,22,0.025)';
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.paddingLeft = '0.4rem';
                        e.currentTarget.style.background = 'transparent';
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', minWidth: 0 }}>
                        <span style={{
                          width: 38, height: 38, borderRadius: 12,
                          display: 'flex', alignItems: 'center', justifyContent: 'center',
                          fontSize: '0.85rem', fontWeight: 700, color: '#fff',
                          fontFamily: "'Playfair Display', serif",
                          background: AVATAR_GRADIENTS[idx % AVATAR_GRADIENTS.length],
                          flexShrink: 0,
                          boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                        }}>
                          {(user.nickname || user.username || '?').charAt(0).toUpperCase()}
                        </span>
                        <div style={{ minWidth: 0 }}>
                          <span style={{
                            fontSize: '0.9rem', fontWeight: 500, color: '#4A4540',
                            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', display: 'block',
                            fontFamily: "'LXGW WenKai', sans-serif",
                          }}>
                            {user.nickname || user.username}
                          </span>
                          <span style={{ fontSize: '0.72rem', color: '#B5AEA8', fontFamily: "'LXGW WenKai', sans-serif" }}>
                            @{user.username}
                          </span>
                        </div>
                      </div>
                      <span style={{
                        display: 'inline-flex', alignItems: 'center',
                        padding: '0.2rem 0.6rem', borderRadius: 9999,
                        fontSize: '0.73rem', fontWeight: 500,
                        background: 'linear-gradient(135deg, rgba(249,115,22,0.06), rgba(195,155,211,0.04))',
                        color: '#9B9590', letterSpacing: '0.01em',
                        whiteSpace: 'nowrap', flexShrink: 0,
                        fontFamily: "'LXGW WenKai', sans-serif",
                      }}>
                        {formatTimeAgo(user.createTime)}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <div style={{ textAlign: 'center', padding: '2.75rem 1rem' }}>
                  <div style={{
                    width: 64, height: 64, margin: '0 auto 0.85rem', borderRadius: 18,
                    background: 'linear-gradient(135deg, rgba(249,115,22,0.06), rgba(195,155,211,0.04))',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '1.6rem',
                  }}>{'\u{1F4ED}'}</div>
                  <div style={{ fontFamily: "'Playfair Display', serif", fontSize: '0.98rem', fontWeight: 600, color: '#8B857E', marginBottom: '0.3rem' }}>暂无用户记录</div>
                  <div style={{ fontSize: '0.82rem', color: '#B5AEA8', fontFamily: "'LXGW WenKai', sans-serif" }}>当前暂无新注册用户</div>
                </div>
              )}
            </div>
          </section>

          {/* Recent Products */}
          <section style={sectionCard}>
            <div style={sectionHeader}>
              <h2 style={sectionTitle}>
                <span style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  width: 28, height: 28, borderRadius: 8,
                  background: 'linear-gradient(135deg, rgba(195,155,211,0.12), rgba(195,155,211,0.03))',
                  marginRight: '0.15rem',
                }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#C39BD3" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" />
                    <polyline points="3.27 6.96 12 12.01 20.73 6.96" /><line x1="12" y1="22.08" x2="12" y2="12" />
                  </svg>
                </span>
                最近上架商品
              </h2>
              <Link to="/admin/products" style={{
                fontSize: '0.78rem', color: '#C39BD3', fontWeight: 500,
                textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.25rem',
                transition: 'gap 0.2s ease', fontFamily: "'LXGW WenKai', sans-serif",
              }}
                onMouseEnter={(e) => { e.currentTarget.style.gap = '0.5rem'; }}
                onMouseLeave={(e) => { e.currentTarget.style.gap = '0.25rem'; }}
              >
                查看全部
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14M12 5l7 7-7 7" /></svg>
              </Link>
            </div>
            <div style={{ padding: '1.15rem 1.5rem' }}>
              {productsLoading ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.8rem' }}>
                  {Array.from({ length: 3 }).map((_, i) => (
                    <div key={i} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <div style={{ width: 38, height: 38, borderRadius: 12, background: 'rgba(229,224,219,0.35)' }} />
                        <div style={{ width: 104, height: 14, background: 'rgba(229,224,219,0.3)', borderRadius: 6 }} />
                      </div>
                      <div style={{ width: 66, height: 14, background: 'rgba(229,224,219,0.25)', borderRadius: 6 }} />
                    </div>
                  ))}
                </div>
              ) : recentProducts && recentProducts.length > 0 ? (
                <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', margin: 0, padding: 0 }}>
                  {recentProducts.map((product, idx) => (
                    <li key={product.productId} style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      padding: '0.72rem 0.4rem', gap: '1rem',
                      borderBottom: idx < recentProducts.length - 1 ? '1px solid rgba(229,224,219,0.28)' : 'none',
                      transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
                      borderRadius: 10,
                    }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.paddingLeft = '0.85rem';
                        e.currentTarget.style.background = 'rgba(195,155,211,0.025)';
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.paddingLeft = '0.4rem';
                        e.currentTarget.style.background = 'transparent';
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', minWidth: 0 }}>
                        <span style={{
                          width: 38, height: 38, borderRadius: 12,
                          background: 'linear-gradient(135deg, rgba(249,115,22,0.06), rgba(195,155,211,0.05))',
                          display: 'flex', alignItems: 'center', justifyContent: 'center',
                          fontSize: '1.1rem', flexShrink: 0,
                          boxShadow: '0 1px 4px rgba(0,0,0,0.03)',
                        }}>
                          {getProductIcon(product.name)}
                        </span>
                        <div style={{ minWidth: 0 }}>
                          <span style={{
                            fontSize: '0.9rem', fontWeight: 500, color: '#4A4540',
                            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', display: 'block',
                            fontFamily: "'LXGW WenKai', sans-serif",
                          }}>
                            {product.name}
                          </span>
                          {product.price != null && (
                            <span style={{
                              fontSize: '0.72rem', color: '#F97316', fontWeight: 600,
                              fontFamily: "'Cormorant Garamond', 'Playfair Display', serif",
                            }}>
                              ¥{Number(product.price).toLocaleString('zh-CN')}
                            </span>
                          )}
                        </div>
                      </div>
                      <span style={{
                        display: 'inline-flex', alignItems: 'center',
                        padding: '0.2rem 0.6rem', borderRadius: 9999,
                        fontSize: '0.73rem', fontWeight: 500,
                        background: 'linear-gradient(135deg, rgba(249,115,22,0.06), rgba(195,155,211,0.04))',
                        color: '#9B9590', letterSpacing: '0.01em',
                        whiteSpace: 'nowrap', flexShrink: 0,
                        fontFamily: "'LXGW WenKai', sans-serif",
                      }}>
                        {formatTimeAgo(product.createTime)}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <div style={{ textAlign: 'center', padding: '2.75rem 1rem' }}>
                  <div style={{
                    width: 64, height: 64, margin: '0 auto 0.85rem', borderRadius: 18,
                    background: 'linear-gradient(135deg, rgba(195,155,211,0.07), rgba(249,115,22,0.04))',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '1.6rem',
                  }}>{'\u{1F4E6}'}</div>
                  <div style={{ fontFamily: "'Playfair Display', serif", fontSize: '0.98rem', fontWeight: 600, color: '#8B857E', marginBottom: '0.3rem' }}>暂无商品记录</div>
                  <div style={{ fontSize: '0.82rem', color: '#B5AEA8', fontFamily: "'LXGW WenKai', sans-serif" }}>当前暂无新上架商品</div>
                </div>
              )}
            </div>
          </section>
        </div>

        {/* ─── CHARTS SECTION — 3-column grid ─── */}
        <div className="dash-charts-grid-responsive" style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '1.5rem',
          marginTop: '1.5rem',
          animation: 'dashContentReveal 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.5s both',
        }}>
          {/* Trend chart */}
          <section style={sectionCard}>
            <div style={sectionHeader}>
              <h2 style={sectionTitle}>
                <span style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  width: 28, height: 28, borderRadius: 8,
                  background: 'linear-gradient(135deg, rgba(249,115,22,0.1), rgba(249,115,22,0.03))',
                  marginRight: '0.15rem',
                }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#F97316" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                  </svg>
                </span>
                趋势概览
              </h2>
              <Link to="/admin/stats" style={{
                fontSize: '0.78rem', color: '#F97316', fontWeight: 500,
                textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.25rem',
                transition: 'gap 0.2s ease', fontFamily: "'LXGW WenKai', sans-serif",
              }}
                onMouseEnter={(e) => { e.currentTarget.style.gap = '0.5rem'; }}
                onMouseLeave={(e) => { e.currentTarget.style.gap = '0.25rem'; }}
              >
                详情
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14M12 5l7 7-7 7" /></svg>
              </Link>
            </div>
            <div style={{ padding: '0.75rem 0.5rem' }}>
              <TrendChart data={trend} compact height={180} />
            </div>
          </section>

          {/* Activity heatmap */}
          <section style={sectionCard}>
            <div style={sectionHeader}>
              <h2 style={sectionTitle}>
                <span style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  width: 28, height: 28, borderRadius: 8,
                  background: 'linear-gradient(135deg, rgba(251,113,133,0.12), rgba(251,113,133,0.03))',
                  marginRight: '0.15rem',
                }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#FB7185" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" />
                    <rect x="14" y="14" width="7" height="7" /><rect x="3" y="14" width="7" height="7" />
                  </svg>
                </span>
                用户活跃时段
              </h2>
            </div>
            <div style={{ padding: '0.5rem' }}>
              <ActivityHeatmap data={heatmap} />
            </div>
          </section>

          {/* Top products */}
          <section style={sectionCard}>
            <div style={sectionHeader}>
              <h2 style={sectionTitle}>
                <span style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  width: 28, height: 28, borderRadius: 8,
                  background: 'linear-gradient(135deg, rgba(195,155,211,0.12), rgba(195,155,211,0.03))',
                  marginRight: '0.15rem',
                }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#C39BD3" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M6 9l6 6 6-6" />
                  </svg>
                </span>
                Top 浏览量商品
              </h2>
            </div>
            <div style={{ padding: '0.5rem 0.75rem' }}>
              <TopProductsChart data={topProducts} />
            </div>
          </section>
        </div>
      </div>

      {/* ═══════════ ANIMATIONS & RESPONSIVE STYLES ═══════════ */}
      <style>{`
        @keyframes dashHeaderReveal {
          from { opacity: 0; transform: translateY(28px); }
          to { opacity: 1; transform: translateY(0); }
        }

        @keyframes dashGridReveal {
          from { opacity: 0; transform: translateY(20px) scale(0.98); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }

        @keyframes dashStatCardIn {
          from { opacity: 0; transform: translateY(24px) scale(0.95); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }

        @keyframes dashActionsReveal {
          from { opacity: 0; transform: translateY(16px); }
          to { opacity: 1; transform: translateY(0); }
        }

        @keyframes dashActionPop {
          from { opacity: 0; transform: scale(0.85) translateY(8px); }
          to { opacity: 1; transform: scale(1) translateY(0); }
        }

        @keyframes dashAlertReveal {
          from { opacity: 0; transform: translateY(16px) scale(0.98); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }

        @keyframes dashContentReveal {
          from { opacity: 0; transform: translateY(20px); }
          to { opacity: 1; transform: translateY(0); }
        }

        @keyframes dashSlideDown {
          from { opacity: 0; transform: translateY(-12px); }
          to { opacity: 1; transform: translateY(0); }
        }

        @keyframes dashFadeUp {
          from { opacity: 0; transform: translateY(8px); }
          to { opacity: 1; transform: translateY(0); }
        }

        @keyframes dashClockPop {
          from { opacity: 0; transform: scale(0.8); }
          to { opacity: 1; transform: scale(1); }
        }

        @keyframes dashAccentLineIn {
          from { opacity: 0; transform: scaleX(0); transform-origin: left; }
          to { opacity: 1; transform: scaleX(1); transform-origin: left; }
        }

        @keyframes dashUnderlineExpand {
          from { opacity: 0; transform: scaleX(0); transform-origin: left; }
          to { opacity: 1; transform: scaleX(1); transform-origin: left; }
        }

        @keyframes dashPulse {
          0%, 100% { opacity: 1; transform: scale(1); }
          50% { opacity: 0.5; transform: scale(0.8); }
        }

        @keyframes dashBorderShimmer {
          0%, 100% { background-position: 200% 0; }
          50% { background-position: -200% 0; }
        }

        /* Blob floating animations */
        @keyframes dashBlobFloat1 {
          0%, 100% { transform: translate(0, 0) scale(1); }
          33% { transform: translate(30px, -20px) scale(1.05); }
          66% { transform: translate(-15px, 15px) scale(0.97); }
        }

        @keyframes dashBlobFloat2 {
          0%, 100% { transform: translate(0, 0) scale(1); }
          33% { transform: translate(-25px, 20px) scale(1.04); }
          66% { transform: translate(20px, -15px) scale(0.96); }
        }

        @keyframes dashBlobFloat3 {
          0%, 100% { transform: translate(0, 0) scale(1); }
          33% { transform: translate(20px, -15px) scale(1.03); }
          66% { transform: translate(-20px, 10px) scale(0.98); }
        }

        @keyframes dashBlobFloat4 {
          0%, 100% { transform: translate(0, 0) scale(1); }
          50% { transform: translate(-15px, -12px) scale(1.06); }
        }

        @keyframes adminShineSweep {
          from { transform: translateX(-100%); }
          to { transform: translateX(200%); }
        }

        /* ══════ REDUCED MOTION ══════ */
        @media (prefers-reduced-motion: reduce) {
          *, *::before, *::after {
            animation-duration: 0.01ms !important;
            animation-iteration-count: 1 !important;
            transition-duration: 0.01ms !important;
          }
        }

        /* ══════ CHARTS GRID RESPONSIVE ══════ */
        @media (max-width: 1280px) {
          .dash-charts-grid-responsive {
            grid-template-columns: repeat(2, 1fr) !important;
          }
        }
        @media (max-width: 900px) {
          .dash-charts-grid-responsive {
            grid-template-columns: 1fr !important;
          }
        }

        /* ══════ RESPONSIVE: ≤1280px ══════ */
        @media (max-width: 1280px) {
          .dash-stats-grid-responsive {
            grid-template-columns: repeat(3, 1fr) !important;
          }
          .dash-stats-grid-responsive > *:nth-child(4),
          .dash-stats-grid-responsive > *:nth-child(5) {
            grid-column: span 1;
          }
        }

        /* ══════ RESPONSIVE: ≤900px ══════ */
        @media (max-width: 900px) {
          .dash-content-grid-responsive {
            grid-template-columns: 1fr !important;
          }
          .dash-actions-responsive {
            justify-content: center;
          }
        }

        /* ══════ RESPONSIVE: ≤520px ══════ */
        @media (max-width: 520px) {
          .dash-stats-mobile {
            grid-template-columns: repeat(2, 1fr) !important;
          }
          .dash-header-mobile {
            flex-direction: column;
            align-items: flex-start !important;
          }
          .dash-clock-mobile {
            align-self: flex-start !important;
          }
          .dash-actions-mobile {
            justify-content: stretch;
          }
          .dash-action-link-mobile {
            flex: 1 1 calc(50% - 0.35rem);
            justify-content: center;
          }
        }
      `}</style>
    </div>
  );
}
