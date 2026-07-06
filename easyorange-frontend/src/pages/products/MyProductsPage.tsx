import {
    CheckCircle,
    ChevronRight,
    Clock,
    Edit,
    Eye,
    Package,
    Plus,
    RefreshCw,
    ShoppingBag,
    XCircle,
} from 'lucide-react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import {
    Pagination,
    PaginationContent,
    PaginationItem,
    PaginationLink,
    PaginationNext,
    PaginationPrevious,
} from '@/components/ui/pagination';
import { cn } from '@/lib/utils';
import { PRODUCT_STATUS_CODE, STATUS_LABEL_MAP } from '@/constants/product';
import { useMyProducts } from '@/hooks/product/useProducts';
import type { Product, ProductStatus } from '@/types';

import '../orders/payment.css';
import './products-premium.css';

const STATUS_OPTIONS: { id: string; label: string; icon: typeof Package; status?: ProductStatus }[] = [
    { id: 'all', label: '全部', icon: Package },
    { id: 'ONLINE', label: '在售', icon: CheckCircle, status: 'ONLINE' },
    { id: 'PENDING_REVIEW', label: '审核中', icon: Clock, status: 'PENDING_REVIEW' },
    { id: 'DRAFT', label: '草稿', icon: Edit, status: 'DRAFT' },
    { id: 'REJECTED', label: '已驳回', icon: XCircle, status: 'REJECTED' },
    { id: 'OFFLINE', label: '已下架', icon: RefreshCw, status: 'OFFLINE' },
    { id: 'SOLD', label: '已售出', icon: ShoppingBag, status: 'SOLD' },
];

const STATUS_STYLE_MAP: Record<ProductStatus, { bg: string; text: string; border: string; glow: string; dot: string }> =
    {
        DRAFT: {
            bg: 'rgba(168, 160, 152, 0.08)',
            text: '#787068',
            border: 'rgba(168, 160, 152, 0.2)',
            glow: '0 0 20px rgba(168, 160, 152, 0.1)',
            dot: '#A8A098',
        },
        ONLINE: {
            bg: 'rgba(16, 185, 129, 0.08)',
            text: '#059669',
            border: 'rgba(16, 185, 129, 0.2)',
            glow: '0 0 20px rgba(16, 185, 129, 0.15)',
            dot: '#10B981',
        },
        SOLD: {
            bg: 'rgba(59, 130, 246, 0.08)',
            text: '#2563EB',
            border: 'rgba(59, 130, 246, 0.2)',
            glow: '0 0 20px rgba(59, 130, 246, 0.15)',
            dot: '#3B82F6',
        },
        OFFLINE: {
            bg: 'rgba(168, 160, 152, 0.08)',
            text: '#787068',
            border: 'rgba(168, 160, 152, 0.2)',
            glow: '0 0 20px rgba(168, 160, 152, 0.1)',
            dot: '#A8A098',
        },
        PENDING_REVIEW: {
            bg: 'rgba(251, 191, 36, 0.08)',
            text: '#D97706',
            border: 'rgba(251, 191, 36, 0.2)',
            glow: '0 0 20px rgba(251, 191, 36, 0.15)',
            dot: '#FBBF24',
        },
        REJECTED: {
            bg: 'rgba(244, 63, 94, 0.08)',
            text: '#E11D48',
            border: 'rgba(244, 63, 94, 0.2)',
            glow: '0 0 20px rgba(244, 63, 94, 0.15)',
            dot: '#F43F5E',
        },
    };

