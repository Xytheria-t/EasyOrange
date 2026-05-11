import { useState, useCallback } from 'react';
import { AdminTable, type Column } from '../../components/AdminTable';
import { StatusBadge } from '../../components/StatusBadge';
import { ConfirmModal } from '../../components/ConfirmModal';
import { AdminSelect } from '../../components/AdminSelect';

const statusFilterOptions = [
  { value: '', label: '全部状态' },
  { value: '0', label: '待处理' },
  { value: '1', label: '处理中' },
  { value: '2', label: '已处理' },
  { value: '3', label: '已驳回' },
];

const typeFilterOptions = [
  { value: '', label: '全部类型' },
  { value: '0', label: '违规商品' },
  { value: '1', label: '虚假信息' },
  { value: '2', label: '诈骗行为' },
  { value: '3', label: '其他' },
];

interface MockReport {
  id: string;
  reporterName: string;
  targetName: string;
  targetType: string;
  reason: string;
  type: number;
  status: number;
  createTime: string;
}

const MOCK_REPORTS: MockReport[] = [
  { id: '1', reporterName: '张同学', targetName: '假冒AirPods Pro', targetType: '商品', reason: '商品描述与实物严重不符，涉嫌假冒', type: 0, status: 0, createTime: '2026-05-10 15:20:00' },
  { id: '2', reporterName: '李同学', targetName: '王某某', targetType: '用户', reason: '发布多条虚假商品信息，骗取定金', type: 1, status: 1, createTime: '2026-05-10 11:30:00' },
  { id: '3', reporterName: '赵同学', targetName: '二手iPhone 14', targetType: '商品', reason: '标价远低于市场价，疑似诈骗', type: 2, status: 0, createTime: '2026-05-09 20:45:00' },
  { id: '4', reporterName: '陈同学', targetName: '考研资料包', targetType: '商品', reason: '盗版资料，侵犯知识产权', type: 0, status: 2, createTime: '2026-05-09 14:10:00' },
  { id: '5', reporterName: '周同学', targetName: '刘某某', targetType: '用户', reason: '恶意骚扰，多次发送不当信息', type: 3, status: 0, createTime: '2026-05-08 09:30:00' },
  { id: '6', reporterName: '吴同学', targetName: '游戏账号转让', targetType: '商品', reason: '虚拟商品禁止交易', type: 0, status: 3, createTime: '2026-05-07 22:15:00' },
];

const TYPE_LABELS: Record<number, { emoji: string; label: string; color: string }> = {
  0: { emoji: '📦', label: '违规商品', color: '#E11D48' },
  1: { emoji: '🎭', label: '虚假信息', color: '#D97706' },
  2: { emoji: '⚠️', label: '诈骗行为', color: '#DC2626' },
  3: { emoji: '📝', label: '其他', color: '#6B7280' },
};

