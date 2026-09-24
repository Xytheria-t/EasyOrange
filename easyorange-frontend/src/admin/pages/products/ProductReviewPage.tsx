import { ClipboardCheck, Eye, Package } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { usePagination } from '@/hooks/usePagination';
import type { ProductStatus } from '@/types';
import { formatRelativeTime } from '@/utils/format';
import { AdminFilterField, AdminSearchInput, AdminToolbar } from '../../components/AdminControls';
import { AdminCard, AdminPage, AdminPageHeader, ToolbarDivider } from '../../components/AdminPage';
import { AdminTable, type Column } from '../../components/AdminTable';
import { StatusBadge, statusFilterOptions } from '../../components/StatusBadge';
import { useAdminCategories } from '../../hooks/useAdminCategories';
import { useBatchAuditProducts } from '../../hooks/useAdminProductAudit';
import { useAdminProducts } from '../../hooks/useAdminProducts';
import { notify } from '../../notify';
import type { AdminProduct } from '../../types/admin';
import { BatchAuditBar } from './BatchAuditBar';
import { ProductDetailDrawer } from './ProductDetailDrawer';

/** 后端审核动作：1 通过 / 2 驳回（BatchAuditRequest.AuditItem.action） */
const AUDIT_APPROVE = 1 as const;
const AUDIT_REJECT = 2 as const;

