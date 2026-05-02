import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCurrentUser, useLogout } from '@/hooks'
import { userApi } from '@/api/userApi'
import { favoriteApi } from '@/api/favoriteApi'
import { useQueryClient } from '@tanstack/react-query'
import {
  User, Mail, Phone, Calendar, Camera, KeyRound, LogOut, X, Check, Pencil,
  GraduationCap, BadgeCheck, Heart, ShoppingBag, MessageSquare, Package,
  TrendingUp, Shield, ChevronRight, Sparkles,
  Award, Zap, Eye, Star, Clock, MapPin, Settings, Trash2
} from 'lucide-react'
import { useUIStore } from '@/store/uiStore'
import { errorHandler } from '@/utils/errorHandler'
import '@/styles/main.css'
import '@/styles/profile.css'

type EditableField = 'nickname' | 'email' | 'phone' | 'realName' | 'studentId'
type TabType = 'overview' | 'activity' | 'security' | 'preferences'

export function ProfilePage() {
  const { data: user, isLoading } = useCurrentUser()
  const logoutMutation = useLogout()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const addToast = useUIStore((s) => s.addToast)

  const [activeTab, setActiveTab] = useState<TabType>('overview')
  const [editingField, setEditingField] = useState<EditableField | null>(null)
  const [editValue, setEditValue] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [showPasswordModal, setShowPasswordModal] = useState(false)
  const [passwordForm, setPasswordForm] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const [isChangingPassword, setIsChangingPassword] = useState(false)
  const [avatarHover, setAvatarHover] = useState(false)
  const [animateIn, setAnimateIn] = useState(false)
  const [favoriteCount, setFavoriteCount] = useState(0)

  useEffect(() => {
    const timer = setTimeout(() => setAnimateIn(true), 100)
    return () => clearTimeout(timer)
  }, [])

  useEffect(() => {
    favoriteApi.getCount()
      .then((res) => setFavoriteCount(res.data ?? 0))
      .catch(() => {})
  }, [])

  const handleEdit = (field: EditableField, currentValue: string) => {
    setEditingField(field)
    setEditValue(currentValue || '')
  }

  const handleSave = async () => {
    if (!editingField) return
    setIsSaving(true)
    try {
      const data: Record<string, unknown> = {}
      data[editingField] = editValue
      await userApi.updateProfile(data as { nickname?: string; email?: string; phone?: string; realName?: string; studentId?: string })
      await queryClient.invalidateQueries({ queryKey: ['auth', 'user'] })
      setEditingField(null)
      addToast({ type: 'success', message: '更新成功' })
    } catch (err) {
      const msg = errorHandler.handle(err as Error)
      addToast({ type: 'error', message: msg })
    } finally {
      setIsSaving(false)
    }
  }

  const handleCancelEdit = () => {
    setEditingField(null)
    setEditValue('')
  }

  const handleAvatarClick = () => {
    fileInputRef.current?.click()
  }

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
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

  const handleChangePassword = async () => {
    if (!passwordForm.oldPassword || !passwordForm.newPassword) {
      addToast({ type: 'warning', message: '请填写完整信息' })
      return
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      addToast({ type: 'warning', message: '两次输入的新密码不一致' })
      return
    }
    setIsChangingPassword(true)
    try {
      await userApi.changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      setShowPasswordModal(false)
      setPasswordForm({ oldPassword: '', newPassword: '', confirmPassword: '' })
      addToast({ type: 'success', message: '密码修改成功，请重新登录' })
      logoutMutation.mutateAsync()
      navigate('/login')
    } catch (err) {
      const msg = errorHandler.handle(err as Error)
      addToast({ type: 'error', message: msg })
    } finally {
      setIsChangingPassword(false)
    }
  }

  const handleLogout = async () => {
    try {
      await logoutMutation.mutateAsync()
      navigate('/')
    } catch {
      navigate('/')
    }
  }

  if (isLoading) {
    return (
      <div className="profile-body">
        <div className="profile-main">
          <div className="loading-container">
            <div className="loading-spinner-lg"></div>
            <p className="loading-text">加载中...</p>
          </div>
        </div>
      </div>
    )
  }

  const editableFields: { key: EditableField; label: string; value?: string | null; icon: typeof User }[] = [
    { key: 'nickname', label: '昵称', value: user?.nickname, icon: Sparkles },
    { key: 'realName', label: '真实姓名', value: user?.realName, icon: User },
    { key: 'studentId', label: '学号', value: user?.studentId, icon: GraduationCap },
    { key: 'email', label: '邮箱', value: user?.email, icon: Mail },
    { key: 'phone', label: '手机', value: user?.phone, icon: Phone },
  ]

  const readonlyFields = [
    { key: 'username', label: '用户名', value: user?.username, icon: BadgeCheck },
    { key: 'createTime', label: '注册时间', value: user?.createTime, icon: Calendar },
  ]

  const navItems: { id: TabType; label: string; icon: typeof User }[] = [
    { id: 'overview', label: '总览', icon: Sparkles },
    { id: 'activity', label: '动态', icon: Zap },
    { id: 'security', label: '安全', icon: Shield },
    { id: 'preferences', label: '偏好', icon: Settings },
  ]

  const quickActions = [
    { label: '我的收藏', icon: Heart, count: favoriteCount, path: '/favorites', color: 'rose' },
    { label: '我的订单', icon: ShoppingBag, count: 5, path: '/orders', color: 'blue' },
    { label: '我的发布', icon: Package, count: 8, path: '/products?seller=me', color: 'orange' },
    { label: '消息中心', icon: MessageSquare, count: 3, path: '/messages', color: 'green' },
  ]

  const activities = [
    { title: '发布了商品 iPhone 15 Pro', desc: '售价 ¥6,999', time: '2小时前', icon: Package, color: 'orange' },
    { title: '购买了 机械键盘', desc: '¥299', time: '昨天', icon: ShoppingBag, color: 'blue' },
    { title: '收藏了 AirPods Pro', desc: '¥1,899', time: '3天前', icon: Heart, color: 'rose' },
    { title: '收到了新评价', desc: '买家给了五星好评', time: '1周前', icon: Star, color: 'yellow' },
  ]

  const securityItems = [
    { title: '登录密码', desc: '定期更换密码可保护账号安全', status: '已设置', icon: KeyRound, action: () => setShowPasswordModal(true) },
    { title: '手机绑定', desc: user?.phone ? `已绑定 ${user.phone}` : '绑定手机可提高安全性', status: user?.phone ? '已绑定' : '未绑定', icon: Phone, action: () => !user?.phone && handleEdit('phone', '') },
    { title: '邮箱绑定', desc: user?.email ? `已绑定 ${user.email}` : '绑定邮箱可接收通知', status: user?.email ? '已绑定' : '未绑定', icon: Mail, action: () => !user?.email && handleEdit('email', '') },
    { title: '实名认证', desc: user?.realName ? `已认证 ${user.realName}` : '完成实名认证可提升信任度', status: user?.realName ? '已认证' : '未认证', icon: Shield, action: () => !user?.realName && handleEdit('realName', '') },
  ]

  return (
    <div className="profile-body">
      {/* Ambient Background */}
      <div className="ambient-bg">
        <div className="gradient-orb orb-1"></div>
        <div className="gradient-orb orb-2"></div>
        <div className="gradient-orb orb-3"></div>
        <div className="noise-overlay"></div>
      </div>

      <div className="profile-main">
        <div className="profile-grid">
          {/* Sidebar */}
          <aside className={`profile-sidebar ${animateIn ? 'animate-in' : ''}`}>
            {/* User Profile Card */}
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
                <button className="btn-secondary" onClick={handleLogout}>
                  <LogOut size={18} />
                  退出
                </button>
              </div>
            </div>

            {/* Navigation */}
            <nav className="profile-nav">
              {navItems.map((item) => {
                const Icon = item.icon
                return (
                  <button
                    key={item.id}
                    className={`nav-item ${activeTab === item.id ? 'active' : ''}`}
                    onClick={() => {
                      setActiveTab(item.id)
                      window.scrollTo({ top: 0, behavior: 'smooth' })
                    }}
                  >
                    <span className="nav-icon">
                      <Icon />
                    </span>
                    <span className="nav-text">{item.label}</span>
                    <span className="nav-indicator"></span>
                  </button>
                )
              })}
            </nav>
          </aside>

          {/* Main Content */}
          <main className={`profile-content ${animateIn ? 'animate-in-delayed' : ''}`}>
            {/* Overview Tab */}
            <div className={`content-section ${activeTab === 'overview' ? 'active' : ''}`}>
              {/* Section Header */}
              <div className="section-header">
                <div className="header-title">
                  <h2>数据概览</h2>
                  <p className="header-subtitle">实时追踪你的校园交易数据</p>
                </div>
                <div className="header-actions">
                  <button className="btn-ghost" onClick={() => navigate('/orders')}>
                    <ShoppingBag size={16} />
                    查看订单
                  </button>
                </div>
              </div>

              {/* Metrics Grid */}
              <div className="metrics-grid">
                <div className="metric-card primary">
                  <div className="metric-bg"></div>
                  <div className="metric-content">
                    <div className="metric-top">
                      <div className="metric-icon">
                        <TrendingUp size={18} />
                      </div>
                      <span className="metric-trend up">
                        <TrendingUp size={10} />
                        +23.5%
                      </span>
                    </div>
                    <div className="metric-data">
                      <span className="metric-value">¥12,580</span>
                      <span className="metric-label">本月交易额</span>
                    </div>
                  </div>
                </div>
                <div className="metric-card">
                  <div className="metric-content">
                    <div className="metric-top">
                      <div className="metric-icon blue">
                        <Eye size={18} />
                      </div>
                      <span className="metric-trend up">
                        <TrendingUp size={10} />
                        +15.2%
                      </span>
                    </div>
                    <div className="metric-data">
                      <span className="metric-value">1,234</span>
                      <span className="metric-label">商品浏览</span>
                    </div>
                  </div>
                </div>
                <div className="metric-card">
                  <div className="metric-content">
                    <div className="metric-top">
                      <div className="metric-icon purple">
                        <Heart size={18} />
                      </div>
                      <span className="metric-trend up">
                        <TrendingUp size={10} />
                        +8.7%
                      </span>
                    </div>
                    <div className="metric-data">
                      <span className="metric-value">{favoriteCount}</span>
                      <span className="metric-label">收藏商品</span>
                    </div>
                  </div>
                </div>
                <div className="metric-card">
                  <div className="metric-content">
                    <div className="metric-top">
                      <div className="metric-icon orange">
                        <Star size={18} />
                      </div>
                      <span className="metric-trend flat">
                        持平
                      </span>
                    </div>
                    <div className="metric-data">
                      <span className="metric-value">4.9</span>
                      <span className="metric-label">平均评分</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Quick Actions */}
              <div className="quick-actions">
                <h3 className="section-subtitle">快捷入口</h3>
                <div className="action-cards">
                  {quickActions.map((action) => {
                    const Icon = action.icon
                    return (
                      <button
                        key={action.label}
                        className="action-card"
                        onClick={() => navigate(action.path)}
                      >
                        <div className={`action-icon ${action.color}`}>
                          <Icon size={22} />
                        </div>
                        <div className="action-content">
                          <span className="action-title">{action.label}</span>
                          <span className="action-count">{action.count} 条记录</span>
                        </div>
                        <ChevronRight size={20} className="action-arrow" />
                      </button>
                    )
                  })}
                </div>
              </div>

              {/* Personal Info */}
              <div className="info-cards">
                <div className="info-card-large">
                  <div className="card-header">
                    <h3>个人信息</h3>
                  </div>
                  <div className="info-list">
                    {readonlyFields.map(({ key, label, value, icon: Icon }) => (
                      <div className="info-row" key={key}>
                        <div className="info-label">
                          <Icon size={18} />
                          {label}
                        </div>
                        <span className="info-value">{value || '未设置'}</span>
                      </div>
                    ))}
                    {editableFields.map(({ key, label, value, icon: Icon }) => (
                      <div className="info-row" key={key}>
                        <div className="info-label">
                          <Icon size={18} />
                          {label}
                        </div>
                        {editingField === key ? (
                          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flex: 1, justifyContent: 'flex-end' }}>
                            <input
                              className="form-input"
                              style={{ padding: '0.375rem 0.75rem', fontSize: '0.875rem', maxWidth: 200 }}
                              value={editValue}
                              onChange={(e) => setEditValue(e.target.value)}
                              autoFocus
                              onKeyDown={(e) => { if (e.key === 'Enter') handleSave(); if (e.key === 'Escape') handleCancelEdit() }}
                            />
                            <button className="profile-action-btn profile-action-save" onClick={handleSave} disabled={isSaving}>
                              <Check size={14} />
                            </button>
                            <button className="profile-action-btn profile-action-cancel" onClick={handleCancelEdit}>
                              <X size={14} />
                            </button>
                          </div>
                        ) : (
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <span className="info-value" style={{ color: value ? 'var(--profile-ink)' : 'var(--profile-ink-soft)' }}>
                              {value || '未设置'}
                            </span>
                            <button
                              className="profile-edit-btn"
                              onClick={() => handleEdit(key, value || '')}
                              style={{ opacity: 0.6 }}
                            >
                              <Pencil size={12} />
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>

                <div className="info-card-large">
                  <div className="card-header">
                    <h3>账号状态</h3>
                  </div>
                  <div className="info-list">
                    <div className="info-row">
                      <div className="info-label">
                        <Shield size={18} />
                        账号状态
                      </div>
                      <span className="status-badge active">正常</span>
                    </div>
                    <div className="info-row">
                      <div className="info-label">
                        <Award size={18} />
                        会员等级
                      </div>
                      <span className="info-value">黄金会员</span>
                    </div>
                    <div className="info-row">
                      <div className="info-label">
                        <MapPin size={18} />
                        所在校区
                      </div>
                      <span className="info-value">主校区</span>
                    </div>
                    <div className="info-row">
                      <div className="info-label">
                        <Clock size={18} />
                        最后登录
                      </div>
                      <span className="info-value">刚刚</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Activity Tab */}
            <div className={`content-section ${activeTab === 'activity' ? 'active' : ''}`}>
              <div className="section-header">
                <div className="header-title">
                  <h2>最近动态</h2>
                  <p className="header-subtitle">追踪你的校园交易足迹</p>
                </div>
              </div>

              <div className="activity-section">
                <div className="activity-list">
                  {activities.map((activity, index) => {
                    const Icon = activity.icon
                    return (
                      <div className="activity-item" key={index}>
                        <div className={`activity-icon ${activity.color}`}>
                          <Icon size={20} />
                        </div>
                        <div className="activity-content">
                          <p className="activity-title">{activity.title}</p>
                          <p className="activity-desc">{activity.desc}</p>
                        </div>
                        <span className="activity-time">{activity.time}</span>
                      </div>
                    )
                  })}
                </div>
              </div>
            </div>

            {/* Security Tab */}
            <div className={`content-section ${activeTab === 'security' ? 'active' : ''}`}>
              <div className="section-header">
                <div className="header-title">
                  <h2>安全中心</h2>
                  <p className="header-subtitle">保护你的账号安全</p>
                </div>
              </div>

              <div className="security-dashboard">
                <div className="security-score-card">
                  <div className="score-visual">
                    <svg className="score-ring" viewBox="0 0 120 120">
                      <defs>
                        <linearGradient id="scoreGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                          <stop offset="0%" stopColor="#ff9347" />
                          <stop offset="100%" stopColor="#ef7d23" />
                        </linearGradient>
                      </defs>
                      <circle className="score-bg" cx="60" cy="60" r="54" />
                      <circle
                        className="score-progress"
                        cx="60"
                        cy="60"
                        r="54"
                        strokeDasharray="339.292"
                        strokeDashoffset="50"
                      />
                    </svg>
                    <div className="score-text">
                      <span className="score-number">85</span>
                      <span className="score-label">安全评分</span>
                    </div>
                  </div>
                  <div className="score-details">
                    <h3>账号安全良好</h3>
                    <p>建议绑定手机和邮箱以提升安全等级</p>
                    <div className="score-actions">
                      <button className="btn-outline" onClick={() => setShowPasswordModal(true)}>
                        <KeyRound size={16} />
                        修改密码
                      </button>
                    </div>
                  </div>
                </div>

                <div className="security-settings">
                  {securityItems.map((item, index) => {
                    const Icon = item.icon
                    const isActive = item.status.includes('已')
                    return (
                      <div className="setting-item" key={index}>
                        <div className="setting-info">
                          <div className="setting-icon">
                            <Icon size={20} />
                          </div>
                          <div className="setting-text">
                            <h4>{item.title}</h4>
                            <p>{item.desc}</p>
                          </div>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                          <span className={`status-badge ${isActive ? 'active' : ''}`}>
                            {item.status}
                          </span>
                          <button className="btn-outline" onClick={item.action}>
                            {isActive ? '修改' : '去设置'}
                          </button>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            </div>

            {/* Preferences Tab */}
            <div className={`content-section ${activeTab === 'preferences' ? 'active' : ''}`}>
              <div className="section-header">
                <div className="header-title">
                  <h2>偏好设置</h2>
                  <p className="header-subtitle">自定义你的使用体验</p>
                </div>
              </div>

              <div className="preferences-grid">
                <div className="pref-card">
                  <h3>主题外观</h3>
                  <div className="theme-options">
                    <div className="theme-option active">
                      <div className="theme-preview light">
                        <div className="preview-header"></div>
                        <div className="preview-content">
                          <div className="preview-line"></div>
                          <div className="preview-line short"></div>
                        </div>
                      </div>
                      <span>浅色</span>
                    </div>
                    <div className="theme-option">
                      <div className="theme-preview dark">
                        <div className="preview-header"></div>
                        <div className="preview-content">
                          <div className="preview-line"></div>
                          <div className="preview-line short"></div>
                        </div>
                      </div>
                      <span>深色</span>
                    </div>
                    <div className="theme-option">
                      <div className="theme-preview auto">
                        <div className="preview-header"></div>
                        <div className="preview-content">
                          <div className="preview-line"></div>
                          <div className="preview-line short"></div>
                        </div>
                      </div>
                      <span>自动</span>
                    </div>
                  </div>
                </div>

                <div className="pref-card">
                  <h3>通知设置</h3>
                  <div className="pref-list">
                    <div className="pref-item">
                      <div className="pref-info">
                        <span className="pref-title">订单通知</span>
                        <span className="pref-desc">接收订单状态变更提醒</span>
                      </div>
                      <label className="switch">
                        <input type="checkbox" defaultChecked />
                        <span className="switch-slider"></span>
                      </label>
                    </div>
                    <div className="pref-item">
                      <div className="pref-info">
                        <span className="pref-title">消息提醒</span>
                        <span className="pref-desc">接收私信和系统消息</span>
                      </div>
                      <label className="switch">
                        <input type="checkbox" defaultChecked />
                        <span className="switch-slider"></span>
                      </label>
                    </div>
                    <div className="pref-item">
                      <div className="pref-info">
                        <span className="pref-title">营销推送</span>
                        <span className="pref-desc">接收优惠活动和推荐</span>
                      </div>
                      <label className="switch">
                        <input type="checkbox" />
                        <span className="switch-slider"></span>
                      </label>
                    </div>
                  </div>
                </div>

                <div className="pref-card">
                  <h3>隐私设置</h3>
                  <div className="pref-list">
                    <div className="pref-item">
                      <div className="pref-info">
                        <span className="pref-title">公开个人资料</span>
                        <span className="pref-desc">其他用户可查看你的资料</span>
                      </div>
                      <label className="switch">
                        <input type="checkbox" defaultChecked />
                        <span className="switch-slider"></span>
                      </label>
                    </div>
                    <div className="pref-item">
                      <div className="pref-info">
                        <span className="pref-title">显示在线状态</span>
                        <span className="pref-desc">其他用户可看到你的在线状态</span>
                      </div>
                      <label className="switch">
                        <input type="checkbox" defaultChecked />
                        <span className="switch-slider"></span>
                      </label>
                    </div>
                  </div>
                </div>

                <div className="pref-card">
                  <h3>危险操作</h3>
                  <div className="pref-list">
                    <div className="pref-item" style={{ cursor: 'pointer' }}>
                      <div className="pref-info">
                        <span className="pref-title" style={{ color: 'var(--error)' }}>
                          <Trash2 size={16} style={{ display: 'inline', marginRight: 6, verticalAlign: 'middle' }} />
                          注销账号
                        </span>
                        <span className="pref-desc">永久删除账号及所有数据</span>
                      </div>
                      <ChevronRight size={20} color="var(--text-tertiary)" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </main>
        </div>
      </div>

      {/* Password Modal */}
      {showPasswordModal && (
        <div className="modal-overlay active" onClick={() => setShowPasswordModal(false)}>
          <div className="modal modal-content-large" style={{ opacity: 1, visibility: 'visible', pointerEvents: 'auto', transform: 'translate(-50%, -50%) scale(1)' }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>修改密码</h3>
              <button className="modal-close" onClick={() => setShowPasswordModal(false)}>
                <X size={18} />
              </button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">旧密码</label>
                <input
                  className="form-input"
                  type="password"
                  value={passwordForm.oldPassword}
                  onChange={(e) => setPasswordForm((p) => ({ ...p, oldPassword: e.target.value }))}
                  placeholder="请输入旧密码"
                />
              </div>
              <div className="form-group">
                <label className="form-label">新密码</label>
                <input
                  className="form-input"
                  type="password"
                  value={passwordForm.newPassword}
                  onChange={(e) => setPasswordForm((p) => ({ ...p, newPassword: e.target.value }))}
                  placeholder="需包含大小写字母和数字，6-20位"
                />
              </div>
              <div className="form-group">
                <label className="form-label">确认新密码</label>
                <input
                  className="form-input"
                  type="password"
                  value={passwordForm.confirmPassword}
                  onChange={(e) => setPasswordForm((p) => ({ ...p, confirmPassword: e.target.value }))}
                  placeholder="再次输入新密码"
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary btn-md" onClick={() => setShowPasswordModal(false)}>取消</button>
              <button className="btn btn-primary btn-md" onClick={handleChangePassword} disabled={isChangingPassword}>
                {isChangingPassword ? '修改中...' : '确认修改'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
