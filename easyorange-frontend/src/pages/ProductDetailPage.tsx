import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MapPin, User, Eye, Heart, Share2, MessageCircle, ChevronLeft, ChevronRight, Pencil, ShoppingCart, X, ArrowLeft, Clock, Shield, Tag, ChevronRight as BreadcrumbSep } from 'lucide-react';
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
  const [imageLoaded, setImageLoaded] = useState(false);

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

  useEffect(() => {
    setImageLoaded(false);
    setCurrentImageIndex(0);
  }, [productId]);

  if (isLoading) return (
    <div className="pdp-loading">
      <div className="pdp-loading-ambient">
        <div className="pdp-ambient-orb pdp-ambient-orb-1" />
        <div className="pdp-ambient-orb pdp-ambient-orb-2" />
      </div>
      <div className="pdp-loading-content">
        <div className="pdp-loading-ring" />
        <span className="pdp-loading-text">加载商品详情...</span>
      </div>
    </div>
  );

  if (!product) return (
    <div className="pdp-empty">
      <div className="pdp-empty-visual">
        <div className="pdp-empty-icon">📦</div>
        <div className="pdp-empty-ring" />
      </div>
      <h3 className="pdp-empty-title">商品不存在</h3>
      <p className="pdp-empty-desc">该商品可能已下架或被删除</p>
      <button className="pdp-empty-btn" onClick={() => navigate('/products')}>
        <ArrowLeft size={16} />
        返回商城
      </button>
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
  const isOnline = product.status === 'ONLINE';
  const isSold = product.status === 'SOLD';
  const hasDiscount = product.originalPrice != null && product.originalPrice > product.price;
  const discountPercent = hasDiscount
    ? Math.round((1 - product.price / product.originalPrice!) * 100)
    : 0;

  const handlePrevImage = () => {
    setCurrentImageIndex(prev => (prev - 1 + images.length) % images.length);
  };

  const handleNextImage = () => {
    setCurrentImageIndex(prev => (prev + 1) % images.length);
  };

  const handleFavoriteToggle = async () => {
    if (!token) {
      navigate(`/login?redirect=${encodeURIComponent(window.location.pathname)}`);
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
      navigate(`/login?redirect=${encodeURIComponent(window.location.pathname)}`);
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

  const formatRelativeTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / (1000 * 60));
    if (minutes < 1) return '刚刚';
    if (minutes < 60) return `${minutes}分钟前`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}小时前`;
    const days = Math.floor(hours / 24);
    if (days === 1) return '昨天';
    if (days < 7) return `${days}天前`;
    if (days < 30) return `${Math.floor(days / 7)}周前`;
    return `${date.getMonth() + 1}月${date.getDate()}日`;
  };

  return (
    <div className="pdp-page">
      <div className="pdp-ambient">
        <div className="pdp-ambient-orb pdp-ambient-orb-1" />
        <div className="pdp-ambient-orb pdp-ambient-orb-2" />
        <div className="pdp-ambient-orb pdp-ambient-orb-3" />
        <div className="pdp-ambient-noise" />
      </div>

      <div className="pdp-container">
        <nav className="pdp-breadcrumb">
          <button className="pdp-back-btn" onClick={() => navigate(-1)}>
            <ArrowLeft size={16} />
            返回
          </button>
          <div className="pdp-breadcrumb-trail">
            <span className="pdp-breadcrumb-item" onClick={() => navigate('/products')}>商城</span>
            <BreadcrumbSep size={14} className="pdp-breadcrumb-sep" />
            {product.categoryName && (
              <>
                <span className="pdp-breadcrumb-item">{product.categoryName}</span>
                <BreadcrumbSep size={14} className="pdp-breadcrumb-sep" />
              </>
            )}
            <span className="pdp-breadcrumb-current">{product.title}</span>
          </div>
        </nav>

        <div className="pdp-hero">
          <div className="pdp-gallery">
            <div className="pdp-gallery-main">
              <div className={`pdp-gallery-image-wrapper ${imageLoaded ? 'loaded' : ''}`}>
                <Image
                  src={images[currentImageIndex]}
                  alt={`${product.title} - 图片 ${currentImageIndex + 1}`}
                  className="pdp-gallery-image"
                  loading="eager"
                  fetchPriority="high"
                  placeholder="blur"
                  onLoad={() => setImageLoaded(true)}
                  style={{ width: '100%', aspectRatio: '1', objectFit: 'cover' }}
                />
              </div>

              {isSold && (
                <div className="pdp-sold-overlay">
                  <span className="pdp-sold-badge">已售出</span>
                </div>
              )}

              {images.length > 1 && (
                <>
                  <button onClick={handlePrevImage} className="pdp-gallery-nav pdp-gallery-prev">
                    <ChevronLeft size={20} />
                  </button>
                  <button onClick={handleNextImage} className="pdp-gallery-nav pdp-gallery-next">
                    <ChevronRight size={20} />
                  </button>
                  <div className="pdp-gallery-counter">
                    {currentImageIndex + 1} / {images.length}
                  </div>
                </>
              )}

              <div className="pdp-gallery-actions">
                <button
                  className={`pdp-action-fab ${isFavorited ? 'favorited' : ''}`}
                  onClick={handleFavoriteToggle}
                  disabled={isFavoriteLoading}
                >
                  <Heart size={18} fill={isFavorited ? 'currentColor' : 'none'} />
                </button>
                <button className="pdp-action-fab">
                  <Share2 size={18} />
                </button>
              </div>
            </div>

            {images.length > 1 && (
              <div className="pdp-gallery-thumbs">
                {images.map((img, idx) => (
                  <button
                    key={idx}
                    onClick={() => setCurrentImageIndex(idx)}
                    className={`pdp-thumb ${idx === currentImageIndex ? 'active' : ''}`}
                  >
                    <Image
                      src={img}
                      alt={`缩略图 ${idx + 1}`}
                      loading="lazy"
                      placeholder="skeleton"
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                    <div className="pdp-thumb-indicator" />
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="pdp-info">
            <div className="pdp-info-sticky">
              <div className="pdp-title-section">
                <div className="pdp-title-badges">
                  <span className={`pdp-status-chip ${isOnline ? 'online' : isSold ? 'sold' : 'offline'}`}>
                    {statusLabel}
                  </span>
                  {conditionLabel && (
                    <span className="pdp-condition-chip">{conditionLabel}</span>
                  )}
                  {product.categoryName && (
                    <span className="pdp-category-chip">
                      <Tag size={12} />
                      {product.categoryName}
                    </span>
                  )}
                </div>
                <h1 className="pdp-title">{product.title}</h1>
                <div className="pdp-meta-row">
                  <span className="pdp-meta-item">
                    <Eye size={14} />
                    {product.views} 次浏览
                  </span>
                  <span className="pdp-meta-item">
                    <Heart size={14} />
                    {product.favorites} 人收藏
                  </span>
                  <span className="pdp-meta-item">
                    <Clock size={14} />
                    {formatRelativeTime(product.createTime)}发布
                  </span>
                </div>
              </div>

              <div className="pdp-price-card">
                <div className="pdp-price-main">
                  <span className="pdp-price-value">¥{product.price.toFixed(2)}</span>
                  {hasDiscount && (
                    <div className="pdp-price-discount">
                      <span className="pdp-price-original">¥{product.originalPrice!.toFixed(2)}</span>
                      <span className="pdp-discount-badge">-{discountPercent}%</span>
                    </div>
                  )}
                </div>
                {hasDiscount && (
                  <div className="pdp-savings">
                    比原价省 <strong>¥{(product.originalPrice! - product.price).toFixed(2)}</strong>
                  </div>
                )}
              </div>

              <div className="pdp-details-card">
                <div className="pdp-detail-item">
                  <div className="pdp-detail-icon-wrap">
                    <MapPin size={16} />
                  </div>
                  <div className="pdp-detail-content">
                    <span className="pdp-detail-label">交易地点</span>
                    <span className="pdp-detail-value">{product.location || '未指定'}</span>
                  </div>
                </div>

                <div className="pdp-detail-item">
                  <div className="pdp-detail-icon-wrap">
                    <Shield size={16} />
                  </div>
                  <div className="pdp-detail-content">
                    <span className="pdp-detail-label">商品成色</span>
                    <span className="pdp-detail-value">{conditionLabel || '未标注'}</span>
                  </div>
                </div>

                <div className="pdp-detail-item pdp-seller-item">
                  <div className="pdp-detail-icon-wrap pdp-seller-avatar-wrap">
                    <User size={16} />
                  </div>
                  <div className="pdp-detail-content">
                    <span className="pdp-detail-label">卖家</span>
                    <span className="pdp-detail-value">{product.sellerName}</span>
                  </div>
                  <button
                    className="pdp-contact-btn"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <MessageCircle size={14} />
                    联系
                  </button>
                </div>
              </div>

              <div className="pdp-actions">
                {isOwner ? (
                  <button
                    className="pdp-btn pdp-btn-primary"
                    onClick={() => navigate(`/products/${id}/edit`)}
                  >
                    <Pencil size={18} />
                    编辑商品
                  </button>
                ) : (
                  <>
                    <button
                      className="pdp-btn pdp-btn-primary"
                      onClick={handleBuyClick}
                      disabled={isSold}
                    >
                      <ShoppingCart size={18} />
                      {isSold ? '已售出' : '立即购买'}
                    </button>
                    <button className="pdp-btn pdp-btn-secondary">
                      <MessageCircle size={18} />
                      联系卖家
                    </button>
                    <button
                      className={`pdp-btn pdp-btn-fav ${isFavorited ? 'favorited' : ''}`}
                      onClick={handleFavoriteToggle}
                      disabled={isFavoriteLoading}
                    >
                      <Heart size={18} fill={isFavorited ? 'currentColor' : 'none'} />
                    </button>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="pdp-description-section">
          <div className="pdp-section-header">
            <div className="pdp-section-accent" />
            <h3 className="pdp-section-title">商品描述</h3>
          </div>
          <div className="pdp-description-body">
            <p className="pdp-description-text">{product.description || '暂无描述'}</p>
          </div>
        </div>
      </div>

      {showOrderModal && (
        <div className="pdp-modal-overlay" onClick={() => setShowOrderModal(false)}>
          <div className="pdp-modal" onClick={(e) => e.stopPropagation()}>
            <div className="pdp-modal-header">
              <h2 className="pdp-modal-title">确认购买</h2>
              <button className="pdp-modal-close" onClick={() => setShowOrderModal(false)}>
                <X size={20} />
              </button>
            </div>

            <div className="pdp-modal-product">
              <div className="pdp-modal-product-image">
                {images.length > 0 ? (
                  <Image
                    src={images[0]}
                    alt={product.title}
                    loading="lazy"
                    placeholder="skeleton"
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                ) : (
                  <div className="pdp-modal-product-placeholder">
                    <ShoppingCart size={20} />
                  </div>
                )}
              </div>
              <div className="pdp-modal-product-info">
                <p className="pdp-modal-product-name">{product.title}</p>
                <p className="pdp-modal-product-price">¥{product.price.toFixed(2)}</p>
              </div>
            </div>

            <div className="pdp-modal-form">
              <div className="pdp-form-group">
                <label className="pdp-form-label">
                  收货地址 <span className="pdp-form-required">*</span>
                </label>
                <input
                  type="text"
                  value={orderForm.address}
                  onChange={(e) => handleOrderFormChange('address', e.target.value)}
                  placeholder="请输入收货地址"
                  className="pdp-form-input"
                />
              </div>
              <div className="pdp-form-group">
                <label className="pdp-form-label">
                  联系电话 <span className="pdp-form-required">*</span>
                </label>
                <input
                  type="tel"
                  value={orderForm.phone}
                  onChange={(e) => handleOrderFormChange('phone', e.target.value)}
                  placeholder="请输入手机号"
                  maxLength={11}
                  className="pdp-form-input"
                />
              </div>
              <div className="pdp-form-group">
                <label className="pdp-form-label">备注</label>
                <textarea
                  value={orderForm.remark}
                  onChange={(e) => handleOrderFormChange('remark', e.target.value)}
                  placeholder="选填，对卖家留言"
                  rows={3}
                  className="pdp-form-textarea"
                />
              </div>
            </div>

            <div className="pdp-modal-footer">
              <button
                onClick={() => setShowOrderModal(false)}
                className="pdp-modal-btn pdp-modal-btn-cancel"
              >
                取消
              </button>
              <button
                onClick={handleSubmitOrder}
                disabled={createOrder.isPending}
                className="pdp-modal-btn pdp-modal-btn-submit"
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
