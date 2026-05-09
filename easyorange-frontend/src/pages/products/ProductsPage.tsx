import { useState, useMemo, useCallback, useEffect, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useVirtualizer } from '@tanstack/react-virtual';
import { X } from 'lucide-react';
import { useProducts, useCategories, useFavoriteCheck } from '@/hooks';
import { ProductCard } from '@/components/product/ProductCard';
import { preloadImages } from '@/components/ui/Image';

import { ToolsPlaza } from '@/components/product/ToolsPlaza';
import { FilterSidebar, type FilterState } from '@/components/product/FilterSidebar';
import { useAuthStore } from '@/store/authStore';
import type { ProductQueryParams, Product } from '@/types';
import './products-premium.css';

function useColumnCount() {
  const [columnCount, setColumnCount] = useState(() => {
    if (typeof window === 'undefined') {return 4;}
    const width = window.innerWidth;
    if (width <= 480) {return 2;}
    if (width <= 768) {return 2;}
    if (width <= 1200) {return 3;}
    return 4;
  });

  useEffect(() => {
    const handleResize = () => {
      const width = window.innerWidth;
      if (width <= 480) {setColumnCount(2);}
      else if (width <= 768) {setColumnCount(2);}
      else if (width <= 1200) {setColumnCount(3);}
      else {setColumnCount(4);}
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return columnCount;
}

function ProductsPage() {
  const [searchParams] = useSearchParams();
  const { token } = useAuthStore();
  const navigate = useNavigate();
  const { checkFavorites, isFavorited, toggleFavorite } = useFavoriteCheck();
  const initialCategoryId = searchParams.get('category') || searchParams.get('categoryId');
  const initialKeyword = searchParams.get('keyword');
  const [isFilterOpen, setIsFilterOpen] = useState(false);

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

  const [allProducts, setAllProducts] = useState<Product[]>([]);
  const [isLooping, setIsLooping] = useState(false);
  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isLoading && products.length > 0) {
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
  }, [products, isLoading, params.pageNum, isLooping]);

  useEffect(() => {
    if (allProducts.length > 0 && token) {
      checkFavorites(allProducts.map(p => p.id));
    }
  }, [allProducts, token, checkFavorites]);

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !isLoading) {
          if (allProducts.length < total && total > 0) {
            handleLoadNext();
          } else if (!isLooping && allProducts.length > 0) {
            handleLoopBack();
          }
        }
      },
      { rootMargin: '200px' }
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [allProducts.length, total, isLoading, isLooping]);

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
    const result: Product[][] = [];
    for (let i = 0; i < allProducts.length; i += COLUMN_COUNT) {
      result.push(allProducts.slice(i, i + COLUMN_COUNT));
    }
    return result;
  }, [allProducts, COLUMN_COUNT]);

  const parentRef = useRef<HTMLDivElement>(null);

  const rowVirtualizer = useVirtualizer({
    count: rows.length,
    getScrollElement: () => (typeof window !== 'undefined' ? window.document.documentElement : null) as HTMLElement | null,
    estimateSize: () => 520,
    overscan: 3,
  });

  const sortOptions: { value: NonNullable<ProductQueryParams['sort']>; label: string }[] = [
    { value: 'newest', label: '最新发布' },
    { value: 'price_asc', label: '价格从低到高' },
    { value: 'price_desc', label: '价格从高到低' },
    { value: 'popular', label: '最受欢迎' },
  ];

  const handleSortChange = useCallback((sort: NonNullable<ProductQueryParams['sort']>) => {
    resetAllProducts();
    setParams((prev) => ({ ...prev, sort, pageNum: 1 }));
  }, [resetAllProducts]);

  const handleFilterChange = useCallback((_filter: string) => {
  }, []);

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

  const handleFavorite = useCallback(async (productId: number, shouldFavorite: boolean) => {
    if (!token) {
      navigate('/login');
      return;
    }
    toggleFavorite(productId, shouldFavorite);
  }, [token, navigate, toggleFavorite]);

  if (isLoading && allProducts.length === 0) {
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

        <div ref={parentRef} style={{ height: `${rowVirtualizer.getTotalSize()}px`, width: '100%', position: 'relative' }}>
          {rowVirtualizer.getVirtualItems().map((virtualRow) => {
            const row = rows[virtualRow.index];
            if (!row) return null;
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
                {row.map((product: Product, colIndex: number) => (
                  <ProductCard
                    key={product.id}
                    product={product}
                    index={virtualRow.index * COLUMN_COUNT + colIndex}
                    isFavorited={isFavorited(product.id)}
                    onFavorite={handleFavorite}
                  />
                ))}
              </div>
            );
          })}
        </div>

        {allProducts.length > 0 && (
          <div ref={sentinelRef} className="scroll-sentinel" />
        )}

        {!isLoading && allProducts.length === 0 && (
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

export default ProductsPage;
