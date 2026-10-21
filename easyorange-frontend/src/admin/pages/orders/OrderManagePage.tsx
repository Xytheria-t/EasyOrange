import { Eye, ReceiptText } from 'lucide-react';
import type { CSSProperties } from 'react';
import { useCallback, useState } from 'react';
import { Button } from '@/components/ui/button';
import { usePagination } from '@/hooks/usePagination';
import type { OrderStatus } from '@/types';
import { formatDate } from '@/utils/format';
import { AdminFilterField, AdminSearchInput } from '../../components/AdminControls';
import { AdminListCard, AdminListCount, AdminPage, AdminPageHeader } from '../../components/AdminPage';
import { AdminTable, type Column } from '../../components/AdminTable';
import { StatusBadge, statusFilterOptions } from '../../components/StatusBadge';
import { useAdminOrders } from '../../hooks';
import type { AdminOrder } from '../../types/admin';
import { OrderDetailModal } from './OrderDetailModal';

export default function OrderManagePage() {
    const [statusFilter, setStatusFilter] = useState<OrderStatus | ''>('');
    const [keyword, setKeyword] = useState('');
    const [searchInput, setSearchInput] = useState('');
    const {
        pageNum: page,
        pageSize,
        goTo,
    } = usePagination({
        resetDeps: [keyword, statusFilter],
    });
    const [detailOrderId, setDetailOrderId] = useState<string | null>(null);

    const { data, isLoading, isError, error, refetch } = useAdminOrders({
        pageNum: page,
        pageSize,
        orderNo: keyword || undefined,
        status: statusFilter || undefined,
    });

    const handleSearch = useCallback(() => {
        setKeyword(searchInput);
        goTo(1);
    }, [searchInput, goTo]);

    const total = data?.total ?? 0;

    const columns: Column<AdminOrder>[] = [
        {
            key: 'orderNo',
            title: '订单号',
            render: value => <span className="admin-cell-strong admin-mono">{value as string}</span>,
        },
        {
            key: 'items',
            title: '商品',
            render: value => {
                const items = value as AdminOrder['items'];
                const firstName = items?.[0]?.productName || '—';
                const multi = items && items.length > 1;
                return (
                    <span className="admin-cell-sub" style={{ '--clamp-w': 180 } as CSSProperties}>
                        {firstName}
                        {multi ? ` 等${items.length}件` : ''}
                    </span>
                );
            },
        },
        {
            key: 'buyerName',
            title: '认领方',
            render: value => <span className="admin-muted">{value as string}</span>,
        },
        {
            key: 'sellerName',
            title: '资产方',
            render: value => <span className="admin-muted">{value as string}</span>,
        },
        {
            key: 'totalAmount',
            title: '金额',
            render: value => <span className="admin-price">¥{Number(value ?? 0).toFixed(2)}</span>,
        },
        {
            key: 'status',
            title: '状态',
            render: value => <StatusBadge status={value as string} type="order" />,
        },
        {
            key: 'createTime',
            title: '下单时间',
            render: value => <span className="admin-muted">{formatDate(value as string, 'date')}</span>,
        },
        {
            key: 'actions',
            title: '操作',
            render: (_, record) => (
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setDetailOrderId(record.orderId)}
                    className="h-auto min-h-0 admin-link-button"
                >
                    <Eye size={14} aria-hidden="true" />
                    详情
                </Button>
            ),
        },
    ];

    return (
        <AdminPage>
            <AdminPageHeader
                icon={<ReceiptText size={17} />}
                title="订单管理"
                description="查看和管理平台所有交易订单"
            />

            <AdminListCard
                title="订单列表"
                icon={<ReceiptText size={17} />}
                // 失败时不报「共 0 笔」——那会被读成真的没有订单
                count={
                    isError ? undefined : <AdminListCount prefix="共" count={total.toLocaleString()} suffix="笔订单" />
                }
                toolbar={
                    <>
                        <AdminSearchInput
                            value={searchInput}
                            onChange={setSearchInput}
                            onSubmit={handleSearch}
                            placeholder="搜索订单号"
                            loading={isLoading}
                        />
                        <AdminFilterField
                            label="状态"
                            options={statusFilterOptions('order')}
                            value={statusFilter}
                            onChange={val => {
                                setStatusFilter(val as OrderStatus | '');
                                goTo(1);
                            }}
                        />
                    </>
                }
            >
                <AdminTable
                    columns={columns}
                    data={data?.records ?? []}
                    rowKey="orderId"
                    loading={isLoading}
                    error={isError ? error : null}
                    onRetry={() => refetch()}
                    pagination={
                        total > pageSize ? { current: data?.current ?? 1, pageSize, total, onChange: goTo } : undefined
                    }
                    emptyText="暂无订单数据"
                />
            </AdminListCard>

            <OrderDetailModal
                open={detailOrderId !== null}
                orderId={detailOrderId}
                onClose={() => setDetailOrderId(null)}
            />
        </AdminPage>
    );
}
