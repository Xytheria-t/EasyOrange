import { Heart, ShoppingBag, Package, Star } from 'lucide-react'

const activities = [
  { title: '发布了商品 iPhone 15 Pro', desc: '售价 ¥6,999', time: '2小时前', icon: Package, color: 'orange' },
  { title: '购买了 机械键盘', desc: '¥299', time: '昨天', icon: ShoppingBag, color: 'blue' },
  { title: '收藏了 AirPods Pro', desc: '¥1,899', time: '3天前', icon: Heart, color: 'rose' },
  { title: '收到了新评价', desc: '认领方给了五星好评', time: '1周前', icon: Star, color: 'yellow' },
]

export function ProfileActivity() {
  return (
    <div className="content-section active">
      <div className="section-header">
        <div className="header-title">
          <h2>最近动态</h2>
          <p className="header-subtitle">追踪你的智能托管足迹</p>
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
  )
}
