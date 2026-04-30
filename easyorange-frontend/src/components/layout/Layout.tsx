import { Outlet } from 'react-router-dom';
import { Link } from 'react-router-dom';
import { Header } from './Header';
import { ToastContainer } from '../ui/Toast';
import { GlobalLoading } from '../ui/Loading';

function Layout() {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Header />
      <main className="mx-auto max-w-7xl px-4 pb-6 sm:px-6 lg:px-8 flex-1" style={{ paddingTop: 'var(--header-height)' }}>
        <Outlet />
      </main>
      <footer className="footer-editorial mt-auto">
        <div className="footer-gradient-bg"></div>
        <div className="footer-container">
          <div className="footer-main-grid">
            <div className="footer-left-column">
              <div className="footer-brand-section">
                <div className="brand-gradient-orb"></div>
                <Link to="/" className="footer-logo-link">
                  <div className="logo-gradient-box">
                    <svg viewBox="0 0 40 40" fill="none">
                      <defs>
                        <linearGradient id="footerLogoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                          <stop offset="0%" stopColor="#F97316" />
                          <stop offset="40%" stopColor="#FB7185" />
                          <stop offset="100%" stopColor="#C39BD3" />
                        </linearGradient>
                      </defs>
                      <path d="M6 10h11M6 10v20M6 30h11M6 10h7M6 20h9" stroke="url(#footerLogoGradient)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
                      <circle cx="30" cy="20" r="8" stroke="url(#footerLogoGradient)" strokeWidth="2.5" />
                    </svg>
                  </div>
                  <span className="footer-brand-name">EasyOrange</span>
                </Link>
                <p className="footer-tagline">专为大学生打造的校园二手交易平台，让闲置流转，让价值延续。</p>
                <div className="footer-social-links">
                  <a href="#" className="social-link" aria-label="微信">
                    <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-6.656-6.088V8.87c-.135-.004-.272-.012-.407-.012zm-2.53 3.274c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982z"/></svg>
                  </a>
                  <a href="#" className="social-link" aria-label="微博">
                    <svg viewBox="0 0 24 24" fill="currentColor"><path d="M10.098 20c-4.612 0-8.363-2.222-8.363-4.963 0-1.442 1.012-2.838 2.654-3.97 2.1-1.452 4.595-1.9 5.745-1.028.314.239.498.582.498.953 0 1.21-1.253 2.194-2.8 2.194-1.543 0-2.795-.983-2.795-2.194 0-.269.074-.525.202-.751.826-1.473 2.66-2.14 4.073-1.446 2.058 1.01 3.042 3.405 3.042 5.485 0 3.038-2.99 5.72-7.107 5.72zm9.625-14.295c-1.746-1.912-4.16-2.846-6.674-2.846-2.514 0-4.928.934-6.674 2.846C5.286 7.446 4.67 8.71 4.67 10.037c0 1.327.616 2.591 1.675 3.59 1.746 1.912 4.16 2.846 6.674 2.846 2.514 0 4.928-.934 6.674-2.846 1.06-.999 1.675-2.263 1.675-3.59 0-1.327-.616-2.591-1.675-3.59zM17.51 8.71c-.696.762-1.536 1.299-2.437 1.657-.902.358-1.878.538-2.84.538-.962 0-1.938-.18-2.84-.538-.902-.358-1.741-.895-2.437-1.657C6.09 10.23 5.51 11.494 5.51 12.82c0 1.327.58 2.591 1.446 3.59.696.762 1.536 1.299 2.437 1.657.902.358 1.878.538 2.84.538.962 0 1.938-.18 2.84-.538.902-.358 1.741-.895 2.437-1.657.866-.999 1.446-2.263 1.446-3.59 0-1.326-.58-2.59-1.446-3.59zm-2.438 6.17c-1.074.59-2.315.886-3.595.886-1.28 0-2.521-.295-3.595-.886-1.074-.59-1.93-1.44-2.478-2.479-.548-1.04-.823-2.198-.823-3.4 0-1.202.275-2.36.823-3.4.548-1.04 1.404-1.89 2.478-2.479C9.156 4.295 10.397 4 11.677 4c1.28 0 2.521.295 3.595.886 1.074.59 1.93 1.44 2.478 2.479.548 1.04.823 2.198.823 3.4 0 1.202-.275 2.36-.823 3.4-.548 1.04-1.404 1.89-2.478 2.479z"/></svg>
                  </a>
                </div>
              </div>
            </div>

            <div className="footer-right-column">
              <div className="footer-stats-row">
                <div className="stat-item-compact">
                  <span className="stat-value-lg">10,000+</span>
                  <span className="stat-label-sm">活跃用户</span>
                </div>
                <div className="stat-divider-vertical"></div>
                <div className="stat-item-compact">
                  <span className="stat-value-lg">50,000+</span>
                  <span className="stat-label-sm">在售商品</span>
                </div>
                <div className="stat-divider-vertical"></div>
                <div className="stat-item-compact">
                  <span className="stat-value-lg">100+</span>
                  <span className="stat-label-sm">合作高校</span>
                </div>
              </div>

              <nav className="footer-nav-compact">
                <div className="nav-compact-group">
                  <span className="nav-compact-heading">平台</span>
                  <Link to="/products" className="nav-compact-link">
                    <span className="link-dot"></span>
                    全部商品
                  </Link>
                  <Link to="/publish" className="nav-compact-link">
                    <span className="link-dot"></span>
                    发布闲置
                  </Link>
                  <a href="#" className="nav-compact-link">
                    <span className="link-dot"></span>
                    平台规则
                  </a>
                </div>
                <div className="nav-compact-group">
                  <span className="nav-compact-heading">支持</span>
                  <a href="#" className="nav-compact-link">
                    <span className="link-dot"></span>
                    帮助中心
                  </a>
                  <a href="#" className="nav-compact-link">
                    <span className="link-dot"></span>
                    联系客服
                  </a>
                  <a href="#" className="nav-compact-link">
                    <span className="link-dot"></span>
                    意见反馈
                  </a>
                </div>
                <div className="nav-compact-group">
                  <span className="nav-compact-heading">关于</span>
                  <a href="#" className="nav-compact-link">
                    <span className="link-dot"></span>
                    关于我们
                  </a>
                  <a href="#" className="nav-compact-link">
                    <span className="link-dot"></span>
                    隐私政策
                  </a>
                  <a href="#" className="nav-compact-link">
                    <span className="link-dot"></span>
                    用户协议
                  </a>
                </div>
              </nav>
            </div>
          </div>

          <div className="footer-bottom-bar">
            <div className="bottom-glow-line"></div>
            <p className="copyright-text">
              © 2026 EasyOrange · 校园二手交易平台
              <span className="heart-icon">❤</span>
            </p>
            <p className="copyright-info">让每一份闲置都能找到新的主人</p>
          </div>
        </div>
      </footer>
      <ToastContainer />
      <GlobalLoading />
    </div>
  );
}

export { Layout };