export default function ReportManagePage() {
  const [statusFilter, setStatusFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [page, setPage] = useState(1);
  const [confirmModal, setConfirmModal] = useState<{ open: boolean; reportId: string; action: 'resolve' | 'dismiss' }>({ open: false, reportId: '', action: 'resolve' });
  const pageSize = 10;

  const handleSearch = useCallback(() => {
    setKeyword(searchInput);
    setPage(1);
  }, [searchInput]);

  const handleKeyPress = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch();
  }, [handleSearch]);

  const filteredReports = MOCK_REPORTS.filter((r) => {
    if (statusFilter && r.status !== Number(statusFilter)) return false;
    if (typeFilter && r.type !== Number(typeFilter)) return false;
    if (keyword && !r.targetName.includes(keyword) && !r.reporterName.includes(keyword) && !r.reason.includes(keyword)) return false;
    return true;
  });

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });

  const handleConfirmAction = async () => {
    setConfirmModal({ open: false, reportId: '', action: 'resolve' });
  };

  const columns: Column<MockReport>[] = [
    {
      key: 'targetName',
      title: '举报对象',
      render: (value, record) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <span style={{ fontWeight: 600, color: '#2A2520', fontSize: '0.87rem' }}>{value as string}</span>
          <span style={{ fontSize: '0.76rem', color: '#9B9590' }}>{record.targetType}</span>
        </div>
      ),
    },
    {
      key: 'type',
      title: '类型',
      render: (value) => {
        const t = TYPE_LABELS[value as number] ?? TYPE_LABELS[3];
        return (
          <span style={{ fontSize: '0.82rem', fontWeight: 600, color: t.color }}>
            {t.emoji} {t.label}
          </span>
        );
      },
    },
    {
      key: 'reason',
      title: '举报原因',
      render: (value) => (
        <span style={{ color: '#6B6460', fontSize: '0.84rem', maxWidth: 220, display: 'inline-block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {value as string}
        </span>
      ),
    },
    {
      key: 'reporterName',
      title: '举报人',
      render: (value) => (
        <span style={{ fontSize: '0.86rem', color: '#6B6460' }}>{value as string}</span>
      ),
    },
    {
      key: 'status',
      title: '状态',
      render: (value) => <StatusBadge status={value as number} type="report" />,
    },
    {
      key: 'createTime',
      title: '举报时间',
      sortable: true,
      render: (value) => (
        <span style={{ color: '#9B9590', fontSize: '0.84rem' }}>{formatDate(value as string)}</span>
      ),
    },
    {
      key: 'actions',
      title: '操作',
      render: (_, record) => (
        <div style={{ display: 'flex', gap: '0.4rem' }}>
          {record.status === 0 && (
            <>
              <button
                onClick={(e) => { e.stopPropagation(); setConfirmModal({ open: true, reportId: record.id, action: 'resolve' }); }}
                style={{
                  display: 'inline-flex', alignItems: 'center', gap: '0.3rem',
                  padding: '0.35rem 0.7rem', borderRadius: 10,
                  fontSize: '0.78rem', fontWeight: 600,
                  color: '#059669', background: 'rgba(16,185,129,0.07)',
                  border: '1px solid transparent', cursor: 'pointer',
                  transition: 'all 0.2s ease',
                }}
                onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(16,185,129,0.14)'; }}
                onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(16,185,129,0.07)'; }}
              >
                处理
              </button>
              <button
                onClick={(e) => { e.stopPropagation(); setConfirmModal({ open: true, reportId: record.id, action: 'dismiss' }); }}
                style={{
                  display: 'inline-flex', alignItems: 'center', gap: '0.3rem',
                  padding: '0.35rem 0.7rem', borderRadius: 10,
                  fontSize: '0.78rem', fontWeight: 600,
                  color: '#9B9590', background: 'rgba(155,149,144,0.07)',
                  border: '1px solid transparent', cursor: 'pointer',
                  transition: 'all 0.2s ease',
                }}
                onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(155,149,144,0.14)'; }}
                onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(155,149,144,0.07)'; }}
              >
                驳回
              </button>
            </>
          )}
          {record.status !== 0 && (
            <span style={{ fontSize: '0.8rem', color: '#B5AEA8' }}>已处理</span>
          )}
        </div>
      ),
    },
  ];

  return (
    <div style={{ position: 'relative', animation: 'pageIn 0.5s ease-out both' }}>
      {/* Background atmosphere */}
      <div style={{
        position: 'fixed', inset: 0, zIndex: 0, pointerEvents: 'none',
        background: `
          radial-gradient(ellipse 55% 35% at 8% 12%, rgba(244,63,94,0.03) 0%, transparent 50%),
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
                  background: 'linear-gradient(135deg, #F43F5E, #E11D48)',
                  color: '#fff', flexShrink: 0,
                }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" />
                    <line x1="4" y1="22" x2="4" y2="15" />
                  </svg>
                </span>
                举报处理
              </h1>
              <p style={{ fontSize: '0.88rem', color: '#9B9590', marginTop: '0.3rem', paddingLeft: '36px' }}>
                审核和处理用户举报，维护平台秩序
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
                  placeholder="搜索举报对象/原因..."
                  style={{
                    width: '100%', padding: '0.68rem 1rem 0.68rem 2.6rem',
                    border: '1.5px solid #E5E0DB', borderRadius: 14,
                    background: '#fff', fontSize: '0.87rem', color: '#2A2520',
                    outline: 'none', boxShadow: '0 1px 3px rgba(42,37,32,0.04)',
                    transition: 'all 0.2s ease',
                  }}
                  onFocus={(e) => { e.currentTarget.style.borderColor = '#F43F5E'; e.currentTarget.style.boxShadow = '0 0 0 3px rgba(244,63,94,0.08), 0 1px 3px rgba(42,37,32,0.04)'; }}
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
                  background: 'linear-gradient(135deg, #F43F5E, #E11D48)',
                  color: '#fff', fontSize: '0.87rem', fontWeight: 600,
                  cursor: 'pointer', transition: 'all 0.2s ease',
                  boxShadow: '0 2px 8px rgba(244,63,94,0.28)',
                  whiteSpace: 'nowrap',
                }}
                onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-1px)'; e.currentTarget.style.boxShadow = '0 4px 14px rgba(244,63,94,0.38)'; }}
                onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 2px 8px rgba(244,63,94,0.28)'; }}
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
          <div style={{ width: 1, height: 20, background: '#E5E0DB', flexShrink: 0 }} />
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.8rem', fontWeight: 500, color: '#9B9590' }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" /></svg>
              类型
            </span>
            <AdminSelect
              options={typeFilterOptions}
              value={typeFilter}
              onChange={(val) => { setTypeFilter(val); setPage(1); }}
            />
          </div>
          <div style={{ flex: 1 }} />
          <span style={{ fontSize: '0.81rem', color: '#9B9590', fontWeight: 500 }}>
            共 <strong style={{ color: '#2A2520' }}>{filteredReports.length}</strong> 条举报
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
            data={filteredReports}
            rowKey="id"
            loading={false}
            pagination={
              filteredReports.length > pageSize
                ? { current: page, pageSize, total: filteredReports.length, onChange: setPage }
                : undefined
            }
            emptyText="暂无举报数据"
          />
        </div>
      </div>

      <ConfirmModal
        open={confirmModal.open}
        title={confirmModal.action === 'resolve' ? '确认处理举报' : '确认驳回举报'}
        content={confirmModal.action === 'resolve' ? '确定要处理该举报吗？将对举报对象采取相应措施。' : '确定要驳回该举报吗？驳回后举报将被关闭。'}
        confirmText={confirmModal.action === 'resolve' ? '确认处理' : '确认驳回'}
        onConfirm={handleConfirmAction}
        onCancel={() => setConfirmModal({ open: false, reportId: '', action: 'resolve' })}
        variant={confirmModal.action === 'resolve' ? 'warning' : 'info'}
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
