import { useCallback, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { AdminSelect } from '../../components/AdminSelect';
import { AdminTable, type Column } from '../../components/AdminTable';
import { StatusBadge } from '../../components/StatusBadge';
import { useAdminUsers, useUpdateUserStatus } from '../../hooks';
import type { AdminUser } from '../../types/admin';
import { UserDetailModal } from './UserDetailModal';

const STATUS_FILTER_OPTIONS = [
    { value: '', label: '全部状态' },
    { value: '0', label: '正常' },
    { value: '1', label: '禁用' },
    { value: '2', label: '锁定' },
];

const USER_TYPE_FILTER_OPTIONS = [
    { value: '', label: '全部类型' },
    { value: '01', label: '学生' },
    { value: '02', label: '教师' },
];

const AVATAR_GRADIENTS = [
    'linear-gradient(135deg, #F97316, #FB923C)',
    'linear-gradient(135deg, #FB7185, #C39BD3)',
    'linear-gradient(135deg, #34D399, #10B981)',
    'linear-gradient(135deg, #FBBF24, #F97316)',
    'linear-gradient(135deg, #C39BD3, #D8B4FE)',
] as const;

export default function UserManagePage() {
    const [page, setPage] = useState(1);
    const [pageSize] = useState(10);
    const [keyword, setKeyword] = useState('');
    const [searchInput, setSearchInput] = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [userTypeFilter, setUserTypeFilter] = useState('');
    const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
    const [modalOpen, setModalOpen] = useState(false);

    const { data, isLoading, isError } = useAdminUsers({
        pageNum: page,
        pageSize,
        keyword: keyword || undefined,
        status: statusFilter || undefined,
        userType: userTypeFilter || undefined,
    });

    const updateStatusMutation = useUpdateUserStatus();

    const handleSearch = useCallback(() => {
        setKeyword(searchInput);
        setPage(1);
    }, [searchInput]);

    const handleKeyPress = useCallback(
        (e: React.KeyboardEvent) => {
            if (e.key === 'Enter') {
                handleSearch();
            }
        },
        [handleSearch]
    );

    const handleViewDetail = useCallback((user: AdminUser) => {
        setSelectedUser(user);
        setModalOpen(true);
    }, []);

    const handleSaveStatus = useCallback(
        async (status: string) => {
            if (!selectedUser) {
                return;
            }
            await updateStatusMutation.mutateAsync({
                id: Number(selectedUser.userId),
                data: { status: Number(status) },
            });
            setModalOpen(false);
            setSelectedUser(null);
        },
        [selectedUser, updateStatusMutation]
    );

    const formatDate = (dateString: string) =>
        new Date(dateString).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });

    const columns: Column<AdminUser>[] = [
        {
            key: 'username',
            title: '用户',
            render: (_value, record) => (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <span
                        style={{
                            width: 36,
                            height: 36,
                            borderRadius: 12,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: '0.85rem',
                            fontWeight: 700,
                            color: '#fff',
                            fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                            background: AVATAR_GRADIENTS[Number(record.userId ?? 0) % AVATAR_GRADIENTS.length],
                            flexShrink: 0,
                        }}
                    >
                        {(record.nickname || record.username || '?').charAt(0).toUpperCase()}
                    </span>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                        <span style={{ fontWeight: 600, color: '#2A2520', fontSize: '0.875rem' }}>
                            {record.username}
                        </span>
                        {record.nickname && (
                            <span style={{ fontSize: '0.78rem', color: '#9B9590' }}>{record.nickname}</span>
                        )}
                    </div>
                </div>
            ),
        },
        {
            key: 'email',
            title: '邮箱',
            render: value => <span style={{ color: '#6B6460', fontSize: '0.86rem' }}>{value as string}</span>,
        },
        {
            key: 'userType',
            title: '类型',
            render: (_value, record) => (
                <span
                    style={{
                        fontWeight: 600,
                        fontSize: '0.82rem',
                        color: record.userType === '01' ? '#2563EB' : '#7C3AED',
                    }}
                >
                    {record.userTypeDesc || (record.userType === '01' ? '🎓 学生' : '👨‍🏫 教师')}
                </span>
            ),
        },
        {
            key: 'status',
            title: '状态',
            render: (_value, record) => <StatusBadge status={record.statusDesc ?? record.status ?? ''} type="user" />,
        },
        {
            key: 'createTime',
            title: '注册时间',
            sortable: true,
            render: value => (
                <span style={{ color: '#9B9590', fontSize: '0.84rem' }}>{formatDate(value as string)}</span>
            ),
        },
        {
            key: 'actions',
            title: '操作',
            render: (_, record) => (
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={e => {
                        e.stopPropagation();
                        handleViewDetail(record);
                    }}
                    style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.35rem',
                        padding: '0.38rem 0.85rem',
                        borderRadius: 10,
                        fontSize: '0.81rem',
                        fontWeight: 600,
                        color: '#EA580C',
                        background: 'rgba(249,115,22,0.07)',
                        border: '1px solid transparent',
                        transition: 'all 0.2s ease',
                        letterSpacing: '0.01em',
                        textDecoration: 'none',
                        height: 'auto',
                        minHeight: 'unset',
                    }}
                    onMouseEnter={e => {
                        e.currentTarget.style.background = 'rgba(249,115,22,0.14)';
                        e.currentTarget.style.transform = 'translateX(2px)';
                    }}
                    onMouseLeave={e => {
                        e.currentTarget.style.background = 'rgba(249,115,22,0.07)';
                        e.currentTarget.style.transform = 'translateX(0)';
                    }}
                >
                    <svg
                        width="14"
                        height="14"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    >
                        <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    详情
                </Button>
            ),
        },
    ];

    const users = data?.records ?? [];
    const total = data?.total ?? 0;

    return (
        <div
            style={{
                position: 'relative',
                minHeight: 'calc(100vh - 80px)',
                animation: 'pageIn 0.5s ease-out both',
            }}
        >
            {/* Background atmosphere */}
            <div
                style={{
                    position: 'absolute',
                    inset: 0,
                    zIndex: 0,
                    pointerEvents: 'none',
                    borderRadius: 20,
                    background: `
            radial-gradient(ellipse 55% 35% at 8% 12%, rgba(249,115,22,0.035) 0%, transparent 50%),
            radial-gradient(ellipse 40% 45% at 92% 85%, rgba(195,155,211,0.03) 0%, transparent 48%),
            linear-gradient(180deg, #FAF8F5 0%, #FDF9F6 40%, #FAF8F5 100%)
          `,
                }}
            />

            {/* Content */}
            <div style={{ position: 'relative', zIndex: 1, display: 'flex', flexDirection: 'column' }}>
                {isError && (
                    <div
                        style={{
                            background: 'linear-gradient(135deg, rgba(244,63,94,0.06), rgba(244,63,94,0.02))',
                            border: '1px solid rgba(244,63,94,0.12)',
                            borderRadius: 16,
                            padding: '1rem 1.25rem',
                            marginBottom: '1.5rem',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '0.75rem',
                        }}
                    >
                        <div
                            style={{
                                width: 36,
                                height: 36,
                                borderRadius: 10,
                                background: 'linear-gradient(135deg, rgba(244,63,94,0.12), rgba(244,63,94,0.06))',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                flexShrink: 0,
                            }}
                        >
                            <svg
                                width="18"
                                height="18"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="#E11D48"
                                strokeWidth="2"
                            >
                                <circle cx="12" cy="12" r="10" />
                                <line x1="12" y1="8" x2="12" y2="12" />
                                <line x1="12" y1="16" x2="12.01" y2="16" />
                            </svg>
                        </div>
                        <div style={{ flex: 1 }}>
                            <div style={{ fontSize: '0.875rem', fontWeight: 600, color: '#E11D48' }}>数据加载失败</div>
                            <div style={{ fontSize: '0.8rem', color: '#9B9590' }}>
                                无法连接到服务器，请检查后端服务是否启动
                            </div>
                        </div>
                        <Button
                            onClick={() => window.location.reload()}
                            variant="default"
                            size="sm"
                            style={{
                                padding: '0.45rem 1rem',
                                borderRadius: 10,
                                background: 'linear-gradient(135deg, #F43F5E, #E11D48)',
                                color: '#fff',
                                fontSize: '0.8rem',
                                fontWeight: 600,
                                border: 'none',
                                boxShadow: '0 2px 8px rgba(244,63,94,0.25)',
                                height: 'auto',
                                minHeight: 'unset',
                            }}
                        >
                            刷新
                        </Button>
                    </div>
                )}

                {/* ===== Header ===== */}
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
                                        background: 'linear-gradient(135deg, #F97316, #FB923C)',
                                        color: '#fff',
                                        flexShrink: 0,
                                    }}
                                >
                                    <svg
                                        width="15"
                                        height="15"
                                        viewBox="0 0 24 24"
                                        fill="none"
                                        stroke="currentColor"
                                        strokeWidth="2"
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                    >
                                        <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                                        <circle cx="9" cy="7" r="4" />
                                        <path d="M23 21v-2a4 4 0 00-3-3.87" />
                                        <path d="M16 3.13a4 4 0 010 7.75" />
                                    </svg>
                                </span>
                                用户管理
                            </h1>
                            <p
                                style={{
                                    fontSize: '0.88rem',
                                    color: '#9B9590',
                                    marginTop: '0.3rem',
                                    paddingLeft: '36px',
                                }}
                            >
                                管理平台所有注册用户，查看详情或调整状态
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
                                    placeholder="搜索用户名/邮箱..."
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
                                        e.currentTarget.style.borderColor = '#F97316';
                                        e.currentTarget.style.boxShadow =
                                            '0 0 0 3px rgba(249,115,22,0.08), 0 1px 3px rgba(42,37,32,0.04)';
                                    }}
                                    onBlur={e => {
                                        e.currentTarget.style.borderColor = '#E5E0DB';
                                        e.currentTarget.style.boxShadow = '0 1px 3px rgba(42,37,32,0.04)';
                                    }}
                                />
                                <svg
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
                                    background: 'linear-gradient(135deg, #F97316, #EA580C)',
                                    color: '#fff',
                                    fontSize: '0.87rem',
                                    fontWeight: 600,
                                    transition: 'all 0.2s ease',
                                    boxShadow: '0 2px 8px rgba(249,115,22,0.28)',
                                    whiteSpace: 'nowrap',
                                    letterSpacing: '0.01em',
                                    height: 'auto',
                                    minHeight: 'unset',
                                }}
                                onMouseEnter={e => {
                                    e.currentTarget.style.transform = 'translateY(-1px)';
                                    e.currentTarget.style.boxShadow = '0 4px 14px rgba(249,115,22,0.38)';
                                }}
                                onMouseLeave={e => {
                                    e.currentTarget.style.transform = 'translateY(0)';
                                    e.currentTarget.style.boxShadow = '0 2px 8px rgba(249,115,22,0.28)';
                                }}
                            >
                                搜索
                            </Button>
                        </div>
                    </div>
                </header>

                {/* ===== Filter Toolbar ===== */}
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
                    {/* Status filter */}
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
                            onChange={val => {
                                setStatusFilter(val);
                                setPage(1);
                            }}
                        />
                    </div>

                    {/* Type filter */}
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
                                width="13"
                                height="13"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            >
                                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                                <circle cx="12" cy="7" r="4" />
                            </svg>
                            类型
                        </span>
                        <AdminSelect
                            options={USER_TYPE_FILTER_OPTIONS}
                            value={userTypeFilter}
                            onChange={val => {
                                setUserTypeFilter(val);
                                setPage(1);
                            }}
                        />
                    </div>

                    {/* Divider */}
                    <div style={{ width: 1, height: 20, background: '#E5E0DB', flexShrink: 0 }} />

                    {/* Spacer + count */}
                    <div style={{ flex: 1 }} />
                    {total > 0 && (
                        <span style={{ fontSize: '0.81rem', color: '#9B9590', fontWeight: 500 }}>
                            共 <strong style={{ color: '#2A2520' }}>{total.toLocaleString()}</strong> 位用户
                        </span>
                    )}
                </div>

                {/* ===== Table with glass card wrapper ===== */}
                <div
                    style={{
                        background: 'rgba(255,255,255,0.72)',
                        backdropFilter: 'blur(20px)',
                        WebkitBackdropFilter: 'blur(20px)',
                        border: '1px solid rgba(255,255,255,0.65)',
                        borderRadius: 20,
                        overflow: 'hidden',
                        transition: 'all 0.35s ease',
                        animation: 'cardIn 0.5s ease-out 0.15s both',
                    }}
                >
                    <AdminTable
                        columns={columns}
                        data={users}
                        rowKey="userId"
                        loading={isLoading}
                        pagination={
                            total > pageSize ? { current: page, pageSize, total, onChange: setPage } : undefined
                        }
                        emptyText="暂无用户数据"
                    />
                </div>

                {/* Modal */}
                <UserDetailModal
                    open={modalOpen}
                    user={selectedUser}
                    onClose={() => {
                        setModalOpen(false);
                        setSelectedUser(null);
                    }}
                    onSave={handleSaveStatus}
                    loading={updateStatusMutation.isPending}
                />
            </div>

            {/* Keyframe animations injected once */}
            <style>{`
        @keyframes pageIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes headerSlide { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes toolbarIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes cardIn { from { opacity: 0; transform: translateY(16px) scale(0.99); } to { opacity: 1; transform: translateY(0) scale(1); } }
      `}</style>
        </div>
    );
}
