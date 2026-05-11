import { useState, useCallback } from 'react';
import { AdminTable, type Column } from '../../components/AdminTable';
import { StatusBadge } from '../../components/StatusBadge';
import { AdminSelect } from '../../components/AdminSelect';

const statusFilterOptions = [
  { value: '', label: '全部状态' },
  { value: '0', label: '待付款' },
  { value: '1', label: '待发货' },
  { value: '2', label: '已发货' },
  { value: '3', label: '已完成' },
  { value: '4', label: '已取消' },
  { value: '5', label: '退款中' },
];

interface MockOrder {
  id: string;
  orderNo: string;
  buyerName: string;
  sellerName: string;
  productName: string;
  amount: number;
  status: number;
  createTime: string;
}

const MOCK_ORDERS: MockOrder[] = [
  { id: '1', orderNo: 'EO20260510001', buyerName: '张同学', sellerName: '李学姐', productName: '高等数学教材（第七版）', amount: 25.00, status: 1, createTime: '2026-05-10 14:30:00' },
  { id: '2', orderNo: 'EO20260510002', buyerName: '王同学', sellerName: '赵同学', productName: '小米台灯 Pro', amount: 68.00, status: 2, createTime: '2026-05-10 12:15:00' },
  { id: '3', orderNo: 'EO20260509003', buyerName: '陈同学', sellerName: '刘学长', productName: 'iPad Air 4 64G', amount: 2800.00, status: 3, createTime: '2026-05-09 20:45:00' },
  { id: '4', orderNo: 'EO20260509004', buyerName: '周同学', sellerName: '吴同学', productName: '自行车 捷安特', amount: 350.00, status: 0, createTime: '2026-05-09 16:20:00' },
  { id: '5', orderNo: 'EO20260508005', buyerName: '孙同学', sellerName: '郑学姐', productName: '考研英语真题集', amount: 15.00, status: 5, createTime: '2026-05-08 09:10:00' },
  { id: '6', orderNo: 'EO20260508006', buyerName: '黄同学', sellerName: '林同学', productName: '罗技鼠标 G304', amount: 45.00, status: 4, createTime: '2026-05-08 08:30:00' },
  { id: '7', orderNo: 'EO20260507007', buyerName: '何同学', sellerName: '马学长', productName: '显示器支架', amount: 35.00, status: 3, createTime: '2026-05-07 22:15:00' },
  { id: '8', orderNo: 'EO20260507008', buyerName: '杨同学', sellerName: '徐同学', productName: 'C语言程序设计', amount: 12.00, status: 1, createTime: '2026-05-07 18:40:00' },
];

