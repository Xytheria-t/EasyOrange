import { useEffect, useRef, useState, useCallback } from 'react'
import { Image } from '@/components/ui/Image'
import { statsApi, type PlatformStats } from '@/api/statsApi'

const HERO_PRODUCT = {
  image: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&auto=format&fit=crop',
  tag: '热门',
  name: 'MacBook Pro 14寸 M3芯片 深空灰',
  price: '¥11,999',
  originalPrice: '¥14,999'
}

const DEFAULT_STATS: PlatformStats = {
  activeUsers: 0,
  onlineProducts: 0,
  completedOrders: 0
}

function animateCounter(el: HTMLSpanElement, target: number) {
  if (!target) {
    el.textContent = '0'
    return
  }

  const duration = 2000
  const startTime = performance.now()

  const animate = (currentTime: number) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    const current = Math.floor(eased * target)

    const shouldUpdate = progress === 1 || elapsed % 48 < 16
    if (shouldUpdate) {
      el.textContent = current.toLocaleString()
    }

    if (progress < 1) {
      requestAnimationFrame(animate)
    } else {
      el.textContent = `${target.toLocaleString()}+`
    }
  }

  requestAnimationFrame(animate)
}

export default function HeroSection() {
  const heroRef = useRef<HTMLElement>(null)
  const statRefs = useRef<(HTMLSpanElement | null)[]>([])
  const floatCardsRef = useRef<(HTMLDivElement | null)[]>([])
  const [stats, setStats] = useState<PlatformStats>(DEFAULT_STATS)
  const statsFetchedRef = useRef(false)

  useEffect(() => {
    if (statsFetchedRef.current) { return }
    statsFetchedRef.current = true

    statsApi.getPlatformStats()
      .then((res) => {
        if (res.data) {
          setStats(res.data)
        }
      })
      .catch(() => {})
  }, [])

  const startCounters = useCallback(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) { return }
          const el = entry.target as HTMLSpanElement
          const target = parseInt(el.dataset.count || '0', 10)
          animateCounter(el, target)
          observer.unobserve(el)
        })
      },
      { threshold: 0.5 }
    )

    statRefs.current.forEach((el) => {
      if (el) { observer.observe(el) }
    })

    return () => observer.disconnect()
  }, [])

  useEffect(() => {
    const cleanup = startCounters()
    return cleanup
  }, [startCounters, stats])

  useEffect(() => {
    const cards = floatCardsRef.current.filter(Boolean)
    const visualCard = document.querySelector('.visual-card') as HTMLElement

    const handleMouseMove = (e: MouseEvent, card: HTMLDivElement) => {
      const rect = card.getBoundingClientRect()
      const centerX = rect.left + rect.width / 2
      const centerY = rect.top + rect.height / 2
      const mouseX = e.clientX - centerX
      const mouseY = e.clientY - centerY

      const rotateX = (mouseY / (rect.height / 2)) * -12
      const rotateY = (mouseX / (rect.width / 2)) * 12

      card.style.transform = `perspective(600px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale(1.08) translateZ(20px)`
    }

    cards.forEach((card) => {
      if (!card) { return }

      const onMove = (e: MouseEvent) => handleMouseMove(e, card)
      const onLeave = () => {
        card.style.transition = 'transform 0.5s cubic-bezier(0.22, 1, 0.36, 1)'
        card.style.transform = 'perspective(600px) rotateX(0deg) rotateY(0deg) scale(1) translateZ(0px)'
        setTimeout(() => {
          card.style.transition = ''
        }, 500)
      }

      card.addEventListener('mousemove', onMove)
      card.addEventListener('mouseleave', onLeave)
    })

    if (visualCard) {
      const onMove = (e: MouseEvent) => {
        const rect = visualCard.getBoundingClientRect()
        const centerX = rect.left + rect.width / 2
        const centerY = rect.top + rect.height / 2
        const mouseX = e.clientX - centerX
        const mouseY = e.clientY - centerY

        const rotateX = (mouseY / (rect.height / 2)) * -6
        const rotateY = (mouseX / (rect.width / 2)) * 6

        visualCard.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-6px)`
      }

      const onLeave = () => {
        visualCard.style.transition = 'transform 0.6s cubic-bezier(0.22, 1, 0.36, 1)'
        visualCard.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) translateY(0px)'
        setTimeout(() => {
          visualCard.style.transition = ''
        }, 600)
      }

      visualCard.addEventListener('mousemove', onMove)
      visualCard.addEventListener('mouseleave', onLeave)

      return () => {
        visualCard.removeEventListener('mousemove', onMove)
        visualCard.removeEventListener('mouseleave', onLeave)
      }
    }
  }, [])

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
              <span className="stat-value" data-count={String(stats.activeUsers)} ref={(el) => { statRefs.current[0] = el }}>0</span>
              <span className="stat-label">活跃用户</span>
            </div>
            <div className="stat-divider"></div>
            <div className="stat-item">
              <span className="stat-value" data-count={String(stats.onlineProducts)} ref={(el) => { statRefs.current[1] = el }}>0</span>
              <span className="stat-label">在售商品</span>
            </div>
            <div className="stat-divider"></div>
            <div className="stat-item">
              <span className="stat-value" data-count={String(stats.completedOrders)} ref={(el) => { statRefs.current[2] = el }}>0</span>
              <span className="stat-label">成功交易</span>
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
                  <Image
                    src={HERO_PRODUCT.image}
                    alt={HERO_PRODUCT.name}
                    containerClassName="w-full h-full"
                    className="w-full h-full object-cover"
                    loading="lazy"
                    placeholder="blur"
                  />
                </div>
                <div className="preview-info">
                  <span className="preview-tag">{HERO_PRODUCT.tag}</span>
                  <h4>{HERO_PRODUCT.name}</h4>
                  <p className="preview-price">{HERO_PRODUCT.price}</p>
                  <span className="preview-original">原价 {HERO_PRODUCT.originalPrice}</span>
                </div>
              </div>
            </div>
          </div>
          <div className="floating-cards">
            <div className="float-card float-card-1 glass-card-premium" ref={(el) => { floatCardsRef.current[0] = el }}>
              <div className="float-card-glow"></div>
              <div className="float-card-inner">
                <span className="float-icon">📚</span>
                <span className="float-text">教材资料</span>
                <span className="float-particles"></span>
              </div>
            </div>
            <div className="float-card float-card-2 glass-card-premium" ref={(el) => { floatCardsRef.current[1] = el }}>
              <div className="float-card-glow"></div>
              <div className="float-card-inner">
                <span className="float-icon">💻</span>
                <span className="float-text">电子产品</span>
                <span className="float-particles"></span>
              </div>
            </div>
            <div className="float-card float-card-3 glass-card-premium" ref={(el) => { floatCardsRef.current[2] = el }}>
              <div className="float-card-glow"></div>
              <div className="float-card-inner">
                <span className="float-icon">🚴</span>
                <span className="float-text">交通工具</span>
                <span className="float-particles"></span>
              </div>
            </div>
            <svg className="float-connections" viewBox="0 0 400 400" preserveAspectRatio="none">
              <defs>
                <linearGradient id="lineGrad1" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" stopColor="rgba(249, 115, 22, 0.15)" />
                  <stop offset="100%" stopColor="rgba(249, 115, 22, 0.05)" />
                </linearGradient>
                <linearGradient id="lineGrad2" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" stopColor="rgba(244, 63, 94, 0.12)" />
                  <stop offset="100%" stopColor="rgba(244, 63, 94, 0.03)" />
                </linearGradient>
                <linearGradient id="lineGrad3" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" stopColor="rgba(251, 191, 36, 0.12)" />
                  <stop offset="100%" stopColor="rgba(251, 191, 36, 0.03)" />
                </linearGradient>
              </defs>
              <path className="connection-line line-1" d="M 60 80 Q 120 120 180 140" stroke="url(#lineGrad1)" strokeWidth="1" fill="none" strokeDasharray="4 6" />
              <path className="connection-line line-2" d="M 340 160 Q 280 180 220 200" stroke="url(#lineGrad2)" strokeWidth="1" fill="none" strokeDasharray="4 6" />
              <path className="connection-line line-3" d="M 80 320 Q 140 280 180 260" stroke="url(#lineGrad3)" strokeWidth="1" fill="none" strokeDasharray="4 6" />
            </svg>
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
