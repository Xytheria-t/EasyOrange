import { useState, useEffect, useRef } from 'react'
import { useLocation, useNavigate, Link } from 'react-router-dom'
import { Search, MessageCircle } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { useUIStore } from '@/store/uiStore'
import { throttle } from '@/utils/function'
import { useAdminGuard } from '@/admin/hooks/useAdminGuard'

export function Header() {
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false)
  const [isScrolled, setIsScrolled] = useState(false)
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
  const userMenuRef = useRef<HTMLDivElement>(null)
  const mobileMenuRef = useRef<HTMLDivElement>(null)
  const location = useLocation()
  const navigate = useNavigate()
  const { token, user, logout } = useAuthStore()
  const addToast = useUIStore((s) => s.addToast)
  const isLoggedIn = !!token
  const { isAdmin } = useAdminGuard()

  const handleLoginClick = () => {
    navigate('/login')
  }

  const handleLogoutClick = () => {
    logout()
    setIsUserMenuOpen(false)
    addToast({ type: 'success', message: '已退出登录' })
    navigate('/')
  }

  useEffect(() => {
    const handleScroll = throttle(() => {
      setIsScrolled(window.scrollY > 20)
    }, 100)
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => {
      handleScroll.cancel()
      window.removeEventListener('scroll', handleScroll)
    }
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
    <header className={`floating-nav ${isScrolled ? 'scrolled' : ''}`} role="banner">
      {/* 环境光晕层 */}
      <div className="floating-nav__glow" aria-hidden="true" />
      <div className="floating-nav__ambient" aria-hidden="true" />

      <div className="floating-nav__inner">
        {/* 品牌 Logo */}
        <Link to="/" className="floating-nav__brand" aria-label="EasyOrange首页">
          <div className="floating-nav__logo">
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
          <span className="floating-nav__brand-name">EasyOrange</span>
        </Link>

        {/* 分隔线 */}
        <div className="floating-nav__divider" aria-hidden="true" />

        {/* 导航链接 */}
        <nav className="floating-nav__links" role="navigation" aria-label="主导航">
          <Link
            to="/"
            className={`floating-nav__link ${location.pathname === '/' ? 'active' : ''}`}
            aria-current={location.pathname === '/' ? 'page' : undefined}
          >
            <span className="floating-nav__link-text">首页</span>
          </Link>
          <Link
            to="/products"
            className={`floating-nav__link ${location.pathname === '/products' ? 'active' : ''}`}
          >
            <span className="floating-nav__link-text">商品</span>
          </Link>
        </nav>

        {/* 右侧操作区 */}
        <div className="floating-nav__actions">
          {/* 搜索按钮 */}
          <button
            className="floating-nav__icon-btn"
            onClick={() => navigate('/search')}
            aria-label="搜索"
          >
            <Search size={19} />
          </button>

          {/* 消息入口（仅登录可见） */}
          {isLoggedIn && (
            <button
              className="floating-nav__icon-btn"
              onClick={() => navigate('/messages')}
              aria-label="消息"
            >
              <MessageCircle size={19} />
            </button>
          )}

          {/* 用户菜单 */}
          <div
            className={`floating-nav__user ${isUserMenuOpen ? 'active' : ''}`}
            ref={userMenuRef}
            style={{ display: isLoggedIn ? 'block' : 'none' }}
          >
            <button
              className="floating-nav__user-btn"
              onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
              aria-expanded={isUserMenuOpen}
              aria-haspopup="true"
              data-testid="btn-user-menu"
            >
              <div className="floating-nav__user-avatar">
                {user?.avatar ? (
                  <img src={user.avatar} alt="头像" className="floating-nav__user-avatar-img" width="32" height="32" />
                ) : (
                  (user?.nickname || user?.username || '用').charAt(0)
                )}
              </div>
              <span className="floating-nav__user-name">
                {user?.nickname || user?.username || '用户'}
              </span>
              <svg
                className="floating-nav__user-dropdown-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>

            <div className="floating-nav__user-menu">
              <Link to="/profile" className="floating-nav__menu-item" onClick={() => setIsUserMenuOpen(false)} data-testid="menu-item-profile">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                <span>个人中心</span>
              </Link>
              <Link to="/favorites" className="floating-nav__menu-item" onClick={() => setIsUserMenuOpen(false)}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                </svg>
                <span>我的收藏</span>
              </Link>
              <Link to="/orders" className="floating-nav__menu-item" onClick={() => setIsUserMenuOpen(false)}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
                  <rect x="8" y="2" width="8" height="4" rx="1" ry="1" />
                </svg>
                <span>我的订单</span>
              </Link>
              {isAdmin && (
                <Link to="/admin" className="floating-nav__menu-item floating-nav__menu-item--admin" onClick={() => setIsUserMenuOpen(false)}>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                    <path d="M9 12l2 2 4-4" />
                  </svg>
                  <span>后台管理</span>
                </Link>
              )}
              <div className="floating-nav__menu-divider" />
              <button className="floating-nav__menu-item floating-nav__menu-item--logout" onClick={handleLogoutClick} data-testid="btn-logout">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                  <polyline points="16 17 21 12 16 7" />
                  <line x1="21" y1="12" x2="9" y2="12" />
                </svg>
                <span>退出登录</span>
              </button>
            </div>
          </div>

          {/* 登录按钮 */}
          {isLoggedIn ? null : (
            <button className="floating-nav__login-btn" onClick={handleLoginClick} data-testid="btn-login">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              <span>登录</span>
            </button>
          )}

          {/* 发布按钮 */}
          <Link to="/publish" className="floating-nav__publish-btn">
            <svg className="plus-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>发布</span>
          </Link>
        </div>
      </div>
    </header>
  )
}