export default function OrderManagePage() {
  const [statusFilter, setStatusFilter] = useState('');
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [page, setPage] = useState(1);
  const pageSize = 10;

  const handleSearch = useCallback(() => {
    setKeyword(searchInput);
    setPage(1);
  }, [searchInput]);

  const handleKeyPress = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch();
  }, [handleSearch]);

  const filteredOrders = MOCK_ORDERS.filter((o) => {
    if (statusFilter && o.status !== Number(statusFilter)) return false;
    if (keyword && !o.orderNo.toLowerCase().includes(keyword.toLowerCase()) && !o.productName.includes(keyword) && !o.buyerName.includes(keyword)) return false;
    return true;
  });

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });

  const columns: Column<MockOrder>[] = [
    {
      key: 'orderNo',
      title: '订单号',
      render: (value) => (
        <span style={{ fontFamily: "'DM Sans', monospace", fontSize: '0.84rem', fontWeight: 600, color: '#2A2520', letterSpacing: '0.02em' }}>
          {value as string}
        </span>
      ),
    },
    {
      key: 'productName',
      title: '商品',
      render: (value) => (
        <span style={{ fontWeight: 500, color: '#4A4540', fontSize: '0.87rem', maxWidth: 180, display: 'inline-block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {value as string}
        </span>
      ),
    },
    {
      key: 'buyerName',
      title: '买家',
      render: (value) => (
        <span style={{ fontSize: '0.86rem', color: '#6B6460' }}>{value as string}</span>
      ),
    },
    {
      key: 'sellerName',
      title: '卖家',
      render: (value) => (
        <span style={{ fontSize: '0.86rem', color: '#6B6460' }}>{value as string}</span>
      ),
    },
    {
      key: 'amount',
      title: '金额',
      render: (value) => (
        <span style={{
          fontFamily: "'DM Sans', sans-serif", fontWeight: 700, fontSize: '0.9rem',
          background: 'linear-gradient(135deg, #F97316, #EA580C)',
          WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text',
        }}>
          ¥{(value as number).toFixed(2)}
        </span>
      ),
    },
    {
      key: 'status',
      title: '状态',
      render: (value) => <StatusBadge status={value as number} type="order" />,
    },
    {
      key: 'createTime',
      title: '下单时间',
      sortable: true,
      render: (value) => (
        <span style={{ color: '#9B9590', fontSize: '0.84rem' }}>{formatDate(value as string)}</span>
      ),
    },
    {
      key: 'actions',
      title: '操作',
      render: (_) => (
        <button
          style={{
            display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
            padding: '0.38rem 0.85rem', borderRadius: 10,
            fontSize: '0.81rem', fontWeight: 600,
            color: '#EA580C', background: 'rgba(249,115,22,0.07)',
            border: '1px solid transparent', cursor: 'pointer',
            transition: 'all 0.2s ease',
          }}
          onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(249,115,22,0.14)'; e.currentTarget.style.transform = 'translateX(2px)'; }}
          onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(249,115,22,0.07)'; e.currentTarget.style.transform = 'translateX(0)'; }}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          </svg>
          详情
        </button>
      ),
    },
  ];

  return (
    <div style={{ position: 'relative', animation: 'pageIn 0.5s ease-out both' }}>
      {/* Background atmosphere */}
      <div style={{
        position: 'fixed', inset: 0, zIndex: 0, pointerEvents: 'none',
        background: `
          radial-gradient(ellipse 55% 35% at 8% 12%, rgba(249,115,22,0.035) 0%, transparent 50%),
          radial-gradient(ellipse 40% 45% at 92% 85%, rgba(195,155,211,0.03) 0%, transparent 48%),
          linear-gradient(180deg, #FAF8F5 0%, #FDF9F6 40%, #FAF8F5 100%)
        `,
      }} />

      <div style={{ position: 'relative', zIndex: 1 }}>
        {/* Header */}
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
                  background: 'linear-gradient(135deg, #FBBF24, #F97316)',
                  color: '#fff', flexShrink: 0,
                }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                    <line x1="16" y1="13" x2="8" y2="13" />
                    <line x1="16" y1="17" x2="8" y2="17" />
                  </svg>
                </span>
                订单管理
              </h1>
              <p style={{ fontSize: '0.88rem', color: '#9B9590', marginTop: '0.3rem', paddingLeft: '36px' }}>
                查看和管理平台所有交易订单
              </p>
            </div>

            {/* Search */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexShrink: 0 }}>
              <div style={{ position: 'relative', width: 280 }}>
                <input
                  type="text"
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  onKeyPress={handleKeyPress}
                  placeholder="搜索订单号/商品/买家..."
                  style={{
                    width: '100%', padding: '0.68rem 1rem 0.68rem 2.6rem',
                    border: '1.5px solid #E5E0DB', borderRadius: 14,
                    background: '#fff', fontSize: '0.87rem', color: '#2A2520',
                    outline: 'none', boxShadow: '0 1px 3px rgba(42,37,32,0.04)',
                    transition: 'all 0.2s ease',
                  }}
                  onFocus={(e) => { e.currentTarget.style.borderColor = '#F97316'; e.currentTarget.style.boxShadow = '0 0 0 3px rgba(249,115,22,0.08), 0 1px 3px rgba(42,37,32,0.04)'; }}
                  onBlur={(e) => { e.currentTarget.style.borderColor = '#E5E0DB'; e.currentTarget.style.boxShadow = '0 1px 3px rgba(42,37,32,0.04)'; }}
                />
                <svg style={{ position: 'absolute', left: '0.85rem', top: '50%', transform: 'translateY(-50%)', width: 17, height: 17, color: '#B5AEA8', pointerEvents: 'none' }} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
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
                  whiteSpace: 'nowrap',
                }}
                onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-1px)'; e.currentTarget.style.boxShadow = '0 4px 14px rgba(249,115,22,0.38)'; }}
                onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 2px 8px rgba(249,115,22,0.28)'; }}
              >
                搜索
              </button>
            </div>
          </div>
        </header>

        {/* Filter toolbar */}
        <div style={{
          display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem',
          padding: '1rem 1.25rem',
          background: 'rgba(255,255,255,0.72)', backdropFilter: 'blur(16px)',
          WebkitBackdropFilter: 'blur(16px)',
          border: '1px solid rgba(255,255,255,0.6)', borderRadius: 16,
          marginBottom: '1.25rem', animation: 'toolbarIn 0.5s ease-out 0.08s both',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.8rem', fontWeight: 500, color: '#9B9590' }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" /></svg>
              状态
            </span>
            <AdminSelect
              options={statusFilterOptions}
              value={statusFilter}
              onChange={(val) => { setStatusFilter(val); setPage(1); }}
            />
          </div>
          <div style={{ flex: 1 }} />
          <span style={{ fontSize: '0.81rem', color: '#9B9590', fontWeight: 500 }}>
            共 <strong style={{ color: '#2A2520' }}>{filteredOrders.length}</strong> 笔订单
          </span>
        </div>

        {/* Table */}
        <div style={{
          background: 'rgba(255,255,255,0.72)', backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          border: '1px solid rgba(255,255,255,0.65)', borderRadius: 20,
          overflow: 'hidden', animation: 'cardIn 0.5s ease-out 0.15s both',
        }}>
          <AdminTable
            columns={columns}
            data={filteredOrders}
            rowKey="id"
            loading={false}
            pagination={
              filteredOrders.length > pageSize
                ? { current: page, pageSize, total: filteredOrders.length, onChange: setPage }
                : undefined
            }
            emptyText="暂无订单数据"
          />
        </div>
      </div>

      <style>{`
        @keyframes pageIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes headerSlide { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes toolbarIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes cardIn { from { opacity: 0; transform: translateY(16px) scale(0.99); } to { opacity: 1; transform: translateY(0) scale(1); } }
      `}</style>
    </div>
  );
}
