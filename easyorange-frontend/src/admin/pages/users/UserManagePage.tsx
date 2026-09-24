import { Eye, Users } from 'lucide-react';
import { useCallback, useState } from 'react';
import { Button } from '@/components/ui/button';
import { usePagination } from '@/hooks/usePagination';
import { formatDate } from '@/utils/format';
import { AdminFilterField, AdminSearchInput, AdminToolbar } from '../../components/AdminControls';
import { AdminCard, AdminErrorBanner, AdminPage, AdminPageHeader, ToolbarDivider } from '../../components/AdminPage';
import { AdminTable, type Column } from '../../components/AdminTable';
import { linkButton, monoText, mutedText } from '../../components/admin-theme';
import { pickAvatarGradient } from '../../components/avatarGradient';
import { StatusBadge } from '../../components/StatusBadge';
import { useAdminUsers, useUpdateUserStatus } from '../../hooks';
import { notify } from '../../notify';
import type { AdminUser } from '../../types/admin';
import { UserDetailModal } from './UserDetailModal';

const STATUS_FILTER_OPTIONS = [
    { value: '', label: '全部状态' },
    { value: 'NORMAL', label: '正常' },
    { value: 'DISABLED', label: '禁用' },
    { value: 'LOCKED', label: '锁定' },
];

const USER_TYPE_FILTER_OPTIONS = [
    { value: '', label: '全部类型' },
    { value: '01', label: '学生' },
    { value: '02', label: '教师' },
];

export default function UserManagePage() {
    const [keyword, setKeyword] = useState('');
    const [searchInput, setSearchInput] = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [userTypeFilter, setUserTypeFilter] = useState('');
    const {
        pageNum: page,
        pageSize,
        goTo,
    } = usePagination({
        resetDeps: [keyword, statusFilter, userTypeFilter],
    });
    const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
    const [modalOpen, setModalOpen] = useState(false);

    const { data, isLoading, isError, error, refetch } = useAdminUsers({
        pageNum: page,
        pageSize,
        keyword: keyword || undefined,
        status: statusFilter || undefined,
        userType: userTypeFilter || undefined,
    });

    const updateStatusMutation = useUpdateUserStatus();

    const handleSearch = useCallback(() => {
        setKeyword(searchInput);
        goTo(1);
    }, [searchInput, goTo]);

    const handleViewDetail = useCallback((user: AdminUser) => {
        setSelectedUser(user);
        setModalOpen(true);
    }, []);

    const handleSaveStatus = useCallback(
        async (status: string) => {
            if (!selectedUser) {
                return;
            }
            try {
                await updateStatusMutation.mutateAsync({
                    id: selectedUser.userId,
                    data: { status },
                });
                notify.success(`已更新「${selectedUser.username}」的状态`);
                setModalOpen(false);
                setSelectedUser(null);
            } catch (e) {
                // 不关弹窗：用户看得到失败原因，可以改回原状态重试
                notify.failure(e, '状态更新失败，请稍后重试');
                throw e;
            }
        },
        [selectedUser, updateStatusMutation]
    );

    const columns: Column<AdminUser>[] = [
        {
            key: 'username',
            title: '用户',
            render: (_value, record) => (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', minWidth: 0 }}>
                    <span
                        aria-hidden="true"
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
                            fontFamily: 'var(--admin-font-title)',
                            background: pickAvatarGradient(record.userId ?? ''),
                            flexShrink: 0,
                        }}
                    >
                        {(record.nickname || record.username || '?').charAt(0).toUpperCase()}
                    </span>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 2, minWidth: 0 }}>
                        <span style={{ fontWeight: 600, color: 'var(--admin-ink)', fontSize: '0.875rem' }}>
                            {record.username}
                        </span>
                        {record.nickname ? <span style={mutedText}>{record.nickname}</span> : null}
                    </div>
                </div>
            ),
        },
        {
            key: 'email',
            title: '邮箱',
            render: value => <span style={monoText}>{(value as string) || '未绑定'}</span>,
        },
        {
            key: 'userType',
            title: '类型',
            // 此前 fallback 带 emoji、有描述时又不带，同一列随数据完整度换表现形式
            render: (_value, record) => (
                <span
                    style={{
                        fontWeight: 600,
                        fontSize: '0.82rem',
                        color: record.userType === '01' ? 'var(--status-info)' : 'var(--plum-600)',
                    }}
                >
                    {record.userTypeDesc || (record.userType === '01' ? '学生' : '教师')}
                </span>
            ),
        },
        {
            key: 'status',
            title: '状态',
            render: (_value, record) => <StatusBadge status={record.status ?? ''} type="user" />,
        },
        {
            key: 'createTime',
            title: '注册时间',
            sortable: true,
            render: value => <span style={mutedText}>{formatDate(value as string, 'date')}</span>,
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
                    className="h-auto min-h-0"
                    style={linkButton()}
                >
                    <Eye size={14} aria-hidden="true" />
                    详情
                </Button>
            ),
        },
    ];

    const users = data?.records ?? [];
    const total = data?.total ?? 0;

    return (
        <AdminPage>
            <AdminErrorBanner
                message={isError ? error?.message || '无法连接到服务器，请检查后端服务是否启动' : null}
                onRetry={() => refetch()}
                retrying={isLoading}
            />

            <AdminPageHeader
                icon={<Users size={17} />}
                title="用户管理"
                description="管理平台所有注册用户，查看详情或调整状态"
            />

            <AdminCard>
                <div style={{ padding: '0.9rem 1.15rem' }}>
                    <AdminToolbar>
                        <AdminSearchInput
                            value={searchInput}
                            onChange={setSearchInput}
                            onSubmit={handleSearch}
                            placeholder="搜索用户名 / 邮箱"
                            loading={isLoading}
                        />
                        <AdminFilterField
                            label="状态"
                            options={STATUS_FILTER_OPTIONS}
                            value={statusFilter}
                            onChange={val => {
                                setStatusFilter(val);
                                goTo(1);
                            }}
                        />
                        <AdminFilterField
                            label="类型"
                            options={USER_TYPE_FILTER_OPTIONS}
                            value={userTypeFilter}
                            onChange={val => {
                                setUserTypeFilter(val);
                                goTo(1);
                            }}
                        />
                        <ToolbarDivider />
                        <div style={{ flex: 1 }} />
                        <span style={mutedText}>
                            共 <strong style={{ color: 'var(--admin-ink)' }}>{total.toLocaleString()}</strong> 位用户
                        </span>
                    </AdminToolbar>
                </div>
            </AdminCard>

            <AdminCard grow>
                <AdminTable
                    columns={columns}
                    data={users}
                    rowKey="userId"
                    loading={isLoading}
                    // 失败时不让表格退化成「暂无用户数据」——两者同屏出现会互相矛盾
                    error={isError ? error : null}
                    onRetry={() => refetch()}
                    pagination={total > pageSize ? { current: page, pageSize, total, onChange: goTo } : undefined}
                    emptyText="暂无用户数据"
                />
            </AdminCard>

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
        </AdminPage>
    );
}
