import { useEffect, useState } from 'react';
import { useAdminProductDetail, useUpdateProductStatus } from '../../hooks/useAdminProducts';
import { ConfirmModal } from '../../components/ConfirmModal';
import type { ProductStatus } from '../../types/admin';

interface ProductDetailDrawerProps {
  open: boolean;
  productId: string | null;
  onClose: () => void;
  onSuccess: () => void;
}

const conditionLabels: Record<number, string> = {
  10: '全新', 9: '9成新', 8: '8成新', 7: '7成新', 6: '6成新及以下',
};

export function ProductDetailDrawer({ open, productId, onClose, onSuccess }: ProductDetailDrawerProps) {
  const [selectedImage, setSelectedImage] = useState(0);
  const [previewImage, setPreviewImage] = useState<string | null>(null);
  const [confirmModal, setConfirmModal] = useState<{
    open: boolean;
    type: 'approve' | 'reject';
  }>({ open: false, type: 'approve' });

  const { data: product, isLoading, refetch } = useAdminProductDetail(productId || '');
  const updateStatus = useUpdateProductStatus();

  useEffect(() => {
    if (open && productId) {
      setSelectedImage(0);
      setPreviewImage(null);
      refetch();
    }
  }, [open, productId, refetch]);

  useEffect(() => {
    if (!open) return;
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !previewImage) onClose();
    };
    document.addEventListener('keydown', handleEscape);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = '';
    };
  }, [open, previewImage, onClose]);

  const handleApprove = () => {
    setConfirmModal({ open: true, type: 'approve' });
  };

  const handleReject = () => {
    setConfirmModal({ open: true, type: 'reject' });
  };

  const handleConfirmAction = async () => {
    if (!product) return;
    const newStatus: ProductStatus = confirmModal.type === 'approve' ? 1 : 3;
    try {
      await updateStatus.mutateAsync({ id: product.id, data: { status: newStatus } });
      setConfirmModal({ open: false, type: 'approve' });
      onSuccess();
      onClose();
    } catch (error) {
      console.error('Failed to update product status:', error);
    }
  };

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleString('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit',
    });

  const formatPrice = (price: number) => `¥${price.toFixed(2)}`;

  if (!open) return null;

  return (
    <>
      <div style={{ position: 'fixed', inset: 0, zIndex: 40 }}>
        {/* Backdrop */}
        <div
          style={{
            position: 'absolute', inset: 0,
            background: 'rgba(42,37,32,0.4)',
            backdropFilter: 'blur(4px)',
            WebkitBackdropFilter: 'blur(4px)',
            animation: 'drawerFadeIn 0.25s ease-out',
          }}
          onClick={onClose}
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
          <div style={{ flex: 1, overflowY: 'auto', padding: '1.5rem' }}>
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
                    style={{
                      position: 'relative', width: '100%', aspectRatio: '16/10',
                      overflow: 'hidden', borderRadius: 16,
                      background: 'linear-gradient(135deg, #F5F2EE, #EDE8E3)',
                      cursor: product.images[selectedImage] ? 'pointer' : 'default',
                    }}
                    onClick={() => product.images[selectedImage] && setPreviewImage(product.images[selectedImage])}
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
                          onClick={() => setSelectedImage(index)}
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
                      {formatPrice(product.price)}
                    </span>
                    {product.originalPrice && product.originalPrice > product.price && (
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
                      { label: '发布时间', value: formatDate(product.createTime) },
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
                  <div style={{
                    display: 'flex', gap: '1.5rem', padding: '0.75rem 0',
                    borderTop: '1px solid rgba(229,224,219,0.4)',
                    borderBottom: '1px solid rgba(229,224,219,0.4)',
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.84rem', color: '#8B857E' }}>
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#F97316" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                      <span style={{ fontWeight: 600, color: '#4A4540' }}>{product.viewCount}</span> 次浏览
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.84rem', color: '#8B857E' }}>
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#FB7185" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                      </svg>
                      <span style={{ fontWeight: 600, color: '#4A4540' }}>{product.favoriteCount}</span> 人收藏
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

          {/* Footer actions */}
          {product && (
            <div style={{
              display: 'flex', gap: '0.65rem',
              padding: '1rem 1.5rem',
              borderTop: '1px solid rgba(229,224,219,0.4)',
              background: 'linear-gradient(180deg, rgba(250,248,245,0.5), rgba(250,248,245,0.9))',
            }}>
              <button
                onClick={handleApprove}
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
                onMouseEnter={(e) => { if (!updateStatus.isPending) { e.currentTarget.style.transform = 'translateY(-1px)'; e.currentTarget.style.boxShadow = '0 5px 18px rgba(16,185,129,0.38)'; } }}
                onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = updateStatus.isPending ? 'none' : '0 3px 12px rgba(16,185,129,0.28)'; }}
              >
                ✅ 通过审核
              </button>
              <button
                onClick={handleReject}
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
                onMouseEnter={(e) => { if (!updateStatus.isPending) { e.currentTarget.style.transform = 'translateY(-1px)'; e.currentTarget.style.boxShadow = '0 5px 18px rgba(244,63,94,0.38)'; } }}
                onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = updateStatus.isPending ? 'none' : '0 3px 12px rgba(244,63,94,0.28)'; }}
              >
                🚫 下架商品
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
                onMouseEnter={(e) => { if (!updateStatus.isPending) e.currentTarget.style.background = 'rgba(229,224,219,0.3)'; }}
                onMouseLeave={(e) => { e.currentTarget.style.background = '#fff'; }}
              >
                关闭
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Image preview overlay */}
      {previewImage && (
        <div
          style={{
            position: 'fixed', inset: 0, zIndex: 50,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            background: 'rgba(42,37,32,0.88)',
            backdropFilter: 'blur(8px)',
            WebkitBackdropFilter: 'blur(8px)',
            animation: 'drawerFadeIn 0.2s ease-out',
          }}
          onClick={() => setPreviewImage(null)}
        >
          <button
            style={{
              position: 'absolute', top: '1.25rem', right: '1.25rem',
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
            style={{ maxHeight: '90vh', maxWidth: '90vw', objectFit: 'contain', borderRadius: 12 }}
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}

      <ConfirmModal
        open={confirmModal.open}
        title={confirmModal.type === 'approve' ? '确认通过审核' : '确认下架商品'}
        content={confirmModal.type === 'approve' ? '确定要通过该商品的审核吗？通过后商品将上架销售。' : '确定要下架该商品吗？下架后商品将不再展示。'}
        confirmText={confirmModal.type === 'approve' ? '确认通过' : '确认下架'}
        onConfirm={handleConfirmAction}
        onCancel={() => setConfirmModal({ open: false, type: 'approve' })}
        loading={updateStatus.isPending}
        variant={confirmModal.type === 'approve' ? 'info' : 'danger'}
      />

      <style>{`
        @keyframes drawerFadeIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes spin { to { transform: rotate(360deg); } }
      `}</style>
    </>
  );
}
