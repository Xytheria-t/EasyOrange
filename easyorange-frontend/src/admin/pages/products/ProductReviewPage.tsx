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
import { AdminFilterField, AdminSearchInput } from '../../components/AdminControls';
import { AdminListCard, AdminListCount, AdminPage, AdminPageHeader } from '../../components/AdminPage';
import { AdminTable, type Column } from '../../components/AdminTable';
import { StatusBadge, statusFilterOptions } from '../../components/StatusBadge';
import { useAdminCategoryTree } from '../../hooks/useAdminCategories';
import { useBatchAuditProducts } from '../../hooks/useAdminProductAudit';
import { useAdminProducts } from '../../hooks/useAdminProducts';
import { notify } from '../../notify';
import type { AdminProduct, CategoryResponse } from '../../types/admin';
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

    // 分类选项取真实分类树的**全层级**叶子：useAdminCategories 打的是 /admin/categories 且不传 parentId，
    // 后端只返回一级分类，而商品挂在二级分类上 —— 配上限级筛选每个选项都是 0 条。后端按子树匹配。
    const { data: categoryTree } = useAdminCategoryTree();
    const categoryOptions = useMemo(() => {
        const leaves: { value: string; label: string }[] = [];
        const walk = (nodes: CategoryResponse[], depth: number) => {
            for (const node of nodes) {
                if (node.children.length > 0) {
                    walk(node.children, depth + 1);
                } else {
                    leaves.push({ value: node.id, label: `${'　'.repeat(depth)}${node.name}` });
                }
            }
        };
        walk(categoryTree ?? [], 0);
        return [{ value: '', label: '全部分类' }, ...leaves];
    }, [categoryTree]);

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
                    <div className="admin-media-tile">
                        {src ? (
                            <img src={src} alt={`${record.name ?? '商品'}的缩略图`} loading="lazy" decoding="async" />
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
                <span className="admin-cell-strong admin-cell-clamp" style={{ maxWidth: 220 }}>
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

            <AdminListCard
                title="商品列表"
                icon={<ClipboardCheck size={17} />}
                count={
                    isError ? undefined : <AdminListCount prefix="共" count={total.toLocaleString()} suffix="件商品" />
                }
                toolbar={
                    <>
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
                    </>
                }
            >
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
            </AdminListCard>

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
