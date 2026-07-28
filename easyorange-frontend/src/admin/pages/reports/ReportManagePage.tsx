import { useCallback, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { usePagination } from '@/hooks/usePagination';
import { AdminSelect } from '../../components/AdminSelect';
import { AdminTable, type Column } from '../../components/AdminTable';
import { ConfirmModal } from '../../components/ConfirmModal';
import { StatusBadge } from '../../components/StatusBadge';
import { useAdminReports, useHandleReport } from '../../hooks';
import type { AdminReport } from '../../types/admin';

const STATUS_FILTER_OPTIONS = [
    { value: '', label: '全部状态' },
    { value: '0', label: '待处理' },
    { value: '1', label: '处理中' },
    { value: '2', label: '已处理' },
    { value: '3', label: '已驳回' },
];

const TYPE_FILTER_OPTIONS = [
    { value: '', label: '全部类型' },
    { value: '0', label: '违规商品' },
    { value: '1', label: '虚假信息' },
    { value: '2', label: '诈骗行为' },
    { value: '3', label: '其他' },
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
    const [confirmModal, setConfirmModal] = useState<{
        open: boolean;
        reportId: string;
        action: 'resolve' | 'dismiss';
    }>({ open: false, reportId: '0', action: 'resolve' });
    const {
        pageNum: page,
        pageSize,
        goTo,
    } = usePagination({
        resetDeps: [keyword, statusFilter, typeFilter],
    });

    const { data, isLoading } = useAdminReports({
        pageNum: page,
        pageSize,
        status: statusFilter ? Number(statusFilter) : undefined,
        type: typeFilter ? Number(typeFilter) : undefined,
        keyword: keyword || undefined,
    });

    const handleReport = useHandleReport();

    const handleSearch = useCallback(() => {
        setKeyword(searchInput);
        goTo(1);
    }, [searchInput, goTo]);

    const handleKeyPress = useCallback(
        (e: React.KeyboardEvent) => {
            if (e.key === 'Enter') {
                handleSearch();
            }
        },
        [handleSearch]
    );

    const formatDate = (dateString: string) =>
        new Date(dateString).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });

    const handleConfirmAction = async () => {
        try {
            await handleReport.mutateAsync({ id: confirmModal.reportId, data: { action: confirmModal.action } });
            setConfirmModal({ open: false, reportId: '0', action: 'resolve' });
        } catch {
            setConfirmModal(prev => ({ ...prev, open: false }));
        }
    };

    const columns: Column<AdminReport>[] = [
        {
            key: 'productName',
            title: '举报对象',
            render: (value, _record) => {
                const productName = value as string | undefined;
                return (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                        <span style={{ fontWeight: 600, color: '#2A2520', fontSize: '0.87rem' }}>
                            {productName || '未知商品'}
                        </span>
                        <span style={{ fontSize: '0.76rem', color: '#9B9590' }}>商品</span>
                    </div>
                );
            },
        },
        {
            key: 'reason',
            title: '类型',
            render: () => {
                const typeLabel = TYPE_LABELS[0];
                return (
                    <span style={{ fontSize: '0.82rem', fontWeight: 600, color: typeLabel.color }}>
                        {typeLabel.emoji} {typeLabel.label}
                    </span>
                );
            },
        },
        {
            key: 'reason',
            title: '举报原因',
            render: value => {
                const reason = value as string;
                return (
                    <span
                        style={{
                            color: '#6B6460',
                            fontSize: '0.84rem',
                            maxWidth: 220,
                            display: 'inline-block',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {reason}
                    </span>
                );
            },
        },
        {
            key: 'reporterName',
            title: '举报人',
            render: value => {
                const reporterName = value as string;
                return <span style={{ fontSize: '0.86rem', color: '#6B6460' }}>{reporterName}</span>;
            },
        },
        {
            key: 'status',
            title: '状态',
            render: value => {
                const status = value as string | number;
                return <StatusBadge status={status} type="report" />;
            },
        },
        {
            key: 'createTime',
            title: '举报时间',
            sortable: true,
            render: value => {
                const timeString = value as string | undefined;
                return (
                    <span style={{ color: '#9B9590', fontSize: '0.84rem' }}>
                        {timeString ? formatDate(timeString) : '-'}
                    </span>
                );
            },
        },
        {
            key: 'actions',
            title: '操作',
            render: (_, record) => (
                <div style={{ display: 'flex', gap: '0.4rem' }}>
                    {record.status === 0 && (
                        <>
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={e => {
                                    e.stopPropagation();
                                    setConfirmModal({
                                        open: true,
                                        reportId: record.reportId,
                                        action: 'resolve',
                                    });
                                }}
                                style={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: '0.3rem',
                                    padding: '0.35rem 0.7rem',
                                    borderRadius: 10,
                                    fontSize: '0.78rem',
                                    fontWeight: 600,
                                    color: '#059669',
                                    background: 'rgba(16,185,129,0.07)',
                                    border: '1px solid transparent',
                                    transition: 'all 0.2s ease',
                                    height: 'auto',
                                    minHeight: 'unset',
                                }}
                                onMouseEnter={e => {
                                    e.currentTarget.style.background = 'rgba(16,185,129,0.14)';
                                }}
                                onMouseLeave={e => {
                                    e.currentTarget.style.background = 'rgba(16,185,129,0.07)';
                                }}
                            >
                                处理
                            </Button>
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={e => {
                                    e.stopPropagation();
                                    setConfirmModal({
                                        open: true,
                                        reportId: record.reportId,
                                        action: 'dismiss',
                                    });
                                }}
                                style={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: '0.3rem',
                                    padding: '0.35rem 0.7rem',
                                    borderRadius: 10,
                                    fontSize: '0.78rem',
                                    fontWeight: 600,
                                    color: '#9B9590',
                                    background: 'rgba(155,149,144,0.07)',
                                    border: '1px solid transparent',
                                    transition: 'all 0.2s ease',
                                    height: 'auto',
                                    minHeight: 'unset',
                                }}
                                onMouseEnter={e => {
                                    e.currentTarget.style.background = 'rgba(155,149,144,0.14)';
                                }}
                                onMouseLeave={e => {
                                    e.currentTarget.style.background = 'rgba(155,149,144,0.07)';
                                }}
                            >
                                驳回
                            </Button>
                        </>
                    )}
                    {record.status !== 0 && <span style={{ fontSize: '0.8rem', color: '#B5AEA8' }}>已处理</span>}
                </div>
            ),
        },
    ];

    return (
        <div style={{ position: 'relative', minHeight: 'calc(100vh - 80px)', animation: 'pageIn 0.5s ease-out both' }}>
            {/* Background atmosphere */}
            <div
                style={{
                    position: 'absolute',
                    inset: 0,
                    zIndex: 0,
                    pointerEvents: 'none',
                    borderRadius: 20,
                    background: `
          radial-gradient(ellipse 55% 35% at 8% 12%, rgba(244,63,94,0.03) 0%, transparent 50%),
          radial-gradient(ellipse 40% 45% at 92% 85%, rgba(195,155,211,0.03) 0%, transparent 48%),
          linear-gradient(180deg, #FAF8F5 0%, #FDF9F6 40%, #FAF8F5 100%)
        `,
                }}
            />

            <div style={{ position: 'relative', zIndex: 1, display: 'flex', flexDirection: 'column' }}>
                {/* Header */}
                <header style={{ marginBottom: '1.75rem', animation: 'headerSlide 0.6s ease-out both' }}>
                    <div
                        style={{
                            display: 'flex',
                            alignItems: 'flex-end',
                            justifyContent: 'space-between',
                            gap: '1rem',
                            flexWrap: 'wrap',
                        }}
                    >
                        <div>
                            <h1
                                style={{
                                    fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                                    fontSize: 'clamp(1.5rem, 2.5vw, 1.875rem)',
                                    fontWeight: 700,
                                    color: '#2A2520',
                                    letterSpacing: '-0.03em',
                                    lineHeight: 1.2,
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '0.65rem',
                                }}
                            >
                                <span
                                    style={{
                                        width: 30,
                                        height: 30,
                                        borderRadius: 10,
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        background: 'linear-gradient(135deg, #F43F5E, #E11D48)',
                                        color: '#fff',
                                        flexShrink: 0,
                                    }}
                                >
                                    <svg
                                        aria-hidden="true"
                                        width="15"
                                        height="15"
                                        viewBox="0 0 24 24"
                                        fill="none"
                                        stroke="currentColor"
                                        strokeWidth="2"
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                    >
                                        <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" />
                                        <line x1="4" y1="22" x2="4" y2="15" />
                                    </svg>
                                </span>
                                举报处理
                            </h1>
                            <p
                                style={{
                                    fontSize: '0.88rem',
                                    color: '#9B9590',
                                    marginTop: '0.3rem',
                                    paddingLeft: '36px',
                                }}
                            >
                                审核和处理用户举报，维护平台秩序
                            </p>
                        </div>

                        {/* Search */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexShrink: 0 }}>
                            <div style={{ position: 'relative', width: 280 }}>
                                <Input
                                    type="text"
                                    value={searchInput}
                                    onChange={e => setSearchInput(e.target.value)}
                                    onKeyPress={handleKeyPress}
                                    placeholder="搜索举报对象/原因..."
                                    className="h-auto"
                                    style={{
                                        width: '100%',
                                        padding: '0.68rem 1rem 0.68rem 2.6rem',
                                        border: '1.5px solid #E5E0DB',
                                        borderRadius: 14,
                                        background: '#fff',
                                        fontSize: '0.87rem',
                                        color: '#2A2520',
                                        outline: 'none',
                                        boxShadow: '0 1px 3px rgba(42,37,32,0.04)',
                                        transition: 'all 0.2s ease',
                                    }}
                                    onFocus={e => {
                                        e.currentTarget.style.borderColor = '#F43F5E';
                                        e.currentTarget.style.boxShadow =
                                            '0 0 0 3px rgba(244,63,94,0.08), 0 1px 3px rgba(42,37,32,0.04)';
                                    }}
                                    onBlur={e => {
                                        e.currentTarget.style.borderColor = '#E5E0DB';
                                        e.currentTarget.style.boxShadow = '0 1px 3px rgba(42,37,32,0.04)';
                                    }}
                                />
                                <svg
                                    aria-hidden="true"
                                    style={{
                                        position: 'absolute',
                                        left: '0.85rem',
                                        top: '50%',
                                        transform: 'translateY(-50%)',
                                        width: 17,
                                        height: 17,
                                        color: '#B5AEA8',
                                        pointerEvents: 'none',
                                    }}
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2"
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                >
                                    <circle cx="11" cy="11" r="8" />
                                    <line x1="21" y1="21" x2="16.65" y2="16.65" />
                                </svg>
                            </div>
                            <Button
                                variant="default"
                                size="sm"
                                onClick={handleSearch}
                                style={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    padding: '0.68rem 1.2rem',
                                    border: 'none',
                                    borderRadius: 14,
                                    background: 'linear-gradient(135deg, #F43F5E, #E11D48)',
                                    color: '#fff',
                                    fontSize: '0.87rem',
                                    fontWeight: 600,
                                    transition: 'all 0.2s ease',
                                    boxShadow: '0 2px 8px rgba(244,63,94,0.28)',
                                    whiteSpace: 'nowrap',
                                    height: 'auto',
                                    minHeight: 'unset',
                                }}
                                onMouseEnter={e => {
                                    e.currentTarget.style.transform = 'translateY(-1px)';
                                    e.currentTarget.style.boxShadow = '0 4px 14px rgba(244,63,94,0.38)';
                                }}
                                onMouseLeave={e => {
                                    e.currentTarget.style.transform = 'translateY(0)';
                                    e.currentTarget.style.boxShadow = '0 2px 8px rgba(244,63,94,0.28)';
                                }}
                            >
                                搜索
                            </Button>
                        </div>
                    </div>
                </header>

                {/* Filter toolbar */}
                <div
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        flexWrap: 'wrap',
                        gap: '0.75rem',
                        padding: '1rem 1.25rem',
                        background: 'rgba(255,255,255,0.72)',
                        backdropFilter: 'blur(16px)',
                        WebkitBackdropFilter: 'blur(16px)',
                        border: '1px solid rgba(255,255,255,0.6)',
                        borderRadius: 16,
                        marginBottom: '1.25rem',
                        animation: 'toolbarIn 0.5s ease-out 0.08s both',
                    }}
                >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
                        <span
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '0.3rem',
                                fontSize: '0.8rem',
                                fontWeight: 500,
                                color: '#9B9590',
                            }}
                        >
                            <svg
                                aria-hidden="true"
                                width="13"
                                height="13"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            >
                                <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" />
                            </svg>
                            状态
                        </span>
                        <AdminSelect
                            options={STATUS_FILTER_OPTIONS}
                            value={statusFilter}
                            onChange={value => {
                                setStatusFilter(value);
                                goTo(1);
                            }}
                        />
                    </div>
                    <div style={{ width: 1, height: 20, background: '#E5E0DB', flexShrink: 0 }} />
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
                        <span
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '0.3rem',
                                fontSize: '0.8rem',
                                fontWeight: 500,
                                color: '#9B9590',
                            }}
                        >
                            <svg
                                aria-hidden="true"
                                width="13"
                                height="13"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            >
                                <path d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" />
                            </svg>
                            类型
                        </span>
                        <AdminSelect
                            options={TYPE_FILTER_OPTIONS}
                            value={typeFilter}
                            onChange={value => {
                                setTypeFilter(value);
                                goTo(1);
                            }}
                        />
                    </div>
                    <div style={{ flex: 1 }} />
                    <span style={{ fontSize: '0.81rem', color: '#9B9590', fontWeight: 500 }}>
                        共 <strong style={{ color: '#2A2520' }}>{data?.total ?? 0}</strong> 条举报
                    </span>
                </div>

                {/* Table */}
                <div
                    style={{
                        flex: 1,
                        background: 'rgba(255,255,255,0.72)',
                        backdropFilter: 'blur(20px)',
                        WebkitBackdropFilter: 'blur(20px)',
                        border: '1px solid rgba(255,255,255,0.65)',
                        borderRadius: 20,
                        overflow: 'hidden',
                        animation: 'cardIn 0.5s ease-out 0.15s both',
                        minHeight: 300,
                    }}
                >
                    <AdminTable
                        columns={columns}
                        data={data?.records ?? []}
                        rowKey="reportId"
                        loading={isLoading}
                        pagination={
                            data && data.total > pageSize
                                ? { current: page, pageSize, total: data.total, onChange: goTo }
                                : undefined
                        }
                        emptyText="暂无举报数据"
                    />
                </div>
            </div>

            <ConfirmModal
                isOpen={confirmModal.open}
                title={confirmModal.action === 'resolve' ? '确认处理举报' : '确认驳回举报'}
                content={
                    confirmModal.action === 'resolve'
                        ? '确定要处理该举报吗？将对举报对象采取相应措施。'
                        : '确定要驳回该举报吗？驳回后举报将被关闭。'
                }
                confirmText={confirmModal.action === 'resolve' ? '确认处理' : '确认驳回'}
                onConfirm={handleConfirmAction}
                isLoading={handleReport.isPending}
                onCancel={() => setConfirmModal({ open: false, reportId: '0', action: 'resolve' })}
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
