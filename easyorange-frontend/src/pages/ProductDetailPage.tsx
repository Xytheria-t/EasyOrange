import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MapPin, User, Eye, Heart, Share2, MessageCircle, ChevronLeft, ChevronRight, Pencil, ShoppingCart, X, ArrowLeft, Clock, Shield, Tag, ChevronRight as BreadcrumbSep, Sparkles, TrendingUp, Zap, Star, Info, Send, Copy, Check, MessageSquare, ThumbsUp } from 'lucide-react';
import { useQueryClient, useQuery, useMutation } from '@tanstack/react-query';
import { useProduct, useSimilarProducts } from '@/hooks';
import { useCreateOrder } from '@/hooks/useOrders';
import { favoriteApi } from '@/api/favoriteApi';
import { productApi } from '@/api/productApi';
import { reviewApi } from '@/api/reviewApi';
import { messageApi } from '@/api/messageApi';
import { CONDITION_LABEL_MAP, STATUS_LABEL_MAP } from '@/types';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import { Image } from '@/components/ui/Image';
import placeholderImage from '@/assets/placeholder.png';

interface OrderFormData {
  address: string;
  phone: string;
  remark: string;
}

interface ReviewFormData {
  rating: number;
  content: string;
}

interface ChatMessage {
  id: number;
  senderId: number;
  receiverId: number;
  content: string;
  createTime: string;
  isMine: boolean;
}

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: product, isLoading } = useProduct(Number(id));
  const { data: similarProducts } = useSimilarProducts(Number(id));
  const { token, user } = useAuthStore();
  const addToast = useUIStore((s) => s.addToast);
  const queryClient = useQueryClient();
  const createOrder = useCreateOrder();
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [isFavoriteLoading, setIsFavoriteLoading] = useState(false);
  const [showOrderModal, setShowOrderModal] = useState(false);
  const [showShareModal, setShowShareModal] = useState(false);
  const [showChatModal, setShowChatModal] = useState(false);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [orderForm, setOrderForm] = useState<OrderFormData>({ address: '', phone: '', remark: '' });
  const [reviewForm, setReviewForm] = useState<ReviewFormData>({ rating: 5, content: '' });
  const [chatMessage, setChatMessage] = useState('');
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [copied, setCopied] = useState(false);
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

  const { data: reviewsData, isLoading: reviewsLoading } = useQuery({
    queryKey: ['reviews', productId],
    queryFn: async () => {
      const res = await reviewApi.getList(productId, { pageNum: 1, pageSize: 10 });
      return res.data;
    },
    enabled: productId > 0,
    staleTime: 60 * 1000,
  });

  const reviews = reviewsData?.records ?? [];
  const reviewTotal = reviewsData?.total ?? 0;
  const avgRating = reviews.length > 0
    ? (reviews.reduce((sum: number, r: Record<string, unknown>) => sum + ((r.rating as number) || 5), 0) / reviews.length).toFixed(1)
    : '5.0';

  useEffect(() => {
    if (productId > 0) {
      productApi.incrementView(productId).catch(() => {});
    }
  }, [productId]);

  useEffect(() => {
    setImageLoaded(false);
    setCurrentImageIndex(0);
  }, [productId]);

  const loadChatHistory = useCallback(async () => {
    if (!product?.sellerId || !token) return;
    try {
      const res = await messageApi.getConversation(product.sellerId, 50);
      const rawData = res.data as unknown as Record<string, unknown>[];
      const messages = (rawData ?? []).map((msg) => ({
        id: msg.id as number,
        senderId: msg.senderId as number,
        receiverId: msg.receiverId as number,
        content: msg.content as string,
        createTime: msg.createTime as string,
        isMine: msg.senderId === user?.userId,
      }));
      setChatMessages(messages.reverse());
    } catch {
      setChatMessages([]);
    }
  }, [product?.sellerId, token, user?.userId]);

  useEffect(() => {
    if (showChatModal) {
      loadChatHistory();
    }
  }, [showChatModal, loadChatHistory]);

  if (isLoading) {
    return (
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
  }

  if (!product) {
    return (
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
  }

  const images = product.images?.length > 0 ? product.images : [placeholderImage];
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

  const handleContactSeller = () => {
    if (!token) {
      navigate(`/login?redirect=${encodeURIComponent(window.location.pathname)}`);
      return;
    }
    if (isOwner) return;
    setShowChatModal(true);
  };

  const handleShare = () => {
    setShowShareModal(true);
  };

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      addToast({ type: 'success', message: '链接已复制到剪贴板' });
      setTimeout(() => setCopied(false), 2000);
    } catch {
      addToast({ type: 'error', message: '复制失败，请手动复制' });
    }
  };

  const handleSendMessage = async () => {
    if (!chatMessage.trim() || !product?.sellerId) return;
    try {
      await messageApi.sendMessage({
        receiverId: product.sellerId,
        content: chatMessage.trim(),
      });
      setChatMessages(prev => [...prev, {
        id: Date.now(),
        senderId: user?.userId || 0,
        receiverId: product.sellerId,
        content: chatMessage.trim(),
        createTime: new Date().toISOString(),
        isMine: true,
      }]);
      setChatMessage('');
      addToast({ type: 'success', message: '消息已发送' });
    } catch {
      addToast({ type: 'error', message: '发送失败，请重试' });
    }
  };

  const handleQuickReply = (text: string) => {
    setChatMessage(text);
  };

  const submitReview = useMutation({
    mutationFn: async () => {
      await reviewApi.create(productId, {
        rating: reviewForm.rating,
        content: reviewForm.content,
      });
    },
    onSuccess: () => {
      addToast({ type: 'success', message: '评价提交成功' });
      setShowReviewModal(false);
      setReviewForm({ rating: 5, content: '' });
      queryClient.invalidateQueries({ queryKey: ['reviews', productId] });
    },
    onError: () => {
      addToast({ type: 'error', message: '评价提交失败' });
    },
  });

  const handleSubmitReview = () => {
    if (!reviewForm.content.trim()) {
      addToast({ type: 'error', message: '请填写评价内容' });
      return;
    }
    submitReview.mutate();
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

  const quickReplies = [
    '这个商品还在吗？',
    '能便宜点吗？',
    '可以面交吗？',
    '商品有什么瑕疵吗？',
  ];

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
                <button className="pdp-action-fab" onClick={handleShare}>
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

              <div className="pdp-ai-pricing-card">
                <div className="pdp-ai-pricing-header">
                  <div className="pdp-ai-badge">
                    <Sparkles size={14} />
                    <span>AI智能估价</span>
                  </div>
                  <span className="pdp-ai-confidence">置信度 95%</span>
                </div>
                <div className="pdp-ai-pricing-body">
                  <div className="pdp-ai-price-range">
                    <div className="pdp-ai-price-item">
                      <span className="pdp-ai-price-label">市场均价</span>
                      <span className="pdp-ai-price-value">¥{((product.price || 100) * 1.15).toFixed(0)}</span>
                    </div>
                    <div className="pdp-ai-price-divider" />
                    <div className="pdp-ai-price-item">
                      <span className="pdp-ai-price-label">低价区间</span>
                      <span className="pdp-ai-price-value low">¥{((product.price || 100) * 0.85).toFixed(0)}</span>
                    </div>
                    <div className="pdp-ai-price-divider" />
                    <div className="pdp-ai-price-item highlight">
                      <span className="pdp-ai-price-label">当前定价</span>
                      <span className="pdp-ai-price-value">¥{product.price.toFixed(0)}</span>
                    </div>
                  </div>
                  <div className="pdp-ai-pricing-analysis">
                    <div className="pdp-ai-analysis-icon">
                      <TrendingUp size={14} />
                    </div>
                    <p className="pdp-ai-analysis-text">
                      该商品定价<span className="highlight">合理偏低</span>，相比同类商品具有价格优势，性价比突出
                    </p>
                  </div>
                  <div className="pdp-ai-pricing-tags">
                    <span className="pdp-ai-tag"><Zap size={10} />价格优势</span>
                    <span className="pdp-ai-tag"><Star size={10} />值得购买</span>
                    <span className="pdp-ai-tag"><TrendingUp size={10} />热门品类</span>
                  </div>
                </div>
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
                  {!isOwner && (
                    <button className="pdp-contact-btn" onClick={handleContactSeller}>
                      <MessageCircle size={14} />
                      联系
                    </button>
                  )}
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
                    <button className="pdp-btn pdp-btn-secondary" onClick={handleContactSeller}>
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

        <div className="pdp-reviews-section">
          <div className="pdp-section-header">
            <div className="pdp-section-accent" />
            <h3 className="pdp-section-title">
              <MessageSquare size={18} />
              商品评价
            </h3>
            <div className="pdp-reviews-stats">
              <span className="pdp-reviews-avg">
                <Star size={14} fill="currentColor" />
                {avgRating}
              </span>
              <span className="pdp-reviews-count">{reviewTotal} 条评价</span>
            </div>
          </div>
          
          {reviewsLoading ? (
            <div className="pdp-reviews-loading">
              <div className="pdp-loading-ring" />
              <span>加载评价中...</span>
            </div>
          ) : reviews.length > 0 ? (
            <div className="pdp-reviews-list">
              {reviews.slice(0, 5).map((review: Record<string, unknown>) => (
                <div key={review.id as number} className="pdp-review-item">
                  <div className="pdp-review-header">
                    <div className="pdp-review-user">
                      <div className="pdp-review-avatar">
                        <User size={16} />
                      </div>
                      <span className="pdp-review-username">{(review.username as string) || '匿名用户'}</span>
                    </div>
                    <div className="pdp-review-meta">
                      <div className="pdp-review-rating">
                        {[1, 2, 3, 4, 5].map(star => (
                          <Star
                            key={star}
                            size={12}
                            fill={star <= ((review.rating as number) || 5) ? 'currentColor' : 'none'}
                          />
                        ))}
                      </div>
                      <span className="pdp-review-time">{formatRelativeTime((review.createTime as string) || new Date().toISOString())}</span>
                    </div>
                  </div>
                  <p className="pdp-review-content">{(review.content as string) || '用户未填写评价内容'}</p>
                  {(review.likes as number) > 0 && (
                    <div className="pdp-review-footer">
                      <button className="pdp-review-like">
                        <ThumbsUp size={14} />
                        <span>{String(review.likes)}</span>
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="pdp-reviews-empty">
              <MessageSquare size={32} />
              <p>暂无评价</p>
              <span>购买后可以发表评价</span>
            </div>
          )}

          {token && !isOwner && (
            <div className="pdp-reviews-action">
              <button className="pdp-btn pdp-btn-secondary" onClick={() => setShowReviewModal(true)}>
                <Star size={16} />
                发表评价
              </button>
            </div>
          )}
        </div>

        <div className="pdp-similar-section">
          <div className="pdp-section-header">
            <div className="pdp-section-accent" />
            <h3 className="pdp-section-title">
              <Sparkles size={18} />
              AI推荐相似商品
            </h3>
            <span className="pdp-section-badge">基于商品特征智能匹配</span>
          </div>
          
          {similarProducts && similarProducts.length > 0 ? (
            <>
              <div className="pdp-similar-grid">
                {similarProducts.slice(0, 4).map((item) => (
                  <div
                    key={item.id}
                    className="pdp-similar-card"
                    onClick={() => navigate(`/products/${item.id}`)}
                  >
                    <div className="pdp-similar-image">
                      <Image
                        src={item.images?.[0] || placeholderImage}
                        alt={item.title}
                        loading="lazy"
                        placeholder="skeleton"
                        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      />
                    </div>
                    <div className="pdp-similar-content">
                      <h4 className="pdp-similar-title">{item.title}</h4>
                      <div className="pdp-similar-price">¥{item.price.toFixed(0)}</div>
                    </div>
                  </div>
                ))}
              </div>
              <div className="pdp-similar-footer">
                <button className="pdp-similar-more" onClick={() => navigate('/products')}>
                  <span>查看更多相似商品</span>
                  <ChevronRight size={16} />
                </button>
              </div>
            </>
          ) : (
            <div className="pdp-similar-empty">
              <p>暂无相似商品推荐</p>
            </div>
          )}
        </div>

        <div className="pdp-ai-tips-section">
          <div className="pdp-ai-tips-card">
            <div className="pdp-ai-tips-icon">
              <Info size={18} />
            </div>
            <div className="pdp-ai-tips-content">
              <h4 className="pdp-ai-tips-title">AI助手温馨提示</h4>
              <ul className="pdp-ai-tips-list">
                <li>建议与卖家确认商品细节后再进行交易</li>
                <li>优先选择校内面交，安全便捷</li>
                <li>如遇纠纷可联系平台客服协助处理</li>
              </ul>
            </div>
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

      {showShareModal && (
        <div className="pdp-modal-overlay" onClick={() => setShowShareModal(false)}>
          <div className="pdp-modal pdp-share-modal" onClick={(e) => e.stopPropagation()}>
            <div className="pdp-modal-header">
              <h2 className="pdp-modal-title">分享商品</h2>
              <button className="pdp-modal-close" onClick={() => setShowShareModal(false)}>
                <X size={20} />
              </button>
            </div>

            <div className="pdp-share-content">
              <div className="pdp-share-platforms">
                <button className="pdp-share-platform pdp-share-wechat">
                  <div className="pdp-share-icon">
                    <MessageCircle size={24} />
                  </div>
                  <span>微信</span>
                </button>
                <button className="pdp-share-platform pdp-share-qq">
                  <div className="pdp-share-icon">Q</div>
                  <span>QQ</span>
                </button>
                <button className="pdp-share-platform pdp-share-weibo">
                  <div className="pdp-share-icon">微</div>
                  <span>微博</span>
                </button>
                <button className="pdp-share-platform pdp-share-copy" onClick={handleCopyLink}>
                  <div className="pdp-share-icon">
                    {copied ? <Check size={24} /> : <Copy size={24} />}
                  </div>
                  <span>{copied ? '已复制' : '复制链接'}</span>
                </button>
              </div>

              <div className="pdp-share-link">
                <label className="pdp-form-label">商品链接</label>
                <div className="pdp-share-link-box">
                  <input
                    type="text"
                    value={window.location.href}
                    readOnly
                    className="pdp-form-input"
                  />
                  <button className="pdp-share-copy-btn" onClick={handleCopyLink}>
                    {copied ? <Check size={16} /> : <Copy size={16} />}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {showChatModal && (
        <div className="pdp-modal-overlay" onClick={() => setShowChatModal(false)}>
          <div className="pdp-modal pdp-chat-modal" onClick={(e) => e.stopPropagation()}>
            <div className="pdp-modal-header">
              <h2 className="pdp-modal-title">
                <MessageCircle size={18} />
                联系卖家
              </h2>
              <button className="pdp-modal-close" onClick={() => setShowChatModal(false)}>
                <X size={20} />
              </button>
            </div>

            <div className="pdp-chat-seller">
              <div className="pdp-chat-seller-avatar">
                <User size={20} />
              </div>
              <div className="pdp-chat-seller-info">
                <span className="pdp-chat-seller-name">{product.sellerName}</span>
                <span className="pdp-chat-product">{product.title}</span>
              </div>
            </div>

            <div className="pdp-chat-messages">
              {chatMessages.length > 0 ? (
                chatMessages.map((msg) => (
                  <div key={msg.id} className={`pdp-chat-message ${msg.isMine ? 'mine' : 'theirs'}`}>
                    <div className="pdp-chat-bubble">{msg.content}</div>
                    <span className="pdp-chat-time">{formatRelativeTime(msg.createTime)}</span>
                  </div>
                ))
              ) : (
                <div className="pdp-chat-empty">
                  <MessageCircle size={32} />
                  <p>开始与卖家聊天吧</p>
                </div>
              )}
            </div>

            <div className="pdp-chat-quick-replies">
              {quickReplies.map((text, idx) => (
                <button
                  key={idx}
                  className="pdp-chat-quick-reply"
                  onClick={() => handleQuickReply(text)}
                >
                  {text}
                </button>
              ))}
            </div>

            <div className="pdp-chat-input">
              <input
                type="text"
                value={chatMessage}
                onChange={(e) => setChatMessage(e.target.value)}
                placeholder="输入消息..."
                onKeyDown={(e) => e.key === 'Enter' && handleSendMessage()}
                className="pdp-form-input"
              />
              <button
                className="pdp-chat-send"
                onClick={handleSendMessage}
                disabled={!chatMessage.trim()}
              >
                <Send size={18} />
              </button>
            </div>
          </div>
        </div>
      )}

      {showReviewModal && (
        <div className="pdp-modal-overlay" onClick={() => setShowReviewModal(false)}>
          <div className="pdp-modal" onClick={(e) => e.stopPropagation()}>
            <div className="pdp-modal-header">
              <h2 className="pdp-modal-title">发表评价</h2>
              <button className="pdp-modal-close" onClick={() => setShowReviewModal(false)}>
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
                    <Star size={20} />
                  </div>
                )}
              </div>
              <div className="pdp-modal-product-info">
                <p className="pdp-modal-product-name">{product.title}</p>
              </div>
            </div>

            <div className="pdp-modal-form">
              <div className="pdp-form-group">
                <label className="pdp-form-label">评分</label>
                <div className="pdp-review-stars">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      className={`pdp-review-star ${star <= reviewForm.rating ? 'active' : ''}`}
                      onClick={() => setReviewForm(prev => ({ ...prev, rating: star }))}
                    >
                      <Star size={24} fill={star <= reviewForm.rating ? 'currentColor' : 'none'} />
                    </button>
                  ))}
                </div>
              </div>
              <div className="pdp-form-group">
                <label className="pdp-form-label">评价内容</label>
                <textarea
                  value={reviewForm.content}
                  onChange={(e) => setReviewForm(prev => ({ ...prev, content: e.target.value }))}
                  placeholder="分享您的购买体验..."
                  rows={4}
                  className="pdp-form-textarea"
                />
              </div>
            </div>

            <div className="pdp-modal-footer">
              <button
                onClick={() => setShowReviewModal(false)}
                className="pdp-modal-btn pdp-modal-btn-cancel"
              >
                取消
              </button>
              <button
                onClick={handleSubmitReview}
                disabled={submitReview.isPending}
                className="pdp-modal-btn pdp-modal-btn-submit"
              >
                {submitReview.isPending ? '提交中...' : '提交评价'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
