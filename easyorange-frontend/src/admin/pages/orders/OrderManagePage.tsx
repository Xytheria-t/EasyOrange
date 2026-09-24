import { Eye, ReceiptText } from 'lucide-react';
import { useCallback, useState } from 'react';
import { Button } from '@/components/ui/button';
import { usePagination } from '@/hooks/usePagination';
import type { OrderStatus } from '@/types';
import { formatDate } from '@/utils/format';
import { AdminFilterField, AdminSearchInput, AdminToolbar } from '../../components/AdminControls';
import { AdminCard, AdminPage, AdminPageHeader, ToolbarDivider } from '../../components/AdminPage';
import { AdminTable, type Column } from '../../components/AdminTable';
import { linkButton, monoText, mutedText, priceText } from '../../components/admin-theme';
import { StatusBadge } from '../../components/StatusBadge';
import { useAdminOrders } from '../../hooks';
import type { AdminOrder } from '../../types/admin';
import { OrderDetailModal } from './OrderDetailModal';

const STATUS_FILTER_OPTIONS = [
    { value: '', label: '全部状态' },
    { value: 'PENDING_PAYMENT', label: '待付款' },
    { value: 'PAID', label: '待发货' },
    { value: 'SHIPPED', label: '已发货' },
    { value: 'COMPLETED', label: '已完成' },
    { value: 'CANCELLED', label: '已取消' },
    { value: 'REFUNDED', label: '退款中' },
];

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
            render: value => (
                <span style={{ ...monoText, fontWeight: 600, color: 'var(--admin-ink)' }}>{value as string}</span>
            ),
        },
        {
            key: 'items',
            title: '商品',
            render: value => {
                const items = value as AdminOrder['items'];
                const firstName = items?.[0]?.productName || '—';
                const multi = items && items.length > 1;
                return (
                    <span
                        style={{
                            fontWeight: 500,
                            color: 'var(--admin-ink-soft)',
                            fontSize: '0.87rem',
                            maxWidth: 180,
                            display: 'inline-block',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            verticalAlign: 'bottom',
                        }}
                    >
                        {firstName}
                        {multi ? ` 等${items.length}件` : ''}
                    </span>
                );
            },
        },
        {
            key: 'buyerName',
            title: '认领方',
            render: value => <span style={mutedText}>{value as string}</span>,
        },
        {
            key: 'sellerName',
            title: '资产方',
            render: value => <span style={mutedText}>{value as string}</span>,
        },
        {
            key: 'totalAmount',
            title: '金额',
            render: value => <span style={priceText}>¥{Number(value ?? 0).toFixed(2)}</span>,
        },
        {
            key: 'status',
            title: '状态',
            render: value => <StatusBadge status={value as string} type="order" />,
        },
        {
            key: 'createTime',
            title: '下单时间',
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
                    onClick={() => setDetailOrderId(record.orderId)}
                    className="h-auto min-h-0"
                    style={linkButton()}
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

            <AdminCard>
                <div style={{ padding: '0.9rem 1.15rem' }}>
                    <AdminToolbar>
                        <AdminSearchInput
                            value={searchInput}
                            onChange={setSearchInput}
                            onSubmit={handleSearch}
                            placeholder="搜索订单号 / 认领方"
                            loading={isLoading}
                        />
                        <AdminFilterField
                            label="状态"
                            options={STATUS_FILTER_OPTIONS}
                            value={statusFilter}
                            onChange={val => {
                                setStatusFilter(val as OrderStatus | '');
                                goTo(1);
                            }}
                        />
                        <ToolbarDivider />
                        <div style={{ flex: 1 }} />
                        {/* 失败时不报「共 0 笔」——那会被读成真的没有订单 */}
                        {isError ? null : (
                            <span style={mutedText}>
                                共 <strong style={{ color: 'var(--admin-ink)' }}>{total.toLocaleString()}</strong>{' '}
                                笔订单
                            </span>
                        )}
                    </AdminToolbar>
                </div>
            </AdminCard>

            <AdminCard grow>
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
            </AdminCard>

            <OrderDetailModal
                open={detailOrderId !== null}
                orderId={detailOrderId}
                onClose={() => setDetailOrderId(null)}
            />
        </AdminPage>
    );
}
