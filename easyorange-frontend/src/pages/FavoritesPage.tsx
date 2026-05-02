import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Trash2, RefreshCw, ArrowRight, Sparkles, Image as ImageIcon } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { favoriteApi } from '@/api/favoriteApi';
import { useUIStore } from '@/store/uiStore';
import { CONDITION_LABEL_MAP, type Favorite } from '@/types';
import '@/styles/favorites.css';

const CONDITION_ICONS: Record<number, string> = {
  1: '✨',
  2: '💎',
  3: '🌿',
  4: '📦',
};

export function FavoritesPage() {
  const navigate = useNavigate();
  const addToast = useUIStore((s) => s.addToast);
  const queryClient = useQueryClient();
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [removingId, setRemovingId] = useState<number | null>(null);
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
    mutationFn: async (productId: number) => {
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
    mutationFn: async (ids: number[]) => {
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

  const toggleSelect = (id: number) => {
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
    if (selectedIds.size === 0) return;
    removeManyMutation.mutate(Array.from(selectedIds));
  };

  const handleRemove = (e: React.MouseEvent, productId: number) => {
    e.stopPropagation();
    e.preventDefault();
    setRemovingId(productId);
    removeMutation.mutate(productId);
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    if (days === 0) return '今天';
    if (days === 1) return '昨天';
    if (days < 7) return `${days}天前`;
    if (days < 30) return `${Math.floor(days / 7)}周前`;
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

          <div className="favorites-grid">
            {favorites.map((item) => {
              const fav = item as Favorite;
              const product = fav.product;
              if (!product) return null;

              const conditionLabel = CONDITION_LABEL_MAP[product.condition] ?? '';
              const conditionIcon = CONDITION_ICONS[product.condition] ?? '';

              return (
                <div
                  key={fav.id}
                  className={`fav-card${selectedIds.has(fav.id) ? ' selected' : ''}`}
                  onClick={() => navigate(`/products/${product.id}`)}
                >
                  <div className="fav-card-image">
                    {product.images?.[0] ? (
                      <img
                        src={product.images[0]}
                        alt={product.title}
                        loading="lazy"
                      />
                    ) : (
                      <div className="placeholder-icon">
                        <ImageIcon />
                      </div>
                    )}

                    <div className="fav-card-checkbox">
                      <input
                        type="checkbox"
                        id={`fav-${fav.id}`}
                        checked={selectedIds.has(fav.id)}
                        onChange={(e) => {
                          e.stopPropagation();
                          toggleSelect(fav.id);
                        }}
                      />
                      <label
                        htmlFor={`fav-${fav.id}`}
                        onClick={(e) => e.stopPropagation()}
                      />
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

                    {conditionLabel && (
                      <div className="fav-card-condition">
                        {conditionIcon} {conditionLabel}
                      </div>
                    )}
                  </div>

                  <div className="fav-card-content">
                    {product.categoryName && (
                      <span className="fav-card-category">{product.categoryName}</span>
                    )}
                    <h3 className="fav-card-title">{product.title}</h3>
                    <div className="fav-card-footer">
                      <div className="fav-card-price">
                        <span className="price-current">¥{product.price?.toFixed(2)}</span>
                        {product.originalPrice && product.originalPrice > product.price && (
                          <span className="price-original">¥{product.originalPrice.toFixed(2)}</span>
                        )}
                      </div>
                      <span className="fav-card-time">{formatDate(fav.createTime)}</span>
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
