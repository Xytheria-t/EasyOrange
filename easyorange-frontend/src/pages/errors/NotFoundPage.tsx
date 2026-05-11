import { Link, useNavigate } from 'react-router-dom';

function NotFoundPage() {
  const navigate = useNavigate();

  const suggestions = [
    {
      to: '/',
      icon: (
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z" /><polyline points="9 22 9 12 15 12 15 22" />
        </svg>
      ),
      label: '浏览首页',
      desc: '发现最新上架的好物',
      color: '#F97316', bg: 'rgba(249,115,22,0.10)',
    },
    {
      to: '/search',
      icon: (
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
      ),
      label: '搜索商品',
      desc: '输入关键词找到你想要的',
      color: '#C39BD3', bg: 'rgba(195,155,211,0.12)',
    },
    {
      to: '/products',
      icon: (
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" /><polyline points="17 6 23 6 23 12" />
        </svg>
      ),
      label: '热门商品',
      desc: '看看大家都在关注什么',
      color: '#FB7185', bg: 'rgba(251,113,133,0.10)',
    },
  ];

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: '1rem', position: 'relative', overflow: 'hidden',
      background: `
        linear-gradient(180deg, #FAF8F5 0%, #FFF9F5 30%, #FEF5F3 60%, #FAF4FF 100%)
      `,
    }}>
      {/* Animated background orbs */}
      <div style={{
        position: 'absolute', inset: 0, pointerEvents: 'none', overflow: 'hidden',
      }}>
        {/* Large orange orb - top right */}
        <div style={{
          position: 'absolute', top: '-120px', right: '-80px',
          width: 420, height: 420, borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(249,115,22,0.09) 0%, rgba(251,146,60,0.04) 50%, transparent 70%)',
          animation: 'nfFloatOrbA 18s ease-in-out infinite',
        }} />
        {/* Rose orb - bottom left */}
        <div style={{
          position: 'absolute', bottom: '-80px', left: '-100px',
          width: 360, height: 360, borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(251,113,133,0.07) 0%, rgba(195,155,211,0.05) 50%, transparent 70%)',
          animation: 'nfFloatOrbB 22s ease-in-out infinite reverse',
        }} />
        {/* Purple orb - center */}
        <div style={{
          position: 'absolute', top: '35%', left: '50%', transform: 'translateX(-50%)',
          width: 280, height: 280, borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(195,155,211,0.06) 0%, transparent 65%)',
          animation: 'nfFloatOrbC 25s ease-in-out infinite',
        }} />

        {/* Floating geometric shapes */}
        <div style={{
          position: 'absolute', top: '14%', left: '12%',
          width: 20, height: 20, borderRadius: 6,
          background: 'linear-gradient(135deg, rgba(249,115,22,0.15), rgba(251,146,60,0.08))',
          transform: 'rotate(15deg)', animation: 'nfShapeFloat 8s ease-in-out infinite',
        }} />
        <div style={{
          position: 'absolute', top: '28%', right: '18%',
          width: 14, height: 14, borderRadius: '50%',
          background: 'linear-gradient(135deg, rgba(195,155,211,0.18), rgba(216,180,254,0.08))',
          animation: 'nfShapeFloat 10s ease-in-out infinite 1.5s',
        }} />
        <div style={{
          position: 'absolute', bottom: '28%', left: '18%',
          width: 26, height: 26, borderRadius: 8,
          background: 'linear-gradient(135deg, rgba(251,113,133,0.12), rgba(244,63,94,0.05))',
          transform: 'rotate(-20deg)', animation: 'nfShapeFloat 12s ease-in-out infinite 3s',
        }} />
        <div style={{
          position: 'absolute', bottom: '18%', right: '12%',
          width: 16, height: 16,
          background: 'linear-gradient(135deg, rgba(251,191,36,0.14), rgba(249,115,22,0.06))',
          clipPath: 'polygon(50% 0%, 100% 100%, 0% 100%)',
          animation: 'nfShapeFloat 9s ease-in-out infinite 2s',
        }} />
      </div>

      <div style={{ position: 'relative', zIndex: 1, textAlign: 'center', maxWidth: 520, width: '100%' }}>
        {/* 404 Illustration */}
        <div style={{ marginBottom: '2rem', animation: 'nfIllustrationIn 0.8s cubic-bezier(0.16, 1, 0.3, 1) both' }}>
          <div style={{ position: 'relative', display: 'inline-block' }}>
            {/* Big 404 text */}
            <div style={{
              fontFamily: "'Playfair Display', serif",
              fontSize: 'clamp(7rem, 18vw, 11rem)', fontWeight: 800,
              lineHeight: 1, letterSpacing: '-0.03em',
              position: 'relative',
              animation: 'nfTextFloat 5s ease-in-out infinite',
            }}>
              {/* Layered gradient numbers */}
              <span style={{ position: 'relative', zIndex: 2 }}>
                <span style={{
                  background: 'linear-gradient(135deg, #F97316 0%, #FB7185 35%, #C39BD3 75%, #D8B4FE 100%)',
                  WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text',
                }}>4</span>
                <span style={{
                  background: 'linear-gradient(135deg, #FB7185 0%, #F43F5E 45%, #F97316 100%)',
                  WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text',
                }}>0</span>
                <span style={{
                  background: 'linear-gradient(135deg, #C39BD3 0%, #FB7185 35%, #F97316 75%, #EA580C 100%)',
                  WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text',
                }}>4</span>
              </span>

              {/* Blur glow behind */}
              <span style={{
                position: 'absolute', inset: 0,
                WebkitTextStroke: '0 transparent',
              }}>
                <span style={{
                  background: 'linear-gradient(135deg, #F97316 0%, #FB7185 35%, #C39BD3 75%, #D8B4FE 100%)',
                  WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text',
                  filter: 'blur(28px)', opacity: 0.55,
                }}>404</span>
              </span>
            </div>
          </div>
        </div>

        {/* Title */}
        <h1 style={{
          fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
          fontSize: 'clamp(1.5rem, 3vw, 1.85rem)', fontWeight: 700,
          color: '#2A2520', marginBottom: '0.65rem', letterSpacing: '-0.02em',
          animation: 'nfFadeUp 0.6s ease-out 0.15s both',
        }}>
          页面走丢了
        </h1>

        {/* Subtitle */}
        <p style={{
          fontSize: 'clamp(0.92rem, 1.5vw, 1.02rem)', color: '#8B857E',
          lineHeight: 1.7, maxWidth: 380, margin: '0 auto 2.25rem',
          animation: 'nfFadeUp 0.6s ease-out 0.25s both',
        }}>
          抱歉，您访问的页面不存在或已被移除。<br />别担心，这里有几条路可以帮你找到方向。
        </p>

        {/* Action buttons */}
        <div style={{
          display: 'flex', gap: '0.8rem', justifyContent: 'center',
          flexWrap: 'wrap', marginBottom: '2.5rem',
          animation: 'nfFadeUp 0.6s ease-out 0.35s both',
        }}>
          <button
            onClick={() => navigate(-1)}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: '0.45rem',
              padding: '0.72rem 1.5rem', borderRadius: 14,
              border: '1.5px solid #E5E0DB', background: '#fff',
              fontSize: '0.9rem', fontWeight: 600, color: '#4A4540',
              cursor: 'pointer', transition: 'all 0.25s ease',
              boxShadow: '0 1px 3px rgba(42,37,32,0.04)',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(229,224,219,0.3)'; e.currentTarget.style.transform = 'translateY(-2px)'; }}
            onMouseLeave={(e) => { e.currentTarget.style.background = '#fff'; e.currentTarget.style.transform = 'translateY(0)'; }}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            返回上页
          </button>
          <Link
            to="/"
            style={{
              display: 'inline-flex', alignItems: 'center', gap: '0.45rem',
              padding: '0.72rem 1.6rem', borderRadius: 14,
              border: 'none',
              background: 'linear-gradient(135deg, #F97316, #FB7185)',
              color: '#fff', fontSize: '0.9rem', fontWeight: 600,
              textDecoration: 'none', cursor: 'pointer',
              transition: 'all 0.25s ease',
              boxShadow: '0 4px 16px rgba(249,115,22,0.3)',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-2px)'; e.currentTarget.style.boxShadow = '0 8px 24px rgba(249,115,22,0.4)'; }}
            onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 4px 16px rgba(249,115,22,0.3)'; }}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z" /><polyline points="9 22 9 12 15 12 15 22" />
            </svg>
            回到首页
          </Link>
        </div>

        {/* AI Suggestions card */}
        <div style={{
          background: 'rgba(255,255,255,0.72)',
          backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)',
          border: '1px solid rgba(255,255,255,0.65)',
          borderRadius: 22, padding: '1.5rem',
          boxShadow: '0 4px 24px rgba(42,37,32,0.05), 0 1px 3px rgba(42,37,32,0.03)',
          maxWidth: 440, margin: '0 auto',
          animation: 'nfFadeUp 0.6s ease-out 0.5s both',
        }}>
          {/* Card header */}
          <div style={{
            display: 'flex', alignItems: 'center', gap: '0.7rem',
            marginBottom: '1.1rem', paddingBottom: '1rem',
            borderBottom: '1px solid rgba(229,224,219,0.4)',
          }}>
            <div style={{
              width: 38, height: 38, borderRadius: 12,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              background: 'linear-gradient(135deg, #F97316, #FB7185)',
              color: '#fff', boxShadow: '0 3px 10px rgba(249,115,22,0.25)',
            }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
              </svg>
            </div>
            <div style={{ textAlign: 'left' }}>
              <h3 style={{
                fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                fontSize: '0.95rem', fontWeight: 700, color: '#2A2520', margin: 0,
              }}>
                智能导航
              </h3>
              <span style={{ fontSize: '0.78rem', color: '#9B9590' }}>为你推荐可能想去的页面</span>
            </div>
          </div>

          {/* Suggestions list */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {suggestions.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                style={{
                  display: 'flex', alignItems: 'center', gap: '0.75rem',
                  padding: '0.7rem 0.85rem', borderRadius: 14,
                  background: item.bg, border: '1px solid transparent',
                  textDecoration: 'none', transition: 'all 0.2s ease',
                }}
                onMouseEnter={(e) => {
                  const el = e.currentTarget;
                  el.style.background = `linear-gradient(135deg, ${item.bg.replace('0.10', '0.16').replace('0.12', '0.18')}, transparent)`;
                  el.style.borderColor = `${item.color}20`;
                  el.style.transform = 'translateX(4px)';
                }}
                onMouseLeave={(e) => {
                  const el = e.currentTarget;
                  el.style.background = item.bg;
                  el.style.borderColor = 'transparent';
                  el.style.transform = 'translateX(0)';
                }}
              >
                <div style={{
                  width: 34, height: 34, borderRadius: 10,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  background: '#fff', color: item.color, flexShrink: 0,
                  boxShadow: `0 1px 4px ${item.color}15`,
                }}>
                  {item.icon}
                </div>
                <div style={{ flex: 1, textAlign: 'left', minWidth: 0 }}>
                  <span style={{
                    display: 'block', fontSize: '0.87rem', fontWeight: 600, color: '#2A2520',
                  }}>{item.label}</span>
                  <span style={{ display: 'block', fontSize: '0.74rem', color: '#9B9590' }}>{item.desc}</span>
                </div>
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#B5AEA8" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0, transition: 'all 0.2s ease' }}>
                  <path d="M5 12h14M12 5l7 7-7 7" />
                </svg>
              </Link>
            ))}
          </div>
        </div>
      </div>

      <style>{`
        @keyframes nfIllustrationIn {
          from { opacity: 0; transform: translateY(24px) scale(0.96); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }
        @keyframes nfFadeUp {
          from { opacity: 0; transform: translateY(16px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes nfTextFloat {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-8px); }
        }
        @keyframes nfFloatOrbA {
          0%, 100% { transform: translate(0, 0) scale(1); }
          33% { transform: translate(25px, -20px) scale(1.06); }
          66% { transform: translate(-15px, 15px) scale(0.94); }
        }
        @keyframes nfFloatOrbB {
          0%, 100% { transform: translate(0, 0) scale(1); }
          33% { transform: translate(-20px, 18px) scale(1.04); }
          66% { transform: translate(18px, -12px) scale(0.96); }
        }
        @keyframes nfFloatOrbC {
          0%, 100% { transform: translateX(-50%) translateY(0); }
          50% { transform: translateX(-50%) translateY(-15px); }
        }
        @keyframes nfShapeFloat {
          0%, 100% { opacity: 0.5; transform: translateY(0) rotate(var(--rot, 0deg)); }
          50% { opacity: 0.9; transform: translateY(-12px) rotate(calc(var(--rot, 0deg) + 8deg)); }
        }
      `}</style>
    </div>
  );
}

export default NotFoundPage;
