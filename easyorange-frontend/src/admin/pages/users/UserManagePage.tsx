import { Eye, Users } from 'lucide-react';
import { useCallback, useState } from 'react';
import { Button } from '@/components/ui/button';
import { usePagination } from '@/hooks/usePagination';
import { formatDate } from '@/utils/format';
import { AdminFilterField, AdminSearchInput } from '../../components/AdminControls';
import { AdminListCard, AdminListCount, AdminPage, AdminPageHeader } from '../../components/AdminPage';
import { AdminTable, type Column } from '../../components/AdminTable';
import { pickAvatarGradient } from '../../components/avatarGradient';
import {
    StatusBadge,
    statusFilterOptions,
    userTypeColor,
    userTypeFilterOptions,
    userTypeLabel,
} from '../../components/StatusBadge';
import { useAdminUsers, useUpdateUserStatus } from '../../hooks';
import { notify } from '../../notify';
import type { AdminUser } from '../../types/admin';
import { UserDetailModal } from './UserDetailModal';

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
                <div className="admin-identity">
                    <span
                        aria-hidden="true"
                        className="admin-avatar"
                        style={{ background: pickAvatarGradient(record.userId ?? '') }}
                    >
                        {(record.nickname || record.username || '?').charAt(0).toUpperCase()}
                    </span>
                    <div className="admin-identity-text">
                        <span className="admin-cell-strong">{record.username}</span>
                        {record.nickname ? <span className="admin-muted">{record.nickname}</span> : null}
                    </div>
                </div>
            ),
        },
        {
            key: 'email',
            title: '邮箱',
            render: value => <span className="admin-mono">{(value as string) || '未绑定'}</span>,
        },
        {
            key: 'userType',
            title: '类型',
            render: (_value, record) => (
                <span className="admin-cell-accent" style={{ color: userTypeColor(record.userType) }}>
                    {userTypeLabel(record.userType, record.userTypeDesc)}
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
            render: value => <span className="admin-muted">{formatDate(value as string, 'date')}</span>,
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
                    className="h-auto min-h-0 admin-link-button"
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
            <AdminPageHeader
                icon={<Users size={17} />}
                title="用户管理"
                description="管理平台所有注册用户，查看详情或调整状态"
            />

            <AdminListCard
                title="用户列表"
                icon={<Users size={17} />}
                // 失败时不报「共 0 位」——那会被读成真的没有用户
                count={
                    isError ? undefined : <AdminListCount prefix="共" count={total.toLocaleString()} suffix="位用户" />
                }
                toolbar={
                    <>
                        <AdminSearchInput
                            value={searchInput}
                            onChange={setSearchInput}
                            onSubmit={handleSearch}
                            placeholder="搜索用户名 / 邮箱"
                            loading={isLoading}
                        />
                        <AdminFilterField
                            label="状态"
                            options={statusFilterOptions('user')}
                            value={statusFilter}
                            onChange={val => {
                                setStatusFilter(val);
                                goTo(1);
                            }}
                        />
                        <AdminFilterField
                            label="类型"
                            options={userTypeFilterOptions()}
                            value={userTypeFilter}
                            onChange={val => {
                                setUserTypeFilter(val);
                                goTo(1);
                            }}
                        />
                    </>
                }
            >
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
            </AdminListCard>

            <UserDetailModal
                // key 绑定用户：切换用户时整棵表单重建，草稿状态不会带着上一位用户的旧选择
                key={selectedUser?.userId ?? 'closed'}
                open={selectedUser !== null}
                user={selectedUser}
                onClose={() => setSelectedUser(null)}
                onSave={handleSaveStatus}
                loading={updateStatusMutation.isPending}
            />
        </AdminPage>
    );
}
