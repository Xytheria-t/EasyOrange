import { useState, useEffect } from 'react'
import { ProductCard } from './ProductCard'
import type { Product } from '@/types'

// 模拟数据
const mockProducts: Product[] = [
  {
    id: 1,
    title: 'MacBook Pro 2023 M3',
    description: '95 新 MacBook Pro，M3 芯片，16GB+512GB',
    price: 8999,
    originalPrice: 12999,
    categoryId: 1,
    categoryName: '数码电子',
    condition: 'LIKE_NEW',
    status: 'ON_SALE',
    images: ['https://picsum.photos/seed/product1/400/400'],
    location: '北京',
    views: 1234,
    favorites: 56,
    sellerId: 1,
    sellerName: '李明',
    sellerAvatar: 'https://i.pravatar.cc/150?u=1',
    sellerRating: 4.9,
    createTime: '2024-01-01T00:00:00Z',
    updateTime: '2024-01-01T00:00:00Z',
    isHot: true,
    discount: 31,
    viewCount: 1234,
    category: '数码电子',
  },
  {
    id: 2,
    title: '高等数学教材全套',
    description: '同济版高等数学上下册，8 成新，有笔记',
    price: 50,
    originalPrice: 180,
    categoryId: 2,
    categoryName: '图书教材',
    condition: 'GOOD',
    status: 'ON_SALE',
    images: ['https://picsum.photos/seed/product2/400/400'],
    location: '北京',
    views: 567,
    favorites: 23,
    sellerId: 2,
    sellerName: '张雨',
    sellerAvatar: 'https://i.pravatar.cc/150?u=2',
    sellerRating: 4.8,
    createTime: '2024-01-02T00:00:00Z',
    updateTime: '2024-01-02T00:00:00Z',
    discount: 72,
    viewCount: 567,
    category: '图书教材',
  },
  {
    id: 3,
    title: '捷安特山地自行车',
    description: '捷安特 ATX 777，9 成新，27 速',
    price: 800,
    originalPrice: 2500,
    categoryId: 4,
    categoryName: '交通工具',
    condition: 'LIKE_NEW',
    status: 'ON_SALE',
    images: ['https://picsum.photos/seed/product3/400/400'],
    location: '上海',
    views: 890,
    favorites: 34,
    sellerId: 3,
    sellerName: '王浩然',
    sellerAvatar: 'https://i.pravatar.cc/150?u=3',
    sellerRating: 4.7,
    createTime: '2024-01-03T00:00:00Z',
    updateTime: '2024-01-03T00:00:00Z',
    isHot: true,
    discount: 68,
    viewCount: 890,
    category: '交通工具',
  },
  {
    id: 4,
    title: 'iPad Air 5 64GB',
    description: 'iPad Air 5 代，64GB WiFi 版，98 新',
    price: 3200,
    originalPrice: 4799,
    categoryId: 1,
    categoryName: '数码电子',
    condition: 'LIKE_NEW',
    status: 'ON_SALE',
    images: ['https://picsum.photos/seed/product4/400/400'],
    location: '上海',
    views: 2345,
    favorites: 89,
    sellerId: 4,
    sellerName: '陈思雨',
    sellerAvatar: 'https://i.pravatar.cc/150?u=4',
    sellerRating: 4.9,
    createTime: '2024-01-04T00:00:00Z',
    updateTime: '2024-01-04T00:00:00Z',
    isHot: true,
    discount: 33,
    viewCount: 2345,
    category: '数码电子',
  },
]

export default function ProductsSection() {
  const [activeFilter, setActiveFilter] = useState('all')
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsLoading(false)
    }, 800)
    return () => clearTimeout(timer)
  }, [])

  const filters = [
    { id: 'all', label: '全部' },
    { id: 'new', label: '最新发布' },
    { id: 'hot', label: '热门推荐' },
    { id: 'discount', label: '超值优惠' },
  ]

  const getFilteredProducts = () => {
    switch (activeFilter) {
      case 'new':
        return [...mockProducts].sort((a, b) => 
          new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
        )
      case 'hot':
        return mockProducts.filter(p => p.isHot || (p.viewCount && p.viewCount > 500))
      case 'discount':
        return mockProducts.filter(p => p.discount && p.discount > 20)
      default:
        return mockProducts
    }
  }

  const filteredProducts = getFilteredProducts()

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
          ) : (
            filteredProducts.map((product, index) => (
              <ProductCard
                key={product.id}
                product={product}
                style={{ animationDelay: `${index * 80}ms` }}
              />
            ))
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
