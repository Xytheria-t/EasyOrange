import { useState, useMemo, useCallback, useEffect, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useVirtualizer } from '@tanstack/react-virtual';
import { useQueryClient } from '@tanstack/react-query';
import { X, Sparkles } from 'lucide-react';
import { useProducts, useCategories, useFavoriteCheck, useColumnCount, useSemanticSearch } from '@/hooks';
import { SemanticSearchToggle } from '@/components/ai/SemanticSearchToggle';
import { ProductCard } from '@/components/product/ProductCard';
import { preloadImages } from '@/components/ui/Image';
import { productApi } from '@/api/productApi';
import { normalizeProduct } from '@/utils/product';
import { PRODUCT_KEYS } from '@/hooks/product/useProducts';

import SortDropdown from '@/components/search/SortDropdown';
import type { SortOption } from '@/components/search/SortDropdown';
import { ToolsPlaza } from '@/components/product/ToolsPlaza';
import { FilterSidebar, type FilterState } from '@/components/product/FilterSidebar';
import { useAuthStore } from '@/store/authStore';
import type { ProductQueryParams, Product } from '@/types';
import './products-premium.css';

function ProductsPage() {
  const [searchParams] = useSearchParams();
  const { token } = useAuthStore();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { checkFavorites, isFavorited, toggleFavorite } = useFavoriteCheck();
  const initialCategoryId = searchParams.get('category') || searchParams.get('categoryId');
  const initialKeyword = searchParams.get('keyword');
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  const [params, setParams] = useState<ProductQueryParams>({
    pageNum: 1,
    pageSize: 20,
    keyword: initialKeyword || undefined,
    categoryId: initialCategoryId ?? undefined,
    sort: 'newest',
  });

  const { data, isLoading } = useProducts(params);
  const {
    results: semanticResults,
    isSearching: isSemanticSearching,
    isSemanticMode,
    total: semanticTotal,
    error: semanticError,
    search: semanticSearch,
    toggleSemanticMode,
  } = useSemanticSearch();

  const [allProducts, setAllProducts] = useState<Product[]>([]);
  const [isLooping, setIsLooping] = useState(false);
  const sentinelRef = useRef<HTMLDivElement>(null);

  const products = isSemanticMode ? semanticResults : (data?.records ?? []);
  const total = isSemanticMode ? semanticTotal : (data?.total ?? 0);
  const isSearchLoading = isSemanticMode ? isSemanticSearching : isLoading;

  useEffect(() => {
    if (isSemanticMode && params.keyword) {
      semanticSearch(params.keyword, params.pageNum, params.pageSize);
    }
  }, [isSemanticMode, params.keyword, params.pageNum, params.pageSize, semanticSearch]);

  const displayProducts = isSemanticMode ? semanticResults : allProducts;

  useEffect(() => {
    if (!isLoading && !isSemanticMode && products.length > 0) {
      setAllProducts(prev => {
        if (params.pageNum === 1 || isLooping) {
          return [...products];
        }
        const existingIds = new Set(prev.map(p => p.id));
        const newItems = products.filter(p => !existingIds.has(p.id));
        const updated = [...prev, ...newItems];
        if (newItems.length > 0) {
          const upcomingImages = newItems.slice(0, 6).map((p: Product) => p.images?.[0]).filter(Boolean) as string[];
          preloadImages(upcomingImages, { width: 300, format: 'webp', quality: 75 }).catch(() => {});
        }
        return updated;
      });
      setIsLooping(false);
    }
  }, [products, isLoading, params.pageNum, isLooping, isSemanticMode]);

  useEffect(() => {
    if (allProducts.length > 0 && token) {
      checkFavorites(allProducts.map(p => p.id));
    }
  }, [allProducts, token, checkFavorites]);

  const hasNextPage = !isSemanticMode && allProducts.length < total && total > 0;

  useEffect(() => {
    if (!isLoading && !isSemanticMode && products.length > 0 && hasNextPage) {
      const nextPageNum = (params.pageNum || 1) + 1;
      const nextParams = { ...params, pageNum: nextPageNum };
      queryClient.prefetchQuery({
        queryKey: PRODUCT_KEYS.list(nextParams),
        queryFn: async () => {
          const response = await productApi.getProducts(nextParams);
          const responseData = response.data;
          return {
            ...responseData,
            records: (responseData.records ?? []).map((r) => normalizeProduct(r as unknown as Record<string, unknown>)),
          };
        },
        staleTime: 30 * 1000,
      }).catch(() => {});
    }
  }, [isLoading, products.length, hasNextPage, params.pageNum, queryClient]);

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel || isSemanticMode) {return;}

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !isLoading) {
          if (hasNextPage) {
            handleLoadNext();
          } else if (!isLooping && allProducts.length > 0) {
            handleLoopBack();
          }
        }
      },
      { rootMargin: '1200px' }
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [allProducts.length, total, isLoading, isLooping, hasNextPage]);

  const handleLoadNext = useCallback(() => {
    setParams(prev => ({ ...prev, pageNum: (prev.pageNum || 1) + 1 }));
  }, []);

  const handleLoopBack = useCallback(() => {
    setIsLooping(true);
    setParams(prev => ({ ...prev, pageNum: 1 }));
  }, []);

  const resetAllProducts = useCallback(() => {
    setAllProducts([]);
    setIsLooping(false);
  }, []);

  const { data: categories } = useCategories();
  const currentCategory = useMemo(() => {
    if (!params.categoryId || !categories) {return null;}
    return categories.find(c => c.id === params.categoryId) || null;
  }, [params.categoryId, categories]);

  const COLUMN_COUNT = useColumnCount();

  const rows = useMemo(() => {
    const result: (Product | null)[][] = [];
    for (let i = 0; i < displayProducts.length; i += COLUMN_COUNT) {
      result.push(displayProducts.slice(i, i + COLUMN_COUNT));
    }
    if (isSearchLoading && displayProducts.length > 0 && hasNextPage) {
      result.push(Array(COLUMN_COUNT).fill(null));
      result.push(Array(COLUMN_COUNT).fill(null));
    }
    return result;
  }, [displayProducts, COLUMN_COUNT, isSearchLoading, hasNextPage]);

  const rowVirtualizer = useVirtualizer({
    count: rows.length,
    getScrollElement: () => (typeof window !== 'undefined' ? window.document.documentElement : null) as HTMLElement | null,
    estimateSize: () => 520,
    overscan: 8,
  });

  const handleSortChange = useCallback((sort: SortOption) => {
    resetAllProducts();
    setParams((prev) => ({ ...prev, sort, pageNum: 1 }));
  }, [resetAllProducts]);

  const handleFilterChange = useCallback((filter: string) => {
    resetAllProducts();
    if (filter === 'all') {
      setParams(prev => ({ ...prev, sort: 'newest', pageNum: 1 }));
    }
    // 'ai' is handled internally by ToolsPlaza via isSemanticMode toggle
    // 'discount' is reserved for future use
  }, [resetAllProducts]);

  const handleApplyFilters = useCallback((filters: FilterState) => {
    resetAllProducts();
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
  }, [resetAllProducts]);

  const handleResetFilters = useCallback(() => {
    resetAllProducts();
    setParams({
      pageNum: 1,
      pageSize: 20,
      sort: 'newest',
    });
  }, [resetAllProducts]);

  const handleClearCategory = useCallback(() => {
    resetAllProducts();
    setParams(prev => ({ ...prev, categoryId: undefined, pageNum: 1 }));
  }, [resetAllProducts]);

  const handleFavorite = useCallback(async (productId: string, shouldFavorite: boolean) => {
    if (!token) {
      navigate('/login');
      return;
    }
    toggleFavorite(productId, shouldFavorite);
  }, [token, navigate, toggleFavorite]);

  if (isSearchLoading && displayProducts.length === 0) {
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
            <SemanticSearchToggle
              isActive={isSemanticMode}
              onToggle={toggleSemanticMode}
            />
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

            <SortDropdown
              value={(params.sort ?? 'newest') as SortOption}
              onChange={handleSortChange}
            />
          </div>
        </div>

        {isSemanticMode && params.keyword && !semanticError && (
          <div className="semantic-search-banner">
            <div className="semantic-banner-icon">
              <Sparkles size={16} />
            </div>
            <div className="semantic-banner-content">
              <span className="semantic-banner-title">AI 语义搜索</span>
              <span className="semantic-banner-query">"{params.keyword}"</span>
            </div>
            <span className="semantic-banner-badge">
              找到 {total} 件商品
            </span>
          </div>
        )}

        <div style={{ height: `${rowVirtualizer.getTotalSize()}px`, width: '100%', position: 'relative' }}>
          {rowVirtualizer.getVirtualItems().map((virtualRow) => {
            const row = rows[virtualRow.index];
            if (!row) {return null;}
            return (
              <div
                key={virtualRow.index}
                style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  width: '100%',
                  height: virtualRow.size,
                  transform: `translateY(${virtualRow.start}px)`,
                  display: 'grid',
                  gridTemplateColumns: `repeat(${COLUMN_COUNT}, 1fr)`,
                  gap: '1.75rem',
                  padding: '0 0 1.75rem 0',
                }}
              >
                {row.map((item: Product | null, colIndex: number) =>
                  item === null ? (
                    <div key={`buffer-${virtualRow.index}-${colIndex}`} className="product-card-loading-premium">
                      <div className="product-image-loading-premium" />
                      <div className="product-info-loading">
                        <div className="loading-line short" />
                        <div className="loading-line" />
                        <div className="loading-line short" />
                      </div>
                    </div>
                  ) : (
                    <ProductCard
                      key={item.id}
                      product={item}
                      index={virtualRow.index * COLUMN_COUNT + colIndex}
                      isFavorited={isFavorited(item.id)}
                      onFavorite={handleFavorite}
                    />
                  )
                )}
              </div>
            );
          })}
        </div>

        {displayProducts.length > 0 && (
          <div ref={sentinelRef} className="scroll-sentinel" />
        )}

        {!isSearchLoading && displayProducts.length === 0 && (
          <div className="no-results-premium">
            <div className={`no-results-icon-premium ${semanticError ? 'error' : ''}`}>
              {semanticError ? (
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
              ) : (
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <circle cx="11" cy="11" r="8" />
                  <path d="M21 21l-4.35-4.35" />
                  <path d="M8 8l6 6M14 8l-6 6" />
                </svg>
              )}
            </div>
            {semanticError ? (
              <>
                <h3>语义搜索暂不可用</h3>
                <p className="no-results-error">{semanticError}</p>
                <div className="no-results-actions">
                  <button
                    className="semantic-retry-btn"
                    onClick={() => params.keyword && semanticSearch(params.keyword)}
                  >
                    重试
                  </button>
                  <button
                    className="semantic-fallback-btn"
                    onClick={toggleSemanticMode}
                  >
                    切换到关键词搜索
                  </button>
                </div>
              </>
            ) : isSemanticMode ? (
              <>
                <h3>未找到相关商品</h3>
                <p>语义搜索未匹配到结果，试试其他关键词</p>
                <div className="no-results-actions">
                  <button
                    className="semantic-fallback-btn"
                    onClick={toggleSemanticMode}
                  >
                    切换到关键词搜索
                  </button>
                </div>
              </>
            ) : (
              <>
                <h3>未找到相关商品</h3>
                <p>尝试调整筛选条件或搜索其他关键词</p>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default ProductsPage;
