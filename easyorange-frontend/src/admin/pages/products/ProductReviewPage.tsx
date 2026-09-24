import { ClipboardCheck, Eye, Package } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { usePagination } from '@/hooks/usePagination';
import type { ProductStatus } from '@/types';
import { AdminFilterField, AdminSearchInput, AdminToolbar } from '../../components/AdminControls';
import { AdminCard, AdminErrorBanner, AdminPage, AdminPageHeader, ToolbarDivider } from '../../components/AdminPage';
import { AdminTable, type Column } from '../../components/AdminTable';
import { linkButton, mutedText, priceText } from '../../components/admin-theme';
import { StatusBadge } from '../../components/StatusBadge';
import { useAdminCategories } from '../../hooks/useAdminCategories';
import { useAdminProducts } from '../../hooks/useAdminProducts';
import type { AdminProduct } from '../../types/admin';
import { ProductDetailDrawer } from './ProductDetailDrawer';

const statusOptions: { value: ProductStatus | ''; label: string }[] = [
    { value: '', label: '全部状态' },
    { value: 'PENDING_REVIEW', label: '待审核' },
    { value: 'REJECTED', label: '已驳回' },
    { value: 'DRAFT', label: '草稿' },
    { value: 'ONLINE', label: '上架' },
    { value: 'SOLD', label: '已售' },
    { value: 'OFFLINE', label: '下架' },
];

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
    const [drawerOpen, setDrawerOpen] = useState(false);

    const { data, isLoading, isError, error, refetch } = useAdminProducts({
        pageNum: page,
        pageSize,
        keyword: keyword || undefined,
        status: statusFilter || undefined,
        categoryId: categoryFilter || undefined,
    });

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
        goTo(1);
    };

    const handleViewDetail = (product: AdminProduct) => {
        setSelectedProductId(product.productId || null);
        setDrawerOpen(true);
    };

    const formatTime = (timeString: string) => {
        const date = new Date(timeString);
        const diff = Date.now() - date.getTime();
        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(diff / 3600000);
        const days = Math.floor(diff / 86400000);
        if (minutes < 1) {
            return '刚刚';
        }
        if (minutes < 60) {
            return `${minutes}分钟前`;
        }
        if (hours < 24) {
            return `${hours}小时前`;
        }
        if (days < 7) {
            return `${days}天前`;
        }
        return date.toLocaleDateString('zh-CN');
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
                            background: 'linear-gradient(135deg, #FDF6EC, #FDE8D4)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            border: '1px solid rgba(249,115,22,0.08)',
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
            render: value => <span style={priceText}>¥{Number(value ?? 0).toFixed(2)}</span>,
        },
        {
            key: 'sellerName',
            title: '资产方',
            render: value => <span style={mutedText}>{value as string}</span>,
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
            sortable: true,
            render: value => <span style={mutedText}>{formatTime(value as string)}</span>,
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
                    className="h-auto min-h-0"
                    style={linkButton()}
                >
                    <Eye size={14} aria-hidden="true" />
                    审核
                </Button>
            ),
        },
    ];

    return (
        <AdminPage>
            <AdminErrorBanner
                message={isError ? error?.message || '无法连接到服务器，请检查后端服务是否启动' : null}
                onRetry={() => refetch()}
                retrying={isLoading}
            />

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
                            options={statusOptions}
                            value={statusFilter}
                            onChange={value => {
                                setStatusFilter(value as ProductStatus | '');
                                goTo(1);
                            }}
                        />
                        <AdminFilterField
                            label="分类"
                            options={categoryOptions}
                            value={categoryFilter}
                            onChange={value => {
                                setCategoryFilter(value);
                                goTo(1);
                            }}
                        />
                        <ToolbarDivider />
                        <div style={{ flex: 1 }} />
                        {isError ? null : (
                            <span style={mutedText}>
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
                    pagination={total > pageSize ? { current: page, pageSize, total, onChange: goTo } : undefined}
                    onRowClick={handleViewDetail}
                    emptyText="暂无商品数据"
                />
            </AdminCard>

            <ProductDetailDrawer
                open={drawerOpen}
                productId={selectedProductId}
                onClose={() => {
                    setDrawerOpen(false);
                    setSelectedProductId(null);
                }}
                onSuccess={() => refetch()}
            />
        </AdminPage>
    );
}
