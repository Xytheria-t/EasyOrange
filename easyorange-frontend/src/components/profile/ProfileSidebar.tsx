import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Camera, LogOut, Package, Sparkles, ShoppingBag, List, Activity, Shield, Settings } from 'lucide-react'
import type { User } from '@/types'
import { useUIStore } from '@/store/uiStore'
import { userApi } from '@/api/userApi'
import { useQueryClient } from '@tanstack/react-query'
import { errorHandler } from '@/utils/errorHandler'

type TabType = 'overview' | 'activity' | 'security' | 'preferences'

interface NavItem {
  id: TabType
  label: string
  icon: React.ComponentType<{ size?: number; className?: string }>
}

interface ProfileSidebarProps {
  user: User | undefined
  activeTab: TabType
  onTabChange: (tab: TabType) => void
  onLogout: () => void
  animateIn: boolean
}

const navItems: NavItem[] = [
  { id: 'overview', label: '总览', icon: Sparkles },
  { id: 'activity', label: '动态', icon: Activity },
  { id: 'security', label: '安全', icon: Shield },
  { id: 'preferences', label: '偏好', icon: Settings },
]

export function ProfileSidebar({ user, activeTab, onTabChange, onLogout, animateIn }: ProfileSidebarProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const addToast = useUIStore((s) => s.addToast)
  const [avatarHover, setAvatarHover] = useState(false)

  const handleAvatarClick = () => {
    fileInputRef.current?.click()
  }

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) {return}
    if (file.size > 5 * 1024 * 1024) {
      addToast({ type: 'warning', message: '头像文件不能超过 5MB' })
      return
    }
    try {
      await userApi.uploadAvatar(file)
      await queryClient.invalidateQueries({ queryKey: ['auth', 'user'] })
      addToast({ type: 'success', message: '头像更新成功' })
    } catch (err) {
      const msg = errorHandler.handle(err as Error)
      addToast({ type: 'error', message: msg })
    }
  }

  return (
    <aside className={`profile-sidebar ${animateIn ? 'animate-in' : ''}`} data-testid="profile-sidebar">
      {/* ═══════════ USER PROFILE CARD ═══════════ */}
      <div className="user-profile-card">
        <div className="card-glow" />
        <div className="card-grain" />

        {/* Avatar + Name row */}
        <div className="ps-avatar-row">
          <button
            type="button"
            className="ps-avatar-wrapper"
            onClick={handleAvatarClick}
            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { handleAvatarClick() } }}
            onMouseEnter={() => setAvatarHover(true)}
            onMouseLeave={() => setAvatarHover(false)}
            aria-label="更换头像"
          >
            <div className="ps-avatar-ring" />
            {user?.avatar ? (
              <img src={user.avatar} alt="头像" className="ps-avatar-img" />
            ) : (
              <div className="ps-avatar-fallback">
                {user?.username?.charAt(0).toUpperCase() || 'U'}
              </div>
            )}
            <div className={`ps-avatar-overlay ${avatarHover ? 'active' : ''}`}>
              <Camera size={16} />
              <span>更换</span>
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              style={{ display: 'none' }}
              onChange={handleAvatarChange}
            />
          </button>

          <div className="ps-user-info">
            <span className="ps-presence">
              <span className="ps-presence-dot" />
              在线
            </span>
            <h2 className="ps-name">{user?.nickname || user?.realName || user?.username || '用户'}</h2>
            <span className="ps-handle">@{user?.username || 'unknown'}</span>
          </div>
        </div>

        {/* Student ID / Tagline */}
        <p className="ps-tagline">
          {user?.studentId ? `学号 ${user.studentId}` : '校园二手交易达人'}
        </p>

        {/* Badges */}
        <div className="ps-badges">
          <span className="ps-badge">
            <Sparkles size={12} className="ps-badge-icon" />
            认证用户
          </span>
          <span className="ps-badge ps-badge-accent">
            <Sparkles size={12} className="ps-badge-icon" />
            VIP
          </span>
        </div>

        {/* Stats with progress bars */}
        <div className="ps-stats">
          <div className="ps-stat">
            <div className="ps-stat-header">
              <span className="ps-stat-label">信用分</span>
              <span className="ps-stat-value">98</span>
            </div>
            <div className="ps-stat-bar">
              <div className="ps-stat-fill" style={{ width: '82%' }} />
            </div>
          </div>
          <div className="ps-stat">
            <div className="ps-stat-header">
              <span className="ps-stat-label">交易数</span>
              <span className="ps-stat-value">42</span>
            </div>
            <div className="ps-stat-bar">
              <div className="ps-stat-fill ps-stat-fill-rose" style={{ width: '45%' }} />
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="ps-actions">
          <button className="ps-btn-primary" onClick={() => navigate('/publish')}>
            <Package size={16} />
            <span className="ps-btn-text">发布商品</span>
            <span className="ps-btn-hint">让闲置找到新主人</span>
            <svg className="ps-btn-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M5 12h14M12 5l7 7-7 7" />
            </svg>
          </button>

          <div className="ps-btn-grid">
            <button className="ps-btn-secondary" onClick={() => navigate('/my-products')}>
              <List size={18} />
              <span className="ps-btn-secondary-text">我的发布</span>
            </button>
            <button className="ps-btn-secondary" onClick={() => navigate('/orders')}>
              <ShoppingBag size={18} />
              <span className="ps-btn-secondary-text">购买记录</span>
            </button>
          </div>

          <button className="ps-btn-logout" onClick={onLogout} data-testid="btn-profile-logout">
            <LogOut size={16} />
            <span>退出登录</span>
          </button>
        </div>
      </div>

      {/* ═══════════ NAVIGATION ═══════════ */}
      <nav className="ps-nav">
        {navItems.map((item) => {
          const Icon = item.icon
          return (
            <button
              key={item.id}
              className={`ps-nav-item ${activeTab === item.id ? 'active' : ''}`}
              onClick={() => {
                onTabChange(item.id)
                window.scrollTo({ top: 0, behavior: 'smooth' })
              }}
            >
              <span className="ps-nav-icon">
                <Icon size={16} />
              </span>
              <span className="ps-nav-text">{item.label}</span>
              <span className="ps-nav-active-bar" />
            </button>
          )
        })}
      </nav>
    </aside>
  )
}
