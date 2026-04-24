import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useProducts } from '@/hooks';
import { ProductCard } from '@/components/react/ProductCard';
import { Button } from '@/components/ui/Button';
import { ToolsPlaza } from '@/components/products/ToolsPlaza';
import { FilterSidebar } from '@/components/products/FilterSidebar';
import type { ProductQueryParams, Product } from '@/types';
import '@/styles/products.css';

export function ProductsPage() {
  const [searchParams] = useSearchParams();
  const initialCategoryId = searchParams.get('categoryId');
  const initialKeyword = searchParams.get('keyword');
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  const [params, setParams] = useState<ProductQueryParams>({
    current: 1,
    size: 20,
    keyword: initialKeyword || undefined,
    categoryId: initialCategoryId ? Number(initialCategoryId) : undefined,
    sort: 'newest',
  });

  const { data, isLoading } = useProducts(params);
  const products = data?.records ?? [];
  const total = data?.total ?? 0;

  const sortOptions: { value: NonNullable<ProductQueryParams['sort']>; label: string }[] = [
    { value: 'newest', label: '最新发布' },
    { value: 'price_asc', label: '价格从低到高' },
    { value: 'price_desc', label: '价格从高到低' },
    { value: 'popular', label: '最受欢迎' },
  ];

  const handleSortChange = (sort: NonNullable<ProductQueryParams['sort']>) => {
    setParams((prev) => ({ ...prev, sort, current: 1 }));
  };

  const handleFilterChange = (_filter: string) => {
  };

  const handleApplyFilters = (_filters: any) => {
    setIsFilterOpen(false);
  };

  const handleResetFilters = () => {
  };

  if (isLoading) {
    return (
      <div className="products-page-wrapper">
        <div className="products-container">
          <div className="products-toolbar">
            <div className="results-info">
              <div className="skeleton-line" style={{ width: 80, height: 20 }} />
            </div>
          </div>
          <div className="products-grid">
            {[...Array(10)].map((_, i) => (
              <div key={i} className="product-card-loading">
                <div className="product-image-loading" />
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

        <div className="products-grid">
          {products.map((product: Product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>

        {products.length > 0 && (
          <div className="load-more-section">
            <Button
              variant="outline"
              onClick={() => setParams((prev) => ({ ...prev, size: (prev.size || 20) + 20 }))}
            >
              加载更多
            </Button>
          </div>
        )}

        {products.length === 0 && (
          <div className="no-results">
            <div className="no-results-icon">
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
