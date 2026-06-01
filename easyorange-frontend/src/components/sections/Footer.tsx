export default function Footer() {
  return (
    <footer className="footer-editorial">
      <div className="footer-gradient-bg"></div>
      <div className="footer-container">
        <div className="footer-main-grid">
          <div className="footer-left-column">
            <div className="footer-brand-section">
              <div className="brand-gradient-orb"></div>
              <a href="/" className="footer-logo-link">
                <div className="logo-gradient-box">
                  <svg viewBox="0 0 40 40" fill="none">
                    <defs>
                      <linearGradient id="footerLogoGradientEditorial" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stopColor="#F97316" />
                        <stop offset="40%" stopColor="#FB7185" />
                        <stop offset="100%" stopColor="#C39BD3" />
                      </linearGradient>
                    </defs>
                    <path d="M6 10h11M6 10v20M6 30h11M6 10h7M6 20h9" stroke="url(#footerLogoGradientEditorial)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
                    <circle cx="30" cy="20" r="8" stroke="url(#footerLogoGradientEditorial)" strokeWidth="2.5" />
                  </svg>
                </div>
                <span className="footer-brand-name">EasyOrange</span>
              </a>
              <p className="footer-tagline">让闲置流转，让价值延续</p>
              <div className="footer-social-links">
                <button type="button" className="social-link" data-platform="wechat" aria-label="微信">
                  <svg viewBox="0 0 24 24" fill="currentColor">
                    <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-6.656-6.088V8.89c-.135-.01-.269-.03-.406-.03zm-2.53 3.274c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982z"/>
                  </svg>
                </button>
                <button type="button" className="social-link" data-platform="weibo" aria-label="微博">
                  <svg viewBox="0 0 24 24" fill="currentColor">
                    <path d="M10.098 20.323c-3.977.391-7.414-1.406-7.672-4.02-.259-2.609 2.759-5.047 6.74-5.441 3.979-.394 7.413 1.404 7.671 4.018.259 2.6-2.759 5.049-6.739 5.443zM9.05 17.219c-.384.616-1.208.884-1.829.602-.612-.279-.793-.991-.406-1.593.379-.595 1.176-.861 1.793-.601.622.263.82.972.442 1.592zm1.27-1.627c-.141.237-.449.353-.689.253-.236-.09-.313-.361-.177-.586.138-.227.436-.346.672-.24.239.09.315.36.194.573zm.176-2.719c-1.893-.493-4.033.45-4.857 2.118-.836 1.704-.026 3.591 1.886 4.21 1.983.64 4.318-.341 5.132-2.179.8-1.793-.201-3.642-2.161-4.149zm7.563-1.224c-.346-.105-.579-.18-.405-.649.381-1.017.422-1.896-.006-2.523-.801-1.169-2.992-1.107-5.528-.03 0 0-.792.346-.589-.283.389-1.229.332-2.258-.276-2.851-1.379-1.345-5.049.051-8.199 3.118C.964 11.652 0 14.31 0 16.552c0 4.283 5.503 6.893 10.89 6.893 7.065 0 11.771-4.104 11.771-7.361 0-1.967-1.66-3.083-3.602-3.435zm2.334-4.845c-.615-.615-1.527-.836-2.344-.634l.018-.018c.281-.07.577-.097.875-.077.615.042 1.174.289 1.597.712.423.423.67.982.712 1.597.02.298-.007.594-.077.875l-.018.018c.202-.817-.019-1.758-.963-2.473z"/>
                  </svg>
                </button>
                <button type="button" className="social-link" data-platform="qq" aria-label="QQ">
                  <svg viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12.003 2c-2.265 0-6.29 1.364-6.29 7.325v1.195S3.55 14.96 3.55 17.474c0 .665.17 1.025.281 1.025.114 0 .902-.484 1.748-2.072 0 0-.18 2.197 1.904 3.967 0 0-1.77.495-1.77 1.182 0 .686 1.865 1.152 4.063 1.152 2.197 0 4.062-.466 4.062-1.152 0-.687-1.77-1.182-1.77-1.182 2.085-1.77 1.905-3.967 1.905-3.967.845 1.588 1.634 2.072 1.746 2.072.111 0 .283-.36.283-1.025 0-2.514-2.166-6.954-2.166-6.954V9.325C18.29 3.364 14.268 2 12.003 2z"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
          <div className="footer-right-column">
            <div className="footer-mini-stats">
              <span className="mini-stat"><strong>5,280+</strong> 活跃用户</span>
              <span className="mini-stat-sep">·</span>
              <span className="mini-stat"><strong>3,560+</strong> 在售商品</span>
              <span className="mini-stat-sep">·</span>
              <span className="mini-stat"><strong>2,180+</strong> 成功交易</span>
            </div>
            <div className="footer-nav-compact">
              <div className="nav-compact-group">
                <span className="nav-compact-heading">平台</span>
                <a href="/products" className="nav-compact-link">
                  <span className="link-dot"></span>
                  <span className="link-text">浏览商品</span>
                </a>
                <a href="/publish" className="nav-compact-link">
                  <span className="link-dot"></span>
                  <span className="link-text">发布商品</span>
                </a>
              </div>

              <div className="nav-compact-group">
                <span className="nav-compact-heading">帮助</span>
                <button type="button" className="nav-compact-link">
                  <span className="link-dot"></span>
                  <span className="link-text">使用指南</span>
                </button>
                <button type="button" className="nav-compact-link">
                  <span className="link-dot"></span>
                  <span className="link-text">交易安全</span>
                </button>
              </div>

              <div className="nav-compact-group">
                <span className="nav-compact-heading">关于</span>
                <button type="button" className="nav-compact-link">
                  <span className="link-dot"></span>
                  <span className="link-text">关于我们</span>
                </button>
                <button type="button" className="nav-compact-link">
                  <span className="link-dot"></span>
                  <span className="link-text">用户协议</span>
                </button>
              </div>
            </div>
          </div>
        </div>
        <div className="footer-bottom-bar">
          <div className="bottom-glow-line"></div>
          <p className="copyright-text">
            <span className="heart-icon">💛</span>
            Made with care for campus life
          </p>
          <p className="copyright-info">© 2025-2026 EasyOrange · 易橙坊</p>
        </div>
      </div>
    </footer>
  )
}
