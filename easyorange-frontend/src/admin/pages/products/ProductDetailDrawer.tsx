import { useEffect, useState, useRef } from 'react';
import { createPortal } from 'react-dom';
import { useAdminProductDetail } from '../../hooks/useAdminProducts';
import { useAuditProduct, useAuditLogs } from '../../hooks/useAdminProductAudit';
import type { AuditLogVO, AuditDimension } from '../../types/admin';

interface ProductDetailDrawerProps {
  open: boolean;
  productId: number | null;
  onClose: () => void;
  onSuccess: () => void;
}

const conditionLabels: Record<number, string> = {
  10: '全新', 9: '9成新', 8: '8成新', 7: '7成新', 6: '6成新及以下',
};

const createInitialState = () => ({
  selectedImage: 0,
  previewImage: null as string | null,
  selectedDimensions: [] as AuditDimension[],
  auditRemark: '',
  rejectReason: '',
  showRejectModal: false,
});

export function ProductDetailDrawer({ open, productId, onClose, onSuccess }: ProductDetailDrawerProps) {
  const prevStateKeyRef = useRef<string | null>(null);
  const stateKey = open && productId ? `${open}-${productId}` : null;
  
  const [state, setState] = useState(createInitialState);
  const { selectedImage, previewImage, selectedDimensions, auditRemark, rejectReason, showRejectModal } = state;

  if (stateKey !== prevStateKeyRef.current) {
    prevStateKeyRef.current = stateKey;
    if (stateKey) {
      setState(createInitialState());
    }
  }

  const { data: product, isLoading, refetch } = useAdminProductDetail(productId ?? 0);
  const updateStatus = useAuditProduct();
  const auditLogs = useAuditLogs(productId);

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

  const handleApproveWithDimensions = () => {
    if (!product) {return;}
    updateStatus.mutateAsync({
      id: product.productId,
      data: { action: 1, dimensions: selectedDimensions, remark: auditRemark || undefined },
    }).then(() => {
      onSuccess();
      onClose();
    });
  };

  const handleRejectWithReason = async () => {
    if (!product || !rejectReason.trim()) {return;}
    try {
      await updateStatus.mutateAsync({
        id: product.productId,
        data: { action: 2, reason: rejectReason, dimensions: selectedDimensions, remark: auditRemark || undefined },
      });
      setState(prev => ({ ...prev, showRejectModal: false, rejectReason: '' }));
      onSuccess();
      onClose();
    } catch (error) {
      console.error('Failed to reject product:', error);
    }
  };

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleString('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit',
    });

  const formatPrice = (price: number) => `¥${price.toFixed(2)}`;

  if (!open) {return null;}

  return createPortal(
    <>
      <div style={{ position: 'fixed', inset: 0, zIndex: 9999 }}>
        {/* Backdrop */}
        <div
          role="button"
          tabIndex={0}
          style={{
            position: 'absolute', inset: 0,
            background: 'rgba(42,37,32,0.4)',
            backdropFilter: 'blur(4px)',
            WebkitBackdropFilter: 'blur(4px)',
            animation: 'drawerFadeIn 0.25s ease-out',
          }}
          onClick={onClose}
          onKeyDown={(e) => { if (e.key === 'Escape' || e.key === 'Enter') { onClose(); } }}
        />

        {/* Drawer panel */}
        <div style={{
          position: 'absolute', right: 0, top: 0, height: '100%',
          width: '100%', maxWidth: 520,
          background: 'rgba(255,255,255,0.94)',
          backdropFilter: 'blur(24px)',
          WebkitBackdropFilter: 'blur(24px)',
          borderLeft: '1px solid rgba(229,224,219,0.4)',
          boxShadow: '-16px 0 48px rgba(42,37,32,0.12)',
          transform: open ? 'translateX(0)' : 'translateX(100%)',
          transition: 'transform 0.35s cubic-bezier(0.16, 1, 0.3, 1)',
          display: 'flex', flexDirection: 'column',
          overflow: 'hidden',
        }}>
          {/* Header */}
          <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '1.15rem 1.5rem',
            borderBottom: '1px solid rgba(229,224,219,0.4)',
            position: 'relative',
          }}>
            <div style={{
              position: 'absolute', bottom: 0, left: '1.5rem', right: '1.5rem', height: 1,
              background: 'linear-gradient(90deg, rgba(251,113,133,0.12), rgba(195,155,211,0.08), transparent)',
            }} />
            <h2 style={{
              fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
              fontSize: '1.1rem', fontWeight: 700, color: '#2A2520',
              display: 'flex', alignItems: 'center', gap: '0.5rem',
            }}>
              <span style={{
                width: 26, height: 26, borderRadius: 8,
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                background: 'linear-gradient(135deg, #FB7185, #C39BD3)',
                color: '#fff', flexShrink: 0,
              }}>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" />
                </svg>
              </span>
              商品详情
            </h2>
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

          {/* Content */}
          <div style={{ flex: 1, overflowY: 'auto', padding: '1.5rem', minHeight: 0 }}>
            {isLoading ? (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.7rem', padding: '4rem 1rem' }}>
                <div style={{
                  width: 28, height: 28,
                  border: '2.5px solid #E5E0DB', borderTopColor: '#F97316',
                  borderRadius: '50%', animation: 'spin 0.7s linear infinite',
                }} />
                <span style={{ fontSize: '0.87rem', color: '#9B9590' }}>加载中...</span>
              </div>
            ) : product ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                {/* Image gallery */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  <div
                    role="button"
                    tabIndex={product.images[selectedImage] ? 0 : -1}
                    style={{
                      position: 'relative', width: '100%', aspectRatio: '16/10',
                      overflow: 'hidden', borderRadius: 16,
                      background: 'linear-gradient(135deg, #F5F2EE, #EDE8E3)',
                      cursor: product.images[selectedImage] ? 'pointer' : 'default',
                    }}
                    onClick={() => product.images[selectedImage] && setState(prev => ({ ...prev, previewImage: product.images[selectedImage] }))}
                    onKeyDown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && product.images[selectedImage]) { setState(prev => ({ ...prev, previewImage: product.images[selectedImage] })); } }}
                  >
                    {product.images[selectedImage] ? (
                      <img
                        src={product.images[selectedImage]}
                        alt={product.name}
                        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      />
                    ) : (
                      <div style={{ display: 'flex', height: '100%', alignItems: 'center', justifyContent: 'center', color: '#B5AEA8' }}>
                        <svg width="40" height="40" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                      </div>
                    )}
                    {product.images[selectedImage] && (
                      <div style={{
                        position: 'absolute', bottom: 8, right: 8,
                        background: 'rgba(42,37,32,0.55)', backdropFilter: 'blur(4px)',
                        color: '#fff', fontSize: '0.72rem', fontWeight: 500,
                        padding: '0.25rem 0.6rem', borderRadius: 8,
                      }}>
                        点击预览
                      </div>
                    )}
                  </div>

                  {product.images.length > 1 && (
                    <div style={{ display: 'flex', gap: '0.5rem', overflowX: 'auto', paddingBottom: '0.25rem' }}>
                      {product.images.map((img, index) => (
                        <button
                          key={index}
                          onClick={() => setState(prev => ({ ...prev, selectedImage: index }))}
                          style={{
                            flexShrink: 0, width: 56, height: 56, borderRadius: 12,
                            overflow: 'hidden',
                            border: selectedImage === index ? '2px solid #F97316' : '2px solid transparent',
                            padding: 0, cursor: 'pointer',
                            transition: 'border-color 0.15s ease',
                            boxShadow: selectedImage === index ? '0 2px 8px rgba(249,115,22,0.2)' : 'none',
                          }}
                        >
                          <img src={img} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        </button>
                      ))}
                    </div>
                  )}
                </div>

                {/* Product info */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div>
                    <h3 style={{
                      fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                      fontSize: '1.25rem', fontWeight: 700, color: '#2A2520',
                      lineHeight: 1.3,
                    }}>
                      {product.name}
                    </h3>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem' }}>
                    <span style={{
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: '1.5rem', fontWeight: 700,
                      background: 'linear-gradient(135deg, #F97316, #EA580C)',
                      WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
                      backgroundClip: 'text',
                    }}>
                      {formatPrice(product.price ?? 0)}
                    </span>
                    {product.originalPrice && product.originalPrice > (product.price ?? 0) && (
                      <span style={{ fontSize: '0.85rem', color: '#B5AEA8', textDecoration: 'line-through' }}>
                        {formatPrice(product.originalPrice)}
                      </span>
                    )}
                  </div>

                  {/* Detail grid */}
                  <div style={{
                    display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem',
                  }}>
                    {[
                      { label: '新旧程度', value: conditionLabels[product.conditionLevel || 8] || '未知' },
                      { label: '分类', value: product.categoryName },
                      { label: '卖家', value: product.sellerName },
                      { label: '发布时间', value: formatDate(product.createTime ?? '') },
                    ].map((item) => (
                      <div key={item.label} style={{
                        padding: '0.6rem 0.8rem',
                        background: 'rgba(255,255,255,0.6)',
                        border: '1px solid rgba(229,224,219,0.4)',
                        borderRadius: 12,
                      }}>
                        <p style={{ fontSize: '0.72rem', color: '#9B9590', marginBottom: 2, fontWeight: 500 }}>{item.label}</p>
                        <p style={{ fontSize: '0.85rem', color: '#2A2520', fontWeight: 600 }}>{item.value}</p>
                      </div>
                    ))}
                  </div>

                  {/* Stats */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', padding: '0.75rem 0',
                      borderTop: '1px solid rgba(229,224,219,0.4)',
                      borderBottom: '1px solid rgba(229,224,219,0.4)',
                    }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.84rem', color: '#8B857E' }}>
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#F97316" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                      <span style={{ fontWeight: 600, color: '#4A4540' }}>{product.viewCount ?? 0}</span> 次浏览
                    </div>
                  </div>

                  {/* Description */}
                  {product.description && (
                    <div style={{
                      padding: '1rem',
                      background: 'linear-gradient(135deg, rgba(249,115,22,0.03), rgba(195,155,211,0.02))',
                      borderRadius: 14,
                      border: '1px solid rgba(229,224,219,0.35)',
                    }}>
                      <h4 style={{ fontSize: '0.82rem', fontWeight: 600, color: '#6B6460', marginBottom: '0.45rem' }}>商品描述</h4>
                      <p style={{ fontSize: '0.87rem', color: '#4A4540', lineHeight: 1.65, whiteSpace: 'pre-wrap' }}>{product.description}</p>
                    </div>
                  )}

                  {/* 审核记录时间线 */}
                  {auditLogs.data && auditLogs.data.length > 0 && (
                    <div style={{
                      padding: '1rem',
                      background: 'linear-gradient(135deg, rgba(249,115,22,0.03), rgba(195,155,211,0.02))',
                      borderRadius: 14,
                      border: '1px solid rgba(229,224,219,0.35)',
                    }}>
                      <h4 style={{ fontSize: '0.82rem', fontWeight: 600, color: '#6B6460', marginBottom: '0.65rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                        📜 审核记录
                      </h4>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.7rem' }}>
                        {auditLogs.data.map((log: AuditLogVO) => (
                          <div key={log.id} style={{ display: 'flex', gap: '0.6rem', alignItems: 'flex-start' }}>
                            <div style={{
                              width: 8, height: 8, borderRadius: '50%', marginTop: '5px', flexShrink: 0,
                              background: log.action === 1 ? '#10B981' : log.action === 2 ? '#F43F5E' : '#D6CEC5',
                              border: log.action === 3 ? '1.5px solid #D6CEC5' : 'none',
                            }} />
                            <div style={{ flex: 1, minWidth: 0 }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.15rem', flexWrap: 'wrap' }}>
                                <span style={{ fontSize: '0.78rem', color: '#9B9590' }}>{log.createTime?.replace('T', ' ').slice(0, 16)}</span>
                                <span style={{ fontSize: '0.81rem', fontWeight: 600, color: '#2A2520' }}>{log.operatorName}</span>
                                <span style={{
                                  fontSize: '0.75rem', fontWeight: 600, padding: '0.1rem 0.45rem', borderRadius: 6,
                                  color: log.action === 1 ? '#059669' : log.action === 2 ? '#E11D48' : '#8B857E',
                                  background: log.action === 1 ? 'rgba(16,185,129,0.08)' : log.action === 2 ? 'rgba(244,63,94,0.08)' : 'rgba(139,133,126,0.08)',
                                }}>
                                  {log.actionDesc}
                                </span>
                              </div>
                              {log.reason && (
                                <div style={{ fontSize: '0.82rem', color: '#4A4540', lineHeight: 1.55 }}>{log.reason}</div>
                              )}
                              {log.dimensions && log.dimensions.length > 0 && (
                                <div style={{ display: 'flex', gap: '0.3rem', marginTop: '0.2rem', flexWrap: 'wrap' }}>
                                  {log.dimensions.map((d) => (
                                    <span key={d} style={{
                                      fontSize: '0.72rem', padding: '0.1rem 0.4rem', borderRadius: 4,
                                      background: 'rgba(249,115,22,0.06)', color: '#C2410C',
                                    }}>
                                      {d === 'basic' ? '基本信息' : d === 'compliance' ? '内容合规' : d === 'image' ? '图片质量' : '价格合理'}
                                    </span>
                                  ))}
                                </div>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Location */}
                  {product.location && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', fontSize: '0.85rem' }}>
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#F97316" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                        <path d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                      <span style={{ color: '#9B9590' }}>交易地点：</span>
                      <span style={{ color: '#2A2520', fontWeight: 600 }}>{product.location}</span>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', padding: '4rem 1rem', color: '#B5AEA8' }}>
                <span style={{ fontSize: '2rem', opacity: 0.4 }}>📭</span>
                <span style={{ fontSize: '0.9rem' }}>商品不存在或已被删除</span>
              </div>
            )}
          </div>

          {/* Footer actions - 完整审核操作区 */}
          {product && (
            <div style={{
              display: 'flex', flexDirection: 'column', gap: '0.75rem',
              padding: '1rem 1.5rem',
              borderTop: '1px solid rgba(229,224,219,0.4)',
              background: 'linear-gradient(180deg, rgba(250,248,245,0.5), rgba(250,248,245,0.9))',
            }}>
              {/* 审核维度 */}
              <div>
                <div style={{ fontSize: '0.78rem', fontWeight: 600, color: '#6B6460', marginBottom: '0.45rem' }}>
                  审核维度
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                  {[
                    { key: 'basic' as AuditDimension, label: '基本信息合规' },
                    { key: 'compliance' as AuditDimension, label: '内容无违规' },
                    { key: 'image' as AuditDimension, label: '图片质量合格' },
                    { key: 'price' as AuditDimension, label: '价格合理' },
                  ].map((dim) => (
                    <button
                      key={dim.key}
                      onClick={() => setState(prev => ({
                        ...prev,
                        selectedDimensions: prev.selectedDimensions.includes(dim.key)
                          ? prev.selectedDimensions.filter(d => d !== dim.key)
                          : [...prev.selectedDimensions, dim.key]
                      }))}
                      style={{
                        padding: '0.35rem 0.7rem', borderRadius: 8,
                        border: selectedDimensions.includes(dim.key)
                          ? '2px solid #F97316'
                          : '1.5px solid #E5E0DB',
                        background: selectedDimensions.includes(dim.key)
                          ? 'rgba(249,115,22,0.08)'
                          : '#fff',
                        color: selectedDimensions.includes(dim.key) ? '#EA580C' : '#8B857E',
                        fontSize: '0.79rem', fontWeight: 500,
                        cursor: 'pointer', transition: 'all 0.15s ease',
                      }}
                    >
                      {selectedDimensions.includes(dim.key) ? '✓ ' : ''}
                      {dim.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* 审核意见 */}
              <div>
                <textarea
                  placeholder="审核意见（选填）..."
                  value={auditRemark}
                  onChange={(e) => setState(prev => ({ ...prev, auditRemark: e.target.value }))}
                  style={{
                    width: '100%', padding: '0.6rem 0.8rem', borderRadius: 10,
                    border: '1.5px solid #E5E0DB', background: '#fff',
                    fontSize: '0.84rem', color: '#2A2520', resize: 'vertical',
                    minHeight: '52px', outline: 'none', fontFamily: 'inherit',
                  }}
                  onFocus={(e) => { e.currentTarget.style.borderColor = '#F97316'; }}
                  onBlur={(e) => { e.currentTarget.style.borderColor = '#E5E0DB'; }}
                />
              </div>

              {/* 操作按钮行 */}
              <div style={{ display: 'flex', gap: '0.65rem' }}>
                <button
                  onClick={() => handleApproveWithDimensions()}
                  disabled={updateStatus.isPending}
                  style={{
                    flex: 1, padding: '0.65rem 1rem', borderRadius: 12,
                    border: 'none',
                    background: updateStatus.isPending ? '#D6CEC5' : 'linear-gradient(135deg, #10B981, #059669)',
                    color: '#fff', fontSize: '0.87rem', fontWeight: 600,
                    cursor: updateStatus.isPending ? 'not-allowed' : 'pointer',
                    transition: 'all 0.2s ease',
                    boxShadow: updateStatus.isPending ? 'none' : '0 3px 12px rgba(16,185,129,0.28)',
                  }}
                  onMouseEnter={(e) => {
                    if (!updateStatus.isPending) {
                      e.currentTarget.style.transform = 'translateY(-1px)';
                      e.currentTarget.style.boxShadow = '0 5px 18px rgba(16,185,129,0.38)';
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!updateStatus.isPending) {
                      e.currentTarget.style.transform = 'translateY(0)';
                      e.currentTarget.style.boxShadow = '0 3px 12px rgba(16,185,129,0.28)';
                    }
                  }}
                >
                  ✅ 通过审核
                </button>
                <button
                  onClick={() => setState(prev => ({ ...prev, showRejectModal: true }))}
                  disabled={updateStatus.isPending}
                  style={{
                    flex: 1, padding: '0.65rem 1rem', borderRadius: 12,
                    border: 'none',
                    background: updateStatus.isPending ? '#D6CEC5' : 'linear-gradient(135deg, #F43F5E, #E11D48)',
                    color: '#fff', fontSize: '0.87rem', fontWeight: 600,
                    cursor: updateStatus.isPending ? 'not-allowed' : 'pointer',
                    transition: 'all 0.2s ease',
                    boxShadow: updateStatus.isPending ? 'none' : '0 3px 12px rgba(244,63,94,0.28)',
                  }}
                  onMouseEnter={(e) => {
                    if (!updateStatus.isPending) {
                      e.currentTarget.style.transform = 'translateY(-1px)';
                      e.currentTarget.style.boxShadow = '0 5px 18px rgba(244,63,94,0.38)';
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!updateStatus.isPending) {
                      e.currentTarget.style.transform = 'translateY(0)';
                      e.currentTarget.style.boxShadow = '0 3px 12px rgba(244,63,94,0.28)';
                    }
                  }}
                >
                  🚫 驳回商品
                </button>
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
            </div>
          )}
        </div>
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
            animation: 'drawerFadeIn 0.2s ease-out',
          }}
          onClick={() => setState(prev => ({ ...prev, previewImage: null }))}
          onKeyDown={(e) => { if (e.key === 'Escape' || e.key === 'Enter') { setState(prev => ({ ...prev, previewImage: null })); } }}
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
            onClick={() => setState(prev => ({ ...prev, previewImage: null }))}
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

      {/* 驳回弹窗 */}
      {showRejectModal && (
        <div style={{
          position: 'fixed', inset: 0, zIndex: 50,
          background: 'rgba(42,37,32,0.5)', backdropFilter: 'blur(4px)',
        }}>
          <div style={{
            position: 'fixed', left: '50%', top: '50%',
            transform: 'translate(-50%, -50%)',
            width: 'calc(100% - 2rem)', maxWidth: 420,
            background: 'rgba(255,255,255,0.96)', backdropFilter: 'blur(24px)',
            borderRadius: 20, padding: '1.5rem',
            border: '1px solid rgba(229,224,219,0.4)',
            boxShadow: '0 20px 60px rgba(42,37,32,0.15)',
            animation: 'modalIn 0.25s ease-out',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#E11D48', display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
                ⚠️ 确认驳回商品
              </h3>
              <button onClick={() => { setState(prev => ({ ...prev, showRejectModal: false, rejectReason: '' })); }} style={{
                width: 30, height: 30, borderRadius: 8, border: '1.5px solid #E5E0DB', background: '#fff',
                color: '#8B857E', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>✕</button>
            </div>

            <p style={{ fontSize: '0.85rem', color: '#6B6460', marginBottom: '0.85rem', lineHeight: 1.5 }}>
              确定要驳回该商品吗？驳回后卖家可修改并重新提交。
            </p>

            {/* 快捷理由选项 */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem', marginBottom: '0.75rem' }}>
              {['信息不完整', '图片模糊', '疑似虚假信息', '价格异常', '违规内容', '其他'].map((tag) => (
                <button key={tag} onClick={() => {
                  setState(prev => ({ ...prev, rejectReason: (prev.rejectReason ? `${prev.rejectReason}；` : '') + tag }));
                }} style={{
                  padding: '0.28rem 0.55rem', borderRadius: 6,
                  border: '1.5px solid #E5E0DB', background: '#fff',
                  color: '#8B857E', fontSize: '0.76rem', fontWeight: 500, cursor: 'pointer',
                  transition: 'all 0.12s ease',
                }}
                onMouseEnter={(e) => { e.currentTarget.style.borderColor = '#F43F5E'; e.currentTarget.style.color = '#E11D48'; }}
                onMouseLeave={(e) => { e.currentTarget.style.borderColor = '#E5E0DB'; e.currentTarget.style.color = '#8B857E'; }}
                >{tag}</button>
              ))}
            </div>

            {/* 原因输入框 */}
            <textarea
              placeholder="请填写驳回原因（必填）..."
              value={rejectReason}
              onChange={(e) => setState(prev => ({ ...prev, rejectReason: e.target.value }))}
              rows={3}
              style={{
                width: '100%', padding: '0.65rem 0.8rem', borderRadius: 10,
                border: '2px solid #E5E0DB', background: '#fff',
                fontSize: '0.85rem', color: '#2A2520', resize: 'vertical',
                outline: 'none', fontFamily: 'inherit', marginBottom: '1rem',
              }}
              onFocus={(e) => { e.currentTarget.style.borderColor = '#F43F5E'; }}
              onBlur={(e) => { e.currentTarget.style.borderColor = rejectReason ? '#F97316' : '#E5E0DB'; }}
            />

            <div style={{ display: 'flex', gap: '0.6rem', justifyContent: 'flex-end' }}>
              <button onClick={() => { setState(prev => ({ ...prev, showRejectModal: false, rejectReason: '' })); }} style={{
                padding: '0.55rem 1.1rem', borderRadius: 10, border: '1.5px solid #E5E0DB',
                background: '#fff', color: '#6B6460', fontSize: '0.84rem', fontWeight: 600, cursor: 'pointer',
              }}>取消</button>
              <button
                onClick={handleRejectWithReason}
                disabled={!rejectReason.trim() || updateStatus.isPending}
                style={{
                  padding: '0.55rem 1.3rem', borderRadius: 10, border: 'none',
                  background: (!rejectReason.trim() || updateStatus.isPending) ? '#D6CEC5' : 'linear-gradient(135deg, #F43F5E, #E11D48)',
                  color: '#fff', fontSize: '0.84rem', fontWeight: 600,
                  cursor: (!rejectReason.trim() || updateStatus.isPending) ? 'not-allowed' : 'pointer',
                  transition: 'all 0.2s ease',
                }}
              >确认驳回</button>
            </div>
          </div>
        </div>
      )}

      <style>{`
        @keyframes drawerFadeIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes spin { to { transform: rotate(360deg); } }
        @keyframes modalIn { from { opacity: 0; } to { opacity: 1; } }
      `}</style>
    </>,
    document.body
  );
}
