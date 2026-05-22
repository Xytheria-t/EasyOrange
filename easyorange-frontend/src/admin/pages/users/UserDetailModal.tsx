import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import type { AdminUser } from '../../types/admin';

export interface UserDetailModalProps {
  open: boolean;
  user: AdminUser | null;
  onClose: () => void;
  onSave: (status: string) => Promise<void>;
  loading?: boolean;
}

const statusOptions: { value: string; label: string; emoji: string }[] = [
  { value: '0', label: '正常', emoji: '✅' },
  { value: '1', label: '禁用', emoji: '🚫' },
  { value: '2', label: '锁定', emoji: '🔒' },
];

const AVATAR_GRADIENTS = [
  'linear-gradient(135deg, #F97316, #FB923C)',
  'linear-gradient(135deg, #FB7185, #C39BD3)',
  'linear-gradient(135deg, #34D399, #10B981)',
  'linear-gradient(135deg, #FBBF24, #F97316)',
  'linear-gradient(135deg, #C39BD3, #D8B4FE)',
] as const;

const STATUS_STYLES: Record<string, { bg: string; color: string; dot: string; label: string }> = {
  '0': { bg: 'linear-gradient(135deg, rgba(16,185,129,0.10), rgba(52,211,153,0.06))', color: '#059669', dot: '#10B981', label: '正常' },
  '1': { bg: 'linear-gradient(135deg, rgba(244,63,94,0.10), rgba(251,113,133,0.06))', color: '#E11D48', dot: '#F43F5E', label: '禁用' },
  '2': { bg: 'linear-gradient(135deg, rgba(245,158,11,0.10), rgba(251,191,36,0.06))', color: '#D97706', dot: '#F59E0B', label: '锁定' },
};

