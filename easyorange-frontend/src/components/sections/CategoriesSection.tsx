import { useCategories, useScrollReveal } from '@/hooks';

interface CategoryDisplay {
  id: number;
  name: string;
  desc: string;
  count: string;
  icon: React.ReactNode;
  gradient: string;
  isAll?: boolean;
}

const CATEGORY_ICONS: Record<string, { icon: React.ReactNode; gradient: string }> = {
  '电子数码': {
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
        <line x1="8" y1="21" x2="16" y2="21" />
        <line x1="12" y1="17" x2="12" y2="21" />
      </svg>
    ),
    gradient: 'gradient-2',
  },
  '书籍教材': {
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
        <line x1="8" y1="6" x2="16" y2="6" />
        <line x1="8" y1="10" x2="14" y2="10" />
      </svg>
    ),
    gradient: 'gradient-1',
  },
  '服饰鞋包': {
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <path d="M20.38 3.46L16 2a4 4 0 01-8 0L3.62 3.46a2 2 0 00-1.34 2.23l.58 3.47a1 1 0 00.99.84H6v10c0 1.1.9 2 2 2h8a2 2 0 002-2V10h2.15a1 1 0 00.99-.84l.58-3.47a2 2 0 00-1.34-2.23z" />
      </svg>
    ),
    gradient: 'gradient-3',
  },
  '生活用品': {
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
        <polyline points="9 22 9 12 15 12 15 22" />
      </svg>
    ),
    gradient: 'gradient-4',
  },
  '运动健身': {
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <circle cx="12" cy="12" r="10" />
        <path d="M12 2a15.3 15.3 0 014 10 15.3 15.3 0 01-4 10 15.3 15.3 0 01-4-10 15.3 15.3 0 014-10z" />
        <line x1="2" y1="12" x2="22" y2="12" />
      </svg>
    ),
    gradient: 'gradient-6',
  },
  '虚拟物品': {
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <path d="M12 2L2 7l10 5 10-5-10-5z" />
        <path d="M2 17l10 5 10-5" />
        <path d="M2 12l10 5 10-5" />
      </svg>
    ),
    gradient: 'gradient-7',
  },
};

const DEFAULT_CATEGORY_CONFIG = {
  icon: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="3" y="3" width="7" height="7" />
      <rect x="14" y="3" width="7" height="7" />
      <rect x="14" y="14" width="7" height="7" />
      <rect x="3" y="14" width="7" height="7" />
    </svg>
  ),
  gradient: 'gradient-8',
};

const CATEGORY_DESCS: Record<string, string> = {
  '电子数码': '手机电脑 · 数码配件 · 智能设备',
  '书籍教材': '专业课本 · 考研资料 · 课外读物',
  '服饰鞋包': '潮流服饰 · 品牌鞋履 · 箱包配饰',
  '生活用品': '家居日用 · 生活百货 · 厨房用品',
  '运动健身': '健身器材 · 运动装备 · 户外用品',
  '虚拟物品': '游戏账号 · 虚拟货币 · 会员服务',
};

function getCategoryDesc(name: string): string {
  return CATEGORY_DESCS[name] || '精选好物 · 品质保证';
}

export default function CategoriesSection() {
  useScrollReveal();
  const { data: categories } = useCategories();

  const displayCategories: CategoryDisplay[] = categories?.map(cat => {
    const config = CATEGORY_ICONS[cat.name] || DEFAULT_CATEGORY_CONFIG;
    const productCount = cat.productCount ?? 0;
    return {
      id: cat.id,
      name: cat.name,
      desc: getCategoryDesc(cat.name),
      count: productCount > 0 ? `${productCount.toLocaleString()}+` : '暂无商品',
      icon: config.icon,
      gradient: config.gradient,
    };
  }) || [];

  displayCategories.push({
    id: 0,
    name: '更多分类',
    desc: '探索全部分类',
    count: '查看全部 →',
    icon: DEFAULT_CATEGORY_CONFIG.icon,
    gradient: DEFAULT_CATEGORY_CONFIG.gradient,
    isAll: true,
  });

  return (
    <section className="categories-section" id="categories">
      {/* 顶部弥散光晕过渡 */}
      <div className="orb-transition orb-transition-top">
        <div className="transition-orb" style={{
          width: '400px', height: '400px',
          background: 'radial-gradient(circle, rgba(249, 115, 22, 0.12) 0%, transparent 70%)',
          top: '-100px', left: '10%',
          animationDelay: '0s'
        }} />
        <div className="transition-orb" style={{
          width: '300px', height: '300px',
          background: 'radial-gradient(circle, rgba(251, 113, 133, 0.1) 0%, transparent 70%)',
          top: '-80px', right: '15%',
          animationDelay: '-5s'
        }} />
      </div>

      <div className="container">
        <div className="section-header reveal">
          <span className="section-tag">探索分类</span>
          <h2 className="section-title">发现你需要的</h2>
          <p className="section-desc">精选热门品类，快速找到心仪好物</p>
        </div>

        <div className="categories-grid reveal-stagger">
          {displayCategories.map((category) => (
            <a
              key={category.id}
              href={category.isAll ? '/products' : `/products?category=${category.id}`}
              className={`category-card glass-card${category.isAll ? ' card-more' : ''}`}
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
