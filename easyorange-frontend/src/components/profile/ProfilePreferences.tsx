import { ChevronRight, Trash2 } from 'lucide-react'
import { Switch } from '@/components/ui/switch'

export function ProfilePreferences() {
  return (
    <div className="content-section active">
      <div className="section-header">
        <div className="header-title">
          <h2>偏好设置</h2>
          <p className="header-subtitle">自定义你的使用体验</p>
        </div>
      </div>

      <div className="preferences-grid">
        <div className="pref-card">
          <h3>通知设置</h3>
          <div className="pref-list">
            <div className="pref-item">
              <div className="pref-info">
                <span className="pref-title">订单通知</span>
                <span className="pref-desc">接收订单状态变更提醒</span>
              </div>
              <Switch defaultChecked aria-label="订单通知" />
            </div>
            <div className="pref-item">
              <div className="pref-info">
                <span className="pref-title">消息提醒</span>
                <span className="pref-desc">接收私信和系统消息</span>
              </div>
              <Switch defaultChecked aria-label="消息提醒" />
            </div>
            <div className="pref-item">
              <div className="pref-info">
                <span className="pref-title">营销推送</span>
                <span className="pref-desc">接收优惠活动和推荐</span>
              </div>
              <Switch aria-label="营销推送" />
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
              <Switch defaultChecked aria-label="公开个人资料" />
            </div>
            <div className="pref-item">
              <div className="pref-info">
                <span className="pref-title">显示在线状态</span>
                <span className="pref-desc">其他用户可看到你的在线状态</span>
              </div>
              <Switch defaultChecked aria-label="显示在线状态" />
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
  )
}
