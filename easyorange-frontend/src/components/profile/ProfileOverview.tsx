import { useNavigate } from 'react-router-dom'
import {
  User, Mail, Phone, Calendar, Check, Pencil, X,
  GraduationCap, BadgeCheck, Heart, ShoppingBag, MessageSquare, Package,
  TrendingUp, Shield, ChevronRight, Sparkles,
  Award, Eye, Star
} from 'lucide-react'
import type { User as UserType } from '@/types'

type EditableField = 'nickname' | 'email' | 'phone' | 'realName' | 'studentId'

interface ProfileOverviewProps {
  user: UserType | undefined
  favoriteCount: number
  editingField: EditableField | null
  editValue: string
  isSaving: boolean
  onEdit: (field: EditableField, value: string) => void
  onSave: () => void
  onCancel: () => void
  onEditValueChange: (value: string) => void
}

export function ProfileOverview({
  user,
  favoriteCount,
  editingField,
  editValue,
  isSaving,
  onEdit,
  onSave,
  onCancel,
  onEditValueChange,
}: ProfileOverviewProps) {
  const navigate = useNavigate()

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

  const quickActions = [
    { label: '我的收藏', icon: Heart, count: favoriteCount, path: '/favorites', color: 'rose' },
    { label: '我的订单', icon: ShoppingBag, count: 5, path: '/orders', color: 'blue' },
    { label: '我的发布', icon: Package, count: 8, path: '/products?seller=me', color: 'orange' },
    { label: '消息中心', icon: MessageSquare, count: 3, path: '/messages', color: 'green' },
  ]

  return (
    <div className="content-section active">
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
                      onChange={(e) => onEditValueChange(e.target.value)}
                      autoFocus
                      onKeyDown={(e) => { if (e.key === 'Enter') {onSave();} if (e.key === 'Escape') {onCancel()} }}
                    />
                    <button className="profile-action-btn profile-action-save" onClick={onSave} disabled={isSaving}>
                      <Check size={14} />
                    </button>
                    <button className="profile-action-btn profile-action-cancel" onClick={onCancel}>
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
                      onClick={() => onEdit(key, value || '')}
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
                <span style={{ fontSize: 18 }}>🕐</span>
                最后登录
              </div>
              <span className="info-value">刚刚</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
