import { useEffect, useCallback, type ReactNode } from 'react';
import { createPortal } from 'react-dom';

export interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  content: ReactNode;
  confirmText?: string;
  cancelText?: string;
  onConfirm: () => Promise<void> | void;
  onCancel: () => void;
  isLoading?: boolean;
  confirmDisabled?: boolean;
  variant?: 'danger' | 'warning' | 'info';
}

const VARIANT_CONFIG = {
  danger: {
    gradient: 'linear-gradient(135deg, #F43F5E, #E11D48)',
    shadow: 'rgba(244,63,94,0.28)',
    hoverShadow: 'rgba(244,63,94,0.4)',
    iconBg: 'linear-gradient(135deg, rgba(244,63,94,0.10), rgba(251,113,133,0.06))',
    iconColor: '#E11D48',
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
      </svg>
    ),
  },
  warning: {
    gradient: 'linear-gradient(135deg, #F97316, #EA580C)',
    shadow: 'rgba(249,115,22,0.28)',
    hoverShadow: 'rgba(249,115,22,0.4)',
    iconBg: 'linear-gradient(135deg, rgba(249,115,22,0.10), rgba(251,146,60,0.06))',
    iconColor: '#EA580C',
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
  },
  info: {
    gradient: 'linear-gradient(135deg, #3B82F6, #2563EB)',
    shadow: 'rgba(59,130,246,0.28)',
    hoverShadow: 'rgba(59,130,246,0.4)',
    iconBg: 'linear-gradient(135deg, rgba(59,130,246,0.10), rgba(96,165,250,0.06))',
    iconColor: '#2563EB',
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
  },
};

export function ConfirmModal({
  isOpen,
  title,
  content,
  confirmText = '确认',
  cancelText = '取消',
  onConfirm,
  onCancel,
  isLoading = false,
  variant = 'danger',
}: ConfirmModalProps) {
  const handleConfirm = useCallback(async () => {
    await onConfirm();
  }, [onConfirm]);

  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen && !isLoading) {
        onCancel();
      }
    };
    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [isOpen, isLoading, onCancel]);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  if (!isOpen) {return null;}

  const variantConfig = VARIANT_CONFIG[variant];

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
          animation: 'confirmFadeIn 0.2s ease-out',
        }}
        onClick={() => !isLoading && onCancel()}
        onKeyDown={(e) => e.key === 'Enter' && !isLoading && onCancel()}
        aria-label="关闭对话框"
      />

      {/* Modal */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-modal-title"
        style={{
        position: 'fixed', left: '50%', top: '50%',
        transform: 'translate(-50%, -50%)',
        width: 'calc(100% - 2rem)', maxWidth: 440,
        background: 'rgba(255,255,255,0.94)',
        backdropFilter: 'blur(24px)',
        WebkitBackdropFilter: 'blur(24px)',
        border: '1px solid rgba(255,255,255,0.7)',
        borderRadius: 24,
        boxShadow: '0 24px 64px rgba(42,37,32,0.18), 0 8px 24px rgba(0,0,0,0.04)',
        overflow: 'hidden',
        animation: 'confirmSlideIn 0.35s cubic-bezier(0.16, 1, 0.3, 1)',
      }}>
        {/* Body */}
        <div style={{ padding: '1.75rem 1.5rem' }}>
          <div style={{ display: 'flex', gap: '1rem' }}>
            {/* Icon */}
            <div style={{
              width: 44, height: 44, borderRadius: 14, flexShrink: 0,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              background: variantConfig.iconBg, color: variantConfig.iconColor,
            }}>
              {variantConfig.icon}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <h3 id="confirm-modal-title" style={{
                fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                fontSize: '1.05rem', fontWeight: 700, color: '#2A2520',
                marginBottom: '0.4rem', lineHeight: 1.3,
              }}>
                {title}
              </h3>
              <p style={{ fontSize: '0.87rem', color: '#6B6460', lineHeight: 1.6 }}>
                {content}
              </p>
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
            onClick={onCancel}
            disabled={isLoading}
            style={{
              padding: '0.6rem 1.2rem', borderRadius: 12,
              border: '1.5px solid #E5E0DB', background: '#fff',
              fontSize: '0.87rem', fontWeight: 600, color: '#6B6460',
              cursor: isLoading ? 'not-allowed' : 'pointer',
              transition: 'all 0.15s ease', opacity: isLoading ? 0.5 : 1,
            }}
            onMouseEnter={(e) => { if (!isLoading) {e.currentTarget.style.background = 'rgba(229,224,219,0.3)';} }}
            onMouseLeave={(e) => { e.currentTarget.style.background = '#fff'; }}
          >
            {cancelText}
          </button>
          <button
            onClick={handleConfirm}
            disabled={isLoading}
            style={{
              padding: '0.6rem 1.5rem', borderRadius: 12,
              border: 'none',
              background: isLoading ? '#D6CEC5' : variantConfig.gradient,
              fontSize: '0.87rem', fontWeight: 600, color: '#fff',
              cursor: isLoading ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s ease',
              boxShadow: isLoading ? 'none' : `0 3px 12px ${variantConfig.shadow}`,
            }}
            onMouseEnter={(e) => { if (!isLoading) { e.currentTarget.style.transform = 'translateY(-1px)'; e.currentTarget.style.boxShadow = `0 5px 18px ${variantConfig.hoverShadow}`; } }}
            onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = isLoading ? 'none' : `0 3px 12px ${variantConfig.shadow}`; }}
          >
            {isLoading ? (
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <svg style={{ width: 15, height: 15, animation: 'spin 0.7s linear infinite' }} fill="none" viewBox="0 0 24 24">
                  <circle style={{ opacity: 0.25 }} cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path style={{ opacity: 0.75 }} fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                处理中...
              </span>
            ) : confirmText}
          </button>
        </div>
      </div>

      <style>{`
        @keyframes confirmFadeIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes confirmSlideIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes spin { to { transform: rotate(360deg); } }
      `}</style>
    </div>,
    document.body
  );
}
