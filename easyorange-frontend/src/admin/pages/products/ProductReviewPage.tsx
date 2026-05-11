import { useState, useMemo } from 'react';
import { AdminTable, type Column } from '../../components/AdminTable';
import { StatusBadge } from '../../components/StatusBadge';
import { AdminSelect } from '../../components/AdminSelect';
import { useAdminProducts } from '../../hooks/useAdminProducts';
import { ProductDetailDrawer } from './ProductDetailDrawer';
import type { AdminProduct, ProductStatus } from '../../types/admin';

const statusOptions: { value: ProductStatus | ''; label: string }[] = [
  { value: '', label: '全部状态' },
  { value: 0, label: '草稿' },
  { value: 1, label: '上架' },
  { value: 2, label: '已售' },
  { value: 3, label: '下架' },
];

const categoryOptions = [
  { value: '', label: '全部分类' },
  { value: 'electronics', label: '电子产品' },
  { value: 'books', label: '图书教材' },
  { value: 'clothes', label: '服饰鞋包' },
  { value: 'sports', label: '运动户外' },
  { value: 'daily', label: '生活用品' },
  { value: 'transport', label: '交通工具' },
  { value: 'other', label: '其他' },
];

const sortOptions = [
  { value: 'createTime', label: '发布时间' },
  { value: 'viewCount', label: '浏览量' },
  { value: 'price', label: '价格' },
];

