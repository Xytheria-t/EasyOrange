export default function FeaturedSection() {
  const features = [
    {
      id: 1,
      label: '校园认证',
      desc: '实名认证 · 安全交易',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          <path d="M9 12l2 2 4-4" />
        </svg>
      ),
      gradient: 'gradient-1',
    },
    {
      id: 2,
      label: '快速发布',
      desc: '30 秒发布 · 智能分类',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
        </svg>
      ),
      gradient: 'gradient-2',
    },
    {
      id: 3,
      label: '精准推荐',
      desc: '智能算法 · 发现所需',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="10" />
          <circle cx="12" cy="12" r="6" />
          <circle cx="12" cy="12" r="2" />
        </svg>
      ),
      gradient: 'gradient-3',
    },
    {
      id: 4,
      label: '无忧售后',
      desc: '平台保障 · 放心购物',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
        </svg>
      ),
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
              className="featured-card glass-card"
            >
              <div className="featured-card-content">
                <div className="featured-icon-wrapper">
                  <div className={`featured-icon ${feature.gradient}`}>
                    {feature.icon}
                  </div>
                </div>
                <div className="featured-text">
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
