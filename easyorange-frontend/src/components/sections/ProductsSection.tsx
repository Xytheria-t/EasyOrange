import { useState } from 'react'
import { ProductCard } from './ProductCard'
import { useProducts } from '@/hooks'
import type { Product, ProductQueryParams } from '@/types'

type FilterKey = 'all' | 'new' | 'hot' | 'discount'

const FILTER_PARAMS: Record<FilterKey, ProductQueryParams> = {
  all: { sort: 'newest', current: 1, size: 8 },
  new: { sort: 'newest', current: 1, size: 8 },
  hot: { sort: 'popular', current: 1, size: 8 },
  discount: { sort: 'price_asc', current: 1, size: 8 },
}

export default function ProductsSection() {
  const [activeFilter, setActiveFilter] = useState<FilterKey>('all')
  const { data, isLoading } = useProducts(FILTER_PARAMS[activeFilter])

  const products = data?.records ?? []

  const filters: { id: FilterKey; label: string }[] = [
    { id: 'all', label: '全部' },
    { id: 'new', label: '最新发布' },
    { id: 'hot', label: '热门推荐' },
    { id: 'discount', label: '超值优惠' },
  ]

  return (
    <section className="products-section">
      <div className="container">
        <div className="section-header">
          <span className="section-tag">精选推荐</span>
          <h2 className="section-title">热门好物</h2>
          <p className="section-desc">精心挑选的优质商品，总有一款适合你</p>
        </div>

        <div className="products-filter">
          <div className="filter-tabs">
            {filters.map(filter => (
              <button
                key={filter.id}
                className={`filter-tab ${activeFilter === filter.id ? 'active' : ''}`}
                data-filter={filter.id}
                onClick={() => setActiveFilter(filter.id)}
              >
                {filter.label}
              </button>
            ))}
          </div>
          <a href="/products" className="view-all-link">
            查看全部
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </a>
        </div>

        <div className="products-grid" id="productsGrid">
          {isLoading ? (
            <div className="skeleton-grid">
              <div className="skeleton-card"><div className="skeleton-image"></div><div className="skeleton-content"><div className="skeleton-line"></div><div className="skeleton-line short"></div></div></div>
              <div className="skeleton-card"><div className="skeleton-image"></div><div className="skeleton-content"><div className="skeleton-line"></div><div className="skeleton-line short"></div></div></div>
              <div className="skeleton-card"><div className="skeleton-image"></div><div className="skeleton-content"><div className="skeleton-line"></div><div className="skeleton-line short"></div></div></div>
              <div className="skeleton-card"><div className="skeleton-image"></div><div className="skeleton-content"><div className="skeleton-line"></div><div className="skeleton-line short"></div></div></div>
            </div>
          ) : products.length > 0 ? (
            products.map((product: Product, index: number) => (
              <ProductCard
                key={product.id}
                product={product}
                style={{ animationDelay: `${index * 80}ms` }}
              />
            ))
          ) : (
            <div className="text-center py-12" style={{ gridColumn: '1 / -1' }}>
              <p style={{ color: 'var(--text-secondary)' }}>暂无商品</p>
            </div>
          )}
        </div>

        <div className="products-more">
          <a href="/products" className="btn btn-outline btn-lg">
            <span>查看更多商品</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </a>
        </div>
      </div>
    </section>
  )
}
