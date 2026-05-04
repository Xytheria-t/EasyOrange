import { useEffect, useRef, useState } from 'react'

export default function ServicesSection() {
  const [activeCard, setActiveCard] = useState<number | null>(null)
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

  const services = [
    {
      id: 1,
      title: '实名认证',
      desc: '学生认证 · 安全交易',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
          <path d="M16 11l2 2 4-4" />
        </svg>
      ),
      gradient: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
      accent: '#10b981',
    },
    {
      id: 2,
      title: '快速发布',
      desc: '30秒发布 · 智能分类',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
        </svg>
      ),
      gradient: 'linear-gradient(135deg, #06b6d4 0%, #0891b2 100%)',
      accent: '#06b6d4',
    },
    {
      id: 3,
      title: '担保交易',
      desc: '平台担保 · 放心购物',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          <path d="M9 12l2 2 4-4" />
        </svg>
      ),
      gradient: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
      accent: '#f59e0b',
    },
    {
      id: 4,
      title: '精准推荐',
      desc: '智能算法 · 发现所需',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <circle cx="12" cy="12" r="6" />
          <circle cx="12" cy="12" r="2" />
        </svg>
      ),
      gradient: 'linear-gradient(135deg, #ec4899 0%, #db2777 100%)',
      accent: '#ec4899',
    },
    {
      id: 5,
      title: '快速响应',
      desc: '24小时在线客服',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          <path d="M8 10h.01M12 10h.01M16 10h.01" />
        </svg>
      ),
      gradient: 'linear-gradient(135deg, #f43f5e 0%, #e11d48 100%)',
      accent: '#f43f5e',
    },
    {
      id: 6,
      title: '纠纷调解',
      desc: '公正调解 · 保障权益',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <path d="M8 12h8M12 8v8" />
        </svg>
      ),
      gradient: 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)',
      accent: '#8b5cf6',
    },
    {
      id: 7,
      title: '隐私保护',
      desc: '严格保护用户隐私',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
      ),
      gradient: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
      accent: '#3b82f6',
    },
    {
      id: 8,
      title: '校园配送',
      desc: '校内面交 · 方便快捷',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="3" />
          <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
        </svg>
      ),
      gradient: 'linear-gradient(135deg, #f97316 0%, #ea580c 100%)',
      accent: '#f97316',
    },
  ]

  return (
    <section ref={sectionRef} className="services-orbit-section">
      <div className="services-orbit-bg">
        <div className="orbit-bg-gradient" />
        <div className="orbit-grid-pattern" />
        <div className="orbit-floating-particles">
          {[...Array(12)].map((_, i) => (
            <div key={i} className={`orbit-particle orbit-particle-${i + 1}`} />
          ))}
        </div>
      </div>

      <div className="container">
        <div className={`section-header reveal ${isVisible ? 'revealed' : ''}`}>
          <span className="section-tag">平台保障</span>
          <h2 className="section-title">全方位的服务保障</h2>
          <p className="section-desc">八大核心优势，为您的校园交易保驾护航</p>
        </div>

        <div className={`services-orbit-container ${isVisible ? 'visible' : ''}`}>
          <div className="orbit-center">
            <div className="orbit-center-inner">
              <div className="orbit-center-ring orbit-ring-1" />
              <div className="orbit-center-ring orbit-ring-2" />
              <div className="orbit-center-ring orbit-ring-3" />
              <div className="orbit-center-core">
                <div className="orbit-core-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                    <path d="M9 12l2 2 4-4" />
                  </svg>
                </div>
                <div className="orbit-core-stat">
                  <span className="orbit-core-value">100%</span>
                  <span className="orbit-core-label">安全保障</span>
                </div>
              </div>
            </div>
          </div>

          <div className="orbit-track">
            <svg className="orbit-track-svg" viewBox="0 0 600 600">
              <defs>
                <linearGradient id="orbitGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="rgba(249, 115, 22, 0.3)" />
                  <stop offset="50%" stopColor="rgba(236, 72, 153, 0.2)" />
                  <stop offset="100%" stopColor="rgba(139, 92, 246, 0.3)" />
                </linearGradient>
              </defs>
              <circle
                cx="300"
                cy="300"
                r="240"
                fill="none"
                stroke="url(#orbitGradient)"
                strokeWidth="1"
                strokeDasharray="8 12"
                className="orbit-track-circle"
              />
            </svg>
          </div>

          <div className="orbit-connections">
            {services.map((service, index) => {
              const angle = (index * 45 - 90) * (Math.PI / 180)
              const x = 50 + Math.cos(angle) * 40
              const y = 50 + Math.sin(angle) * 40
              return (
                <div
                  key={service.id}
                  className={`orbit-connection orbit-connection-${index + 1}`}
                  style={{
                    background: `radial-gradient(circle at ${x}% ${y}%, ${service.accent}20, transparent 50%)`,
                  }}
                />
              )
            })}
          </div>

          <div className="orbit-cards">
            {services.map((service, index) => {
              const angle = index * 45 - 90
              const isActive = activeCard === index
              return (
                <div
                  key={service.id}
                  className={`orbit-card ${isVisible ? 'visible' : ''} ${isActive ? 'active' : ''}`}
                  style={{
                    '--orbit-angle': `${angle}deg`,
                    '--orbit-delay': `${index * 80}ms`,
                    '--accent-color': service.accent,
                  } as React.CSSProperties}
                  onMouseEnter={() => setActiveCard(index)}
                  onMouseLeave={() => setActiveCard(null)}
                >
                  <div className="orbit-card-glow" style={{ background: service.gradient }} />
                  <div className="orbit-card-border" />
                  <div className="orbit-card-content">
                    <div className="orbit-card-icon" style={{ background: service.gradient }}>
                      <div className="orbit-icon-inner">{service.icon}</div>
                    </div>
                    <div className="orbit-card-info">
                      <h3 className="orbit-card-title">{service.title}</h3>
                      <p className="orbit-card-desc">{service.desc}</p>
                    </div>
                    <div className="orbit-card-pulse" style={{ background: service.accent }} />
                  </div>
                </div>
              )
            })}
          </div>

          <div className="orbit-flow-lines">
            <svg className="orbit-flow-svg" viewBox="0 0 600 600">
              <defs>
                <linearGradient id="flowGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" stopColor="transparent" />
                  <stop offset="50%" stopColor="rgba(249, 115, 22, 0.6)" />
                  <stop offset="100%" stopColor="transparent" />
                </linearGradient>
              </defs>
              {[...Array(8)].map((_, i) => {
                const angle = i * 45 - 90
                const rad = angle * (Math.PI / 180)
                const x1 = 300 + Math.cos(rad) * 80
                const y1 = 300 + Math.sin(rad) * 80
                const x2 = 300 + Math.cos(rad) * 200
                const y2 = 300 + Math.sin(rad) * 200
                return (
                  <line
                    key={i}
                    x1={x1}
                    y1={y1}
                    x2={x2}
                    y2={y2}
                    stroke="url(#flowGradient)"
                    strokeWidth="2"
                    className={`orbit-flow-line orbit-flow-${i + 1}`}
                  />
                )
              })}
            </svg>
          </div>
        </div>
      </div>
    </section>
  )
}
