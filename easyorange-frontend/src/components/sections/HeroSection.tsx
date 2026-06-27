import { useEffect, useRef, useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Image } from '@/components/ui/Image'

const HERO_PRODUCT = {
  image: 'https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=800&auto=format&fit=crop',
  tag: '热门',
  name: 'Notion 团队版会员 剩余8个月',
  price: '¥299',
  originalPrice: '¥599'
}

const PLATFORM_STATS = {
  activeUsers: 5280,
  onlineProducts: 3560,
  completedOrders: 2180
}

function animateCounter(el: HTMLSpanElement, target: number) {
  if (!target) {
    el.textContent = '0'
    return
  }

  const duration = 2000
  const startTime = performance.now()
  let lastValue = -1

  const animate = (currentTime: number) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    const current = Math.floor(eased * target)

    if (current !== lastValue) {
      lastValue = current
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
  const stats = PLATFORM_STATS
  const navigate = useNavigate()
  const [searchKeyword, setSearchKeyword] = useState('')

  const handleSearchSubmit = useCallback((e?: React.FormEvent) => {
    e?.preventDefault()
    const trimmed = searchKeyword.trim()
    if (trimmed) {
      navigate(`/search?keyword=${encodeURIComponent(trimmed)}`)
    }
  }, [searchKeyword, navigate])

  const handleHotTagClick = useCallback((tag: string) => {
    navigate(`/search?keyword=${encodeURIComponent(tag)}`)
  }, [navigate])

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
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReducedMotion) {return}

    const cards = floatCardsRef.current.filter(Boolean) as HTMLDivElement[]
    const visualCard = document.querySelector('.visual-card') as HTMLDivElement

    let rafId: number | null = null
    let lastProcessTime = 0
    const throttleMs = 16
    const pendingMouseEvents: Map<HTMLElement, { x: number; y: number }> = new Map()
    const rectCache: WeakMap<HTMLElement, DOMRect> = new WeakMap()
    const abortController = new AbortController()

    const getCachedRect = (el: HTMLElement): DOMRect => {
      let rect = rectCache.get(el)
      if (!rect) {
        rect = el.getBoundingClientRect()
        rectCache.set(el, rect)
      }
      return rect
    }

    const processMouseMoves = (timestamp: number) => {
      if (timestamp - lastProcessTime < throttleMs) {
        rafId = requestAnimationFrame(processMouseMoves)
        return
      }
      lastProcessTime = timestamp

      pendingMouseEvents.forEach((pos, card) => {
        const rect = getCachedRect(card)
        const centerX = rect.left + rect.width / 2
        const centerY = rect.top + rect.height / 2
        const mouseX = pos.x - centerX
        const mouseY = pos.y - centerY

        if (card.classList.contains('visual-card')) {
          const rotateX = (mouseY / (rect.height / 2)) * -6
          const rotateY = (mouseX / (rect.width / 2)) * 6
          card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-6px)`
        } else {
          const rotateX = (mouseY / (rect.height / 2)) * -12
          const rotateY = (mouseX / (rect.width / 2)) * 12
          card.style.transform = `perspective(600px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale(1.08) translateZ(20px)`
        }
      })
      pendingMouseEvents.clear()
      rafId = null
    }

    cards.forEach((card) => {
      if (!card) {return}

      const onMove = (e: MouseEvent) => {
        pendingMouseEvents.set(card, { x: e.clientX, y: e.clientY })
        if (!rafId) {
          rafId = requestAnimationFrame(processMouseMoves)
        }
      }
      const onLeave = () => {
        card.classList.add('is-resetting')
        if (card.classList.contains('visual-card')) {
          card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) translateY(0px)'
        } else {
          card.style.transform = 'perspective(600px) rotateX(0deg) rotateY(0deg) scale(1) translateZ(0px)'
        }
        setTimeout(() => {
          card.classList.remove('is-resetting')
        }, 600)
      }

      card.addEventListener('mousemove', onMove, { passive: true, signal: abortController.signal })
      card.addEventListener('mouseleave', onLeave, { signal: abortController.signal })
    })

    if (visualCard && !cards.includes(visualCard)) {
      const onMove = (e: MouseEvent) => {
        pendingMouseEvents.set(visualCard, { x: e.clientX, y: e.clientY })
        if (!rafId) {
          rafId = requestAnimationFrame(processMouseMoves)
        }
      }
      const onLeave = () => {
        visualCard.classList.add('is-resetting')
        visualCard.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) translateY(0px)'
        setTimeout(() => {
          visualCard.classList.remove('is-resetting')
        }, 600)
      }

      visualCard.addEventListener('mousemove', onMove, { passive: true, signal: abortController.signal })
      visualCard.addEventListener('mouseleave', onLeave, { signal: abortController.signal })
    }

    return () => {
      abortController.abort()
      if (rafId) {cancelAnimationFrame(rafId)}
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
              <span className="title-char">每</span>
              <span className="title-char">一</span>
              <span className="title-char">份</span>
              <span className="title-char">资</span>
              <span className="title-char">产</span>
            </span>
            <span className="title-line title-line-2">
              <span className="title-char">运</span>
              <span className="title-char">转</span>
              <span className="title-char">不</span>
              <span className="title-char">息</span>
            </span>
            <span className="title-glow"></span>
          </h1>

          <p className="hero-subtitle animate-slide-up delay-1">
            砍业务,撑架构 · C2C 资产流转全栈架构 demo
          </p>

          <div className="hero-search animate-slide-up delay-2">
            <form className="search-wrapper glass-card" onSubmit={handleSearchSubmit}>
              <svg className="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="text"
                className="search-input"
                placeholder="搜索你想要的资产..."
                aria-label="搜索资产"
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
              />
              <button type="submit" className="search-btn btn btn-primary">
                <span>搜索</span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
              </button>
            </form>
            <div className="search-tags">
              <span className="tag-label">热门搜索:</span>
              <button type="button" className="tag-btn" onClick={() => handleHotTagClick('教材')}>教材</button>
              <button type="button" className="tag-btn" onClick={() => handleHotTagClick('视频会员')}>视频会员</button>
              <button type="button" className="tag-btn" onClick={() => handleHotTagClick('设计素材')}>设计素材</button>
              <button type="button" className="tag-btn" onClick={() => handleHotTagClick('考研资料')}>考研资料</button>
            </div>
          </div>

          <div className="hero-ai-entry animate-slide-up delay-3">
            <button className="ai-entry-btn">
              <div className="ai-entry-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
                </svg>
                <div className="ai-icon-ring" />
              </div>
              <div className="ai-entry-content">
                <span className="ai-entry-title">AI 资产管理</span>
                <span className="ai-entry-desc">拍照估值 · 智能写描述 · 一键发布</span>
              </div>
              <div className="ai-entry-indicator">
                <div className="ai-indicator-dot" />
                <div className="ai-indicator-shimmer" />
              </div>
            </button>
          </div>

          <div className="hero-stats animate-slide-up delay-4">
            <div className="stat-item">
              <span className="stat-value" data-count={String(stats.activeUsers)} ref={(el) => { statRefs.current[0] = el }}>0</span>
              <span className="stat-label">活跃用户</span>
            </div>
            <div className="stat-divider"></div>
            <div className="stat-item">
              <span className="stat-value" data-count={String(stats.onlineProducts)} ref={(el) => { statRefs.current[1] = el }}>0</span>
              <span className="stat-label">在管资产</span>
            </div>
            <div className="stat-divider"></div>
            <div className="stat-item">
              <span className="stat-value" data-count={String(stats.completedOrders)} ref={(el) => { statRefs.current[2] = el }}>0</span>
              <span className="stat-label">AI 成交</span>
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