export default function ProductReviewPage() {
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [statusFilter, setStatusFilter] = useState<ProductStatus | ''>('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [sortBy, setSortBy] = useState('createTime');
  const [selectedProductId, setSelectedProductId] = useState<string | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const { data, isLoading, refetch } = useAdminProducts({
    page,
    size: pageSize,
    keyword: keyword || undefined,
    status: statusFilter || undefined,
    categoryId: categoryFilter || undefined,
  });

  const products = data?.data?.records ?? [];
  const total = data?.data?.total ?? 0;

  const handleSearch = () => {
    setKeyword(searchInput);
    setPage(1);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') handleSearch();
  };

  const handleViewDetail = (product: AdminProduct) => {
    setSelectedProductId(product.id);
    setDrawerOpen(true);
  };

  const handleDrawerSuccess = () => refetch();

  const formatPrice = (price: number) => `¥${price.toFixed(2)}`;

  const formatTime = (timeStr: string) => {
    const date = new Date(timeStr);
    const diff = Date.now() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    if (minutes < 1) return '刚刚';
    if (minutes < 60) return `${minutes}分钟前`;
    if (hours < 24) return `${hours}小时前`;
    if (days < 7) return `${days}天前`;
    return date.toLocaleDateString('zh-CN');
  };

  const columns: Column<AdminProduct>[] = useMemo(
    () => [
      {
        key: 'images',
        title: '图片',
        render: (images: string[]) => (
          <div style={{
            width: 46, height: 46, borderRadius: 12,
            overflow: 'hidden', flexShrink: 0,
            background: 'linear-gradient(135deg, #FDF6EC, #FDE8D4)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            border: '1px solid rgba(249,115,22,0.08)',
          }}>
            {images && images[0] ? (
              <img src={images[0]} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            ) : (
              <span style={{ fontSize: '1.15rem' }}>📦</span>
            )}
          </div>
        ),
      },
      {
        key: 'name',
        title: '商品名称',
        render: (name: string) => (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <span style={{ fontWeight: 600, color: '#2A2520', fontSize: '0.875rem', maxWidth: 220 }} className="truncate block">{name}</span>
          </div>
        ),
      },
      {
        key: 'price',
        title: '价格',
        render: (price: number) => (
          <span style={{
            fontFamily: "'DM Sans', sans-serif",
            fontWeight: 700,
            background: 'linear-gradient(135deg, #F97316, #EA580C)',
            WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
            fontSize: '0.95rem',
          }}>
            {formatPrice(price)}
          </span>
        ),
      },
      {
        key: 'sellerName',
        title: '卖家',
        render: (sellerName: string) => (
          <span style={{ color: '#6B6460', fontSize: '0.85rem' }}>{sellerName}</span>
        ),
      },
      {
        key: 'status',
        title: '状态',
        render: (status: ProductStatus) => <StatusBadge status={status} type="product" />,
      },
      {
        key: 'createTime',
        title: '发布时间',
        sortable: true,
        render: (time: string) => (
          <span style={{ color: '#9B9590', fontSize: '0.82rem' }}>{formatTime(time)}</span>
        ),
      },
      {
        key: 'actions',
        title: '操作',
        render: (_: unknown, record: AdminProduct) => (
          <button
            onClick={(e) => { e.stopPropagation(); handleViewDetail(record); }}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
              padding: '0.38rem 0.85rem', borderRadius: 10,
              fontSize: '0.81rem', fontWeight: 600,
              color: '#EA580C', background: 'rgba(249,115,22,0.07)',
              border: '1px solid transparent', cursor: 'pointer',
              transition: 'all 0.2s ease', letterSpacing: '0.01em',
              textDecoration: 'none',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(249,115,22,0.14)'; e.currentTarget.style.transform = 'translateX(2px)'; }}
            onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(249,115,22,0.07)'; e.currentTarget.style.transform = 'translateX(0)'; }}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
            审核
          </button>
        ),
      },
    ],
    []
  );

  return (
    <div style={{ position: 'relative', animation: 'pageIn 0.5s ease-out both' }}>
      {/* Background atmosphere */}
      <div
        style={{
          position: 'fixed', inset: 0, zIndex: 0, pointerEvents: 'none',
          background: `
            radial-gradient(ellipse 50% 40% at 12% 10%, rgba(251,113,133,0.03) 0%, transparent 50%),
            radial-gradient(ellipse 42% 50% at 88% 82%, rgba(195,155,211,.03) 0%, transparent 48%),
            linear-gradient(180deg, #FAF8F5 0%, #FDF9F6 40%, #FAF8F5 100%)
          `,
        }}
      />

      {/* Content */}
      <div style={{ position: 'relative', zIndex: 1 }}>

        {/* ===== Header ===== */}
        <header style={{ marginBottom: '1.75rem', animation: 'headerSlide 0.6s ease-out both' }}>
          <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
            <div>
              <h1 style={{
                fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                fontSize: 'clamp(1.5rem, 2.5vw, 1.875rem)', fontWeight: 700,
                color: '#2A2520', letterSpacing: '-0.03em', lineHeight: 1.2,
                display: 'flex', alignItems: 'center', gap: '0.65rem',
              }}>
                <span style={{
                  width: 30, height: 30, borderRadius: 10,
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                  background: 'linear-gradient(135deg, #FB7185, #C39BD3)',
                  color: '#fff', flexShrink: 0,
                }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" />
                    <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
                    <line x1="12" y1="22.08" x2="12" y2="12" />
                  </svg>
                </span>
                商品审核
              </h1>
              <p style={{ fontSize: '0.88rem', color: '#9B9590', marginTop: '0.3rem', paddingLeft: '36px' }}>
                审核平台商品信息，管理上架与下架状态
              </p>
            </div>

            {/* Search */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexShrink: 0 }}>
              <div style={{ position: 'relative', width: 280 }}>
                <input
                  type="text"
                  placeholder="搜索商品名称..."
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  style={{
                    width: '100%', padding: '0.68rem 1rem 0.68rem 2.6rem',
                    border: '1.5px solid #E5E0DB', borderRadius: 14,
                    background: '#fff', fontSize: '0.87rem', color: '#2A2520',
                    outline: 'none', boxShadow: '0 1px 3px rgba(42,37,32,0.04)',
                    transition: 'all 0.2s ease',
                  }}
                  onFocus={(e) => { e.currentTarget.style.borderColor = '#FB7185'; e.currentTarget.style.boxShadow = '0 0 0 3px rgba(251,113,133,0.08), 0 1px 3px rgba(42,37,32,0.04)'; }}
                  onBlur={(e) => { e.currentTarget.style.borderColor = '#E5E0DB'; e.currentTarget.style.boxShadow = '0 1px 3px rgba(42,37,32,0.04)'; }}
                />
                <svg style={{
                  position: 'absolute', left: '0.85rem', top: '50%', transform: 'translateY(-50%)',
                  width: 17, height: 17, color: '#B5AEA8', pointerEvents: 'none',
                }} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
              </div>
              <button
                onClick={handleSearch}
                style={{
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                  padding: '0.68rem 1.2rem', border: 'none', borderRadius: 14,
                  background: 'linear-gradient(135deg, #F97316, #EA580C)',
                  color: '#fff', fontSize: '0.87rem', fontWeight: 600,
                  cursor: 'pointer', transition: 'all 0.2s ease',
                  boxShadow: '0 2px 8px rgba(249,115,22,0.28)',
                  whiteSpace: 'nowrap', letterSpacing: '0.01em',
                }}
                onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-1px)'; e.currentTarget.style.boxShadow = '0 4px 14px rgba(249,115,22,0.38)'; }}
                onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 2px 8px rgba(249,115,22,0.28)'; }}
              >
                搜索
              </button>
            </div>
          </div>
        </header>

        {/* ===== Filter Toolbar ===== */}
        <div style={{
          display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem',
          padding: '1rem 1.25rem',
          background: 'rgba(255,255,255,0.72)', backdropFilter: 'blur(16px)',
          WebkitBackdropFilter: 'blur(16px)',
          border: '1px solid rgba(255,255,255,0.6)', borderRadius: 16,
          marginBottom: '1.25rem', animation: 'toolbarIn 0.5s ease-out 0.08s both',
        }}>
          {/* Status filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.8rem', fontWeight: 500, color: '#9B9590' }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
              状态
            </span>
            <AdminSelect
              options={statusOptions}
              value={statusFilter}
              onChange={(val) => { setStatusFilter(val as ProductStatus | ''); setPage(1); }}
            />
          </div>

          {/* Category filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.8rem', fontWeight: 500, color: '#9B9590' }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82zM7 7h.01"/></svg>
              分类
            </span>
            <AdminSelect
              options={categoryOptions}
              value={categoryFilter}
              onChange={(val) => { setCategoryFilter(val); setPage(1); }}
              minWidth={120}
            />
          </div>

          {/* Divider */}
          <div style={{ width: 1, height: 20, background: '#E5E0DB', flexShrink: 0 }} />

          {/* Sort */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.8rem', fontWeight: 500, color: '#9B9590' }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4"/></svg>
              排序
            </span>
            <AdminSelect
              options={sortOptions}
              value={sortBy}
              onChange={(val) => { setSortBy(val); setPage(1); }}
              minWidth={120}
            />
          </div>

          {/* Spacer + count */}
          <div style={{ flex: 1 }} />
          {total > 0 && (
            <span style={{ fontSize: '0.81rem', color: '#9B9590', fontWeight: 500 }}>
              共 <strong style={{ color: '#2A2520' }}>{total.toLocaleString()}</strong> 件商品
            </span>
          )}
        </div>

        {/* ===== Table with glass card wrapper ===== */}
        <div style={{
          background: 'rgba(255,255,255,0.72)', backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          border: '1px solid rgba(255,255,255,0.65)', borderRadius: 20,
          overflow: 'hidden',
          transition: 'all 0.35s ease',
          animation: 'cardIn 0.5s ease-out 0.15s both',
        }}>
          <AdminTable
            columns={columns}
            data={products}
            rowKey="id"
            loading={isLoading}
            pagination={{
              current: page,
              pageSize,
              total,
              onChange: setPage,
            }}
            onRowClick={handleViewDetail}
            emptyText="暂无商品数据"
          />
        </div>

        {/* Drawer */}
        <ProductDetailDrawer
          open={drawerOpen}
          productId={selectedProductId}
          onClose={() => { setDrawerOpen(false); setSelectedProductId(null); }}
          onSuccess={handleDrawerSuccess}
        />
      </div>

      {/* Keyframe animations */}
      <style>{`
        @keyframes pageIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes headerSlide { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes toolbarIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes cardIn { from { opacity: 0; transform: translateY(16px) scale(0.99); } to { opacity: 1; transform: translateY(0) scale(1); } }
      `}</style>
    </div>
  );
}
