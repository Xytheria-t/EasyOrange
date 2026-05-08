import { useEffect, useRef, useState } from 'react'

interface Review {
  id: number
  avatar: string
  name: string
  school: string
  rating: number
  content: string
  product: string
  date: string
}

const REVIEWS: Review[] = [
  {
    id: 1,
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&h=100&fit=crop',
    name: '李同学',
    school: '清华大学',
    rating: 5,
    content: '卖家很靠谱，教材保存得很好，还送了一些笔记，超值！平台担保交易让我很放心。',
    product: '考研数学复习全书',
    date: '2024-03-15'
  },
  {
    id: 2,
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop',
    name: '王同学',
    school: '北京大学',
    rating: 5,
    content: '买到了心仪已久的相机，价格比市场价便宜了将近一半，成色也很新，非常满意！',
    product: '索尼 A7M3 相机',
    date: '2024-03-12'
  },
  {
    id: 3,
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop',
    name: '张同学',
    school: '复旦大学',
    rating: 5,
    content: '第一次在这里买东西，体验很好！卖家很耐心解答问题，交易过程很顺利。',
    product: '二手自行车',
    date: '2024-03-10'
  },
  {
    id: 4,
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&h=100&fit=crop',
    name: '刘同学',
    school: '浙江大学',
    rating: 5,
    content: '快速卖掉了闲置的健身器材，不仅清理了空间，还赚了一笔生活费，平台很方便！',
    product: '哑铃套装',
    date: '2024-03-08'
  },
  {
    id: 5,
    avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=100&h=100&fit=crop',
    name: '陈同学',
    school: '上海交通大学',
    rating: 5,
    content: '买到了学长精心保养的吉他，还教了我一些弹奏技巧，收获满满！',
    product: '雅马哈吉他',
    date: '2024-03-05'
  },
  {
    id: 6,
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop',
    name: '赵同学',
    school: '南京大学',
    rating: 5,
    content: '平台推荐很精准，很快就找到了需要的专业课本，省了不少钱！',
    product: '计算机专业教材',
    date: '2024-03-01'
  }
]

export default function ReviewsSection() {
  const [isVisible, setIsVisible] = useState(false)
  const sectionRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          setIsVisible(true)
        }
      },
      { threshold: 0.1 }
    )

    if (sectionRef.current) {
      observer.observe(sectionRef.current)
    }

    return () => observer.disconnect()
  }, [])

  const renderStars = (rating: number) => {
    return (
      <div className="stars">
        {[...Array(5)].map((_, i) => (
          <svg
            key={i}
            viewBox="0 0 24 24"
            fill={i < rating ? 'currentColor' : 'none'}
            stroke="currentColor"
            strokeWidth="2"
          >
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
          </svg>
        ))}
      </div>
    )
  }

  return (
    <section ref={sectionRef} className="reviews-section">
      <div className="reviews-bg">
        <div className="reviews-gradient-orb orb-1" />
        <div className="reviews-gradient-orb orb-2" />
        <div className="reviews-mesh" />
      </div>

      <div className="container">
        <div className={`section-header reveal ${isVisible ? 'revealed' : ''}`}>
          <span className="section-tag">用户评价</span>
          <h2 className="section-title">听听他们怎么说</h2>
          <p className="section-desc">真实用户的真实反馈</p>
        </div>

        <div className={`reviews-grid ${isVisible ? 'visible' : ''}`}>
          {REVIEWS.map((review, index) => (
            <div
              key={review.id}
              className="review-card glass-card"
              style={{
                '--delay': `${index * 80}ms`
              } as React.CSSProperties}
            >
              <div className="review-header">
                <img src={review.avatar} alt={review.name} className="review-avatar" />
                <div className="review-user-info">
                  <h4 className="review-name">{review.name}</h4>
                  <p className="review-school">{review.school}</p>
                </div>
                {renderStars(review.rating)}
              </div>

              <p className="review-content">"{review.content}"</p>

              <div className="review-footer">
                <div className="review-product">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                    <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                  </svg>
                  <span>{review.product}</span>
                </div>
                <span className="review-date">{review.date}</span>
              </div>
            </div>
          ))}
        </div>

        <div className={`reviews-summary ${isVisible ? 'visible' : ''}`}>
          <div className="summary-card glass-card">
            <div className="summary-content">
              <div className="summary-rating">
                <span className="rating-value">4.9</span>
                <div className="rating-stars">
                  {[...Array(5)].map((_, i) => (
                    <svg
                      key={i}
                      viewBox="0 0 24 24"
                      fill="currentColor"
                      stroke="currentColor"
                      strokeWidth="2"
                    >
                      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                    </svg>
                  ))}
                </div>
                <span className="rating-count">基于 1,200+ 条评价</span>
              </div>

              <div className="summary-stats">
                <div className="summary-stat">
                  <span className="stat-value">98%</span>
                  <span className="stat-label">好评率</span>
                </div>
                <div className="summary-stat">
                  <span className="stat-value">95%</span>
                  <span className="stat-label">推荐率</span>
                </div>
                <div className="summary-stat">
                  <span className="stat-value">92%</span>
                  <span className="stat-label">复购率</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
