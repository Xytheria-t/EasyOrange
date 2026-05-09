import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Trash2, RefreshCw, ArrowRight, Sparkles, MapPin, Clock, TrendingDown, Bell, Zap, Brain } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { favoriteApi } from '@/api/favoriteApi';
import { useUIStore } from '@/store/uiStore';
import { CONDITION_LABEL_MAP } from '@/constants';
import type { Favorite } from '@/types';
import { Image } from '@/components/ui/Image';
import './favorites.css';

const CONDITION_ICONS: Record<number, string> = {
  1: '✨',
  2: '💎',
  3: '🌿',
  4: '📦',
};

function FavoritesPage() {
  const navigate = useNavigate();
  const addToast = useUIStore((s) => s.addToast);
  const queryClient = useQueryClient();
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const pageSize = 20;

  const { data: favoritesData, isLoading, error } = useQuery({
    queryKey: ['favorites', pageNum, pageSize],
    queryFn: async () => {
      const response = await favoriteApi.getList({ pageNum, pageSize });
      return response.data;
    },
    staleTime: 30 * 1000,
  });

  const removeMutation = useMutation({
    mutationFn: async (productId: string) => {
      await favoriteApi.remove(productId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
      addToast({ type: 'success', message: '已取消收藏' });
      setRemovingId(null);
    },
    onError: () => {
      addToast({ type: 'error', message: '取消收藏失败' });
      setRemovingId(null);
    },
  });

  const removeManyMutation = useMutation({
    mutationFn: async (ids: string[]) => {
      await favoriteApi.removeMany(ids);
    },
    onSuccess: () => {
      setSelectedIds(new Set());
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
      addToast({ type: 'success', message: '已批量取消收藏' });
    },
    onError: () => {
      addToast({ type: 'error', message: '批量取消收藏失败' });
    },
  });

  const favorites = favoritesData?.records ?? [];
  const total = favoritesData?.total ?? 0;
  const totalPages = favoritesData?.pages ?? Math.ceil(total / pageSize);

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const handleBatchRemove = () => {
    if (selectedIds.size === 0) {return;}
    removeManyMutation.mutate(Array.from(selectedIds));
  };

  const handleRemove = (e: React.MouseEvent, productId: string) => {
    e.stopPropagation();
    e.preventDefault();
    setRemovingId(productId);
    removeMutation.mutate(productId);
  };

  const formatPrice = (price: number) => {
    return price % 1 === 0 ? price.toString() : price.toFixed(2);
  };

  const formatRelativeTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / (1000 * 60));
    if (minutes < 1) {return '刚刚';}
    if (minutes < 60) {return `${minutes}分钟前`;}
    const hours = Math.floor(minutes / 60);
    if (hours < 24) {return `${hours}小时前`;}
    const days = Math.floor(hours / 24);
    if (days === 1) {return '昨天';}
    if (days < 7) {return `${days}天前`;}
    if (days < 30) {return `${Math.floor(days / 7)}周前`;}
    return `${date.getMonth() + 1}月${date.getDate()}日`;
  };

  if (isLoading) {
    return (
      <div className="favorites-body">
        <div className="favorites-ambient">
          <div className="ambient-orb ambient-orb-1" />
          <div className="ambient-orb ambient-orb-2" />
          <div className="ambient-orb ambient-orb-3" />
          <div className="ambient-noise" />
        </div>
        <div className="favorites-main">
          <div className="favorites-container">
            <div className="favorites-loading">
              <div className="loading-ring" />
              <span className="loading-text">正在加载你的收藏...</span>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="favorites-body">
        <div className="favorites-ambient">
          <div className="ambient-orb ambient-orb-1" />
          <div className="ambient-orb ambient-orb-2" />
          <div className="ambient-orb ambient-orb-3" />
          <div className="ambient-noise" />
        </div>
        <div className="favorites-main">
          <div className="favorites-container">
            <div className="favorites-error">
              <div className="error-icon">
                <RefreshCw />
              </div>
              <h3>加载失败</h3>
              <p>无法获取收藏列表，请检查网络后重试</p>
              <button
                className="retry-btn"
                onClick={() => queryClient.invalidateQueries({ queryKey: ['favorites'] })}
              >
                <RefreshCw />
                重新加载
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (favorites.length === 0) {
    return (
      <div className="favorites-body">
        <div className="favorites-ambient">
          <div className="ambient-orb ambient-orb-1" />
          <div className="ambient-orb ambient-orb-2" />
          <div className="ambient-orb ambient-orb-3" />
          <div className="ambient-noise" />
        </div>
        <div className="favorites-main">
          <div className="favorites-container">
            <div className="favorites-header">
              <div className="favorites-header-left">
                <div className="favorites-kicker">
                  <span className="kicker-dot" />
                  My Collection
                </div>
                <h1 className="favorites-title">我的收藏</h1>
                <p className="favorites-subtitle">你精心挑选的心仪好物</p>
              </div>
            </div>
            <div className="favorites-empty">
              <div className="empty-visual">
                <div className="empty-orbit" />
                <div className="empty-sparkle empty-sparkle-1" />
                <div className="empty-sparkle empty-sparkle-2" />
                <div className="empty-sparkle empty-sparkle-3" />
                <div className="empty-heart">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z" />
                  </svg>
                </div>
              </div>
              <h3>收藏夹空空如也</h3>
              <p>去发现那些让你心动的宝贝，将它们收藏在这里吧</p>
              <Link to="/products" className="explore-btn">
                <Sparkles />
                探索好物
                <ArrowRight />
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="favorites-body">
      <div className="favorites-ambient">
        <div className="ambient-orb ambient-orb-1" />
        <div className="ambient-orb ambient-orb-2" />
        <div className="ambient-orb ambient-orb-3" />
        <div className="ambient-noise" />
      </div>

      <div className="favorites-main">
        <div className="favorites-container">
          <div className="favorites-header">
            <div className="favorites-header-left">
              <div className="favorites-kicker">
                <span className="kicker-dot" />
                My Collection
              </div>
              <h1 className="favorites-title">我的收藏</h1>
              <p className="favorites-subtitle">你精心挑选的心仪好物</p>
            </div>
            <div className="favorites-header-right">
              <div className="favorites-count-badge">
                <span className="count-num">{total}</span>
                <span className="count-label">件商品</span>
              </div>
              {selectedIds.size > 0 && (
                <button
                  className="favorites-batch-btn"
                  onClick={handleBatchRemove}
                  disabled={removeManyMutation.isPending}
                >
                  <Trash2 />
                  删除选中 ({selectedIds.size})
                </button>
              )}
            </div>
          </div>

          <div className="favorites-ai-section">
            <div className="favorites-ai-card">
              <div className="favorites-ai-header">
                <div className="favorites-ai-icon">
                  <Brain size={18} />
                </div>
                <div className="favorites-ai-title">
                  <h3>AI收藏分析</h3>
                  <span className="favorites-ai-badge">
                    <Zap size={10} />
                    实时监控
                  </span>
                </div>
              </div>
              <div className="favorites-ai-features">
                <div className="favorites-ai-feature">
                  <div className="feature-icon price-drop">
                    <TrendingDown size={14} />
                  </div>
                  <div className="feature-content">
                    <span className="feature-value">3件</span>
                    <span className="feature-label">降价提醒</span>
                  </div>
                </div>
                <div className="favorites-ai-feature">
                  <div className="feature-icon alert">
                    <Bell size={14} />
                  </div>
                  <div className="feature-content">
                    <span className="feature-value">2件</span>
                    <span className="feature-label">库存紧张</span>
                  </div>
                </div>
                <div className="favorites-ai-feature">
                  <div className="feature-icon recommend">
                    <Sparkles size={14} />
                  </div>
                  <div className="feature-content">
                    <span className="feature-value">5件</span>
                    <span className="feature-label">相似推荐</span>
                  </div>
                </div>
              </div>
              <button className="favorites-ai-btn">
                <Sparkles size={14} />
                查看智能推荐
              </button>
            </div>
          </div>

          <div className="favorites-grid">
            {favorites.map((item) => {
              const fav = item as Favorite;
              const product = fav.product;
              if (!product) {return null;}

              const conditionLabel = CONDITION_LABEL_MAP[product.condition] ?? '';
              const conditionIcon = CONDITION_ICONS[product.condition] ?? '';
              const hasDiscount = product.originalPrice != null && product.originalPrice > product.price;
              const discountPercent = hasDiscount
                ? Math.round((1 - product.price / product.originalPrice!) * 100)
                : 0;
              const isHot = product.views != null && product.views > 200;
              const quickLocation = product.location?.trim() || '校内面交';
              const sellerName = product.username || '匿名用户';

              return (
                <div
                  key={fav.id}
                  className={`fav-card${selectedIds.has(fav.id) ? ' selected' : ''}`}
                  onClick={() => navigate(`/products/${product.id}`)}
                >
                  <div className="fav-card-image">
                    <Image
                      src={product.images?.[0] || ''}
                      alt={product.title}
                      loading="lazy"
                      placeholder="blur"
                      containerClassName="fav-card-img-wrap"
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />

                    <div className="fav-card-checkbox" onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        id={`fav-${fav.id}`}
                        checked={selectedIds.has(fav.id)}
                        onChange={() => toggleSelect(fav.id)}
                      />
                      <label htmlFor={`fav-${fav.id}`} />
                    </div>

                    <button
                      className={`fav-card-heart${removingId === product.id ? ' removing' : ''}`}
                      onClick={(e) => handleRemove(e, product.id)}
                      disabled={removeMutation.isPending}
                    >
                      <svg viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z" />
                      </svg>
                    </button>

                    <div className="fav-card-badges">
                      {conditionLabel && (
                        <span className="fav-badge fav-badge-condition">
                          {conditionIcon} {conditionLabel}
                        </span>
                      )}
                      {hasDiscount && (
                        <span className="fav-badge fav-badge-discount">
                          -{discountPercent}%
                        </span>
                      )}
                      {isHot && (
                        <span className="fav-badge fav-badge-hot">
                          <span className="hot-pulse" />
                          热门
                        </span>
                      )}
                    </div>

                    <div className="fav-card-quick-meta">
                      <span className="fav-quick-pill">
                        <MapPin size={11} strokeWidth={2.5} /> {quickLocation}
                      </span>
                      <span className="fav-quick-pill">
                        <Clock size={11} strokeWidth={2.5} /> {formatRelativeTime(product.createTime)}
                      </span>
                    </div>
                  </div>

                  <div className="fav-card-content">
                    <div className="fav-card-eyebrow">
                      {product.categoryName && (
                        <span className="fav-card-category">{product.categoryName}</span>
                      )}
                      <span className={`fav-card-signal${isHot ? ' is-hot' : ''}`}>
                        {product.views != null && product.views > 0
                          ? `${product.views}次浏览`
                          : '新上架'}
                      </span>
                    </div>

                    <h3 className="fav-card-title">{product.title}</h3>

                    <div className="fav-card-price-row">
                      <div className="fav-card-price">
                        <span className="price-current">¥{formatPrice(product.price)}</span>
                        {hasDiscount && (
                          <span className="price-original">¥{formatPrice(product.originalPrice!)}</span>
                        )}
                      </div>
                      {hasDiscount && (
                        <span className="fav-card-savings">
                          省 ¥{formatPrice(product.originalPrice! - product.price)}
                        </span>
                      )}
                    </div>

                    <div className="fav-card-seller">
                      {product.userAvatar ? (
                        <>
                          <div className="fav-seller-avatar">
                            <Image
                              src={product.userAvatar}
                              alt={sellerName}
                              loading="lazy"
                              placeholder="skeleton"
                              style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }}
                            />
                          </div>
                          <span className="fav-seller-name">{sellerName}</span>
                        </>
                      ) : (
                        <span className="fav-seller-name fav-seller-anon">{sellerName}</span>
                      )}
                      <span className="fav-card-fav-time">
                        <Clock size={11} strokeWidth={2} /> {formatRelativeTime(fav.createTime)}收藏
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {totalPages > 1 && (
            <div className="favorites-pagination">
              <button
                className="pagination-btn"
                onClick={() => setPageNum((p) => Math.max(1, p - 1))}
                disabled={pageNum <= 1}
              >
                上一页
              </button>
              <span className="pagination-info">
                {pageNum} / {totalPages}
              </span>
              <button
                className="pagination-btn"
                onClick={() => setPageNum((p) => Math.min(totalPages, p + 1))}
                disabled={pageNum >= totalPages}
              >
                下一页
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default FavoritesPage;
