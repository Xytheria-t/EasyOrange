import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { useAdminOrderDetail } from '../../hooks';
import type { AdminOrderDetail } from '../../types/admin';

export interface OrderDetailModalProps {
  open: boolean;
  orderId: number | null;
  onClose: () => void;
}

const STATUS_CONFIG: Record<number, { bg: string; color: string; dot: string; label: string }> = {
  0: { bg: 'linear-gradient(135deg, rgba(249,158,11,0.10), rgba(251,191,36,0.06))', color: '#D97706', dot: '#F59E0B', label: '待付款' },
  1: { bg: 'linear-gradient(135deg, rgba(59,130,246,0.10), rgba(96,165,250,0.06))', color: '#2563EB', dot: '#3B82F6', label: '待发货' },
  2: { bg: 'linear-gradient(135deg, rgba(139,92,246,0.10), rgba(167,139,250,0.06))', color: '#7C3AED', dot: '#8B5CF6', label: '已发货' },
  3: { bg: 'linear-gradient(135deg, rgba(16,185,129,0.10), rgba(52,211,153,0.06))', color: '#059669', dot: '#10B981', label: '已完成' },
  4: { bg: 'linear-gradient(135deg, rgba(156,163,175,0.10), rgba(209,213,219,0.06))', color: '#6B7280', dot: '#9CA3AF', label: '已取消' },
  5: { bg: 'linear-gradient(135deg, rgba(244,63,94,0.10), rgba(251,113,133,0.06))', color: '#E11D48', dot: '#F43F5E', label: '退款中' },
};

const PAYMENT_STATUS: Record<number, string> = {
  0: '未支付',
  1: '已支付',
  2: '已退款',
};

function formatDateTime(dateString: string | null) {
  if (!dateString) {return '—';}
  return new Date(dateString).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  });
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

