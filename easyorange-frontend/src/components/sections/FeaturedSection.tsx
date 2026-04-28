export default function FeaturedSection() {
  const features = [
    {
      id: 1,
      label: '校园认证',
      desc: '实名认证 · 安全交易',
      icon: '✓',
      gradient: 'gradient-1',
    },
    {
      id: 2,
      label: '快速发布',
      desc: '30 秒发布 · 智能分类',
      icon: '⚡',
      gradient: 'gradient-2',
    },
    {
      id: 3,
      label: '精准推荐',
      desc: '智能算法 · 发现所需',
      icon: '🎯',
      gradient: 'gradient-3',
    },
    {
      id: 4,
      label: '无忧售后',
      desc: '平台保障 · 放心购物',
      icon: '🛡️',
      gradient: 'gradient-4',
    },
  ]

  return (
    <section className="featured-section">
      <div className="container">
        <div className="section-header">
          <span className="section-tag">平台优势</span>
          <h2 className="section-title">为什么选择 EasyOrange</h2>
          <p className="section-desc">专为大学生打造的二手交易平台，让校园交易更简单、更安全</p>
        </div>

        <div className="featured-grid">
          {features.map((feature) => (
            <a
              key={feature.id}
              href="/features"
              className="featured-card footer-featured-card glass-card"
            >
              <div className="featured-card-inner">
                <div className="featured-icon-wrapper">
                  <span className={`featured-icon ${feature.gradient}`}>{feature.icon}</span>
                </div>
                <div className="featured-content">
                  <span className="featured-label">{feature.label}</span>
                  <p className="featured-desc">{feature.desc}</p>
                </div>
                <div className="featured-arrow">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="5" y1="12" x2="19" y2="12" />
                    <polyline points="12 5 19 12 12 19" />
                  </svg>
                </div>
              </div>
            </a>
          ))}
        </div>
      </div>
    </section>
  )
}
