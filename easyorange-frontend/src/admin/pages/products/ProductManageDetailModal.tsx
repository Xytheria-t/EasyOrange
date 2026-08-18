import { useEffect, useState } from 'react';
import { AdminDetailModal, InfoCell } from '@/admin/components/AdminDetailModal';
import { ImagePreviewOverlay } from '@/admin/components/ImagePreviewOverlay';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import type { ProductStatus } from '@/types';
import { formatDate } from '@/utils/format';
import { useAdminProductDetail, useUpdateProductStatus } from '../../hooks';
import type { AdminProduct } from '../../types/admin';

export interface ProductManageDetailModalProps {
    open: boolean;
    productId: string | null;
    onClose: () => void;
    onSuccess: () => void;
}

const conditionLabels: Record<number, string> = {
    10: '全新',
    9: '9成新',
    8: '8成新',
    7: '7成新',
    6: '6成新及以下',
};

const PRODUCT_STATUS = {
    DRAFT: 'DRAFT',
    ONLINE: 'ONLINE',
    SOLD: 'SOLD',
    OFFLINE: 'OFFLINE',
    PENDING_REVIEW: 'PENDING_REVIEW',
    REJECTED: 'REJECTED',
} as const;

function getAvailableActions(
    status: ProductStatus | null
): { targetStatus: ProductStatus; label: string; variant: 'online' | 'offline' }[] {
    const actions: { targetStatus: ProductStatus; label: string; variant: 'online' | 'offline' }[] = [];
    if (status === PRODUCT_STATUS.ONLINE) {
        actions.push({ targetStatus: PRODUCT_STATUS.OFFLINE, label: '下架', variant: 'offline' });
    } else if (
        status === PRODUCT_STATUS.OFFLINE ||
        status === PRODUCT_STATUS.DRAFT ||
        status === PRODUCT_STATUS.REJECTED
    ) {
        actions.push({ targetStatus: PRODUCT_STATUS.ONLINE, label: '上架', variant: 'online' });
    }
    return actions;
}

