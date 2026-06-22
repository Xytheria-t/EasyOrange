import { Link } from 'react-router-dom';

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

interface PendingItems {
  pendingProducts?: number;
  pendingReports?: number;
  pendingOrders?: number;
}

interface QuickActionsPanelProps {
  pendingItems?: PendingItems;
}

export function QuickActionsPanel({ pendingItems }: QuickActionsPanelProps) {
  return (
    <section style={{
      marginBottom: '2rem',
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
  );
}
