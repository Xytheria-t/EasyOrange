import { Link } from 'react-router-dom';
import { formatRelativeTime } from '@/utils';

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

interface User {
  userId: string;
  nickname?: string;
  username: string;
  createTime?: string;
}

interface Product {
  productId: string;
  name: string;
  price?: number;
  createTime?: string;
}

interface RecentItemsListsProps {
  recentUsers?: User[];
  recentProducts?: Product[];
  usersLoading: boolean;
  productsLoading: boolean;
}

export function RecentItemsLists({
  recentUsers,
  recentProducts,
  usersLoading,
  productsLoading,
}: RecentItemsListsProps) {
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
                    {formatRelativeTime(user.createTime ?? '')}
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
                    {formatRelativeTime(product.createTime ?? '')}
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
  );
}
