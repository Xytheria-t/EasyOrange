import { KeyRound, Phone, Mail, Shield } from 'lucide-react'
import type { User } from '@/types'

interface ProfileSecurityProps {
  user: User | undefined
  onEdit: (field: 'phone' | 'email' | 'realName', value: string) => void
  onShowPasswordModal: () => void
}

export function ProfileSecurity({ user, onEdit, onShowPasswordModal }: ProfileSecurityProps) {
  const securityItems = [
    { title: '登录密码', desc: '定期更换密码可保护账号安全', status: '已设置', icon: KeyRound, action: onShowPasswordModal },
    { title: '手机绑定', desc: user?.phone ? `已绑定 ${user.phone}` : '绑定手机可提高安全性', status: user?.phone ? '已绑定' : '未绑定', icon: Phone, action: () => !user?.phone && onEdit('phone', '') },
    { title: '邮箱绑定', desc: user?.email ? `已绑定 ${user.email}` : '绑定邮箱可接收通知', status: user?.email ? '已绑定' : '未绑定', icon: Mail, action: () => !user?.email && onEdit('email', '') },
    { title: '实名认证', desc: user?.realName ? `已认证 ${user.realName}` : '完成实名认证可提升信任度', status: user?.realName ? '已认证' : '未认证', icon: Shield, action: () => !user?.realName && onEdit('realName', '') },
  ]

  return (
    <div className="content-section active">
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
              <button className="btn-outline" onClick={onShowPasswordModal}>
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
  )
}
