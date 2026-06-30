import { useState, useRef, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { ProductCard } from '@/components/product/ProductCard'
import { useProducts, useFavoriteCheck } from '@/hooks'
import { useAuthStore } from '@/store/authStore'
import { Button } from '@/components/ui/button'
import type { Product, ProductQueryParams } from '@/types'

type FilterKey = 'all' | 'new' | 'hot' | 'discount'

const FILTER_PARAMS: Record<FilterKey, ProductQueryParams> = {
  all: { sort: 'newest', pageNum: 1, pageSize: 8 },
  new: { sort: 'newest', pageNum: 1, pageSize: 8 },
  hot: { sort: 'popular', pageNum: 1, pageSize: 8 },
  discount: { sort: 'price_asc', pageNum: 1, pageSize: 8 },
}

export default function ProductsSection() {
  const [activeFilter, setActiveFilter] = useState<FilterKey>('all')
  const { data, isLoading } = useProducts(FILTER_PARAMS[activeFilter])
  const { token } = useAuthStore()
  const navigate = useNavigate()
  const { checkFavorites, isFavorited, toggleFavorite } = useFavoriteCheck()

  const products = data?.records ?? []

  // Use a ref to stabilize the products reference and avoid cascading re-renders
  const prevProductsKeyRef = useRef<string>('');
  const productsKey = products.length > 0 ? products.map(p => p.id).join(',') : '';
  useEffect(() => {
    if (productsKey && productsKey !== prevProductsKeyRef.current && token) {
      prevProductsKeyRef.current = productsKey;
      checkFavorites(productsKey.split(','));
    }
  }, [productsKey, token, checkFavorites])

  const handleFavorite = useCallback(async (productId: string, shouldFavorite: boolean) => {
    if (!token) {
      navigate('/login')
      return
    }
    toggleFavorite(productId, shouldFavorite)
  }, [token, navigate, toggleFavorite])

  const filters: { id: FilterKey; label: string }[] = [
    { id: 'all', label: '全部' },
    { id: 'new', label: '最新发布' },
    { id: 'hot', label: '热门推荐' },
    { id: 'discount', label: '超值优惠' },
  ]

  const sectionRef = useRef<HTMLElement>(null)
  const indicatorRef = useRef<HTMLDivElement>(null)
  const tabsRef = useRef<HTMLDivElement>(null)
  const [isVisible, setIsVisible] = useState(false)

  useEffect(() => {
    const section = sectionRef.current
    if (!section) {return}

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true)
          observer.unobserve(section)
        }
      },
      { threshold: 0.1, rootMargin: '0px 0px -50px 0px' }
    )

    observer.observe(section)
    return () => observer.disconnect()
  }, [])

  // Sliding indicator for filter tabs
  const updateIndicator = useCallback((tabElement: HTMLButtonElement | null) => {
    const indicator = indicatorRef.current
    const tabs = tabsRef.current
    if (!indicator || !tabs || !tabElement) {return}

    const tabsRect = tabs.getBoundingClientRect()
    const tabRect = tabElement.getBoundingClientRect()

    indicator.style.width = `${tabRect.width}px`
    indicator.style.transform = `translateX(${tabRect.left - tabsRect.left}px)`
  }, [])

  // Update indicator on mount and filter change
  useEffect(() => {
    const tabs = tabsRef.current
    if (!tabs) {return}

    const activeTab = tabs.querySelector(`[data-filter="${activeFilter}"]`) as HTMLButtonElement
    if (activeTab) {
      // Small delay to ensure layout is complete
      requestAnimationFrame(() => updateIndicator(activeTab))
    }
  }, [activeFilter, updateIndicator])

  // Update indicator on resize
  useEffect(() => {
    const handleResize = () => {
      const tabs = tabsRef.current
      if (!tabs) {return}
      const activeTab = tabs.querySelector(`[data-filter="${activeFilter}"]`) as HTMLButtonElement
      if (activeTab) {updateIndicator(activeTab)}
    }

    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [activeFilter, updateIndicator])

  const handleFilterClick = useCallback(
    (filterId: FilterKey) => (e: React.MouseEvent<HTMLButtonElement>) => {
      setActiveFilter(filterId)
      updateIndicator(e.currentTarget)
    },
    [updateIndicator]
  )

  return (
    <section ref={sectionRef} className="products-section">
      {/* 顶部弥散光晕过渡 */}
      <div className="orb-transition orb-transition-top">
        <div
          className="transition-orb"
          style={{
            width: '500px',
            height: '500px',
            background: 'radial-gradient(circle, rgba(249, 115, 22, 0.08) 0%, transparent 70%)',
            top: '-150px',
            left: '60%',
            animationDelay: '-3s',
          }}
        />
        <div
          className="transition-orb"
          style={{
            width: '350px',
            height: '350px',
            background: 'radial-gradient(circle, rgba(251, 191, 36, 0.06) 0%, transparent 70%)',
            top: '-100px',
            left: '20%',
            animationDelay: '-7s',
          }}
        />
      </div>

      <div className="container">
        <div className={`section-header reveal ${isVisible ? 'revealed' : ''}`}>
          <span className="section-tag">精选推荐</span>
          <h2 className="section-title">热门资产</h2>
          <p className="section-desc">精心挑选的优质资产，总有一款适合你</p>
        </div>

        <div className={`products-filter reveal ${isVisible ? 'revealed' : ''}`} style={{ transitionDelay: '100ms' }}>
          <div className="filter-tabs" ref={tabsRef}>
            {/* Sliding indicator */}
            <div
              ref={indicatorRef}
              className="filter-indicator"
              style={{
                position: 'absolute',
                bottom: 0,
                left: 0,
                height: '2px',
                background: 'linear-gradient(90deg, #F97316, #FB7185)',
                borderRadius: '2px',
                transition: 'transform 0.3s var(--ease-spring), width 0.3s var(--ease-spring)',
                boxShadow: '0 0 8px rgba(249, 115, 22, 0.4)',
              }}
            />
            {filters.map((filter) => (
              <Button
                key={filter.id}
                variant="ghost"
                className={`filter-tab ${activeFilter === filter.id ? 'active' : ''}`}
                data-filter={filter.id}
                onClick={handleFilterClick(filter.id)}
                aria-pressed={activeFilter === filter.id}
              >
                {filter.label}
              </Button>
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

        <div
          className={`products-grid-premium reveal ${isVisible ? 'revealed' : ''}`}
          id="productsGrid"
        >
          {isLoading ? (
            <div className="skeleton-grid">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="skeleton-card">
                  <div className="skeleton-image" />
                  <div className="skeleton-content">
                    <div className="skeleton-line" />
                    <div className="skeleton-line short" />
                  </div>
                </div>
              ))}
            </div>
          ) : products.length > 0 ? (
            products.map((product: Product, index: number) => (
              <ProductCard
                key={product.id}
                product={product}
                isFavorited={isFavorited(product.id)}
                onFavorite={handleFavorite}
                style={{
                  animationDelay: `${index * 80}ms`,
                  opacity: isVisible ? 1 : 0,
                  transform: isVisible ? 'translateY(0)' : 'translateY(30px)',
                  transition: `opacity 0.6s var(--ease-out) ${index * 80}ms, transform 0.6s var(--ease-out) ${index * 80}ms`,
                }}
              />
            ))
          ) : (
            <div className="text-center py-12" style={{ gridColumn: '1 / -1' }}>
              <p className="text-secondary">暂无资产</p>
            </div>
          )}
        </div>

        <div className={`products-more reveal ${isVisible ? 'revealed' : ''}`} style={{ transitionDelay: '400ms' }}>
          <Button variant="outline" size="lg" onClick={() => navigate('/products')}>
            <span>查看更多资产</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </Button>
        </div>
      </div>
    </section>
  )
}
