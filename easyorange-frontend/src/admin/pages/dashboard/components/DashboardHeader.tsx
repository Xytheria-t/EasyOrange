import { useState, useEffect } from 'react';

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

export function DashboardHeader() {
  const [currentTime, setCurrentTime] = useState(getCurrentTime());

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(getCurrentTime()), 30000);
    return () => clearInterval(timer);
  }, []);

  return (
    <header style={{ marginBottom: '2.5rem', animation: 'dashHeaderReveal 0.8s cubic-bezier(0.16, 1, 0.3, 1) both' }}>
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: '1.5rem', flexWrap: 'wrap' }}>
        <div>
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

          <div style={{
            marginTop: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.5rem',
            animation: 'dashUnderlineExpand 0.8s ease-out 0.5s both',
          }}>
            <div style={{ width: 48, height: 2, borderRadius: 1, background: 'linear-gradient(90deg, #F97316, transparent)' }} />
            <div style={{ width: 6, height: 6, borderRadius: '50%', border: '1.5px solid #F97316', opacity: 0.35 }} />
          </div>
        </div>

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
  );
}
