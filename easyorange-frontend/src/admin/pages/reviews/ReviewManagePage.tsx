import { useState, useCallback } from 'react';
import { AdminTable, type Column } from '../../components/AdminTable';
import { StatusBadge } from '../../components/StatusBadge';
import { AdminSelect } from '../../components/AdminSelect';
import { ConfirmModal } from '../../components/ConfirmModal';
import { useAdminReviews, useDeleteReview } from '../../hooks';
import type { AdminReview } from '../../types/admin';

const STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: '1', label: '正常' },
  { value: '0', label: '隐藏' },
];

const RATING_OPTIONS = [
  { value: '', label: '全部评分' },
  { value: '5', label: '5星' },
  { value: '4', label: '4星' },
  { value: '3', label: '3星' },
  { value: '2', label: '2星' },
  { value: '1', label: '1星' },
];

const RATING_LABELS: Record<number, { emoji: string; label: string; color: string }> = {
  5: { emoji: '⭐⭐⭐⭐⭐', label: '非常好', color: '#059669' },
  4: { emoji: '⭐⭐⭐⭐', label: '好', color: '#65A30D' },
  3: { emoji: '⭐⭐⭐', label: '一般', color: '#D97706' },
  2: { emoji: '⭐⭐', label: '差', color: '#EA580C' },
  1: { emoji: '⭐', label: '非常差', color: '#DC2626' },
};

