import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import './ai-features.css'

interface AIFeature {
  id: string
  icon: React.ReactNode
  title: string
  description: string
  demo: string
  gradient: string
  glowColor: string
}

const aiFeatures: AIFeature[] = [
  {
    id: 'pricing',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83" />
        <circle cx="12" cy="12" r="4" />
      </svg>
    ),
    title: 'AI智能估价',
    description: '拍照识别商品，AI自动分析市场行情，给出合理定价建议',
    demo: '拍照估价',
    gradient: 'linear-gradient(135deg, #F97316 0%, #FB7185 100%)',
    glowColor: 'rgba(249, 115, 22, 0.3)'
  },
  {
    id: 'recommend',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
      </svg>
    ),
    title: '智能推荐',
    description: '基于浏览习惯和偏好，精准推荐你可能感兴趣的商品',
    demo: '查看推荐',
    gradient: 'linear-gradient(135deg, #FB7185 0%, #C39BD3 100%)',
    glowColor: 'rgba(195, 155, 211, 0.3)'
  },
  {
    id: 'classify',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </svg>
    ),
    title: '图像识别分类',
    description: '上传商品图片，AI自动识别类别，一键填写商品信息',
    demo: '试试看',
    gradient: 'linear-gradient(135deg, #C39BD3 0%, #D8B4FE 100%)',
    glowColor: 'rgba(216, 180, 254, 0.3)'
  },
  {
    id: 'generate',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
        <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
      </svg>
    ),
    title: '智能文案生成',
    description: '输入商品关键词，AI自动生成吸引人的商品描述和标题',
    demo: '生成文案',
    gradient: 'linear-gradient(135deg, #FBBF24 0%, #F97316 100%)',
    glowColor: 'rgba(251, 191, 36, 0.3)'
  }
]

const FEATURE_ROUTES: Record<string, string> = {
  pricing: '/publish',
  recommend: '/products',
  classify: '/publish',
  generate: '/publish',
}

function AIFeatureCard({ feature, index }: { feature: AIFeature; index: number }) {
  const [isVisible, setIsVisible] = useState(false)
  const cardRef = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setTimeout(() => setIsVisible(true), index * 100)
        }
      },
      { threshold: 0.2 }
    )

    if (cardRef.current) {
      observer.observe(cardRef.current)
    }

    return () => observer.disconnect()
  }, [index])

  return (
    <div
      ref={cardRef}
      className={`ai-feature-card ${isVisible ? 'visible' : ''}`}
      style={{
        '--card-gradient': feature.gradient,
        '--card-glow': feature.glowColor
      } as React.CSSProperties}
    >
      <div className="ai-feature-glow" />
      <div className="ai-feature-content">
        <div className="ai-feature-icon" style={{ background: feature.gradient }}>
          {feature.icon}
        </div>
        <h3 className="ai-feature-title">{feature.title}</h3>
        <p className="ai-feature-description">{feature.description}</p>
        <button
          className="ai-feature-demo"
          onClick={() => {
            const route = FEATURE_ROUTES[feature.id]
            if (route) navigate(route)
          }}
        >
          <span>{feature.demo}</span>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M5 12h14M12 5l7 7-7 7" />
          </svg>
        </button>
      </div>
      <div className="ai-feature-particles">
        {[...Array(6)].map((_, i) => (
          <div key={i} className="particle" style={{ animationDelay: `${i * 0.5}s` }} />
        ))}
      </div>
    </div>
  )
}

function AIFeaturesSection() {
  return (
    <section className="ai-features-section">
      <div className="ai-features-bg">
        <div className="ai-gradient-orb orb-1" />
        <div className="ai-gradient-orb orb-2" />
        <div className="ai-flow-lines" />
      </div>

      <div className="container">
        <div className="ai-features-header">
          <div className="ai-badge">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
            </svg>
            <span>AI智能助手</span>
          </div>
          <h2 className="ai-features-title">
            <span className="gradient-text">智能科技</span>
            <span>让交易更简单</span>
          </h2>
          <p className="ai-features-subtitle">
            基于先进AI技术，为您提供智能估价、精准推荐、自动识别等一站式智能服务
          </p>
        </div>

        <div className="ai-features-grid">
          {aiFeatures.map((feature, index) => (
            <AIFeatureCard key={feature.id} feature={feature} index={index} />
          ))}
        </div>

        <div className="ai-features-stats">
          <div className="ai-stat-item">
            <div className="ai-stat-value">
              <span className="gradient-text">98%</span>
            </div>
            <div className="ai-stat-label">估价准确率</div>
          </div>
          <div className="ai-stat-divider" />
          <div className="ai-stat-item">
            <div className="ai-stat-value">
              <span className="gradient-text">3秒</span>
            </div>
            <div className="ai-stat-label">平均识别速度</div>
          </div>
          <div className="ai-stat-divider" />
          <div className="ai-stat-item">
            <div className="ai-stat-value">
              <span className="gradient-text">50万+</span>
            </div>
            <div className="ai-stat-label">AI处理请求</div>
          </div>
          <div className="ai-stat-divider" />
          <div className="ai-stat-item">
            <div className="ai-stat-value">
              <span className="gradient-text">100+</span>
            </div>
            <div className="ai-stat-label">商品类别识别</div>
          </div>
        </div>
      </div>
    </section>
  )
}

export default AIFeaturesSection
