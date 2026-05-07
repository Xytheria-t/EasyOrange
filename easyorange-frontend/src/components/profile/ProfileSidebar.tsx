import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Camera, LogOut, Package, Award, Sparkles } from 'lucide-react'
import type { User } from '@/types'
import { useUIStore } from '@/store/uiStore'
import { userApi } from '@/api/userApi'
import { useQueryClient } from '@tanstack/react-query'
import { errorHandler } from '@/utils/errorHandler'

type TabType = 'overview' | 'activity' | 'security' | 'preferences'

interface NavItem {
  id: TabType
  label: string
  icon: React.ComponentType<{ size?: number }>
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
  { id: 'activity', label: '动态', icon: ({ size }) => <span style={{ fontSize: size }}>⚡</span> },
  { id: 'security', label: '安全', icon: ({ size }) => <span style={{ fontSize: size }}>🛡️</span> },
  { id: 'preferences', label: '偏好', icon: ({ size }) => <span style={{ fontSize: size }}>⚙️</span> },
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
      <div className="user-profile-card">
        <div className="card-glow"></div>
        <div className="card-grain"></div>

        <div className="profile-header">
          <div className="profile-header-top">
            <span className="profile-kicker">Personal Center</span>
            <div className="profile-presence">
              <span className="presence-dot"></span>
              <span>在线</span>
            </div>
          </div>

          <div
            className="profile-avatar-wrapper"
            onClick={handleAvatarClick}
            onMouseEnter={() => setAvatarHover(true)}
            onMouseLeave={() => setAvatarHover(false)}
          >
            {user?.avatar ? (
              <img src={user.avatar} alt="头像" className="profile-avatar-img" />
            ) : (
              <div className="profile-avatar-fallback">
                {user?.username?.charAt(0).toUpperCase() || 'U'}
              </div>
            )}
            <div className={`profile-avatar-overlay ${avatarHover ? 'active' : ''}`}>
              <Camera size={20} />
              <span>更换头像</span>
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              style={{ display: 'none' }}
              onChange={handleAvatarChange}
            />
          </div>

          <div className="profile-info">
            <p className="profile-eyebrow">Welcome back</p>
            <h2 className="profile-name">{user?.nickname || user?.realName || user?.username || '用户'}</h2>
            <p className="profile-handle">@{user?.username || 'unknown'}</p>
            <p className="profile-intro">
              {user?.studentId ? `学号: ${user.studentId}` : '校园二手交易达人'}
            </p>
          </div>

          <div className="profile-badges">
            <span className="profile-badge">
              <Award size={14} className="badge-icon" />
              认证用户
            </span>
            <span className="profile-badge subtle">
              <Sparkles size={14} className="badge-icon" />
              VIP
            </span>
          </div>

          <div className="profile-presence-grid">
            <div className="presence-card">
              <span className="presence-label">信用分</span>
              <span className="presence-value">98</span>
            </div>
            <div className="presence-card">
              <span className="presence-label">交易数</span>
              <span className="presence-value">42</span>
            </div>
          </div>
        </div>

        <div className="profile-actions">
          <button className="btn-primary" onClick={() => navigate('/publish')}>
            <Package size={16} />
            发布商品
          </button>
          <button className="btn-secondary" onClick={onLogout} data-testid="btn-profile-logout">
            <LogOut size={18} />
            退出
          </button>
        </div>
      </div>

      <nav className="profile-nav">
        {navItems.map((item) => {
          const Icon = item.icon
          return (
            <button
              key={item.id}
              className={`nav-item ${activeTab === item.id ? 'active' : ''}`}
              onClick={() => {
                onTabChange(item.id)
                window.scrollTo({ top: 0, behavior: 'smooth' })
              }}
            >
              <span className="nav-icon">
                <Icon size={18} />
              </span>
              <span className="nav-text">{item.label}</span>
              <span className="nav-indicator"></span>
            </button>
          )
        })}
      </nav>
    </aside>
  )
}
