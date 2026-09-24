import { ImageOff, ScrollText } from 'lucide-react';
import { useEffect, useState } from 'react';
import { ImagePreviewOverlay } from '@/admin/components/ImagePreviewOverlay';
import { ErrorState } from '@/components/feedback/StateDisplay';
import { Button, Sheet, SheetContent, SheetHeader, SheetTitle, Textarea } from '@/components/ui';
import { cn } from '@/lib/utils';
import { ConfirmModal } from '../../components/ConfirmModal';
import { useAuditLogs, useAuditProduct } from '../../hooks/useAdminProductAudit';
import { useAdminProductDetail } from '../../hooks/useAdminProducts';
import { notify } from '../../notify';
import type { AuditDimension, AuditLogResponse } from '../../types/admin';

interface ProductDetailDrawerProps {
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

const createInitialState = () => ({
    selectedImage: 0,
    previewImage: null as string | null,
    selectedDimensions: [] as AuditDimension[],
    auditRemark: '',
    rejectReason: '',
    showRejectModal: false,
    showApproveModal: false,
});

const DIMENSIONS: { key: AuditDimension; label: string }[] = [
    { key: 'basic', label: '基本信息合规' },
    { key: 'compliance', label: '内容无违规' },
    { key: 'image', label: '图片质量合格' },
    { key: 'price', label: '价格合理' },
];

const REJECT_TAGS = ['信息不完整', '图片模糊', '疑似虚假信息', '价格异常', '违规内容', '其他'];

export function ProductDetailDrawer({ open, productId, onClose, onSuccess }: ProductDetailDrawerProps) {
    const [state, setState] = useState(createInitialState);
    const {
        selectedImage,
        previewImage,
        selectedDimensions,
        auditRemark,
        rejectReason,
        showRejectModal,
        showApproveModal,
    } = state;

    const { data: product, isLoading, isError, error, refetch } = useAdminProductDetail(productId ?? '');
    const updateStatus = useAuditProduct();
    const auditLogs = useAuditLogs(productId);

    useEffect(() => {
        if (open && productId) {
            setState(createInitialState());
            refetch();
        }
    }, [open, productId, refetch]);

    const handleApproveWithDimensions = async () => {
        if (!product) {
            return;
        }
        try {
            await updateStatus.mutateAsync({
                id: product.productId,
                data: { action: 1, dimensions: selectedDimensions, remark: auditRemark || undefined },
            });
            // 审核动作要有即时反馈：只关抽屉的话，点完「通过审核」界面几乎无变化，
            // 演示者会以为没点上
            notify.success('审核已通过，商品已上架');
            setState(prev => ({ ...prev, showApproveModal: false }));
            onSuccess();
            onClose();
        } catch (e) {
            notify.failure(e, '审核失败，请重试');
        }
    };

    const handleRejectWithReason = async () => {
        if (!product || !rejectReason.trim()) {
            return;
        }
        try {
            await updateStatus.mutateAsync({
                id: product.productId,
                data: {
                    action: 2,
                    reason: rejectReason,
                    dimensions: selectedDimensions,
                    remark: auditRemark || undefined,
                },
            });
            setState(prev => ({ ...prev, showRejectModal: false, rejectReason: '' }));
            notify.success('已驳回，理由已记录在审核日志');
            onSuccess();
            onClose();
        } catch (e) {
            notify.failure(e, '驳回失败，请重试');
        }
    };

    const toggleDimension = (key: AuditDimension) => {
        setState(prev => ({
            ...prev,
            selectedDimensions: prev.selectedDimensions.includes(key)
                ? prev.selectedDimensions.filter(d => d !== key)
                : [...prev.selectedDimensions, key],
        }));
    };

    const appendRejectTag = (tag: string) => {
        setState(prev => ({
            ...prev,
            rejectReason: (prev.rejectReason ? `${prev.rejectReason}；` : '') + tag,
        }));
    };

    const formatDate = (dateString: string) =>
        new Date(dateString).toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
        });

    const formatPrice = (price: number) => `¥${price.toFixed(2)}`;

    if (!open) {
        return null;
    }

    return (
        <Sheet
            open={open}
            onOpenChange={isOpen => {
                if (!isOpen) {
                    onClose();
                }
            }}
        >
            <SheetContent
                side="right"
                className="[&>button]:hidden flex h-full w-full max-w-[520px] flex-col gap-0 overflow-hidden border-l border-[rgba(229,224,219,0.4)] bg-white/92 p-0 shadow-[-16px_0_48px_rgba(42,37,32,0.12)]"
                style={{ backdropFilter: 'blur(24px)', WebkitBackdropFilter: 'blur(24px)' }}
            >
                {/* Header */}
                <SheetHeader className="relative flex-row items-center justify-between border-b border-[rgba(229,224,219,0.4)] px-6 py-[1.15rem] text-left">
                    <div
                        className="absolute bottom-0 left-6 right-6 h-px"
                        style={{
                            background:
                                'linear-gradient(90deg, rgba(251,113,133,0.12), rgba(195,155,211,0.08), transparent)',
                        }}
                    />
                    <SheetTitle
                        className="flex items-center gap-2 text-[1.1rem] font-bold text-[#2A2520]"
                        style={{ fontFamily: "'Playfair Display', 'Noto Serif SC', serif" }}
                    >
                        <span className="inline-flex h-[26px] w-[26px] shrink-0 items-center justify-center rounded-lg bg-[linear-gradient(135deg,#FB7185,#C39BD3)] text-white">
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
                        </span>
                        商品详情
                    </SheetTitle>
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={onClose}
                        disabled={updateStatus.isPending}
                        className="inline-flex h-8 w-8 items-center justify-center rounded-[10px] border-[1.5px] border-[#E5E0DB] bg-white text-[#6E6862] transition-all duration-150 hover:border-[rgba(244,63,94,0.2)] hover:bg-[rgba(244,63,94,0.06)] hover:text-[#E11D48] disabled:cursor-not-allowed disabled:opacity-50"
                        aria-label="关闭"
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
                        >
                            <path d="M18 6L6 18M6 6l12 12" />
                        </svg>
                    </Button>
                </SheetHeader>

                {/* Content */}
                <div className="min-h-0 flex-1 overflow-y-auto p-6">
                    {isLoading ? (
                        <div
                            className="flex flex-col items-center justify-center gap-[0.7rem] py-16 px-4"
                            role="status"
                            aria-busy="true"
                        >
                            <div className="h-7 w-7 animate-spin rounded-full border-[2.5px] border-[#E5E0DB] border-t-[#F97316]" />
                            <span className="text-[0.87rem] text-[#6E6862]">加载中...</span>
                        </div>
                    ) : isError ? (
                        <ErrorState
                            title="商品详情加载失败"
                            description={error?.message}
                            onRetry={() => {
                                refetch();
                            }}
                        />
                    ) : product ? (
                        <div className="flex flex-col gap-6">
                            {/* Image gallery */}
                            <div className="flex flex-col gap-3">
                                <Button
                                    variant="ghost"
                                    className="relative w-full overflow-hidden rounded-2xl bg-[linear-gradient(135deg,#F5F2EE,#EDE8E3)]"
                                    style={{
                                        aspectRatio: '16/10',
                                        cursor: product.images[selectedImage] ? 'pointer' : 'default',
                                    }}
                                    // 缩略图/主图此前都没有动作名，读屏只会念出商品名
                                    aria-label={
                                        product.images[selectedImage]
                                            ? `放大预览第 ${selectedImage + 1} 张图片`
                                            : undefined
                                    }
                                    onClick={() =>
                                        product.images[selectedImage] &&
                                        setState(prev => ({ ...prev, previewImage: product.images[selectedImage] }))
                                    }
                                >
                                    {product.images[selectedImage] ? (
                                        <img
                                            src={product.images[selectedImage]}
                                            alt={product.name}
                                            className="h-full w-full object-cover"
                                            loading="lazy"
                                            decoding="async"
                                        />
                                    ) : (
                                        <div className="flex h-full items-center justify-center text-[#6E6862]">
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
                                    {product.images[selectedImage] && (
                                        <div className="absolute bottom-2 right-2 rounded-lg bg-[rgba(42,37,32,0.55)] px-[0.6rem] py-1 text-[0.72rem] font-medium text-white backdrop-blur-sm">
                                            点击预览
                                        </div>
                                    )}
                                </Button>

                                {product.images.length > 1 && (
                                    <div className="flex gap-2 overflow-x-auto pb-1">
                                        {product.images.map((img, index) => (
                                            <Button
                                                key={img}
                                                variant="ghost"
                                                size="icon"
                                                onClick={() => setState(prev => ({ ...prev, selectedImage: index }))}
                                                aria-label={`查看第 ${index + 1} 张图片`}
                                                aria-current={selectedImage === index}
                                                className="shrink-0 overflow-hidden rounded-xl border-2 p-0 transition-all duration-150"
                                                style={{
                                                    width: 56,
                                                    height: 56,
                                                    minHeight: 'unset',
                                                    borderColor: selectedImage === index ? '#F97316' : 'transparent',
                                                    boxShadow:
                                                        selectedImage === index
                                                            ? '0 2px 8px rgba(249,115,22,0.2)'
                                                            : 'none',
                                                }}
                                            >
                                                <img
                                                    src={img}
                                                    alt=""
                                                    className="h-full w-full object-cover"
                                                    loading="lazy"
                                                    decoding="async"
                                                />
                                            </Button>
                                        ))}
                                    </div>
                                )}
                            </div>

                            {/* Product info */}
                            <div className="flex flex-col gap-4">
                                <div>
                                    <h3
                                        className="text-[1.25rem] font-bold leading-tight text-[#2A2520]"
                                        style={{ fontFamily: "'Playfair Display', 'Noto Serif SC', serif" }}
                                    >
                                        {product.name}
                                    </h3>
                                </div>

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
                                        {formatPrice(product.price ?? 0)}
                                    </span>
                                    {product.originalPrice && product.originalPrice > (product.price ?? 0) && (
                                        <span className="text-[0.85rem] text-[#6E6862] line-through">
                                            {formatPrice(product.originalPrice)}
                                        </span>
                                    )}
                                </div>

                                {/* Detail grid */}
                                <div className="admin-field-grid">
                                    {[
                                        {
                                            label: '新旧程度',
                                            value: conditionLabels[product.conditionLevel || 8] || '未知',
                                        },
                                        { label: '分类', value: product.categoryName },
                                        { label: '资产方', value: product.sellerName },
                                        { label: '发布时间', value: formatDate(product.createTime ?? '') },
                                    ].map(item => (
                                        <div
                                            key={item.label}
                                            className="rounded-xl border border-[rgba(229,224,219,0.4)] bg-white/60 px-[0.8rem] py-[0.6rem]"
                                        >
                                            <p className="mb-0.5 text-[0.72rem] font-medium text-[#6E6862]">
                                                {item.label}
                                            </p>
                                            <p className="text-[0.85rem] font-semibold text-[#2A2520]">{item.value}</p>
                                        </div>
                                    ))}
                                </div>

                                {/* Stats */}
                                <div className="flex items-center gap-6 border-y border-[rgba(229,224,219,0.4)] py-3">
                                    <div className="flex items-center gap-[0.4rem] text-[0.84rem] text-[#6E6862]">
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
                                            <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                            <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                        </svg>
                                        <span className="font-semibold text-[#4A4540]">{product.viewCount ?? 0}</span>{' '}
                                        次浏览
                                    </div>
                                </div>

                                {/* Description */}
                                {product.description && (
                                    <div className="rounded-[14px] border border-[rgba(229,224,219,0.35)] bg-[linear-gradient(135deg,rgba(249,115,22,0.03),rgba(195,155,211,0.02))] p-4">
                                        <h4 className="mb-[0.45rem] text-[0.82rem] font-semibold text-[#6B6460]">
                                            商品描述
                                        </h4>
                                        <p className="whitespace-pre-wrap text-[0.87rem] leading-relaxed text-[#4A4540]">
                                            {product.description}
                                        </p>
                                    </div>
                                )}

                                {/* 审核记录时间线：加载中 / 失败 / 无记录三态分开，
                                    此前只判 data && length>0，加载中和失败都显示成「没有记录」 */}
                                <div className="rounded-[14px] border border-[rgba(229,224,219,0.35)] bg-[linear-gradient(135deg,rgba(249,115,22,0.03),rgba(195,155,211,0.02))] p-4">
                                    <h4 className="mb-[0.65rem] flex items-center gap-[0.35rem] text-[0.82rem] font-semibold text-[#6B6460]">
                                        <ScrollText size={14} aria-hidden="true" />
                                        审核记录
                                    </h4>
                                    {auditLogs.isLoading ? (
                                        <p className="text-[0.82rem] text-[#6E6862]" role="status" aria-busy="true">
                                            加载中…
                                        </p>
                                    ) : auditLogs.isError ? (
                                        <p className="text-[0.82rem] text-[#E11D48]" role="alert">
                                            审核记录加载失败
                                            <Button
                                                variant="link"
                                                size="sm"
                                                className="ml-2 h-auto min-h-0 p-0 text-[0.82rem]"
                                                onClick={() => auditLogs.refetch()}
                                            >
                                                重试
                                            </Button>
                                        </p>
                                    ) : !auditLogs.data?.length ? (
                                        <p className="text-[0.82rem] text-[#6E6862]">该商品还没有审核记录</p>
                                    ) : (
                                        <div className="flex flex-col gap-[0.7rem]">
                                            {auditLogs.data.map((log: AuditLogResponse) => (
                                                <div key={log.id} className="flex items-start gap-[0.6rem]">
                                                    <div
                                                        className="mt-[5px] h-2 w-2 shrink-0 rounded-full"
                                                        style={{
                                                            background:
                                                                log.action === 1
                                                                    ? '#10B981'
                                                                    : log.action === 2
                                                                      ? '#F43F5E'
                                                                      : '#D6CEC5',
                                                            border: log.action === 3 ? '1.5px solid #D6CEC5' : 'none',
                                                        }}
                                                    />
                                                    <div className="min-w-0 flex-1">
                                                        <div className="mb-[0.15rem] flex flex-wrap items-center gap-2">
                                                            <span className="text-[0.78rem] text-[#6E6862]">
                                                                {log.createTime?.replace('T', ' ').slice(0, 16)}
                                                            </span>
                                                            <span className="text-[0.81rem] font-semibold text-[#2A2520]">
                                                                {log.operatorName}
                                                            </span>
                                                            <span
                                                                className="rounded-md px-[0.45rem] py-[0.1rem] text-[0.75rem] font-semibold"
                                                                style={{
                                                                    color:
                                                                        log.action === 1
                                                                            ? '#059669'
                                                                            : log.action === 2
                                                                              ? '#E11D48'
                                                                              : '#6E6862',
                                                                    background:
                                                                        log.action === 1
                                                                            ? 'rgba(16,185,129,0.08)'
                                                                            : log.action === 2
                                                                              ? 'rgba(244,63,94,0.08)'
                                                                              : 'rgba(139,133,126,0.08)',
                                                                }}
                                                            >
                                                                {log.actionDesc}
                                                            </span>
                                                        </div>
                                                        {log.reason && (
                                                            <div className="text-[0.82rem] leading-relaxed text-[#4A4540]">
                                                                {log.reason}
                                                            </div>
                                                        )}
                                                        {log.dimensions && log.dimensions.length > 0 && (
                                                            <div className="mt-[0.2rem] flex flex-wrap gap-[0.3rem]">
                                                                {log.dimensions.map(dimension => (
                                                                    <span
                                                                        key={dimension}
                                                                        className="rounded bg-[rgba(249,115,22,0.06)] px-[0.4rem] py-[0.1rem] text-[0.72rem] text-[#C2410C]"
                                                                    >
                                                                        {dimension === 'basic'
                                                                            ? '基本信息'
                                                                            : dimension === 'compliance'
                                                                              ? '内容合规'
                                                                              : dimension === 'image'
                                                                                ? '图片质量'
                                                                                : '价格合理'}
                                                                    </span>
                                                                ))}
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>

                                {/* Location */}
                                {product.location && (
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
                                        <span className="text-[#6E6862]">交易地点：</span>
                                        <span className="font-semibold text-[#2A2520]">{product.location}</span>
                                    </div>
                                )}
                            </div>
                        </div>
                    ) : (
                        <div className="flex flex-col items-center justify-center gap-2 py-16 text-[#6E6862]">
                            <ImageOff size={32} aria-hidden="true" style={{ opacity: 0.4 }} />
                            <span className="text-[0.9rem]">商品不存在或已被删除</span>
                        </div>
                    )}
                </div>

                {/* Footer actions */}
                {product && (
                    <div className="flex flex-col gap-3 border-t border-[rgba(229,224,219,0.4)] bg-[linear-gradient(180deg,rgba(250,248,245,0.5),rgba(250,248,245,0.9))] px-6 py-4">
                        {/* 审核维度 */}
                        <div>
                            <div className="mb-[0.45rem] text-[0.78rem] font-semibold text-[#6B6460]">审核维度</div>
                            <div className="flex flex-wrap gap-[0.4rem]">
                                {DIMENSIONS.map(dim => {
                                    const active = selectedDimensions.includes(dim.key);
                                    return (
                                        <Button
                                            key={dim.key}
                                            variant="ghost"
                                            size="sm"
                                            onClick={() => toggleDimension(dim.key)}
                                            disabled={updateStatus.isPending}
                                            className={cn(
                                                'h-auto min-h-0 rounded-lg px-[0.7rem] py-[0.35rem] text-[0.79rem] font-medium transition-all duration-150 disabled:cursor-not-allowed disabled:opacity-60',
                                                active
                                                    ? 'border-2 border-[#F97316] bg-[rgba(249,115,22,0.08)] text-[#EA580C] hover:bg-[rgba(249,115,22,0.08)] hover:text-[#EA580C]'
                                                    : 'border-[1.5px] border-[#E5E0DB] bg-white text-[#6E6862] hover:bg-white hover:text-[#6E6862]'
                                            )}
                                        >
                                            {active ? '✓ ' : ''}
                                            {dim.label}
                                        </Button>
                                    );
                                })}
                            </div>
                        </div>

                        {/* 审核意见 */}
                        <div>
                            <label
                                htmlFor="audit-remark"
                                className="mb-[0.45rem] block text-[0.78rem] font-semibold text-[#6B6460]"
                            >
                                审核意见（选填）
                            </label>
                            <Textarea
                                id="audit-remark"
                                placeholder="记录本次审核的判断依据..."
                                value={auditRemark}
                                onChange={e => setState(prev => ({ ...prev, auditRemark: e.target.value }))}
                                className="min-h-[52px] resize-y rounded-[10px] border-[1.5px] border-[#E5E0DB] bg-white px-[0.8rem] py-[0.6rem] text-[0.84rem] text-[#2A2520] placeholder:text-[#6E6862] focus-visible:border-[#F97316] focus-visible:ring-0 focus-visible:ring-offset-0"
                            />
                        </div>

                        {/* 操作按钮行：窄屏由 .admin-footer-actions 换行 */}
                        <div className="admin-footer-actions">
                            <Button
                                onClick={() => setState(prev => ({ ...prev, showApproveModal: true }))}
                                disabled={updateStatus.isPending}
                                className="h-10 rounded-xl border-none bg-[linear-gradient(135deg,#10B981,#059669)] text-[0.87rem] font-semibold text-white shadow-[0_3px_12px_rgba(16,185,129,0.28)] transition-all duration-200 hover:-translate-y-0.5"
                            >
                                通过审核
                            </Button>
                            <Button
                                onClick={() => setState(prev => ({ ...prev, showRejectModal: true }))}
                                disabled={updateStatus.isPending}
                                className="h-10 rounded-xl border-none bg-[linear-gradient(135deg,#F43F5E,#E11D48)] text-[0.87rem] font-semibold text-white shadow-[0_3px_12px_rgba(244,63,94,0.28)] transition-all duration-200 hover:-translate-y-0.5"
                            >
                                驳回商品
                            </Button>
                            <Button
                                variant="outline"
                                onClick={onClose}
                                disabled={updateStatus.isPending}
                                className="h-10 rounded-xl border-[1.5px] border-[#E5E0DB] bg-white px-5 text-[0.87rem] font-semibold text-[#6E6862] hover:bg-[rgba(229,224,219,0.3)] hover:text-[#6E6862]"
                            >
                                关闭
                            </Button>
                        </div>
                    </div>
                )}

                {/* Image preview overlay */}
                {previewImage && (
                    <ImagePreviewOverlay
                        src={previewImage}
                        onClose={() => setState(prev => ({ ...prev, previewImage: null }))}
                    />
                )}
            </SheetContent>

            {/* 通过审核 = 直接上架，此前没有任何确认，驳回反而有确认，危险度与确认强度倒挂 */}
            <ConfirmModal
                isOpen={showApproveModal}
                title="确认通过审核"
                variant="info"
                confirmText="通过并上架"
                content={
                    <span>
                        「{product?.name}」通过后将<b>立即上架</b>，所有买家可见。
                        {selectedDimensions.length > 0
                            ? `已勾选审核维度：${selectedDimensions.length} 项。`
                            : '尚未勾选任何审核维度。'}
                    </span>
                }
                isLoading={updateStatus.isPending}
                onConfirm={handleApproveWithDimensions}
                onCancel={() => setState(prev => ({ ...prev, showApproveModal: false }))}
            />

            {/* 驳回：手写 fixed 遮罩替换为 ConfirmModal，走 Radix 焦点陷阱与 Esc 关闭 */}
            <ConfirmModal
                isOpen={showRejectModal}
                title="确认驳回商品"
                confirmText="确认驳回"
                isLoading={updateStatus.isPending}
                confirmDisabled={!rejectReason.trim()}
                onConfirm={handleRejectWithReason}
                onCancel={() => setState(prev => ({ ...prev, showRejectModal: false }))}
                content={
                    <div className="flex flex-col gap-3">
                        <p style={{ margin: 0 }}>确定要驳回该资产吗？驳回后资产方可修改并重新提交。</p>
                        <div className="flex flex-wrap gap-[0.35rem]">
                            {REJECT_TAGS.map(tag => (
                                <Button
                                    key={tag}
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => appendRejectTag(tag)}
                                    className="h-auto min-h-0 rounded-md border-[1.5px] border-[#E5E0DB] bg-white px-[0.55rem] py-[0.28rem] text-[0.76rem] font-medium text-[#6E6862]"
                                >
                                    {tag}
                                </Button>
                            ))}
                        </div>
                        <div>
                            <label
                                htmlFor="reject-reason"
                                className="mb-1.5 block text-[0.78rem] font-semibold text-[#6B6460]"
                            >
                                驳回原因（必填）
                            </label>
                            <Textarea
                                id="reject-reason"
                                placeholder="说明不通过的原因，资产方会看到这段文字"
                                value={rejectReason}
                                onChange={e => setState(prev => ({ ...prev, rejectReason: e.target.value }))}
                                rows={3}
                                aria-invalid={!rejectReason.trim()}
                                className="min-h-[80px] resize-y rounded-[10px] border-[1.5px] border-[#E5E0DB] bg-white px-[0.8rem] py-[0.65rem] text-[0.85rem] text-[#2A2520] placeholder:text-[#6E6862] focus-visible:border-[#F43F5E] focus-visible:ring-0 focus-visible:ring-offset-0"
                            />
                        </div>
                    </div>
                }
            />
        </Sheet>
    );
}