export default function ProductReviewPage() {
    const [keyword, setKeyword] = useState('');
    const [searchInput, setSearchInput] = useState('');
    const [statusFilter, setStatusFilter] = useState<ProductStatus | ''>('');
    const [categoryFilter, setCategoryFilter] = useState('');
    const {
        pageNum: page,
        pageSize,
        goTo,
    } = usePagination({
        resetDeps: [keyword, statusFilter, categoryFilter],
    });
    const [selectedProductId, setSelectedProductId] = useState<string | null>(null);

    // 批量勾选：跨筛选/翻页后残留的勾选没有意义，翻页与改筛选时统一清空
    const [selectedIds, setSelectedIds] = useState<ReadonlySet<string>>(new Set());
    const [rejectOpen, setRejectOpen] = useState(false);
    const [rejectReason, setRejectReason] = useState('');

    const { data, isLoading, isError, error, refetch } = useAdminProducts({
        pageNum: page,
        pageSize,
        keyword: keyword || undefined,
        status: statusFilter || undefined,
        categoryId: categoryFilter || undefined,
    });

    const batchAudit = useBatchAuditProducts();

    // 分类选项取真实分类树：此前写死 7 个英文 ID，分类改名 / 新增后筛选直接失效
    const { data: categories } = useAdminCategories();
    const categoryOptions = useMemo(
        () => [
            { value: '', label: '全部分类' },
            ...(categories ?? []).map(c => ({ value: c.categoryId, label: c.name })),
        ],
        [categories]
    );

    const products = data?.records ?? [];
    const total = data?.total ?? 0;

    const handleSearch = () => {
        setKeyword(searchInput);
        setSelectedIds(new Set());
        goTo(1);
    };

    /** 翻页同时清空勾选：批量审核针对「当前看到的这批」，翻页即换一批 */
    const handlePageChange = (nextPage: number) => {
        setSelectedIds(new Set());
        goTo(nextPage);
    };

    const handleViewDetail = (product: AdminProduct) => {
        setSelectedProductId(product.productId || null);
    };

    /** 逐条提交（后端非全有全无），返回体带每条成败；完成即清空勾选，列表由 hook 自动失效重取 */
    const runBatch = async (action: typeof AUDIT_APPROVE | typeof AUDIT_REJECT, reason?: string) => {
        const items = [...selectedIds].map(productId => ({ productId, action, reason }));
        try {
            const res = await batchAudit.mutateAsync({ items });
            if (res.failed === 0) {
                notify.success(`已${action === AUDIT_APPROVE ? '通过' : '驳回'} ${res.success} 件商品`);
            } else {
                notify.error(
                    `批量审核完成：成功 ${res.success} 件，失败 ${res.failed} 件${res.errors[0] ? `。${res.errors[0]}` : ''}`
                );
            }
            setSelectedIds(new Set());
            setRejectOpen(false);
            setRejectReason('');
        } catch (e) {
            // 整体请求失败（网络/鉴权）：勾选保留，用户可直接重试
            notify.failure(e, '批量审核失败，请稍后重试');
        }
    };

    const columns: Column<AdminProduct>[] = [
        {
            key: 'mainImage',
            title: '图片',
            render: (_value, record) => {
                const src = record.mainImage || record.images?.[0];
                return (
                    <div
                        style={{
                            width: 46,
                            height: 46,
                            borderRadius: 12,
                            overflow: 'hidden',
                            flexShrink: 0,
                            background:
                                'linear-gradient(135deg, color-mix(in srgb, var(--admin-chart-4) 12%, var(--admin-surface-solid)), color-mix(in srgb, var(--admin-chart-4) 22%, var(--admin-surface-solid)))',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            border: '1px solid var(--admin-accent-soft-border)',
                        }}
                    >
                        {src ? (
                            <img
                                src={src}
                                alt={`${record.name ?? '商品'}的缩略图`}
                                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                                loading="lazy"
                                decoding="async"
                            />
                        ) : (
                            <Package size={18} aria-hidden="true" style={{ color: 'var(--admin-faint)' }} />
                        )}
                    </div>
                );
            },
        },
        {
            key: 'name',
            title: '商品名称',
            render: value => (
                <span
                    className="truncate block"
                    style={{ fontWeight: 600, color: 'var(--admin-ink)', fontSize: '0.875rem', maxWidth: 220 }}
                >
                    {value as string}
                </span>
            ),
        },
        {
            key: 'price',
            title: '价格',
            render: value => <span className="admin-price">¥{Number(value ?? 0).toFixed(2)}</span>,
        },
        {
            key: 'sellerName',
            title: '资产方',
            render: value => <span className="admin-muted">{value as string}</span>,
        },
        {
            key: 'status',
            title: '状态',
            render: (_value, record) => (
                <StatusBadge status={record.status ?? record.statusDesc ?? ''} type="product" />
            ),
        },
        {
            key: 'createTime',
            title: '发布时间',
            render: value => <span className="admin-muted">{formatRelativeTime(value as string)}</span>,
        },
        {
            key: 'actions',
            title: '操作',
            render: (_: unknown, record: AdminProduct) => (
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
                    审核
                </Button>
            ),
        },
    ];

    return (
        <AdminPage>
            <AdminPageHeader
                icon={<ClipboardCheck size={17} />}
                title="商品审核"
                description="审核待上架商品，管理通过与驳回"
            />

            <AdminCard>
                <div style={{ padding: '0.9rem 1.15rem' }}>
                    <AdminToolbar>
                        <AdminSearchInput
                            value={searchInput}
                            onChange={setSearchInput}
                            onSubmit={handleSearch}
                            placeholder="搜索商品名称"
                            loading={isLoading}
                        />
                        <AdminFilterField
                            label="状态"
                            options={statusFilterOptions('product')}
                            value={statusFilter}
                            onChange={value => {
                                setStatusFilter(value as ProductStatus | '');
                                setSelectedIds(new Set());
                                goTo(1);
                            }}
                        />
                        <AdminFilterField
                            label="分类"
                            options={categoryOptions}
                            value={categoryFilter}
                            onChange={value => {
                                setCategoryFilter(value);
                                setSelectedIds(new Set());
                                goTo(1);
                            }}
                        />
                        <ToolbarDivider />
                        <div style={{ flex: 1 }} />
                        {isError ? null : (
                            <span className="admin-muted">
                                共 <strong style={{ color: 'var(--admin-ink)' }}>{total.toLocaleString()}</strong>{' '}
                                件商品
                            </span>
                        )}
                    </AdminToolbar>
                </div>
            </AdminCard>

            <AdminCard grow>
                <AdminTable
                    columns={columns}
                    data={products}
                    rowKey="productId"
                    loading={isLoading}
                    error={isError ? error : null}
                    onRetry={() => refetch()}
                    pagination={
                        total > pageSize ? { current: page, pageSize, total, onChange: handlePageChange } : undefined
                    }
                    onRowClick={handleViewDetail}
                    selection={{
                        selectedKeys: selectedIds,
                        onChange: keys => setSelectedIds(new Set(keys)),
                        // 只有待审核的商品能批量操作：对已上架商品「通过」是无效动作
                        isSelectable: record => record.status === 'PENDING_REVIEW',
                        noun: '商品',
                        labelOf: record => record.name ?? '',
                    }}
                    emptyText="暂无商品数据"
                />
            </AdminCard>

            <BatchAuditBar
                count={selectedIds.size}
                submitting={batchAudit.isPending}
                onApprove={() => void runBatch(AUDIT_APPROVE)}
                onReject={() => setRejectOpen(true)}
                onClear={() => setSelectedIds(new Set())}
            />

            <Dialog
                open={rejectOpen}
                onOpenChange={open => {
                    if (!open && !batchAudit.isPending) {
                        setRejectOpen(false);
                    }
                }}
            >
                <DialogContent className="max-w-md">
                    <DialogHeader>
                        <DialogTitle>批量驳回 {selectedIds.size} 件商品</DialogTitle>
                        <DialogDescription>驳回原因会写入审核日志，卖家在商品详情里能看到</DialogDescription>
                    </DialogHeader>
                    <Textarea
                        value={rejectReason}
                        onChange={e => setRejectReason(e.target.value)}
                        placeholder="如：图片与实物不符 / 类目放错 / 描述含违禁词"
                        rows={4}
                        maxLength={500}
                        disabled={batchAudit.isPending}
                    />
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setRejectOpen(false)} disabled={batchAudit.isPending}>
                            取消
                        </Button>
                        <Button
                            onClick={() => void runBatch(AUDIT_REJECT, rejectReason.trim() || undefined)}
                            isLoading={batchAudit.isPending}
                            loadingText="驳回中"
                            className="admin-action-btn admin-action-btn--reject"
                        >
                            确认驳回
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <ProductDetailDrawer
                open={selectedProductId !== null}
                productId={selectedProductId}
                onClose={() => setSelectedProductId(null)}
                onSuccess={() => {
                    // 单件审核也会让勾选集过期（刚通过的还在集合里），一并清掉
                    setSelectedIds(new Set());
                    refetch();
                }}
            />
        </AdminPage>
    );
}
