import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'

interface RecommendedProduct {
  id: number
  title: string
  price: number
  originalPrice?: number
  image: string
  category: string
  reason: string
  matchScore: number
  seller: {
    name: string
    avatar: string
    rating: number
  }
}

const mockProducts: RecommendedProduct[] = [
  {
    id: 1,
    title: '九成新 iPad Pro 11寸 M2芯片',
    price: 4599,
    originalPrice: 6999,
    image: 'https://picsum.photos/seed/ipad/400/400',
    category: '数码产品',
    reason: '基于你对电子产品的浏览偏好',
    matchScore: 95,
    seller: { name: '数码达人小王', avatar: 'https://picsum.photos/seed/user1/100/100', rating: 4.9 }
  },
  {
    id: 2,
    title: '考研英语全套资料+真题',
    price: 89,
    originalPrice: 299,
    image: 'https://picsum.photos/seed/books/400/400',
    category: '图书教材',
    reason: '你最近搜索过考研资料',
    matchScore: 92,
    seller: { name: '研一学姐', avatar: 'https://picsum.photos/seed/user2/100/100', rating: 5.0 }
  },
  {
    id: 3,
    title: '小电驴 电动车 48V20Ah',
    price: 1280,
    originalPrice: 2500,
    image: 'https://picsum.photos/seed/bike/400/400',
    category: '交通工具',
    reason: '城市通勤热门选择',
    matchScore: 88,
    seller: { name: '毕业学长', avatar: 'https://picsum.photos/seed/user3/100/100', rating: 4.8 }
  },
  {
    id: 4,
    title: '小米台灯 护眼LED',
    price: 59,
    originalPrice: 149,
    image: 'https://picsum.photos/seed/lamp/400/400',
    category: '生活用品',
    reason: '与你浏览的宿舍用品相关',
    matchScore: 85,
    seller: { name: '生活小达人', avatar: 'https://picsum.photos/seed/user4/100/100', rating: 4.7 }
  },
  {
    id: 5,
    title: 'AirPods Pro 2代 全新未拆',
    price: 1299,
    originalPrice: 1899,
    image: 'https://picsum.photos/seed/airpods/400/400',
    category: '数码产品',
    reason: '你收藏过类似商品',
    matchScore: 91,
    seller: { name: '数码控', avatar: 'https://picsum.photos/seed/user5/100/100', rating: 4.9 }
  },
  {
    id: 6,
    title: '羽毛球拍 尤尼克斯 双拍套装',
    price: 168,
    originalPrice: 399,
    image: 'https://picsum.photos/seed/badminton/400/400',
    category: '运动户外',
    reason: '城市运动热门推荐',
    matchScore: 82,
    seller: { name: '运动达人', avatar: 'https://picsum.photos/seed/user6/100/100', rating: 4.6 }
  }
]

function ProductRecommendCard({ product, index }: { product: RecommendedProduct; index: number }) {
  const [isVisible, setIsVisible] = useState(false)
  const cardRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setTimeout(() => setIsVisible(true), index * 80)
        }
      },
      { threshold: 0.1 }
    )

    if (cardRef.current) {
      observer.observe(cardRef.current)
    }

    return () => observer.disconnect()
  }, [index])

  const discount = product.originalPrice 
    ? Math.round((1 - product.price / product.originalPrice) * 100) 
    : 0

  return (
    <div
      ref={cardRef}
      className={`product-recommend-card ${isVisible ? 'visible' : ''}`}
    >
      <div className="product-recommend-image">
        <img src={product.image} alt={product.title} loading="lazy" />
        <div className="product-recommend-overlay">
          <button className="quick-view-btn">快速查看</button>
        </div>
        {discount > 0 && (
          <div className="discount-badge">-{discount}%</div>
        )}
        <div className="match-score">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
          </svg>
          <span>{product.matchScore}%</span>
        </div>
      </div>
      
      <div className="product-recommend-content">
        <div className="product-recommend-category">{product.category}</div>
        <h3 className="product-recommend-title">{product.title}</h3>
        
        <div className="product-recommend-price">
          <span className="current-price">¥{product.price}</span>
          {product.originalPrice && (
            <span className="original-price">¥{product.originalPrice}</span>
          )}
        </div>

        <div className="product-recommend-reason">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
          </svg>
          <span>{product.reason}</span>
        </div>

        <div className="product-recommend-seller">
          <img src={product.seller.avatar} alt={product.seller.name} />
          <span className="seller-name">{product.seller.name}</span>
          <div className="seller-rating">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
            </svg>
            <span>{product.seller.rating}</span>
          </div>
        </div>
      </div>
    </div>
  )
}

function AIRecommendSection() {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<'all' | 'electronics' | 'books' | 'life'>('all')

  const tabs = [
    { id: 'all', label: '全部推荐' },
    { id: 'electronics', label: '数码产品' },
    { id: 'books', label: '图书教材' },
    { id: 'life', label: '生活用品' }
  ]

  return (
    <section className="ai-recommend-section">
      <div className="ai-recommend-bg">
        <div className="recommend-wave" />
      </div>

      <div className="container">
        <div className="ai-recommend-header">
          <div className="ai-recommend-title-group">
            <div className="ai-badge ai-badge-alt">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
              </svg>
              <span>智能推荐</span>
            </div>
            <h2 className="ai-recommend-title">
              为你<span className="gradient-text">精选</span>好物
            </h2>
            <p className="ai-recommend-subtitle">
              AI根据你的浏览习惯和偏好，智能推荐最适合你的商品
            </p>
          </div>

          <div className="ai-recommend-tabs">
            {tabs.map(tab => (
              <button
                key={tab.id}
                className={`recommend-tab ${activeTab === tab.id ? 'active' : ''}`}
                onClick={() => setActiveTab(tab.id as typeof activeTab)}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        <div className="ai-recommend-grid">
          {mockProducts.map((product, index) => (
            <ProductRecommendCard key={product.id} product={product} index={index} />
          ))}
        </div>

        <div className="ai-recommend-footer">
          <button className="view-more-btn" onClick={() => navigate('/products')}>
            <span>查看更多推荐</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M5 12h14M12 5l7 7-7 7" />
            </svg>
          </button>
          <p className="ai-tip">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <path d="M12 16v-4M12 8h.01" />
            </svg>
            <span>推荐结果会根据你的浏览行为持续优化</span>
          </p>
        </div>
      </div>
    </section>
  )
}

export default AIRecommendSection
