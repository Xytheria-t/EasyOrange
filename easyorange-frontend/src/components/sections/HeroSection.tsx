import { useEffect, useRef, useState } from 'react'

export default function HeroSection() {
  const heroRef = useRef<HTMLElement>(null)
  const statRefs = useRef<(HTMLSpanElement | null)[]>([])
  const [countersStarted, setCountersStarted] = useState(false)

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
        <div className="hero-blob hero-blob-1"></div>
        <div className="hero-blob hero-blob-2"></div>
        <div className="hero-blob hero-blob-3"></div>
        <div className="hero-aurora"></div>
        <div className="hero-noise"></div>
        <div className="hero-grid"></div>
        <div className="hero-vignette"></div>
      </div>

      <div className="container hero-inner">
        <div className="hero-content">
          <div className="hero-brand animate-fade-in">
            <span className="brand-dot"></span>
            <span>EasyOrange</span>
          </div>

          <h1 className="hero-title animate-title-reveal">
            <span className="title-line title-line-1">
              <span className="title-char">让</span>
              <span className="title-char">闲</span>
              <span className="title-char">置</span>
              <span className="title-char">流</span>
              <span className="title-char">转</span>
            </span>
            <span className="title-line title-line-2">
              <span className="title-char">让</span>
              <span className="title-char">价</span>
              <span className="title-char">值</span>
              <span className="title-char">延</span>
              <span className="title-char">续</span>
            </span>
            <span className="title-glow"></span>
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
          <div className="visual-card glass-card-premium">
            <div className="card-glow-premium"></div>
            <div className="card-shine"></div>
            <div className="card-content">
              <div className="product-preview">
                <div className="preview-image">
                  <img src="https://picsum.photos/seed/hero-product/400/400" alt="热门商品" loading="lazy" />
                </div>
                <div className="preview-info">
                  <span className="preview-tag">热门</span>
                  <h4>MacBook Pro 2023</h4>
                  <p className="preview-price">¥6,999</p>
                  <span className="preview-original">原价 ¥12,999</span>
                </div>
              </div>
            </div>
          </div>
          <div className="floating-cards">
            <div className="float-card float-card-1 glass-card-premium">
              <span className="float-icon">📚</span>
              <span>教材资料</span>
            </div>
            <div className="float-card float-card-2 glass-card-premium">
              <span className="float-icon">💻</span>
              <span>电子产品</span>
            </div>
            <div className="float-card float-card-3 glass-card-premium">
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

      {/* 底部波浪过渡 */}
      <div className="hero-wave-bottom">
        <svg viewBox="0 0 1440 80" preserveAspectRatio="none">
          <path
            d="M0,40 C240,80 480,0 720,40 C960,80 1200,0 1440,40 L1440,80 L0,80 Z"
            fill="#FAF8F5"
            opacity="0.8"
          />
          <path
            d="M0,50 C360,90 720,10 1080,50 C1260,70 1350,60 1440,50 L1440,80 L0,80 Z"
            fill="#FAF8F5"
            opacity="0.5"
          />
        </svg>
      </div>
    </section>
  )
}
