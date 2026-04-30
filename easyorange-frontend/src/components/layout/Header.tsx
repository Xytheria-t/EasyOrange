import { useState, useEffect, useRef } from 'react'
import { useLocation, useNavigate, Link } from 'react-router-dom'
import { Search } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'

export function Header() {
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false)
  const [isScrolled, setIsScrolled] = useState(false)
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
  const userMenuRef = useRef<HTMLDivElement>(null)
  const mobileMenuRef = useRef<HTMLDivElement>(null)
  const location = useLocation()
  const navigate = useNavigate()
  const { token, user, logout } = useAuthStore()
  const isLoggedIn = !!token

  const handleLoginClick = () => {
    navigate('/login')
  }

  const handleLogoutClick = () => {
    logout()
    navigate('/')
  }

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen(!isMobileMenuOpen)
  }

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20)
    }
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setIsUserMenuOpen(false)
      }
      if (mobileMenuRef.current && !mobileMenuRef.current.contains(e.target as Node)) {
        setIsMobileMenuOpen(false)
      }
    }
    document.addEventListener('click', handleClickOutside)
    return () => document.removeEventListener('click', handleClickOutside)
  }, [])

  useEffect(() => {
    if (isMobileMenuOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => {
      document.body.style.overflow = ''
    }
  }, [isMobileMenuOpen])

  return (
    <header className={`unified-header ${isScrolled ? 'header--scrolled' : ''}`} role="banner">
      <div className="header-container">
        <Link to="/" className="brand-logo" aria-label="EasyOrange首页">
          <div className="logo-icon">
            <svg viewBox="0 0 40 40" fill="none">
              <defs>
                <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#F97316" />
                  <stop offset="40%" stopColor="#FB7185" />
                  <stop offset="100%" stopColor="#C39BD3" />
                </linearGradient>
              </defs>
              <path d="M6 10h11M6 10v20M6 30h11M6 10h7M6 20h9" stroke="url(#logoGradient)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
              <circle cx="30" cy="20" r="8" stroke="url(#logoGradient)" strokeWidth="2.5" />
            </svg>
          </div>
          <span className="brand-name">EasyOrange</span>
        </Link>

        <nav className="header-nav" role="navigation" aria-label="主导航">
          <Link to="/" className={`nav-item${location.pathname === '/' ? ' active' : ''}`} aria-current={location.pathname === '/' ? 'page' : undefined}>首页</Link>
          <Link to="/products" className={`nav-item${location.pathname === '/products' ? ' active' : ''}`}>商品</Link>
        </nav>

        <div className="header-actions">
          <button
            className="btn btn-ghost"
            style={{ padding: '0.4rem', borderRadius: '50%' }}
            onClick={() => navigate('/search')}
            aria-label="搜索"
          >
            <Search size={20} />
          </button>
          <div className={`user-menu ${isUserMenuOpen ? 'active' : ''}`} id="userMenu" ref={userMenuRef} style={{ display: isLoggedIn ? 'flex' : 'none' }}>
            <button className="user-avatar-btn" id="userAvatarBtn" onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}>
              <span className="user-name" id="userName">{user?.username || '用户'}</span>
              <svg className="dropdown-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>
            <div className="user-dropdown" id="userDropdown">
              <Link to="/profile" className="dropdown-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                <span>个人中心</span>
              </Link>
              <Link to="/favorites" className="dropdown-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                </svg>
                <span>我的收藏</span>
              </Link>
              <Link to="/orders" className="dropdown-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
                  <rect x="8" y="2" width="8" height="4" rx="1" ry="1" />
                </svg>
                <span>我的订单</span>
              </Link>
              <div className="dropdown-divider"></div>
              <button className="dropdown-item logout-btn" id="logoutBtn" onClick={handleLogoutClick}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                  <polyline points="16 17 21 12 16 7" />
                  <line x1="21" y1="12" x2="9" y2="12" />
                </svg>
                <span>退出登录</span>
              </button>
            </div>
          </div>

          {isLoggedIn ? null : (
            <button className="login-btn" id="loginBtn" onClick={handleLoginClick}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              <span>登录/注册</span>
            </button>
          )}

          <Link to="/publish" className="publish-btn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>发布</span>
          </Link>
        </div>

        <button className="mobile-menu-btn" aria-label="菜单" aria-expanded={isMobileMenuOpen} onClick={toggleMobileMenu}>
          <span className={`hamburger ${isMobileMenuOpen ? 'hamburger--open' : ''}`}></span>
        </button>
      </div>

      <div className={`mobile-nav-drawer ${isMobileMenuOpen ? 'mobile-nav-drawer--open' : ''}`} ref={mobileMenuRef}>
        <div className="mobile-nav-overlay" onClick={toggleMobileMenu}></div>
        <div className="mobile-nav-content">
          <div className="mobile-nav-header">
            <Link to="/" className="brand-logo" onClick={() => setIsMobileMenuOpen(false)}>
              <div className="logo-icon">
                <svg viewBox="0 0 40 40" fill="none">
                  <defs>
                    <linearGradient id="logoGradientMobile" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#F97316" />
                      <stop offset="40%" stopColor="#FB7185" />
                      <stop offset="100%" stopColor="#C39BD3" />
                    </linearGradient>
                  </defs>
                  <path d="M6 10h11M6 10v20M6 30h11M6 10h7M6 20h9" stroke="url(#logoGradientMobile)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
                  <circle cx="30" cy="20" r="8" stroke="url(#logoGradientMobile)" strokeWidth="2.5" />
                </svg>
              </div>
              <span className="brand-name">EasyOrange</span>
            </Link>
            <button className="mobile-nav-close" onClick={toggleMobileMenu} aria-label="关闭菜单">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
          <nav className="mobile-nav-links">
            <Link to="/" className={`mobile-nav-item ${location.pathname === '/' ? 'active' : ''}`} onClick={() => setIsMobileMenuOpen(false)}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
                <polyline points="9 22 9 12 15 12 15 22" />
              </svg>
              首页
            </Link>
            <Link to="/products" className={`mobile-nav-item ${location.pathname === '/products' ? 'active' : ''}`} onClick={() => setIsMobileMenuOpen(false)}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                <line x1="8" y1="21" x2="16" y2="21" />
                <line x1="12" y1="17" x2="12" y2="21" />
              </svg>
              商品
            </Link>
            <Link to="/favorites" className="mobile-nav-item" onClick={() => setIsMobileMenuOpen(false)}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
              </svg>
              我的收藏
            </Link>
            <Link to="/orders" className="mobile-nav-item" onClick={() => setIsMobileMenuOpen(false)}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M16 4h2a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V6a2 2 0 012-2h2" />
                <rect x="8" y="2" width="8" height="4" rx="1" ry="1" />
              </svg>
              我的订单
            </Link>
            <Link to="/profile" className="mobile-nav-item" onClick={() => setIsMobileMenuOpen(false)}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              个人中心
            </Link>
          </nav>
          <div className="mobile-nav-actions">
            {isLoggedIn ? (
              <button className="mobile-nav-logout" onClick={() => { handleLogoutClick(); setIsMobileMenuOpen(false) }}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
                  <polyline points="16 17 21 12 16 7" />
                  <line x1="21" y1="12" x2="9" y2="12" />
                </svg>
                退出登录
              </button>
            ) : (
              <button className="mobile-nav-login" onClick={() => { handleLoginClick(); setIsMobileMenuOpen(false) }}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                登录 / 注册
              </button>
            )}
            <Link to="/publish" className="mobile-nav-publish" onClick={() => setIsMobileMenuOpen(false)}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              发布商品
            </Link>
          </div>
        </div>
      </div>
    </header>
  )
}
