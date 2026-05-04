import { useState, useMemo, useRef, useCallback, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useQueryClient, useQuery } from '@tanstack/react-query';
import { useVirtualizer } from '@tanstack/react-virtual';
import { X } from 'lucide-react';
import { useProducts, useCategories } from '@/hooks';
import { ProductCard } from '@/components/sections/ProductCard';

import { ToolsPlaza } from '@/components/products/ToolsPlaza';
import { FilterSidebar, type FilterState } from '@/components/products/FilterSidebar';
import { favoriteApi } from '@/api/favoriteApi';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import type { ProductQueryParams, Product } from '@/types';
import '@/styles/products-premium.css';

function useColumnCount() {
  const [columnCount, setColumnCount] = useState(() => {
    if (typeof window === 'undefined') return 4;
    const width = window.innerWidth;
    if (width <= 480) return 2;
    if (width <= 768) return 2;
    if (width <= 1200) return 3;
    return 4;
  });

  useEffect(() => {
    const handleResize = () => {
      const width = window.innerWidth;
      if (width <= 480) setColumnCount(2);
      else if (width <= 768) setColumnCount(2);
      else if (width <= 1200) setColumnCount(3);
      else setColumnCount(4);
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return columnCount;
}

export function ProductsPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { token } = useAuthStore();
  const addToast = useUIStore((s) => s.addToast);
  const initialCategoryId = searchParams.get('category') || searchParams.get('categoryId');
  const initialKeyword = searchParams.get('keyword');
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  const { data: favoritesData } = useQuery({
    queryKey: ['favorites', 'ids'],
    queryFn: async () => {
      const res = await favoriteApi.getList({ pageNum: 1, pageSize: 200 });
      return res.data;
    },
    enabled: !!token,
    staleTime: 30 * 1000,
  });

  const favoriteIds = useMemo(() => {
    if (!favoritesData?.records) {
      return new Set<number>();
    }
    return new Set(favoritesData.records.map((f: { productId: number }) => f.productId));
  }, [favoritesData]);

  const [params, setParams] = useState<ProductQueryParams>({
    pageNum: 1,
    pageSize: 20,
    keyword: initialKeyword || undefined,
    categoryId: initialCategoryId ? Number(initialCategoryId) : undefined,
    sort: 'newest',
  });

  const { data, isLoading } = useProducts(params);
  const products = data?.records ?? [];
  const total = data?.total ?? 0;

  const { data: categories } = useCategories();
  const currentCategory = useMemo(() => {
    if (!params.categoryId || !categories) return null;
    return categories.find(c => c.id === params.categoryId) || null;
  }, [params.categoryId, categories]);

  const parentRef = useRef<HTMLDivElement>(null);

  const COLUMN_COUNT = useColumnCount();

  const rows = useMemo(() => {
    const result: Product[][] = [];
    for (let i = 0; i < products.length; i += COLUMN_COUNT) {
      result.push(products.slice(i, i + COLUMN_COUNT));
    }
    return result;
  }, [products, COLUMN_COUNT]);

  const virtualizer = useVirtualizer({
    count: rows.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 450,
    overscan: 3,
    measureElement: (el) => el?.getBoundingClientRect().height ?? 450,
  });

  const sortOptions: { value: NonNullable<ProductQueryParams['sort']>; label: string }[] = [
    { value: 'newest', label: '最新发布' },
    { value: 'price_asc', label: '价格从低到高' },
    { value: 'price_desc', label: '价格从高到低' },
    { value: 'popular', label: '最受欢迎' },
  ];

  const handleSortChange = useCallback((sort: NonNullable<ProductQueryParams['sort']>) => {
    setParams((prev) => ({ ...prev, sort, pageNum: 1 }));
  }, []);

  const handleFilterChange = useCallback((_filter: string) => {
  }, []);

  const handleApplyFilters = useCallback((filters: FilterState) => {
    setParams(prev => ({
      ...prev,
      categoryId: filters.categories.length === 1 ? filters.categories[0] : undefined,
      priceMin: filters.priceMin,
      priceMax: filters.priceMax,
      conditions: filters.conditions.length > 0 ? filters.conditions : undefined,
      sort: filters.sort as ProductQueryParams['sort'],
      pageNum: 1,
    }));
    setIsFilterOpen(false);
  }, []);

  const handleResetFilters = useCallback(() => {
    setParams({
      pageNum: 1,
      pageSize: 20,
      sort: 'newest',
    });
  }, []);

  const handleClearCategory = useCallback(() => {
    setParams(prev => ({ ...prev, categoryId: undefined, pageNum: 1 }));
  }, []);

  const handleFavorite = useCallback(async (productId: number, shouldFavorite: boolean) => {
    if (!token) {
      navigate('/login');
      return;
    }
    try {
      if (shouldFavorite) {
        await favoriteApi.add(productId);
        addToast({ type: 'success', message: '已收藏' });
      } else {
        await favoriteApi.remove(productId);
        addToast({ type: 'success', message: '已取消收藏' });
      }
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
    } catch {
      addToast({ type: 'error', message: shouldFavorite ? '收藏失败' : '取消收藏失败' });
    }
  }, [token, navigate, addToast, queryClient]);

  const handleLoadMore = useCallback(() => {
    setParams((prev) => ({ ...prev, pageNum: (prev.pageNum || 1) + 1 }));
  }, []);

  if (isLoading) {
    return (
      <div className="products-page-wrapper">
        <div className="products-container">
          <div className="products-toolbar">
            <div className="results-info">
              <div className="skeleton-line" style={{ width: 80, height: 20 }} />
            </div>
          </div>
          <div className="products-grid-premium">
            {[...Array(10)].map((_, i) => (
              <div key={i} className="product-card-loading-premium">
                <div className="product-image-loading-premium" />
                <div className="product-info-loading">
                  <div className="loading-line short" />
                  <div className="loading-line" />
                  <div className="loading-line" />
                  <div className="loading-line short" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="products-page-wrapper">
      <FilterSidebar
        isOpen={isFilterOpen}
        onClose={() => setIsFilterOpen(false)}
        onApplyFilters={handleApplyFilters}
        onResetFilters={handleResetFilters}
      />

      <div className="products-container">
        <ToolsPlaza onFilterChange={handleFilterChange} total={total} />

        <div className="products-toolbar">
          <div className="results-info">
            {currentCategory && (
              <button className="results-category" onClick={handleClearCategory}>
                <span className="category-label">分类：</span>
                <span className="category-name">{currentCategory.name}</span>
                <X size={12} className="category-clear" />
              </button>
            )}
            <span className="results-count">{total}</span>
            <span className="results-text"> 件商品</span>
          </div>

          <div className="flex items-center gap-2">
            <button
              className="filter-toggle-btn"
              onClick={() => setIsFilterOpen(true)}
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="4" y1="21" x2="4" y2="14" />
                <line x1="4" y1="10" x2="4" y2="3" />
                <line x1="12" y1="21" x2="12" y2="12" />
                <line x1="12" y1="8" x2="12" y2="3" />
                <line x1="20" y1="21" x2="20" y2="16" />
                <line x1="20" y1="12" x2="20" y2="3" />
              </svg>
              <span>筛选</span>
            </button>

            <div className="view-options">
              {sortOptions.map((option) => (
                <button
                  key={option.value}
                  onClick={() => handleSortChange(option.value)}
                  className={`view-btn ${params.sort === option.value ? 'active' : ''}`}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div
          ref={parentRef}
          style={{
            height: 'calc(100vh - 300px)',
            overflow: 'auto',
            width: '100%',
          }}
        >
          <div
            style={{
              height: `${virtualizer.getTotalSize()}px`,
              width: '100%',
              position: 'relative',
            }}
          >
            {virtualizer.getVirtualItems().map((virtualRow) => {
              const row = rows[virtualRow.index];
              return (
                <div
                  key={virtualRow.key}
                  data-index={virtualRow.index}
                  ref={virtualizer.measureElement}
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100%',
                    transform: `translateY(${virtualRow.start}px)`,
                  }}
                >
                  <div className="products-grid-premium">
                    {row.map((product: Product, colIndex: number) => (
                      <ProductCard
                        key={product.id}
                        product={product}
                        index={virtualRow.index * COLUMN_COUNT + colIndex}
                        isFavorited={favoriteIds.has(product.id)}
                        onFavorite={handleFavorite}
                        onViewDetails={(id) => navigate(`/products/${id}`)}
                      />
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {products.length > 0 && products.length < total && (
          <div className="load-more-premium">
            <button
              className="btn-load-more"
              onClick={handleLoadMore}
            >
              <span>加载更多</span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
                <line x1="12" y1="5" x2="12" y2="19" />
                <polyline points="19 12 12 19 5 12" />
              </svg>
            </button>
          </div>
        )}

        {products.length === 0 && (
          <div className="no-results-premium">
            <div className="no-results-icon-premium">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <circle cx="11" cy="11" r="8" />
                <path d="M21 21l-4.35-4.35" />
                <path d="M8 8l6 6M14 8l-6 6" />
              </svg>
            </div>
            <h3>未找到相关商品</h3>
            <p>尝试调整筛选条件或搜索其他关键词</p>
          </div>
        )}
      </div>
    </div>
  );
}
