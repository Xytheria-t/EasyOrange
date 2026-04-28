import { useState, useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

export function Header() {
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false)
  const [isScrolled, setIsScrolled] = useState(false)
  const userMenuRef = useRef<HTMLDivElement>(null)
  const location = useLocation()
  const navigate = useNavigate()
  const { token, logout } = useAuthStore()
  const isLoggedIn = !!token

  const handleLoginClick = () => {
    navigate('/login')
  }

  const handleLogoutClick = () => {
    logout()
    navigate('/')
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
    }
    document.addEventListener('click', handleClickOutside)
    return () => document.removeEventListener('click', handleClickOutside)
  }, [])

  return (
    <header className={`unified-header ${isScrolled ? 'header--scrolled' : ''}`} role="banner">
      <div className="header-container">
        <a href="/" className="brand-logo" aria-label="EasyOrange首页">
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
        </a>

        <nav className="header-nav" role="navigation" aria-label="主导航">
          <a href="/" className={`nav-item${location.pathname === '/' ? ' active' : ''}`} aria-current={location.pathname === '/' ? 'page' : undefined}>首页</a>
          <a href="/products" className={`nav-item${location.pathname === '/products' ? ' active' : ''}`}>商品</a>
        </nav>

        <div className="header-actions">
          <div className={`user-menu ${isUserMenuOpen ? 'active' : ''}`} id="userMenu" ref={userMenuRef} style={{ display: isLoggedIn ? 'flex' : 'none' }}>
            <button className="user-avatar-btn" id="userAvatarBtn" onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}>
              <span className="user-name" id="userName">李明</span>
              <svg className="dropdown-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>
            <div className="user-dropdown" id="userDropdown">
              <a href="/profile" className="dropdown-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                <span>个人中心</span>
              </a>
              <a href="/favorites" className="dropdown-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                </svg>
                <span>我的收藏</span>
              </a>
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

          <a href="/publish" className="publish-btn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>发布</span>
          </a>
        </div>

        <button className="mobile-menu-btn" aria-label="菜单" aria-expanded="false">
          <span className="hamburger"></span>
        </button>
      </div>
    </header>
  )
}
