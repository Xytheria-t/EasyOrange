import { useEffect, useRef, useState } from 'react'

interface Achievement {
  id: number
  value: string
  label: string
  description: string
  icon: React.ReactNode
  color: string
}

const ACHIEVEMENTS: Achievement[] = [
  {
    id: 1,
    value: '¥2,000,000+',
    label: '累计交易金额',
    description: '帮助用户实现闲置变现',
    color: '#F97316',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <line x1="12" y1="1" x2="12" y2="23" />
        <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
      </svg>
    )
  },
  {
    id: 2,
    value: '15,000+',
    label: '成功交易',
    description: '安全可靠的交易保障',
    color: '#10B981',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
        <polyline points="22 4 12 14.01 9 11.01" />
      </svg>
    )
  },
  {
    id: 3,
    value: '98.5%',
    label: '好评率',
    description: '用户满意度极高',
    color: '#C39BD3',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
      </svg>
    )
  },
  {
    id: 4,
    value: '30分钟',
    label: '平均响应',
    description: '快速沟通高效成交',
    color: '#FB7185',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <circle cx="12" cy="12" r="10" />
        <polyline points="12 6 12 12 16 14" />
      </svg>
    )
  }
]

const TIMELINE_EVENTS = [
  {
    year: '2023',
    title: '平台上线',
    description: 'EasyOrange 正式上线，服务第一批高校用户'
  },
  {
    year: '2024',
    title: '快速发展',
    description: '覆盖全国 200+ 高校，用户突破 5 万'
  },
  {
    year: '2025',
    title: '全面升级',
    description: '推出担保交易、智能推荐等核心功能'
  },
  {
    year: '未来',
    title: '持续成长',
    description: '致力于成为最值得信赖的校园交易平台'
  }
]

export default function AchievementsSection() {
  const [isVisible, setIsVisible] = useState(false)
  const sectionRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          setIsVisible(true)
        }
      },
      { threshold: 0.15 }
    )

    if (sectionRef.current) {
      observer.observe(sectionRef.current)
    }

    return () => observer.disconnect()
  }, [])

  return (
    <section ref={sectionRef} className="achievements-section">
      <div className="achievements-bg">
        <div className="achievements-gradient" />
        <div className="achievements-pattern" />
        <div className="achievements-glow" />
      </div>

      <div className="container">
        <div className={`section-header reveal ${isVisible ? 'revealed' : ''}`}>
          <span className="section-tag">平台成就</span>
          <h2 className="section-title">数字见证成长</h2>
          <p className="section-desc">每一个数字背后，都是用户的信任与支持</p>
        </div>

        <div className={`achievements-grid ${isVisible ? 'visible' : ''}`}>
          {ACHIEVEMENTS.map((achievement, index) => (
            <div
              key={achievement.id}
              className="achievement-card glass-card"
              style={{
                '--accent-color': achievement.color,
                '--delay': `${index * 100}ms`
              } as React.CSSProperties}
            >
              <div className="achievement-icon" style={{ background: achievement.color }}>
                {achievement.icon}
              </div>
              <div className="achievement-value" style={{ color: achievement.color }}>
                {achievement.value}
              </div>
              <h3 className="achievement-label">{achievement.label}</h3>
              <p className="achievement-desc">{achievement.description}</p>
              <div className="achievement-shine" />
            </div>
          ))}
        </div>

        <div className={`timeline-section ${isVisible ? 'visible' : ''}`}>
          <h3 className="timeline-title">发展历程</h3>
          <div className="timeline">
            {TIMELINE_EVENTS.map((event, index) => (
              <div key={index} className="timeline-item">
                <div className="timeline-marker">
                  <div className="marker-dot" />
                  <div className="marker-line" />
                </div>
                <div className="timeline-content">
                  <span className="timeline-year">{event.year}</span>
                  <h4 className="timeline-event-title">{event.title}</h4>
                  <p className="timeline-event-desc">{event.description}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className={`cta-section ${isVisible ? 'visible' : ''}`}>
          <div className="cta-card glass-card">
            <div className="cta-content">
              <h3 className="cta-title">准备好开始了吗？</h3>
              <p className="cta-desc">加入我们，让闲置物品找到新主人</p>
              <div className="cta-buttons">
                <a href="/register" className="btn btn-primary btn-lg">
                  <span>立即注册</span>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="5" y1="12" x2="19" y2="12" />
                    <polyline points="12 5 19 12 12 19" />
                  </svg>
                </a>
                <a href="/products" className="btn btn-outline btn-lg">
                  <span>浏览商品</span>
                </a>
              </div>
            </div>
            <div className="cta-visual">
              <div className="cta-orb cta-orb-1" />
              <div className="cta-orb cta-orb-2" />
              <div className="cta-orb cta-orb-3" />
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
