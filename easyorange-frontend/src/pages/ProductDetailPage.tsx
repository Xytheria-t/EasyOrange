import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MapPin, User, Eye, Heart, Share2, MessageCircle, ChevronLeft, ChevronRight, Pencil, ShoppingCart, X } from 'lucide-react';
import { useQueryClient, useQuery } from '@tanstack/react-query';
import { useProduct } from '@/hooks';
import { useCreateOrder } from '@/hooks/useOrders';
import { favoriteApi } from '@/api/favoriteApi';
import { productApi } from '@/api/productApi';
import { CONDITION_LABEL_MAP, STATUS_LABEL_MAP } from '@/types/product';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import { Image } from '@/components/ui/Image';
import '@/styles/products-premium.css';

interface OrderFormData {
  address: string;
  phone: string;
  remark: string;
}

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: product, isLoading } = useProduct(Number(id));
  const { token, user } = useAuthStore();
  const addToast = useUIStore((s) => s.addToast);
  const queryClient = useQueryClient();
  const createOrder = useCreateOrder();
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [isFavoriteLoading, setIsFavoriteLoading] = useState(false);
  const [showOrderModal, setShowOrderModal] = useState(false);
  const [orderForm, setOrderForm] = useState<OrderFormData>({ address: '', phone: '', remark: '' });

  const productId = Number(id);

  const { data: isFavorited = false } = useQuery({
    queryKey: ['favorite-check', productId],
    queryFn: async () => {
      const res = await favoriteApi.check(productId);
      return res.data === true;
    },
    enabled: !!token && productId > 0,
    staleTime: 30 * 1000,
  });

  useEffect(() => {
    if (productId > 0) {
      productApi.incrementView(productId).catch(() => {
      });
    }
  }, [productId]);

  if (isLoading) return (
    <div className="loading-container">
      <div className="loading-spinner-lg"></div>
      <span className="loading-text">加载中...</span>
    </div>
  );

  if (!product) return (
    <div className="empty-state-container">
      <div className="empty-state-icon">📦</div>
      <p className="empty-state-text">商品不存在</p>
    </div>
  );

  const images = product.images?.length > 0
    ? product.images
    : ['/placeholder-product.png'];

  const isOwner = user && product.sellerId === user.userId;
  const conditionLabel = product.conditionLevel
    ? CONDITION_LABEL_MAP[product.conditionLevel] ?? product.condition
    : product.condition;
  const statusLabel = STATUS_LABEL_MAP[product.status as keyof typeof STATUS_LABEL_MAP] ?? product.status;

  const handlePrevImage = () => {
    setCurrentImageIndex(prev => (prev - 1 + images.length) % images.length);
  };

  const handleNextImage = () => {
    setCurrentImageIndex(prev => (prev + 1) % images.length);
  };

  const handleFavoriteToggle = async () => {
    if (!token) {
      navigate('/login');
      return;
    }
    setIsFavoriteLoading(true);
    try {
      if (isFavorited) {
        await favoriteApi.remove(productId);
        addToast({ type: 'success', message: '已取消收藏' });
      } else {
        await favoriteApi.add(productId);
        addToast({ type: 'success', message: '已收藏' });
      }
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
      queryClient.invalidateQueries({ queryKey: ['favorite-check', productId] });
    } catch {
      addToast({ type: 'error', message: isFavorited ? '取消收藏失败' : '收藏失败' });
    } finally {
      setIsFavoriteLoading(false);
    }
  };

  const handleBuyClick = () => {
    if (!token) {
      navigate('/login');
      return;
    }
    if (!product || isOwner) return;
    setShowOrderModal(true);
  };

  const handleOrderFormChange = (field: keyof OrderFormData, value: string) => {
    setOrderForm(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmitOrder = async () => {
    if (!orderForm.address.trim()) {
      addToast({ type: 'error', message: '请填写收货地址' });
      return;
    }
    if (!orderForm.phone.trim() || !/^1[3-9]\d{9}$/.test(orderForm.phone)) {
      addToast({ type: 'error', message: '请填写正确的手机号' });
      return;
    }

    try {
      const order = await createOrder.mutateAsync({
        productId: product.id,
        address: orderForm.address.trim(),
        phone: orderForm.phone.trim(),
        remark: orderForm.remark.trim() || undefined,
      });
      setShowOrderModal(false);
      setOrderForm({ address: '', phone: '', remark: '' });
      addToast({ type: 'success', message: '订单创建成功' });
      navigate(`/orders/${order.id}`);
    } catch {
      addToast({ type: 'error', message: '创建订单失败，请重试' });
    }
  };

  return (
    <div className="product-detail-page">
      <div className="product-detail-header">
        <h1 className="product-detail-title">{product.title}</h1>
        <div className="product-detail-meta">
          <span className="product-meta-item">
            <Eye size={14} />
            {product.views} 次浏览
          </span>
          <span className="product-meta-item">
            <Heart size={14} />
            {product.favorites} 人收藏
          </span>
          <span className="product-meta-item" style={{
            fontSize: '0.8rem',
            padding: '2px 8px',
            borderRadius: 4,
            background: product.status === 'ONLINE' ? 'var(--color-success)' : product.status === 'SOLD' ? 'var(--color-danger)' : 'var(--bg-tertiary)',
            color: '#fff',
          }}>
            {statusLabel}
          </span>
        </div>
      </div>

      <div className="product-detail-grid">
        <div className="product-image-container">
          <div className="product-image-main-wrapper">
            <Image
              src={images[currentImageIndex]}
              alt={`${product.title} - 图片 ${currentImageIndex + 1}`}
              className="product-detail-image"
              loading="eager"
              fetchPriority="high"
              placeholder="blur"
              style={{ width: '100%', aspectRatio: '1', objectFit: 'cover' }}
            />
            {images.length > 1 && (
              <>
                <button
                  onClick={handlePrevImage}
                  className="gallery-nav-btn prev"
                >
                  <ChevronLeft size={18} />
                </button>
                <button
                  onClick={handleNextImage}
                  className="gallery-nav-btn next"
                >
                  <ChevronRight size={18} />
                </button>
                <div className="gallery-dots">
                  {images.map((_, idx) => (
                    <button
                      key={idx}
                      onClick={() => setCurrentImageIndex(idx)}
                      className={`gallery-dot ${idx === currentImageIndex ? 'active' : ''}`}
                    />
                  ))}
                </div>
              </>
            )}
          </div>
          {images.length > 1 && (
            <div className="gallery-thumbnails-premium">
              {images.map((img, idx) => (
                <button
                  key={idx}
                  onClick={() => setCurrentImageIndex(idx)}
                  className={`gallery-thumb-premium ${idx === currentImageIndex ? 'active' : ''}`}
                >
                  <Image
                    src={img}
                    alt={`缩略图 ${idx + 1}`}
                    loading="lazy"
                    placeholder="skeleton"
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                </button>
              ))}
            </div>
          )}
          <div className="product-image-actions">
            <button
              className={`image-action-btn ${isFavorited ? 'favorited' : ''}`}
              onClick={handleFavoriteToggle}
              disabled={isFavoriteLoading}
              style={{ color: isFavorited ? 'var(--color-danger)' : undefined }}
            >
              <Heart size={18} fill={isFavorited ? 'currentColor' : 'none'} />
            </button>
            <button className="image-action-btn">
              <Share2 size={18} />
            </button>
          </div>
        </div>

        <div className="product-detail-info">
          <div className="product-price-card">
            <div className="product-price-main">
              <span className="price-value-lg">¥{product.price.toFixed(2)}</span>
              {product.originalPrice && (
                <span className="price-original-lg">¥{product.originalPrice.toFixed(2)}</span>
              )}
            </div>

            <div className="product-details-list">
              <div className="product-detail-row">
                <MapPin size={18} className="detail-icon" />
                <div className="detail-content">
                  <p className="detail-label">交易地点</p>
                  <p className="detail-value">{product.location || '未指定'}</p>
                </div>
              </div>

              <div className="product-detail-row">
                <User size={18} className="detail-icon" />
                <div className="detail-content">
                  <p className="detail-label">卖家</p>
                  <p className="detail-value">{product.sellerName}</p>
                </div>
              </div>

              <div className="product-detail-tags">
                <span className="product-tag-premium">{conditionLabel}</span>
                <span className="product-tag-premium">{product.categoryName}</span>
              </div>
            </div>

            <div className="product-action-buttons">
              {isOwner ? (
                <>
                  <button
                    className="btn-premium btn-premium-primary flex-1"
                    onClick={() => navigate(`/products/${id}/edit`)}
                  >
                    <Pencil size={18} />
                    编辑商品
                  </button>
                </>
              ) : (
                <>
                  <button
                    className="btn-premium btn-premium-primary flex-1"
                    onClick={handleBuyClick}
                    disabled={product?.status === 'SOLD'}
                  >
                    <ShoppingCart size={18} />
                    {product?.status === 'SOLD' ? '已售出' : '立即购买'}
                  </button>
                  <button className="btn-premium btn-premium-outline flex-1">
                    <MessageCircle size={18} />
                    联系卖家
                  </button>
                  <button
                    className={`btn-premium btn-premium-icon ${isFavorited ? 'favorited' : ''}`}
                    onClick={handleFavoriteToggle}
                    disabled={isFavoriteLoading}
                    style={{ minWidth: 52 }}
                  >
                    <Heart size={18} fill={isFavorited ? 'currentColor' : 'none'} />
                  </button>
                </>
              )}
            </div>
          </div>

          <div className="product-description-card">
            <h3 className="description-title">商品描述</h3>
            <p className="description-text">{product.description || '暂无描述'}</p>
          </div>
        </div>
      </div>

      {showOrderModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center" style={{ background: 'rgba(0,0,0,0.5)' }}>
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6">
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-lg font-bold text-gray-900">确认购买</h2>
              <button onClick={() => setShowOrderModal(false)} className="p-1 rounded-lg hover:bg-gray-100">
                <X size={20} className="text-gray-400" />
              </button>
            </div>

            <div className="flex gap-3 mb-5 p-3 bg-gray-50 rounded-xl">
              <div className="w-16 h-16 rounded-lg overflow-hidden flex-shrink-0">
                {images.length > 0 ? (
                  <Image
                    src={images[0]}
                    alt={product.title}
                    loading="lazy"
                    placeholder="skeleton"
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                ) : (
                  <div className="w-full h-full bg-gray-200 flex items-center justify-center">
                    <ShoppingCart size={20} className="text-gray-400" />
                  </div>
                )}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">{product.title}</p>
                <p className="text-lg font-bold text-primary-600 mt-1">¥{product.price.toFixed(2)}</p>
              </div>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  收货地址 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={orderForm.address}
                  onChange={(e) => handleOrderFormChange('address', e.target.value)}
                  placeholder="请输入收货地址"
                  className="w-full rounded-xl border border-gray-300 px-4 py-2.5 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500 outline-none"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  联系电话 <span className="text-red-500">*</span>
                </label>
                <input
                  type="tel"
                  value={orderForm.phone}
                  onChange={(e) => handleOrderFormChange('phone', e.target.value)}
                  placeholder="请输入手机号"
                  maxLength={11}
                  className="w-full rounded-xl border border-gray-300 px-4 py-2.5 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500 outline-none"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">备注</label>
                <textarea
                  value={orderForm.remark}
                  onChange={(e) => handleOrderFormChange('remark', e.target.value)}
                  placeholder="选填，对卖家留言"
                  rows={2}
                  className="w-full rounded-xl border border-gray-300 px-4 py-2.5 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500 outline-none resize-none"
                />
              </div>
            </div>

            <div className="flex gap-3 mt-6">
              <button
                onClick={() => setShowOrderModal(false)}
                className="flex-1 rounded-xl border border-gray-300 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                onClick={handleSubmitOrder}
                disabled={createOrder.isPending}
                className="flex-1 rounded-xl bg-primary-600 py-2.5 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-50"
              >
                {createOrder.isPending ? '提交中...' : '提交订单'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