export function OrderDetailModal({ open, orderId, onClose }: OrderDetailModalProps) {
  const { data: order, isLoading } = useAdminOrderDetail(orderId ?? 0);

  useEffect(() => {
    if (!open) {return;}
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {onClose();}
    };
    document.addEventListener('keydown', handleEscape);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = '';
    };
  }, [open, onClose]);

  if (!open || !orderId) {return null;}

  const orderData = order as AdminOrderDetail | undefined;
  const statusCfg = STATUS_CONFIG[orderData?.status ?? 0] ?? STATUS_CONFIG[0];

  return createPortal(
    <div style={{
      position: 'fixed', inset: 0, zIndex: 9999,
    }}>
      <div
        role="button"
        tabIndex={0}
        style={{
          position: 'fixed', inset: 0,
          background: 'rgba(42,37,32,0.45)',
          backdropFilter: 'blur(6px)',
          WebkitBackdropFilter: 'blur(6px)',
          animation: 'odFadeIn 0.2s ease-out',
        }}
        onClick={onClose}
        onKeyDown={(e) => e.key === 'Enter' && onClose()}
        aria-label="关闭对话框"
      />

      <div style={{
        position: 'fixed', left: '50%', top: '50%',
        transform: 'translate(-50%, -50%)',
        width: 'calc(100% - 2rem)', maxWidth: 480,
        maxHeight: 'calc(100vh - 2rem)',
        background: 'rgba(255,255,255,0.94)',
        backdropFilter: 'blur(24px)',
        WebkitBackdropFilter: 'blur(24px)',
        border: '1px solid rgba(255,255,255,0.7)',
        borderRadius: 24,
        boxShadow: '0 24px 64px rgba(42,37,32,0.18), 0 8px 24px rgba(249,115,22,0.06)',
        overflow: 'hidden',
        display: 'flex', flexDirection: 'column',
        animation: 'odSlideIn 0.35s cubic-bezier(0.16, 1, 0.3, 1)',
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
            background: 'linear-gradient(90deg, rgba(251,191,36,0.12), rgba(195,155,211,0.08), transparent)',
          }} />
          <h3 style={{
            fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
            fontSize: '1.1rem', fontWeight: 700, color: '#2A2520',
            display: 'flex', alignItems: 'center', gap: '0.5rem',
          }}>
            <span style={{
              width: 26, height: 26, borderRadius: 8,
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
              background: 'linear-gradient(135deg, #FBBF24, #F97316)',
              color: '#fff', flexShrink: 0,
            }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" /><polyline points="14 2 14 8 20 8" /><line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" />
              </svg>
            </span>
            订单详情
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
                borderRadius: '50%', animation: 'spin 0.7s linear infinite',
              }} />
              <span style={{ fontSize: '0.87rem', color: '#9B9590' }}>加载中...</span>
            </div>
          ) : orderData ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              {/* Status & amount bar */}
              <div style={{
                display: 'flex', alignItems: 'center', gap: '1rem',
                padding: '1rem 1.15rem',
                background: 'linear-gradient(135deg, rgba(251,191,36,0.05), rgba(249,115,22,0.03))',
                borderRadius: 16, border: '1px solid rgba(251,191,36,0.08)',
              }}>
                <div style={{
                  padding: '0.35rem 0.75rem', borderRadius: 10,
                  background: statusCfg.bg, fontSize: '0.82rem', fontWeight: 700,
                  color: statusCfg.color, whiteSpace: 'nowrap', flexShrink: 0,
                }}>
                  {statusCfg.label}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <span style={{ fontFamily: "'DM Sans', sans-serif", fontSize: '1.35rem', fontWeight: 700, color: '#EA580C' }}>
                    ¥{orderData.amount.toFixed(2)}
                  </span>
                </div>
                <span style={{
                  fontFamily: "'DM Sans', monospace", fontSize: '0.78rem', fontWeight: 600,
                  color: '#9B9590', letterSpacing: '0.03em', flexShrink: 0,
                }}>{orderData.orderNo}</span>
              </div>

              {/* Product info */}
              <div style={{
                display: 'flex', gap: '0.85rem', padding: '0.85rem',
                background: 'rgba(255,255,255,0.6)', border: '1px solid rgba(229,224,219,0.4)', borderRadius: 14,
              }}>
                {orderData.product?.mainImage ? (
                  <img src={orderData.product.mainImage} alt="" style={{ width: 60, height: 60, borderRadius: 12, objectFit: 'cover', flexShrink: 0 }} />
                ) : (
                  <div style={{ width: 60, height: 60, borderRadius: 12, background: 'linear-gradient(135deg, #F5F2EE, #EDE8E3)', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#B5AEA8' }}>
                    <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
                  </div>
                )}
                <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                  <p style={{ fontWeight: 700, color: '#2A2520', fontSize: '0.93rem', lineHeight: 1.3, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {orderData.product?.name || '—'}
                  </p>
                  <p style={{ fontSize: '0.81rem', color: '#9B9590', marginTop: 2 }}>单价: ¥{orderData.product?.price?.toFixed(2) ?? '—'}</p>
                </div>
              </div>

              {/* Info grid - row 1 */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <InfoCell label="买家" value={orderData.buyer?.nickname || '—'} />
                <InfoCell label="卖家" value={orderData.seller?.nickname || '—'} />
                <InfoCell label="支付状态" value={PAYMENT_STATUS[orderData.paymentStatus] ?? '未知'} />
                <InfoCell label="支付金额" value={orderData.paidAmount != null ? `¥${orderData.paidAmount.toFixed(2)}` : '—'} />
              </div>

              {/* Time info */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <InfoCell label="下单时间" value={formatDateTime(orderData.createTime)} />
                <InfoCell label="支付时间" value={formatDateTime(orderData.payTime)} />
                <InfoCell label="更新时间" value={formatDateTime(orderData.updateTime)} />
                <InfoCell label="取消时间" value={formatDateTime(orderData.cancelTime)} />
              </div>

              {/* Payment No */}
              {(orderData.paymentNo || orderData.refundedAmount) && (
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  {orderData.paymentNo && <InfoCell label="支付单号" value={<span style={{ fontFamily: "'DM Sans', monospace", fontSize: '0.8rem' }}>{orderData.paymentNo}</span>} />}
                  {orderData.refundedAmount != null && <InfoCell label="退款金额" value={`¥${orderData.refundedAmount.toFixed(2)}`} />}
                </div>
              )}

              {/* Shipping address */}
              {orderData.shippingAddress && (
                <div style={{
                  padding: '0.85rem 1rem',
                  background: 'linear-gradient(135deg, rgba(251,191,36,0.03), rgba(195,155,211,0.02))',
                  borderRadius: 14, border: '1px solid rgba(229,224,219,0.35)',
                }}>
                  <p style={{ fontSize: '0.78rem', fontWeight: 600, color: '#6B6460', marginBottom: '0.45rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <svg width="14" height="14" fill="none" viewBox="0 0 24 24" stroke="#F97316" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                    收货地址
                  </p>
                  <p style={{ fontSize: '0.87rem', color: '#2A2520', lineHeight: 1.55 }}>
                    {orderData.shippingAddress.receiverName} {orderData.shippingAddress.phone}
                    <br />{orderData.shippingAddress.detailAddress}
                  </p>
                </div>
              )}

              {/* Remark / Cancel reason */}
              {orderData.remark && (
                <div style={{
                  padding: '0.85rem 1rem',
                  background: 'rgba(255,255,255,0.6)',
                  border: '1px solid rgba(229,224,219,0.4)', borderRadius: 14,
                }}>
                  <p style={{ fontSize: '0.78rem', fontWeight: 600, color: '#6B6460', marginBottom: '0.35rem' }}>备注</p>
                  <p style={{ fontSize: '0.87rem', color: '#4A4540', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>{orderData.remark}</p>
                </div>
              )}
              {orderData.cancelReason && (
                <div style={{
                  padding: '0.85rem 1rem',
                  background: 'linear-gradient(135deg, rgba(244,63,94,0.04), rgba(251,113,133,0.02))',
                  border: '1px solid rgba(244,63,94,0.15)', borderRadius: 14,
                }}>
                  <p style={{ fontSize: '0.78rem', fontWeight: 600, color: '#E11D48', marginBottom: '0.35rem' }}>取消原因</p>
                  <p style={{ fontSize: '0.87rem', color: '#4A4540', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>{orderData.cancelReason}</p>
                </div>
              )}
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', padding: '4rem 1rem', color: '#B5AEA8' }}>
              <span style={{ fontSize: '2rem', opacity: 0.4 }}>📭</span>
              <span style={{ fontSize: '0.9rem' }}>订单不存在或已被删除</span>
            </div>
          )}
        </div>
      </div>

      <style>{`
        @keyframes odFadeIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes odSlideIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes spin { to { transform: rotate(360deg); } }
      `}</style>
    </div>,
    document.body
  );
}