export default function ReviewManagePage() {
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState('');
  const [ratingFilter, setRatingFilter] = useState('');
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [deleteModal, setDeleteModal] = useState<{ open: boolean; reviewId: string; reviewContent: string }>({
    open: false, reviewId: '', reviewContent: '',
  });
  const [deleteReason, setDeleteReason] = useState('');

  const { data, isLoading, isError } = useAdminReviews({
    pageNum: page,
    pageSize,
    status: statusFilter ? Number(statusFilter) : undefined,
    rating: ratingFilter ? Number(ratingFilter) : undefined,
    keyword: keyword || undefined,
  });

  const deleteReview = useDeleteReview();

  const handleSearch = useCallback(() => {
    setKeyword(searchInput);
    setPage(1);
  }, [searchInput]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') { handleSearch(); }
  }, [handleSearch]);

  const confirmDelete = (review: AdminReview) => {
    setDeleteModal({ open: true, reviewId: review.reviewId, reviewContent: review.content });
    setDeleteReason('');
  };

  const handleDeleteConfirm = async () => {
    if (!deleteReason.trim()) return;
    try {
      await deleteReview.mutateAsync({ id: deleteModal.reviewId, data: { reason: deleteReason } });
      setDeleteModal({ open: false, reviewId: '', reviewContent: '' });
      setDeleteReason('');
    } catch {
      setDeleteModal(prev => ({ ...prev, open: false }));
    }
  };

  const formatDate = (dateString: string) =>
    new Date(dateString).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });

  const columns: Column<AdminReview>[] = [
    {
      key: 'content',
      title: '评价内容',
      render: (value) => {
        const content = value as string;
        return (
          <span style={{
            color: '#2A2520', fontSize: '0.84rem', lineHeight: 1.5,
            display: '-webkit-inline-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical',
            overflow: 'hidden', maxWidth: 300,
          }}>
            {content}
          </span>
        );
      },
    },
    {
      key: 'rating',
      title: '评分',
      render: (value) => {
        const rating = value as number;
        const ratingLabel = RATING_LABELS[rating] ?? { emoji: '⭐', label: '未知', color: '#9B9590' };
        return <span style={{ fontSize: '0.82rem', fontWeight: 600, color: ratingLabel.color }}>{ratingLabel.emoji}</span>;
      },
    },
    {
      key: 'productName',
      title: '商品',
      render: (value) => {
        const name = value as string | undefined;
        return <span style={{ color: '#6B6460', fontSize: '0.84rem', maxWidth: 160 }} className="truncate block">{name || '未知商品'}</span>;
      },
    },
    {
      key: 'username',
      title: '评价人',
      render: (value) => {
        const username = value as string | undefined;
        return <span style={{ fontSize: '0.84rem', color: '#6B6460' }}>{username || '未知用户'}</span>;
      },
    },
    {
      key: 'likes',
      title: '点赞',
      render: (value) => {
        const likes = value as number;
        return <span style={{ color: '#9B9590', fontSize: '0.82rem' }}>{likes ?? 0}</span>;
      },
    },
    {
      key: 'status',
      title: '状态',
      render: (_value, record) => <StatusBadge status={record.status === 1 ? '正常' : '隐藏'} type="product" />,
    },
    {
      key: 'createTime',
      title: '评价时间',
      render: (value) => {
        const timeString = value as string | undefined;
        return <span style={{ color: '#9B9590', fontSize: '0.82rem' }}>{timeString ? formatDate(timeString) : '-'}</span>;
      },
    },
    {
      key: 'actions',
      title: '操作',
      render: (_value, record) => (
        <button
          onClick={(e) => { e.stopPropagation(); confirmDelete(record); }}
          style={{
            display: 'inline-flex', alignItems: 'center', gap: '0.3rem',
            padding: '0.35rem 0.7rem', borderRadius: 10,
            fontSize: '0.78rem', fontWeight: 600,
            color: '#DC2626', background: 'rgba(220,38,38,0.07)',
            border: '1px solid transparent', cursor: 'pointer',
            transition: 'all 0.2s ease',
          }}
          onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(220,38,38,0.14)'; }}
          onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(220,38,38,0.07)'; }}
        >
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
          </svg>
          删除
        </button>
      ),
    },
  ];

  return (
    <div style={{ position: 'relative', minHeight: 'calc(100vh - 80px)', animation: 'pageIn 0.5s ease-out both' }}>
      {/* Background atmosphere */}
      <div style={{
        position: 'absolute', inset: 0, zIndex: 0, pointerEvents: 'none',
        borderRadius: 20,
        background: `
          radial-gradient(ellipse 50% 40% at 12% 10%, rgba(251,113,133,0.03) 0%, transparent 50%),
          radial-gradient(ellipse 42% 50% at 88% 82%, rgba(195,155,211,.03) 0%, transparent 48%),
          linear-gradient(180deg, #FAF8F5 0%, #FDF9F6 40%, #FAF8F5 100%)
        `,
      }} />

      <div style={{ position: 'relative', zIndex: 1, display: 'flex', flexDirection: 'column' }}>
        {/* Error banner */}
        {isError && (
          <div style={{
            background: 'linear-gradient(135deg, rgba(244,63,94,0.06), rgba(244,63,94,0.02))',
            border: '1px solid rgba(244,63,94,0.12)', borderRadius: 16,
            padding: '1rem 1.25rem', marginBottom: '1.5rem',
            display: 'flex', alignItems: 'center', gap: '0.75rem',
          }}>
            <div style={{
              width: 36, height: 36, borderRadius: 10,
              background: 'linear-gradient(135deg, rgba(244,63,94,0.12), rgba(244,63,94,0.06))',
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#E11D48" strokeWidth="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: '0.875rem', fontWeight: 600, color: '#E11D48' }}>数据加载失败</div>
              <div style={{ fontSize: '0.8rem', color: '#9B9590' }}>无法连接到服务器，请检查后端服务是否启动</div>
            </div>
            <button onClick={() => window.location.reload()} style={{
              padding: '0.45rem 1rem', borderRadius: 10, background: 'linear-gradient(135deg, #F43F5E, #E11D48)',
              color: '#fff', fontSize: '0.8rem', fontWeight: 600, border: 'none', cursor: 'pointer',
              boxShadow: '0 2px 8px rgba(244,63,94,0.25)',
            }}>刷新</button>
          </div>
        )}

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
                  background: 'linear-gradient(135deg, #FB7185, #C39BD3)',
                  color: '#fff', flexShrink: 0,
                }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M12 20h9" />
                    <path d="M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z" />
                  </svg>
                </span>
                评价管理
              </h1>
              <p style={{ fontSize: '0.88rem', color: '#9B9590', marginTop: '0.3rem', paddingLeft: '36px' }}>
                查看和管理用户对商品的评价
              </p>
            </div>

            {/* Search */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexShrink: 0 }}>
              <div style={{ position: 'relative', width: 280 }}>
                <input
                  type="text"
                  placeholder="搜索评价内容..."
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
                  onFocus={(e) => { e.currentTarget.style.borderColor = '#FB7185'; e.currentTarget.style.boxShadow = '0 0 0 3px rgba(251,113,133,0.08)'; }}
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

        {/* Filter toolbar */}
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
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" /></svg>
              状态
            </span>
            <AdminSelect
              options={STATUS_OPTIONS}
              value={statusFilter}
              onChange={(value) => { setStatusFilter(value); setPage(1); }}
            />
          </div>

          <div style={{ width: 1, height: 20, background: '#E5E0DB', flexShrink: 0 }} />

          {/* Rating filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.8rem', fontWeight: 500, color: '#9B9590' }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" /></svg>
              评分
            </span>
            <AdminSelect
              options={RATING_OPTIONS}
              value={ratingFilter}
              onChange={(value) => { setRatingFilter(value); setPage(1); }}
            />
          </div>

          <div style={{ flex: 1 }} />
          {data && (
            <span style={{ fontSize: '0.81rem', color: '#9B9590', fontWeight: 500 }}>
              共 <strong style={{ color: '#2A2520' }}>{data.total.toLocaleString()}</strong> 条评价
            </span>
          )}
        </div>

        {/* Table */}
        <div style={{
          flex: 1,
          background: 'rgba(255,255,255,0.72)', backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          border: '1px solid rgba(255,255,255,0.65)', borderRadius: 20,
          overflow: 'hidden',
          animation: 'cardIn 0.5s ease-out 0.15s both',
        }}>
          <AdminTable
            columns={columns}
            data={data?.records ?? []}
            rowKey="reviewId"
            loading={isLoading}
            pagination={
              data && data.total > pageSize
                ? { current: page, pageSize, total: data.total, onChange: setPage }
                : undefined
            }
            emptyText="暂无评价数据"
          />
        </div>
      </div>

      {/* Delete confirm modal */}
      <ConfirmModal
        isOpen={deleteModal.open}
        title="确认删除评价"
        content={
          <div style={{ fontSize: '0.88rem', lineHeight: 1.6, color: '#6B6460' }}>
            <p style={{ marginBottom: '0.75rem' }}>
              确定要删除该评价吗？删除后将无法恢复。
            </p>
            <div style={{
              padding: '0.75rem', borderRadius: 10,
              background: '#FAF8F5', marginBottom: '0.75rem',
              fontSize: '0.84rem', color: '#2A2520', fontStyle: 'italic',
            }}>
              "{deleteModal.reviewContent}"
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#2A2520', marginBottom: '0.35rem' }}>
                删除原因 <span style={{ color: '#DC2626' }}>*</span>
              </label>
              <input
                type="text"
                value={deleteReason}
                onChange={(e) => setDeleteReason(e.target.value)}
                placeholder="请输入删除原因"
                autoFocus
                style={{
                  width: '100%', padding: '0.6rem 0.75rem',
                  border: '1.5px solid #E5E0DB', borderRadius: 10,
                  fontSize: '0.84rem', outline: 'none',
                  transition: 'border-color 0.2s',
                  boxSizing: 'border-box',
                }}
                onFocus={(e) => { e.currentTarget.style.borderColor = '#FB7185'; }}
                onBlur={(e) => { e.currentTarget.style.borderColor = '#E5E0DB'; }}
              />
            </div>
          </div>
        }
        confirmText="确认删除"
        onConfirm={handleDeleteConfirm}
        isLoading={deleteReview.isPending}
        confirmDisabled={!deleteReason.trim()}
        onCancel={() => setDeleteModal({ open: false, reviewId: '', reviewContent: '' })}
        variant="danger"
      />

      <style>{`
        @keyframes pageIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes headerSlide { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes toolbarIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes cardIn { from { opacity: 0; transform: translateY(16px) scale(0.99); } to { opacity: 1; transform: translateY(0) scale(1); } }
      `}</style>
    </div>
  );
}
