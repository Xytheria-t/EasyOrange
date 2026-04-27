export default function BannerSection() {
  return (
    <section className="banner-section">
      <div className="container">
        <div className="banner-card">
          {/* 背景渐变 */}
          <div className="banner-bg"></div>
          
          {/* 装饰元素 */}
          <div className="banner-decorations">
            <div className="banner-orb banner-orb-1"></div>
            <div className="banner-orb banner-orb-2"></div>
            <div className="banner-orb banner-orb-3"></div>
          </div>

          {/* 内容 */}
          <div className="banner-content">
            <div className="banner-text">
              <h2 className="banner-title">
                毕业季特惠
                <br />
                <span className="highlight">全场 5 折起</span>
              </h2>
              <p className="banner-desc">
                毕业生专属优惠，海量优质好物低价处理。教材、电子产品、生活用品应有尽有！
              </p>
              <div className="banner-actions">
                <a
                  href="/products?tag=graduation"
                  className="btn btn-primary btn-lg"
                >
                  <span>立即抢购</span>
                  <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="5" y1="12" x2="19" y2="12" />
                    <polyline points="12 5 19 12 12 19" />
                  </svg>
                </a>
                <a
                  href="/publish?tag=graduation"
                  className="btn btn-outline btn-lg"
                >
                  <span>发布闲置</span>
                  <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                </a>
              </div>
            </div>

            {/* 右侧统计 */}
            <div className="banner-stats">
              <div className="banner-stat-item glass-card">
                <div className="stat-value">10,000+</div>
                <div className="stat-label">毕业季商品</div>
              </div>
              <div className="banner-stat-item glass-card">
                <div className="stat-value">5 折</div>
                <div className="stat-label">最低折扣</div>
              </div>
              <div className="banner-stat-item glass-card">
                <div className="stat-value">24h</div>
                <div className="stat-label">快速发货</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
