export default function ServicesSection() {
  const services = [
    {
      id: 1,
      title: '实名认证',
      desc: '所有用户均需通过学生认证，确保交易双方身份真实可靠',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          <path d="M9 12l2 2 4-4" />
        </svg>
      ),
      gradient: 'gradient-1',
    },
    {
      id: 2,
      title: '担保交易',
      desc: '平台担保交易，确认收货后卖家才能收到款项',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
      ),
      gradient: 'gradient-2',
    },
    {
      id: 3,
      title: '快速响应',
      desc: '24 小时客服在线，任何问题及时解答处理',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </svg>
      ),
      gradient: 'gradient-3',
    },
    {
      id: 4,
      title: '纠纷调解',
      desc: '出现交易纠纷时，平台提供公正的调解服务',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3" />
        </svg>
      ),
      gradient: 'gradient-4',
    },
    {
      id: 5,
      title: '隐私保护',
      desc: '严格保护用户隐私信息，不会泄露给任何第三方',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
      ),
      gradient: 'gradient-1',
    },
    {
      id: 6,
      title: '校园配送',
      desc: '支持校内面交和配送，方便快捷更安全',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <rect x="1" y="3" width="15" height="13" />
          <polygon points="16 8 20 8 23 11 23 16 16 16 8" />
          <circle cx="5.5" cy="18.5" r="2.5" />
          <circle cx="18.5" cy="18.5" r="2.5" />
        </svg>
      ),
      gradient: 'gradient-2',
    },
  ]

  return (
    <section className="services-section">
      <div className="container">
        <div className="section-header">
          <span className="section-tag">服务保障</span>
          <h2 className="section-title">全方位的服务保障</h2>
          <p className="section-desc">六大核心服务，为您的校园交易保驾护航</p>
        </div>

        <div className="services-grid">
          {services.map((service) => (
            <div
              key={service.id}
              className="service-card glass-card"
            >
              <div className={`service-icon-wrapper ${service.gradient}`}>
                <div className="service-icon">
                  {service.icon}
                </div>
              </div>
              <h3 className="service-title">{service.title}</h3>
              <p className="service-desc">{service.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