function MyProductsPage() {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('all');
    const [pageNum, setPageNum] = useState(1);
    const pageSize = 10;

    const queryParams = useMemo(() => {
        const tab = STATUS_OPTIONS.find(t => t.id === activeTab);
        const code = tab?.status
            ? Number(Object.entries(PRODUCT_STATUS_CODE).find(([, v]) => v === tab.status)?.[0])
            : undefined;
        return {
            pageNum,
            pageSize,
            ...(code !== undefined ? { status: code } : {}),
        };
    }, [activeTab, pageNum]);

    const { data, isLoading, isError, refetch } = useMyProducts(queryParams);

    const products = useMemo(() => data?.records ?? [], [data]);
    const totalPages = data?.pages ?? 1;

    return (
        <div className="orders-page-premium">
            <div className="orders-hero">
                <div className="orders-hero-bg" />
                <div className="orders-hero-content">
                    <h1 className="orders-hero-title">
                        <Package size={20} className="orders-hero-icon" />
                        我发布的商品
                    </h1>
                    <p className="orders-hero-subtitle">管理你发布的商品，追踪审核状态</p>
                </div>
            </div>

            <div className="orders-tabs-premium">
                {STATUS_OPTIONS.map((tab, index) => {
                    const Icon = tab.icon;
                    const isActive = activeTab === tab.id;
                    return (
                        <Button
                            key={tab.id}
                            variant="ghost"
                            onClick={() => {
                                setActiveTab(tab.id);
                                setPageNum(1);
                            }}
                            className={`orders-tab-item ${isActive ? 'orders-tab-active' : ''}`}
                            style={{ animationDelay: `${index * 60}ms` }}
                        >
                            <Icon size={15} className="orders-tab-icon" />
                            <span>{tab.label}</span>
                            {isActive && <div className="orders-tab-indicator" />}
                        </Button>
                    );
                })}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem' }}>
                <Button onClick={() => navigate('/publish')} className="orders-empty-cta" style={{ animation: 'none' }}>
                    <Plus size={16} />
                    发布新商品
                </Button>
            </div>

            {isLoading && (
                <div className="orders-loading">
                    <div className="orders-loading-spinner">
                        <RefreshCw size={28} />
                    </div>
                    <span className="orders-loading-text">正在加载商品...</span>
                </div>
            )}

            {isError && (
                <div className="orders-error-card">
                    <div className="orders-error-icon">!</div>
                    <p className="orders-error-text">加载失败，请稍后重试</p>
                    <Button onClick={() => refetch()} className="orders-error-btn">
                        重新加载
                    </Button>
                </div>
            )}

            {!isLoading && !isError && products.length === 0 && (
                <div className="orders-empty-premium">
                    <div className="orders-empty-visual">
                        <div className="orders-empty-orb orders-empty-orb-1" />
                        <div className="orders-empty-orb orders-empty-orb-2" />
                        <div className="orders-empty-icon-wrap">
                            <Package size={40} />
                        </div>
                    </div>
                    <h3 className="orders-empty-title">还没有提交资产</h3>
                    <p className="orders-empty-desc">
                        开始托管你的第一件资产吧
                        <br />让 AI 帮你定价、写描述、发布上线
                    </p>
                    <Button onClick={() => navigate('/publish')} className="orders-empty-cta">
                        提交资产
                        <ChevronRight size={16} />
                    </Button>
                </div>
            )}

            {!isLoading && !isError && products.length > 0 && (
                <>
                    <div className="orders-list-premium">
                        {products.map((product, index) => (
                            <MyProductCard
                                key={product.id}
                                product={product}
                                onClick={() => navigate(`/products/${product.id}`)}
                                onEdit={() => navigate(`/products/${product.id}/edit`)}
                                index={index}
                            />
                        ))}
                    </div>
                    {totalPages > 1 && (
                        <Pagination className="mt-6 mb-4">
                            <PaginationContent>
                                <PaginationItem>
                                    <PaginationPrevious
                                        onClick={() => setPageNum(p => Math.max(1, p - 1))}
                                        className={cn(pageNum <= 1 && 'pointer-events-none opacity-40')}
                                    />
                                </PaginationItem>
                                {Array.from({ length: totalPages }, (_, i) => i + 1).map(p => (
                                    <PaginationItem key={p}>
                                        <PaginationLink
                                            isActive={p === pageNum}
                                            onClick={() => setPageNum(p)}
                                        >
                                            {p}
                                        </PaginationLink>
                                    </PaginationItem>
                                ))}
                                <PaginationItem>
                                    <PaginationNext
                                        onClick={() => setPageNum(p => p + 1)}
                                        className={cn(pageNum >= totalPages && 'pointer-events-none opacity-40')}
                                    />
                                </PaginationItem>
                            </PaginationContent>
                        </Pagination>
                    )}
                </>
            )}
        </div>
    );
}

export default MyProductsPage;

interface MyProductCardProps {
    product: Product;
    onClick: () => void;
    onEdit: () => void;
    index: number;
}

function MyProductCard({ product, onClick, onEdit, index }: MyProductCardProps) {
    const statusKey = product.status;
    const statusLabel = STATUS_LABEL_MAP[statusKey] ?? statusKey;
    const statusStyle = STATUS_STYLE_MAP[statusKey] ?? STATUS_STYLE_MAP.DRAFT;

    return (
        <div className="order-card-premium" style={{ animationDelay: `${index * 80}ms` }}>
            <div className="order-card-shine" />

            <div className="order-card-header-premium">
                <span className="order-card-order-no" style={{ fontSize: '0.8rem' }}>
                    {product.createTime}
                </span>
                <span
                    className="order-card-status-badge"
                    style={{
                        background: statusStyle.bg,
                        color: statusStyle.text,
                        borderColor: statusStyle.border,
                        boxShadow: statusStyle.glow,
                    }}
                >
                    <span className="order-card-status-dot" style={{ background: statusStyle.dot }} />
                    {statusLabel}
                </span>
            </div>

            <button type="button" className="order-card-body-premium" onClick={onClick} style={{ cursor: 'pointer' }}>
                <div className="order-card-image-wrap">
                    <div className="order-card-image-glow" />
                    {product.images?.[0] ? (
                        <img src={product.images[0]} alt={product.title} className="order-card-image-premium" />
                    ) : (
                        <div className="order-card-image-placeholder">
                            <Package size={24} />
                        </div>
                    )}
                </div>

                <div className="order-card-info-premium">
                    <h3 className="order-card-title-premium">{product.title}</h3>
                    <p className="order-card-seller-premium">
                        ¥{product.price?.toFixed(2)} · {product.viewCount ?? product.views ?? 0} 次浏览
                    </p>
                </div>

                <ChevronRight size={18} className="order-card-arrow-premium" />
            </button>

            <div className="order-card-footer-premium">
                <span className="order-card-time-premium">
                    {statusKey === 'DRAFT' && '编辑后可提交审核'}
                    {statusKey === 'REJECTED' && '已驳回，请修改后重新提交'}
                    {statusKey === 'PENDING_REVIEW' && '等待管理员审核'}
                    {!['DRAFT', 'REJECTED', 'PENDING_REVIEW'].includes(statusKey) && ''}
                </span>
                <fieldset className="order-card-actions-premium" aria-label="商品操作">
                    <Button
                        variant="outline"
                        onClick={e => {
                            e.stopPropagation();
                            onEdit();
                        }}
                        className="order-btn-secondary"
                    >
                        <Edit size={14} />
                        编辑
                    </Button>
                    <Button
                        onClick={e => {
                            e.stopPropagation();
                            onClick();
                        }}
                        className="order-btn-primary"
                    >
                        <Eye size={14} />
                        查看
                    </Button>
                </fieldset>
            </div>
        </div>
    );
}