export function ProductManageDetailModal({ open, productId, onClose, onSuccess }: ProductManageDetailModalProps) {
    const [previewImage, setPreviewImage] = useState<string | null>(null);
    const [selectedImage, setSelectedImage] = useState(0);

    const { data: product, isLoading, refetch } = useAdminProductDetail(productId ?? '');
    const updateStatus = useUpdateProductStatus();

    useEffect(() => {
        if (open && productId) {
            refetch();
        }
    }, [open, productId, refetch]);

    useEffect(() => {
        if (!open) {
            setSelectedImage(0);
            setPreviewImage(null);
        }
    }, [open]);

    const handleStatusChange = async (targetStatus: ProductStatus) => {
        if (!product) {
            return;
        }
        try {
            await updateStatus.mutateAsync({ id: product.productId, data: { status: targetStatus } });
            onSuccess();
            onClose();
        } catch {
            // error handled by react-query / global handler
        }
    };

    const formatPrice = (price: number | null) => (price != null ? `¥${price.toFixed(2)}` : '—');

    if (!open || !productId) {
        return null;
    }

    const productData = product as AdminProduct | undefined;
    const images = productData?.images ?? [];
    const actions = getAvailableActions(productData?.status ?? null);

    return (
        <AdminDetailModal
            open={open}
            onClose={onClose}
            title="商品详情"
            maxWidth={520}
            loading={isLoading}
            closeDisabled={updateStatus.isPending}
            notFound={!productData}
            notFoundText="商品不存在或已被删除"
            icon={
                <svg
                    aria-hidden="true"
                    width="13"
                    height="13"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                >
                    <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" />
                </svg>
            }
            footer={
                productData && actions.length > 0 ? (
                    <div className="flex gap-[0.65rem] border-t border-[rgba(229,224,219,0.4)] bg-[linear-gradient(180deg,rgba(250,248,245,0.5),rgba(250,248,245,0.9))] px-6 py-4">
                        {actions.map(action => (
                            <Button
                                key={action.targetStatus}
                                onClick={() => handleStatusChange(action.targetStatus)}
                                disabled={updateStatus.isPending}
                                isLoading={updateStatus.isPending}
                                className={cn(
                                    'h-10 flex-1 rounded-xl border-none text-[0.87rem] font-semibold text-white shadow-[0_3px_12px_rgba(0,0,0,0.12)] transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_5px_18px_rgba(0,0,0,0.18)]',
                                    action.variant === 'online'
                                        ? 'bg-[linear-gradient(135deg,#10B981,#059669)] shadow-[0_3px_12px_rgba(16,185,129,0.28)] hover:shadow-[0_5px_18px_rgba(16,185,129,0.38)]'
                                        : 'bg-[linear-gradient(135deg,#F97316,#EA580C)] shadow-[0_3px_12px_rgba(249,115,22,0.28)] hover:shadow-[0_5px_18px_rgba(249,115,22,0.38)]'
                                )}
                            >
                                <svg
                                    aria-hidden="true"
                                    width="14"
                                    height="14"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2.5"
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    className="mr-[0.35rem]"
                                >
                                    {action.variant === 'online' ? (
                                        <>
                                            <polyline points="9 11 12 14 22 4" />
                                            <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
                                        </>
                                    ) : (
                                        <>
                                            <line x1="18" y1="6" x2="6" y2="18" />
                                            <line x1="6" y1="6" x2="18" y2="18" />
                                        </>
                                    )}
                                </svg>
                                {action.label}
                            </Button>
                        ))}
                        <Button
                            variant="outline"
                            onClick={onClose}
                            disabled={updateStatus.isPending}
                            className="h-10 rounded-xl border-[1.5px] border-[#E5E0DB] bg-white px-5 text-[0.87rem] font-semibold text-[#6B6460] hover:bg-[rgba(229,224,219,0.3)] hover:text-[#6B6460]"
                        >
                            关闭
                        </Button>
                    </div>
                ) : undefined
            }
            overlay={
                previewImage ? (
                    <ImagePreviewOverlay src={previewImage} onClose={() => setPreviewImage(null)} />
                ) : undefined
            }
        >
            {productData ? (
                <div className="flex flex-col gap-5">
                    {/* Image gallery */}
                    {images.length > 0 && (
                        <div className="flex flex-col gap-[0.65rem]">
                            <Button
                                variant="ghost"
                                className="relative w-full overflow-hidden rounded-2xl bg-[linear-gradient(135deg,#F5F2EE,#EDE8E3)]"
                                style={{
                                    aspectRatio: '16/10',
                                    cursor: images[selectedImage] ? 'pointer' : 'default',
                                }}
                                onClick={() => images[selectedImage] && setPreviewImage(images[selectedImage])}
                            >
                                {images[selectedImage] ? (
                                    <img
                                        src={images[selectedImage]}
                                        alt={productData.name}
                                        className="h-full w-full object-cover"
                                    />
                                ) : (
                                    <div className="flex h-full items-center justify-center text-[#B5AEA8]">
                                        <svg
                                            aria-hidden="true"
                                            width="40"
                                            height="40"
                                            fill="none"
                                            viewBox="0 0 24 24"
                                            stroke="currentColor"
                                        >
                                            <path
                                                strokeLinecap="round"
                                                strokeLinejoin="round"
                                                strokeWidth={1}
                                                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                                            />
                                        </svg>
                                    </div>
                                )}
                            </Button>
                            {images.length > 1 && (
                                <div className="flex gap-2 overflow-x-auto pb-1">
                                    {images.map((img, index) => (
                                        <Button
                                            key={img}
                                            variant="ghost"
                                            size="icon"
                                            onClick={() => setSelectedImage(index)}
                                            className="shrink-0 overflow-hidden rounded-[10px] border-2 p-0 transition-colors duration-150"
                                            style={{
                                                width: 52,
                                                height: 52,
                                                minHeight: 'unset',
                                                borderColor: selectedImage === index ? '#F97316' : 'transparent',
                                                boxShadow:
                                                    selectedImage === index ? '0 2px 8px rgba(249,115,22,0.2)' : 'none',
                                            }}
                                        >
                                            <img src={img} alt="" className="h-full w-full object-cover" />
                                        </Button>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    {/* Name + Price */}
                    <div>
                        <h3
                            className="mb-[0.35rem] text-[1.15rem] font-bold leading-tight text-[#2A2520]"
                            style={{ fontFamily: "'Playfair Display', 'Noto Serif SC', serif" }}
                        >
                            {productData.name}
                        </h3>
                        <div className="flex items-baseline gap-2">
                            <span
                                className="text-[1.5rem] font-bold"
                                style={{
                                    fontFamily: "'DM Sans', sans-serif",
                                    background: 'linear-gradient(135deg, #F97316, #EA580C)',
                                    WebkitBackgroundClip: 'text',
                                    WebkitTextFillColor: 'transparent',
                                    backgroundClip: 'text',
                                }}
                            >
                                {formatPrice(productData.price)}
                            </span>
                            {productData.originalPrice && productData.originalPrice > (productData.price ?? 0) && (
                                <span className="text-[0.85rem] text-[#B5AEA8] line-through">
                                    {formatPrice(productData.originalPrice)}
                                </span>
                            )}
                        </div>
                    </div>

                    {/* Info grid */}
                    <div className="grid grid-cols-2 gap-3">
                        <InfoCell label="新旧程度" value={conditionLabels[productData.conditionLevel || 8] || '未知'} />
                        <InfoCell label="分类" value={productData.categoryName || '—'} />
                        <InfoCell label="资产方" value={productData.sellerName || '—'} />
                        <InfoCell label="浏览量" value={productData.viewCount ?? 0} />
                        <InfoCell label="发布时间" value={formatDate(productData.createTime ?? '')} />
                        <InfoCell label="更新时间" value={formatDate(productData.updateTime ?? '')} />
                    </div>

                    {/* Status bar */}
                    <div className="flex items-center gap-3 rounded-[14px] border border-[rgba(249,115,22,0.08)] bg-[linear-gradient(135deg,rgba(249,115,22,0.04),rgba(195,155,211,0.03))] px-4 py-[0.85rem]">
                        <span className="text-[0.82rem] font-medium text-[#6B6460]">当前状态：</span>
                        <span className="rounded-md bg-[rgba(249,115,22,0.06)] px-[0.6rem] py-[0.2rem] text-[0.84rem] font-bold text-[#2A2520]">
                            {productData.statusDesc ?? '未知'}
                        </span>
                    </div>

                    {/* Description */}
                    {productData.description && (
                        <div className="rounded-[14px] border border-[rgba(229,224,219,0.35)] bg-[linear-gradient(135deg,rgba(249,115,22,0.03),rgba(195,155,211,0.02))] px-4 py-[0.85rem]">
                            <p className="mb-[0.35rem] text-[0.78rem] font-semibold text-[#6B6460]">商品描述</p>
                            <p className="whitespace-pre-wrap text-[0.87rem] leading-relaxed text-[#4A4540]">
                                {productData.description}
                            </p>
                        </div>
                    )}

                    {/* Location */}
                    {productData.location && (
                        <div className="flex items-center gap-[0.45rem] text-[0.85rem]">
                            <svg
                                aria-hidden="true"
                                width="15"
                                height="15"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="#F97316"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            >
                                <path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                                <path d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                            </svg>
                            <span className="text-[#9B9590]">交易地点：</span>
                            <span className="font-semibold text-[#2A2520]">{productData.location}</span>
                        </div>
                    )}
                </div>
            ) : null}
        </AdminDetailModal>
    );
}
