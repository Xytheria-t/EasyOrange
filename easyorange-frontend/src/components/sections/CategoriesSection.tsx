export default function CategoriesSection() {
  const categories = [
    {
      id: 1,
      name: '图书教材',
      desc: '专业课本 · 考研资料 · 课外读物',
      count: '2,580+',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
          <line x1="8" y1="6" x2="16" y2="6" />
          <line x1="8" y1="10" x2="14" y2="10" />
        </svg>
      ),
      gradient: 'gradient-1',
      dataCategory: 'books',
    },
    {
      id: 2,
      name: '电子产品',
      desc: '手机电脑 · 数码配件 · 智能设备',
      count: '1,890+',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
          <line x1="8" y1="21" x2="16" y2="21" />
          <line x1="12" y1="17" x2="12" y2="21" />
        </svg>
      ),
      gradient: 'gradient-2',
      dataCategory: 'electronics',
    },
    {
      id: 3,
      name: '服装鞋包',
      desc: '潮流服饰 · 品牌鞋履 · 箱包配饰',
      count: '3,240+',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M20.38 3.46L16 2a4 4 0 01-8 0L3.62 3.46a2 2 0 00-1.34 2.23l.58 3.47a1 1 0 00.99.84H6v10c0 1.1.9 2 2 2h8a2 2 0 002-2V10h2.15a1 1 0 00.99-.84l.58-3.47a2 2 0 00-1.34-2.23z" />
        </svg>
      ),
      gradient: 'gradient-3',
      dataCategory: 'fashion',
    },
    {
      id: 4,
      name: '生活用品',
      desc: '家居日用 · 运动健身 · 美妆护肤',
      count: '4,120+',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
          <polyline points="9 22 9 12 15 12 15 22" />
        </svg>
      ),
      gradient: 'gradient-4',
      dataCategory: 'life',
    },
    {
      id: 5,
      name: '交通工具',
      desc: '自行车 · 电动车 · 滑板轮滑',
      count: '860+',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <circle cx="5.5" cy="17.5" r="2.5" />
          <circle cx="18.5" cy="17.5" r="2.5" />
          <path d="M15 6a1 1 0 100-2 1 1 0 000 2zm-3 11.5V14l-3-3 4-3 2 3h3" />
        </svg>
      ),
      gradient: 'gradient-5',
      dataCategory: 'transport',
    },
    {
      id: 6,
      name: '运动户外',
      desc: '健身器材 · 户外装备 · 球类运动',
      count: '1,450+',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <circle cx="12" cy="12" r="10" />
          <path d="M12 2a15.3 15.3 0 014 10 15.3 15.3 0 01-4 10 15.3 15.3 0 01-4-10 15.3 15.3 0 014-10z" />
          <line x1="2" y1="12" x2="22" y2="12" />
        </svg>
      ),
      gradient: 'gradient-6',
      dataCategory: 'sports',
    },
    {
      id: 7,
      name: '美妆护肤',
      desc: '护肤彩妆 · 香水精油 · 美容仪器',
      count: '980+',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M12 2L2 7l10 5 10-5-10-5z" />
          <path d="M2 17l10 5 10-5" />
          <path d="M2 12l10 5 10-5" />
        </svg>
      ),
      gradient: 'gradient-7',
      dataCategory: 'beauty',
    },
    {
      id: 0,
      name: '更多分类',
      desc: '探索全部分类',
      count: '查看全部 →',
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <rect x="3" y="3" width="7" height="7" />
          <rect x="14" y="3" width="7" height="7" />
          <rect x="14" y="14" width="7" height="7" />
          <rect x="3" y="14" width="7" height="7" />
        </svg>
      ),
      gradient: 'gradient-8',
      dataCategory: 'all',
      isAll: true,
    },
  ]

  return (
    <section className="categories-section" id="categories">
      <div className="container">
        <div className="section-header">
          <span className="section-tag">探索分类</span>
          <h2 className="section-title">发现你需要的</h2>
          <p className="section-desc">精选热门品类，快速找到心仪好物</p>
        </div>

        <div className="categories-grid">
          {categories.map((category) => (
            <a
              key={category.id}
              href={category.isAll ? '/products' : `/products?category=${category.id}`}
              className={`category-card glass-card${category.isAll ? ' card-more' : ''}`}
              data-category={category.dataCategory}
            >
              <div className="card-bg"></div>
              <div className="card-icon">
                <div className={`icon-wrapper ${category.gradient}`}>
                  {category.icon}
                </div>
              </div>
              <h3 className="card-title">{category.name}</h3>
              <p className="card-desc">{category.desc}</p>
              <span className="card-count">{category.count} 件商品</span>
              <div className="card-arrow">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
              </div>
            </a>
          ))}
        </div>
      </div>
    </section>
  )
}