export function UserDetailModal({
  open,
  user,
  onClose,
  onSave,
  loading = false,
}: UserDetailModalProps) {
  const [selectedStatus, setSelectedStatus] = useState<string>(user?.status ?? '0');

  useEffect(() => {
    if (!open) {return;}
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !loading) {onClose();}
    };
    document.addEventListener('keydown', handleEscape);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = '';
    };
  }, [open, loading, onClose]);

  if (!open || !user) {return null;}

  const handleSave = async () => {
    await onSave(selectedStatus);
  };

  const formatDate = (dateString: string) =>
    new Date(dateString).toLocaleString('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit',
    });

  const maskPhone = (phone: string | null) => {
    if (!phone) {return '未绑定';}
    return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
  };

  const maskEmail = (email: string) => {
    const [name, domain] = email.split('@');
    const maskedName = name.length > 2 ? `${name[0]  }***${  name[name.length - 1]}` : name;
    return `${maskedName}@${domain}`;
  };

  const avatarGradient = AVATAR_GRADIENTS[Number(user.userId ?? 0) % AVATAR_GRADIENTS.length];
  const currentStatusStyle = STATUS_STYLES[selectedStatus] ?? STATUS_STYLES['0'];

  return createPortal(
    <div style={{
      position: 'fixed', inset: 0, zIndex: 9999,
    }}>
      {/* Backdrop */}
      <div
        role="button"
        tabIndex={0}
        style={{
          position: 'fixed', inset: 0,
          background: 'rgba(42,37,32,0.45)',
          backdropFilter: 'blur(6px)',
          WebkitBackdropFilter: 'blur(6px)',
          animation: 'modalFadeIn 0.2s ease-out',
        }}
        onClick={() => !loading && onClose()}
        onKeyDown={(e) => e.key === 'Enter' && !loading && onClose()}
        aria-label="关闭对话框"
      />

      {/* Modal */}
      <div style={{
        position: 'fixed', left: '50%', top: '50%',
        transform: 'translate(-50%, -50%)',
        width: 'calc(100% - 2rem)', maxWidth: 440,
        maxHeight: 'calc(100vh - 2rem)',
        background: 'rgba(255,255,255,0.92)',
        backdropFilter: 'blur(24px)',
        WebkitBackdropFilter: 'blur(24px)',
        border: '1px solid rgba(255,255,255,0.7)',
        borderRadius: 24,
        boxShadow: '0 24px 64px rgba(42,37,32,0.18), 0 8px 24px rgba(249,115,22,0.06)',
        overflow: 'hidden',
        display: 'flex', flexDirection: 'column',
        animation: 'modalSlideIn 0.35s cubic-bezier(0.16, 1, 0.3, 1)',
      }}>
        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '1.25rem 1.5rem',
          borderBottom: '1px solid rgba(229,224,219,0.5)',
          position: 'relative',
        }}>
          <div style={{
            position: 'absolute', bottom: 0, left: '1.5rem', right: '1.5rem', height: 1,
            background: 'linear-gradient(90deg, rgba(249,115,22,0.12), rgba(195,155,211,0.08), transparent)',
          }} />
          <h3 style={{
            fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
            fontSize: '1.1rem', fontWeight: 700, color: '#2A2520',
            display: 'flex', alignItems: 'center', gap: '0.5rem',
          }}>
            <span style={{
              width: 26, height: 26, borderRadius: 8,
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
              background: 'linear-gradient(135deg, #F97316, #FB923C)',
              color: '#fff', flexShrink: 0,
            }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" /><circle cx="12" cy="7" r="4" />
              </svg>
            </span>
            用户详情
          </h3>
          <button
            onClick={onClose}
            disabled={loading}
            style={{
              width: 32, height: 32, borderRadius: 10,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              border: '1.5px solid #E5E0DB', background: '#fff',
              color: '#8B857E', cursor: loading ? 'not-allowed' : 'pointer',
              transition: 'all 0.15s ease', opacity: loading ? 0.5 : 1,
            }}
            onMouseEnter={(e) => { if (!loading) { e.currentTarget.style.background = 'rgba(244,63,94,0.06)'; e.currentTarget.style.borderColor = 'rgba(244,63,94,0.2)'; e.currentTarget.style.color = '#E11D48'; } }}
            onMouseLeave={(e) => { e.currentTarget.style.background = '#fff'; e.currentTarget.style.borderColor = '#E5E0DB'; e.currentTarget.style.color = '#8B857E'; }}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Body */}
        <div style={{ padding: '1.5rem', flex: 1, overflowY: 'auto', minHeight: 0 }}>
          {/* User profile card */}
          <div style={{
            display: 'flex', alignItems: 'center', gap: '1rem',
            padding: '1rem 1.15rem',
            background: 'linear-gradient(135deg, rgba(249,115,22,0.04), rgba(195,155,211,0.03))',
            borderRadius: 16, marginBottom: '1.25rem',
            border: '1px solid rgba(249,115,22,0.06)',
          }}>
            <div style={{
              width: 52, height: 52, borderRadius: 16,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '1.25rem', fontWeight: 700, color: '#fff',
              fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
              background: avatarGradient, flexShrink: 0,
              boxShadow: '0 4px 12px rgba(249,115,22,0.18)',
            }}>
              {(user.nickname || user.username || '?').charAt(0).toUpperCase()}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <p style={{ fontWeight: 700, color: '#2A2520', fontSize: '1rem', lineHeight: 1.3 }}>
                {user.nickname || user.username}
              </p>
              <p style={{ fontSize: '0.82rem', color: '#9B9590', marginTop: 2 }}>@{user.username}</p>
            </div>
            <div style={{
              display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
              padding: '0.3rem 0.7rem', borderRadius: 10,
              background: currentStatusStyle.bg, fontSize: '0.78rem', fontWeight: 600,
              color: currentStatusStyle.color,
            }}>
              <span style={{ width: 6, height: 6, borderRadius: '50%', background: currentStatusStyle.dot, flexShrink: 0 }} />
              {currentStatusStyle.label}
            </div>
          </div>

          {/* Info grid */}
          <div style={{
            display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.85rem',
            marginBottom: '1.25rem',
          }}>
            {[
              { label: '用户名', value: user.username },
              { label: '昵称', value: user.nickname || '未设置' },
              { label: '邮箱', value: maskEmail(user.email ?? '') },
              { label: '手机', value: maskPhone(user.phone) },
              { label: '用户类型', value: user.userType === '01' ? '🎓 学生' : '👨‍🏫 教师' },
              { label: '注册时间', value: formatDate(user.createTime ?? '') },
            ].map((item) => (
              <div key={item.label} style={{
                padding: '0.65rem 0.85rem',
                background: 'rgba(255,255,255,0.6)',
                border: '1px solid rgba(229,224,219,0.4)',
                borderRadius: 12,
              }}>
                <p style={{ fontSize: '0.72rem', color: '#9B9590', marginBottom: 2, fontWeight: 500 }}>{item.label}</p>
                <p style={{ fontSize: '0.87rem', color: '#2A2520', fontWeight: 600 }}>{item.value}</p>
              </div>
            ))}
          </div>

          {/* Stats row */}
          <div style={{
            display: 'flex', alignItems: 'center', gap: 0,
            padding: '0.85rem 0',
            borderTop: '1px solid rgba(229,224,219,0.4)',
            borderBottom: '1px solid rgba(229,224,219,0.4)',
            marginBottom: '1.25rem',
          }}>
            <div style={{ flex: 1, textAlign: 'center' }}>
              <p style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: '1.5rem', fontWeight: 700, color: '#F97316',
                lineHeight: 1.2,
              }}>
                —
              </p>
              <p style={{ fontSize: '0.78rem', color: '#9B9590', marginTop: 2 }}>商品数</p>
            </div>
            <div style={{ width: 1, height: 36, background: 'rgba(229,224,219,0.5)' }} />
            <div style={{ flex: 1, textAlign: 'center' }}>
              <p style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: '1.5rem', fontWeight: 700, color: '#C39BD3',
                lineHeight: 1.2,
              }}>
                —
              </p>
              <p style={{ fontSize: '0.78rem', color: '#9B9590', marginTop: 2 }}>订单数</p>
            </div>
          </div>

          {/* Status selector */}
          <div>
            <label htmlFor="status-select" style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#6B6460', marginBottom: '0.5rem' }}>
              调整状态
            </label>
            <div style={{ display: 'flex', gap: '0.5rem' }} id="status-select" role="radiogroup">
              {statusOptions.map((opt) => {
                const isActive = selectedStatus === opt.value;
                const sStyle = STATUS_STYLES[opt.value];
                return (
                  <button
                    key={opt.value}
                    onClick={() => setSelectedStatus(opt.value)}
                    disabled={loading}
                    style={{
                      flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.35rem',
                      padding: '0.6rem 0.5rem', borderRadius: 12,
                      border: isActive ? `1.5px solid ${sStyle.dot}` : '1.5px solid #E5E0DB',
                      background: isActive ? sStyle.bg : '#fff',
                      color: isActive ? sStyle.color : '#8B857E',
                      fontSize: '0.84rem', fontWeight: 600,
                      cursor: loading ? 'not-allowed' : 'pointer',
                      transition: 'all 0.2s ease',
                      opacity: loading ? 0.6 : 1,
                    }}
                  >
                    <span style={{ fontSize: '0.9rem' }}>{opt.emoji}</span>
                    {opt.label}
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.65rem',
          padding: '1rem 1.5rem',
          background: 'linear-gradient(180deg, rgba(250,248,245,0.5), rgba(250,248,245,0.9))',
          borderTop: '1px solid rgba(229,224,219,0.4)',
        }}>
          <button
            onClick={onClose}
            disabled={loading}
            style={{
              padding: '0.6rem 1.2rem', borderRadius: 12,
              border: '1.5px solid #E5E0DB', background: '#fff',
              fontSize: '0.87rem', fontWeight: 600, color: '#6B6460',
              cursor: loading ? 'not-allowed' : 'pointer',
              transition: 'all 0.15s ease', opacity: loading ? 0.5 : 1,
            }}
            onMouseEnter={(e) => { if (!loading) {e.currentTarget.style.background = 'rgba(229,224,219,0.3)';} }}
            onMouseLeave={(e) => { e.currentTarget.style.background = '#fff'; }}
          >
            取消
          </button>
          <button
            onClick={handleSave}
            disabled={loading || selectedStatus === user.status}
            style={{
              padding: '0.6rem 1.5rem', borderRadius: 12,
              border: 'none',
              background: (loading || selectedStatus === user.status)
                ? '#D6CEC5'
                : 'linear-gradient(135deg, #F97316, #EA580C)',
              fontSize: '0.87rem', fontWeight: 600, color: '#fff',
              cursor: (loading || selectedStatus === user.status) ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s ease',
              boxShadow: (loading || selectedStatus === user.status) ? 'none' : '0 3px 12px rgba(249,115,22,0.3)',
            }}
            onMouseEnter={(e) => { if (!loading && selectedStatus !== user.status) { e.currentTarget.style.transform = 'translateY(-1px)'; e.currentTarget.style.boxShadow = '0 5px 18px rgba(249,115,22,0.4)'; } }}
            onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = (loading || selectedStatus === user.status) ? 'none' : '0 3px 12px rgba(249,115,22,0.3)'; }}
          >
            {loading ? (
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <svg style={{ width: 15, height: 15, animation: 'spin 0.7s linear infinite' }} fill="none" viewBox="0 0 24 24">
                  <circle style={{ opacity: 0.25 }} cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path style={{ opacity: 0.75 }} fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                保存中...
              </span>
            ) : '保存修改'}
          </button>
        </div>
      </div>

      <style>{`
        @keyframes modalFadeIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes modalSlideIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes spin { to { transform: rotate(360deg); } }
      `}</style>
    </div>,
    document.body
  );
}
