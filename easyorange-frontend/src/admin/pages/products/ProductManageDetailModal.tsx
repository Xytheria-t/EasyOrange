import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useAdminProductDetail, useUpdateProductStatus } from '../../hooks';
import type { AdminProduct } from '../../types/admin';

export interface ProductManageDetailModalProps {
  open: boolean;
  productId: number | null;
  onClose: () => void;
  onSuccess: () => void;
}

const conditionLabels: Record<number, string> = {
  10: '全新', 9: '9成新', 8: '8成新', 7: '7成新', 6: '6成新及以下',
};

const PRODUCT_STATUS = {
  DRAFT: 0,
  ONLINE: 1,
  SOLD: 2,
  OFFLINE: 3,
  PENDING_REVIEW: 4,
  REJECTED: 5,
} as const;

// 管理态可操作的状态变更
function getAvailableActions(status: number | null): { targetStatus: number; label: string; variant: 'online' | 'offline' }[] {
  const actions: { targetStatus: number; label: string; variant: 'online' | 'offline' }[] = [];
  if (status === PRODUCT_STATUS.ONLINE) {
    actions.push({ targetStatus: PRODUCT_STATUS.OFFLINE, label: '下架', variant: 'offline' });
  } else if (status === PRODUCT_STATUS.OFFLINE || status === PRODUCT_STATUS.DRAFT || status === PRODUCT_STATUS.REJECTED) {
    actions.push({ targetStatus: PRODUCT_STATUS.ONLINE, label: '上架', variant: 'online' });
  }
  return actions;
}

function InfoCell({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div style={{
      padding: '0.65rem 0.85rem',
      background: 'rgba(255,255,255,0.6)',
      border: '1px solid rgba(229,224,219,0.4)',
      borderRadius: 12,
    }}>
      <p style={{ fontSize: '0.72rem', color: '#9B9590', marginBottom: 2, fontWeight: 500 }}>{label}</p>
      <p style={{ fontSize: '0.87rem', color: '#2A2520', fontWeight: 600 }}>{value}</p>
    </div>
  );
}

