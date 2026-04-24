import { useEffect, useRef, useState } from 'react'

export default function HeroSection() {
  const heroRef = useRef<HTMLElement>(null)
  const particlesRef = useRef<HTMLDivElement>(null)
  const statRefs = useRef<(HTMLSpanElement | null)[]>([])

  const [countersStarted, setCountersStarted] = useState(false)

  useEffect(() => {
    if (particlesRef.current) {
      for (let i = 0; i < 12; i++) {
        const particle = document.createElement('div')
        particle.className = 'particle'
        const size = Math.random() * 4 + 2
        const colors = ['rgba(249,115,22,0.15)', 'rgba(251,113,133,0.12)', 'rgba(195,155,211,0.12)', 'rgba(251,191,36,0.10)']
        particle.style.cssText = `
          position: absolute;
          width: ${size}px;
          height: ${size}px;
          border-radius: 50%;
          background: ${colors[Math.floor(Math.random() * colors.length)]};
          left: ${Math.random() * 100}%;
          top: ${Math.random() * 100}%;
          animation: particleFloat ${8 + Math.random() * 12}s ease-in-out infinite ${-Math.random() * 8}s;
          pointer-events: none;
          filter: blur(${size > 4 ? 1 : 0}px);
        `
        particlesRef.current.appendChild(particle)
      }
    }
  }, [])

  useEffect(() => {
    if (countersStarted) return
    setCountersStarted(true)

    statRefs.current.forEach((el) => {
      if (!el) return
      const target = parseInt(el.dataset.count || '0', 10)
      if (!target) return

      const duration = 2000
      const startTime = performance.now()

      const animate = (currentTime: number) => {
        const elapsed = currentTime - startTime
        const progress = Math.min(elapsed / duration, 1)
        const eased = 1 - Math.pow(1 - progress, 3)
        const current = Math.floor(eased * target)

        el.textContent = current.toLocaleString()

        if (progress < 1) {
          requestAnimationFrame(animate)
        } else {
          el.textContent = target.toLocaleString()
          if (target === 100) el.textContent = target + '+'
          else if (target === 10000) el.textContent = target.toLocaleString() + '+'
          else if (target === 50000) el.textContent = target.toLocaleString() + '+'
        }
      }

      const observer = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting) {
            requestAnimationFrame(animate)
            observer.disconnect()
          }
        },
        { threshold: 0.5 }
      )
      observer.observe(el)
    })
  }, [countersStarted])

  return (
    <section className="hero-section" ref={heroRef}>
      <div className="hero-bg">
        <div className="hero-gradient"></div>
        <div className="hero-grid"></div>
        <div className="hero-particles" ref={particlesRef}></div>
      </div>

      <div className="container hero-inner">
        <div className="hero-content">
          <div className="hero-badge animate-fade-in">
            <span className="badge-dot"></span>
            <span>校园专属二手交易平台</span>
          </div>

          <h1 className="hero-title animate-slide-up">
            <span className="title-line">让闲置流转</span>
            <span className="title-line gradient-text">让价值延续</span>
          </h1>

          <p className="hero-subtitle animate-slide-up delay-1">
            安全便捷的校园交易体验，连接每一份闲置与需求
          </p>

          <div className="hero-search animate-slide-up delay-2">
            <div className="search-wrapper glass-card">
              <svg className="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input type="text" className="search-input" placeholder="搜索你想要的商品..." aria-label="搜索商品" />
              <button className="search-btn btn btn-primary">
                <span>搜索</span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
              </button>
            </div>
            <div className="search-tags">
              <span className="tag-label">热门搜索:</span>
              <button className="tag-btn">教材</button>
              <button className="tag-btn">自行车</button>
              <button className="tag-btn">电子产品</button>
              <button className="tag-btn">考研资料</button>
            </div>
          </div>

          <div className="hero-stats animate-slide-up delay-3">
            <div className="stat-item">
              <span className="stat-value" data-count="10000" ref={(el) => { statRefs.current[0] = el }}>0</span>
              <span className="stat-label">活跃用户</span>
            </div>
            <div className="stat-divider"></div>
            <div className="stat-item">
              <span className="stat-value" data-count="50000" ref={(el) => { statRefs.current[1] = el }}>0</span>
              <span className="stat-label">在售商品</span>
            </div>
            <div className="stat-divider"></div>
            <div className="stat-item">
              <span className="stat-value" data-count="100" ref={(el) => { statRefs.current[2] = el }}>0</span>
              <span className="stat-label">合作高校</span>
            </div>
          </div>
        </div>

        <div className="hero-visual animate-float">
          <div className="visual-card glass-card">
            <div className="card-glow"></div>
            <div className="card-content">
              <div className="product-preview">
                <div className="preview-image">
                  <img src="https://picsum.photos/seed/hero-product/400/400" alt="热门商品" loading="lazy" />
                </div>
                <div className="preview-info">
                  <span className="preview-tag">🔥 热门</span>
                  <h4>MacBook Pro 2023</h4>
                  <p className="preview-price">¥6,999</p>
                  <span className="preview-original">原价 ¥12,999</span>
                </div>
              </div>
            </div>
          </div>
          <div className="floating-cards">
            <div className="float-card float-card-1 glass-card">
              <span className="float-icon">📚</span>
              <span>教材资料</span>
            </div>
            <div className="float-card float-card-2 glass-card">
              <span className="float-icon">💻</span>
              <span>电子产品</span>
            </div>
            <div className="float-card float-card-3 glass-card">
              <span className="float-icon">🚴</span>
              <span>交通工具</span>
            </div>
          </div>
        </div>
      </div>

      <div className="hero-bottom-elements">
        <div className="scroll-indicator">
          <div className="scroll-mouse">
            <div className="scroll-wheel"></div>
          </div>
          <span>向下滚动探索更多</span>
        </div>
      </div>
    </section>
  )
}