export function ProductManageDetailModal({ open, productId, onClose, onSuccess }: ProductManageDetailModalProps) {
  const [previewImage, setPreviewImage] = useState<string | null>(null);
  const [selectedImage, setSelectedImage] = useState(0);

  const { data: product, isLoading, refetch } = useAdminProductDetail(productId ?? 0);
  const updateStatus = useUpdateProductStatus();

  useEffect(() => {
    if (open && productId) {
      refetch();
    }
  }, [open, productId, refetch]);

  useEffect(() => {
    if (!open) {return;}
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !previewImage) {onClose();}
    };
    document.addEventListener('keydown', handleEscape);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = '';
    };
  }, [open, previewImage, onClose]);

  useEffect(() => {
    if (!open) {setSelectedImage(0); setPreviewImage(null);}
  }, [open]);

  const handleStatusChange = async (targetStatus: number) => {
    if (!product) {return;}
    try {
      await updateStatus.mutateAsync({ id: product.productId, data: { status: targetStatus } });
      onSuccess();
      onClose();
    } catch {
      // error handled by react-query / global handler
    }
  };

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) {return '—';}
    return new Date(dateStr).toLocaleString('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit',
    });
  };

  const formatPrice = (price: number | null) => (price != null ? `¥${price.toFixed(2)}` : '—');

  if (!open || !productId) {return null;}

  const p = product as AdminProduct | undefined;
  const images = p?.images ?? [];
  const actions = getAvailableActions(p?.status ?? null);

  return createPortal(
    <div style={{ position: 'fixed', inset: 0, zIndex: 9999 }}>
      {/* Backdrop */}
      <div
        role="button"
        tabIndex={0}
        style={{
          position: 'fixed', inset: 0,
          background: 'rgba(42,37,32,0.45)',
          backdropFilter: 'blur(6px)',
          WebkitBackdropFilter: 'blur(6px)',
          animation: 'pmFadeIn 0.2s ease-out',
        }}
        onClick={onClose}
        onKeyDown={(e) => e.key === 'Enter' && onClose()}
        aria-label="关闭对话框"
      />

      {/* Modal */}
      <div style={{
        position: 'fixed', left: '50%', top: '50%',
        transform: 'translate(-50%, -50%)',
        width: 'calc(100% - 2rem)', maxWidth: 520,
        maxHeight: 'calc(100vh - 2rem)',
        background: 'rgba(255,255,255,0.94)',
        backdropFilter: 'blur(24px)',
        WebkitBackdropFilter: 'blur(24px)',
        border: '1px solid rgba(255,255,255,0.7)',
        borderRadius: 24,
        boxShadow: '0 24px 64px rgba(42,37,32,0.18), 0 8px 24px rgba(249,115,22,0.06)',
        overflow: 'hidden',
        display: 'flex', flexDirection: 'column',
        animation: 'pmSlideIn 0.35s cubic-bezier(0.16, 1, 0.3, 1)',
      }}>
        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '1.25rem 1.5rem',
          borderBottom: '1px solid rgba(229,224,219,0.5)',
          position: 'relative', flexShrink: 0,
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
                <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" />
              </svg>
            </span>
            商品详情
          </h3>
          <button
            onClick={onClose}
            style={{
              width: 32, height: 32, borderRadius: 10,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              border: '1.5px solid #E5E0DB', background: '#fff',
              color: '#8B857E', cursor: 'pointer', transition: 'all 0.15s ease',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(244,63,94,0.06)'; e.currentTarget.style.borderColor = 'rgba(244,63,94,0.2)'; e.currentTarget.style.color = '#E11D48'; }}
            onMouseLeave={(e) => { e.currentTarget.style.background = '#fff'; e.currentTarget.style.borderColor = '#E5E0DB'; e.currentTarget.style.color = '#8B857E'; }}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Body */}
        <div style={{ padding: '1.5rem', flex: 1, overflowY: 'auto', minHeight: 0 }}>
          {isLoading ? (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.7rem', padding: '4rem 1rem' }}>
              <div style={{
                width: 28, height: 28,
                border: '2.5px solid #E5E0DB', borderTopColor: '#F97316',
                borderRadius: '50%', animation: 'pmSpin 0.7s linear infinite',
              }} />
              <span style={{ fontSize: '0.87rem', color: '#9B9590' }}>加载中...</span>
            </div>
          ) : p ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              {/* Image gallery */}
              {images.length > 0 && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
                  <div
                    role="button"
                    tabIndex={0}
                    style={{
                      position: 'relative', width: '100%', aspectRatio: '16/10',
                      overflow: 'hidden', borderRadius: 16,
                      background: 'linear-gradient(135deg, #F5F2EE, #EDE8E3)',
                      cursor: images[selectedImage] ? 'pointer' : 'default',
                    }}
                    onClick={() => images[selectedImage] && setPreviewImage(images[selectedImage])}
                    onKeyDown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && images[selectedImage]) { setPreviewImage(images[selectedImage]); } }}
                  >
                    {images[selectedImage] ? (
                      <img src={images[selectedImage]} alt={p.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                    ) : (
                      <div style={{ display: 'flex', height: '100%', alignItems: 'center', justifyContent: 'center', color: '#B5AEA8' }}>
                        <svg width="40" height="40" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                      </div>
                    )}
                  </div>
                  {images.length > 1 && (
                    <div style={{ display: 'flex', gap: '0.5rem', overflowX: 'auto', paddingBottom: '0.25rem' }}>
                      {images.map((img, index) => (
                        <button
                          key={index}
                          onClick={() => setSelectedImage(index)}
                          style={{
                            flexShrink: 0, width: 52, height: 52, borderRadius: 10,
                            overflow: 'hidden',
                            border: selectedImage === index ? '2px solid #F97316' : '2px solid transparent',
                            padding: 0, cursor: 'pointer',
                            transition: 'border-color 0.15s ease',
                          }}
                        >
                          <img src={img} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* Name + Price */}
              <div>
                <h3 style={{
                  fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                  fontSize: '1.15rem', fontWeight: 700, color: '#2A2520',
                  lineHeight: 1.3, marginBottom: '0.35rem',
                }}>
                  {p.name}
                </h3>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem' }}>
                  <span style={{
                    fontFamily: "'DM Sans', sans-serif",
                    fontSize: '1.5rem', fontWeight: 700,
                    background: 'linear-gradient(135deg, #F97316, #EA580C)',
                    WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
                    backgroundClip: 'text',
                  }}>
                    {formatPrice(p.price)}
                  </span>
                  {p.originalPrice && p.originalPrice > (p.price ?? 0) && (
                    <span style={{ fontSize: '0.85rem', color: '#B5AEA8', textDecoration: 'line-through' }}>
                      {formatPrice(p.originalPrice)}
                    </span>
                  )}
                </div>
              </div>

              {/* Info grid */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <InfoCell label="新旧程度" value={conditionLabels[p.conditionLevel || 8] || '未知'} />
                <InfoCell label="分类" value={p.categoryName || '—'} />
                <InfoCell label="卖家" value={p.sellerName || '—'} />
                <InfoCell label="浏览量" value={p.viewCount ?? 0} />
                <InfoCell label="发布时间" value={formatDate(p.createTime)} />
                <InfoCell label="更新时间" value={formatDate(p.updateTime)} />
              </div>

              {/* Status bar */}
              <div style={{
                display: 'flex', alignItems: 'center', gap: '0.75rem',
                padding: '0.85rem 1rem',
                background: 'linear-gradient(135deg, rgba(249,115,22,0.04), rgba(195,155,211,0.03))',
                borderRadius: 14, border: '1px solid rgba(249,115,22,0.08)',
              }}>
                <span style={{ fontSize: '0.82rem', fontWeight: 500, color: '#6B6460' }}>当前状态：</span>
                <span style={{
                  fontSize: '0.84rem', fontWeight: 700, color: '#2A2520',
                  padding: '0.2rem 0.6rem', borderRadius: 6,
                  background: 'rgba(249,115,22,0.06)',
                }}>
                  {p.statusDesc ?? '未知'}
                </span>
              </div>

              {/* Description */}
              {p.description && (
                <div style={{
                  padding: '0.85rem 1rem',
                  background: 'linear-gradient(135deg, rgba(249,115,22,0.03), rgba(195,155,211,0.02))',
                  borderRadius: 14,
                  border: '1px solid rgba(229,224,219,0.35)',
                }}>
                  <p style={{ fontSize: '0.78rem', fontWeight: 600, color: '#6B6460', marginBottom: '0.35rem' }}>商品描述</p>
                  <p style={{ fontSize: '0.87rem', color: '#4A4540', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>{p.description}</p>
                </div>
              )}

              {/* Location */}
              {p.location && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', fontSize: '0.85rem' }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#F97316" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  <span style={{ color: '#9B9590' }}>交易地点：</span>
                  <span style={{ color: '#2A2520', fontWeight: 600 }}>{p.location}</span>
                </div>
              )}
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', padding: '4rem 1rem', color: '#B5AEA8' }}>
              <span style={{ fontSize: '2rem', opacity: 0.4 }}>📭</span>
              <span style={{ fontSize: '0.9rem' }}>商品不存在或已被删除</span>
            </div>
          )}
        </div>

        {/* Footer - Status actions */}
        {p && actions.length > 0 && (
          <div style={{
            display: 'flex', gap: '0.65rem',
            padding: '1rem 1.5rem',
            borderTop: '1px solid rgba(229,224,219,0.4)',
            background: 'linear-gradient(180deg, rgba(250,248,245,0.5), rgba(250,248,245,0.9))',
          }}>
            {actions.map((action) => (
              <button
                key={action.targetStatus}
                onClick={() => handleStatusChange(action.targetStatus)}
                disabled={updateStatus.isPending}
                style={{
                  flex: 1, padding: '0.65rem 1rem', borderRadius: 12,
                  border: 'none',
                  background: action.variant === 'online'
                    ? (updateStatus.isPending ? '#D6CEC5' : 'linear-gradient(135deg, #10B981, #059669)')
                    : (updateStatus.isPending ? '#D6CEC5' : 'linear-gradient(135deg, #F97316, #EA580C)'),
                  color: '#fff', fontSize: '0.87rem', fontWeight: 600,
                  cursor: updateStatus.isPending ? 'not-allowed' : 'pointer',
                  transition: 'all 0.2s ease',
                  boxShadow: updateStatus.isPending ? 'none' : `0 3px 12px rgba(${action.variant === 'online' ? '16,185,129' : '249,115,22'},0.28)`,
                }}
                onMouseEnter={(e) => {
                  if (!updateStatus.isPending) {
                    e.currentTarget.style.transform = 'translateY(-1px)';
                    e.currentTarget.style.boxShadow = `0 5px 18px rgba(${action.variant === 'online' ? '16,185,129' : '249,115,22'},0.38)`;
                  }
                }}
                onMouseLeave={(e) => {
                  if (!updateStatus.isPending) {
                    e.currentTarget.style.transform = 'translateY(0)';
                    e.currentTarget.style.boxShadow = `0 3px 12px rgba(${action.variant === 'online' ? '16,185,129' : '249,115,22'},0.28)`;
                  }
                }}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ marginRight: '0.35rem', verticalAlign: 'middle' }}>
                  {action.variant === 'online' ? (
                    <><polyline points="9 11 12 14 22 4" /><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" /></>
                  ) : (
                    <><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></>
                  )}
                </svg>
                {action.label}
              </button>
            ))}
            <button
              onClick={onClose}
              disabled={updateStatus.isPending}
              style={{
                padding: '0.65rem 1.2rem', borderRadius: 12,
                border: '1.5px solid #E5E0DB', background: '#fff',
                color: '#6B6460', fontSize: '0.87rem', fontWeight: 600,
                cursor: updateStatus.isPending ? 'not-allowed' : 'pointer',
                transition: 'all 0.15s ease',
                opacity: updateStatus.isPending ? 0.5 : 1,
              }}
              onMouseEnter={(e) => { if (!updateStatus.isPending) {e.currentTarget.style.background = 'rgba(229,224,219,0.3)';} }}
              onMouseLeave={(e) => { if (!updateStatus.isPending) {e.currentTarget.style.background = '#fff';} }}
            >
              关闭
            </button>
          </div>
        )}
      </div>

      {/* Image preview overlay */}
      {previewImage && (
        <div
          role="button"
          tabIndex={0}
          style={{
            position: 'fixed', inset: 0, zIndex: 50,
            background: 'rgba(42,37,32,0.88)',
            backdropFilter: 'blur(8px)',
            WebkitBackdropFilter: 'blur(8px)',
            animation: 'pmFadeIn 0.2s ease-out',
          }}
          onClick={() => setPreviewImage(null)}
          onKeyDown={(e) => { if (e.key === 'Escape' || e.key === 'Enter') { setPreviewImage(null); } }}
        >
          <button
            style={{
              position: 'fixed', top: '1.25rem', right: '1.25rem',
              width: 40, height: 40, borderRadius: 12,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              border: '1.5px solid rgba(255,255,255,0.2)', background: 'rgba(255,255,255,0.1)',
              color: '#fff', cursor: 'pointer', transition: 'all 0.15s ease',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(255,255,255,0.2)'; }}
            onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(255,255,255,0.1)'; }}
            onClick={() => setPreviewImage(null)}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
          <img
            src={previewImage}
            alt="预览"
            style={{
              position: 'fixed', left: '50%', top: '50%',
              transform: 'translate(-50%, -50%)',
              maxHeight: '90vh', maxWidth: '90vw',
              objectFit: 'contain', borderRadius: 12,
            }}
          />
        </div>
      )}

      <style>{`
        @keyframes pmFadeIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes pmSlideIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes pmSpin { to { transform: rotate(360deg); } }
      `}</style>
    </div>,
    document.body
  );
}
