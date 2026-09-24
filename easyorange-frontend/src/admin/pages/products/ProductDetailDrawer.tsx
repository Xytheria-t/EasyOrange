import { ImageOff, ScrollText } from 'lucide-react';
import { useState } from 'react';
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

/** 审核动作色走共享状态令牌：通过绿 / 驳回玫红 / 其他中性。 */
const AUDIT_ACTION_COLOR: Record<number, string> = {
    1: 'var(--status-success-dot)',
    2: 'var(--status-error-dot)',
};

const REJECT_TAGS = ['信息不完整', '图片模糊', '疑似虚假信息', '价格异常', '违规内容', '其他'];

/**
 * 抽屉壳：只负责「开 / 关」。
 *
 * 面板整体挂在壳里面，抽屉一开就挂载、关上就卸载——查询因此每次打开都会重新发请求，
 * 本地草稿（选中的图片、审核维度、意见）也随之重置。此前这两件事要靠一个 effect 手动
 * 同步：本地 state 复位 + refetch()，而 refetch 会和 queryKey 变化触发的请求打架。
 * 新鲜度改由查询层声明（见 useAdminProductDetail 的 refetchOnMount）。
 */
export function ProductDetailDrawer({ open, productId, onClose, onSuccess }: ProductDetailDrawerProps) {
    if (!open || !productId) {
        return null;
    }
    return <ProductAuditPanel productId={productId} onClose={onClose} onSuccess={onSuccess} />;
}

function ProductAuditPanel({
    productId,
    onClose,
    onSuccess,
}: {
    productId: string;
    onClose: () => void;
    onSuccess: () => void;
}) {
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

    const { data: product, isLoading, isError, error, refetch } = useAdminProductDetail(productId);
    const updateStatus = useAuditProduct();
    const auditLogs = useAuditLogs(productId);

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

    return (
        <Sheet
            open
            onOpenChange={isOpen => {
                if (!isOpen) {
                    onClose();
                }
            }}
        >
            <SheetContent side="right" className="admin-drawer-panel [&>button]:hidden">
                {/* Header */}
                <SheetHeader className="admin-modal-header admin-drawer-header">
                    <SheetTitle className="admin-modal-title">
                        <span className="admin-modal-title-icon admin-drawer-title-icon">
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
                        className="admin-modal-close"
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
                <div className="admin-modal-body">
                    {isLoading ? (
                        <div className="admin-modal-state admin-modal-state--loading" role="status" aria-busy="true">
                            <div className="admin-spinner animate-spin" />
                            <span>加载中...</span>
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
                                    className="admin-media-frame"
                                    style={{ cursor: product.images[selectedImage] ? 'pointer' : 'default' }}
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
                                        <div className="flex h-full items-center justify-center text-(--admin-muted)">
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
                                    {product.images[selectedImage] && <div className="admin-media-hint">点击预览</div>}
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
                                                className={cn(
                                                    'admin-thumb',
                                                    selectedImage === index && 'admin-thumb--active'
                                                )}
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
                                    <h3 className="admin-detail-title">{product.name}</h3>
                                </div>

                                <div className="flex items-baseline gap-2">
                                    <span className="admin-price-gradient">{formatPrice(product.price ?? 0)}</span>
                                    {product.originalPrice && product.originalPrice > (product.price ?? 0) && (
                                        <span className="admin-muted line-through">
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
                                        <div key={item.label} className="admin-info-cell">
                                            <p className="admin-info-label">{item.label}</p>
                                            <p className="admin-info-value">{item.value}</p>
                                        </div>
                                    ))}
                                </div>

                                {/* Stats */}
                                <div className="admin-stat-row">
                                    <div className="flex items-center gap-[0.4rem] text-[0.84rem] text-(--admin-muted)">
                                        <svg
                                            aria-hidden="true"
                                            width="15"
                                            height="15"
                                            viewBox="0 0 24 24"
                                            fill="none"
                                            className="admin-stat-icon"
                                            strokeWidth="2"
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                        >
                                            <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                            <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                        </svg>
                                        <span className="admin-stat-value">{product.viewCount ?? 0}</span> 次浏览
                                    </div>
                                </div>

                                {/* Description */}
                                {product.description && (
                                    <div className="admin-note-panel">
                                        <h4 className="admin-note-title">商品描述</h4>
                                        <p className="admin-note-body">{product.description}</p>
                                    </div>
                                )}

                                {/* 审核记录时间线：加载中 / 失败 / 无记录三态分开，
                                    此前只判 data && length>0，加载中和失败都显示成「没有记录」 */}
                                <div className="admin-note-panel">
                                    <h4 className="admin-note-title">
                                        <ScrollText size={14} aria-hidden="true" />
                                        审核记录
                                    </h4>
                                    {auditLogs.isLoading ? (
                                        <p className="admin-muted" role="status" aria-busy="true">
                                            加载中…
                                        </p>
                                    ) : auditLogs.isError ? (
                                        <p className="text-[0.82rem] text-(--admin-danger)" role="alert">
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
                                        <p className="admin-muted">该商品还没有审核记录</p>
                                    ) : (
                                        <div className="flex flex-col gap-[0.7rem]">
                                            {auditLogs.data.map((log: AuditLogResponse) => (
                                                <div key={log.id} className="flex items-start gap-[0.6rem]">
                                                    <div
                                                        className="mt-[5px] h-2 w-2 shrink-0 rounded-full"
                                                        style={{
                                                            background:
                                                                AUDIT_ACTION_COLOR[log.action] ??
                                                                'var(--admin-sort-idle)',
                                                            border:
                                                                log.action === 3
                                                                    ? '1.5px solid var(--admin-sort-idle)'
                                                                    : 'none',
                                                        }}
                                                    />
                                                    <div className="min-w-0 flex-1">
                                                        <div className="mb-[0.15rem] flex flex-wrap items-center gap-2">
                                                            <span className="admin-muted text-[0.78rem]">
                                                                {log.createTime?.replace('T', ' ').slice(0, 16)}
                                                            </span>
                                                            <span className="text-[0.81rem] font-semibold text-(--admin-ink)">
                                                                {log.operatorName}
                                                            </span>
                                                            <span
                                                                className="rounded-md px-[0.45rem] py-[0.1rem] text-[0.75rem] font-semibold"
                                                                style={{
                                                                    color:
                                                                        AUDIT_ACTION_COLOR[log.action] ??
                                                                        'var(--admin-muted)',
                                                                    background: `color-mix(in srgb, ${AUDIT_ACTION_COLOR[log.action] ?? 'var(--admin-muted)'} 8%, transparent)`,
                                                                }}
                                                            >
                                                                {log.actionDesc}
                                                            </span>
                                                        </div>
                                                        {log.reason && (
                                                            <div className="text-[0.82rem] leading-relaxed text-(--admin-ink-soft)">
                                                                {log.reason}
                                                            </div>
                                                        )}
                                                        {log.dimensions && log.dimensions.length > 0 && (
                                                            <div className="mt-[0.2rem] flex flex-wrap gap-[0.3rem]">
                                                                {log.dimensions.map(dimension => (
                                                                    <span key={dimension} className="admin-chip">
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
                                            className="admin-stat-icon"
                                            strokeWidth="2"
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                        >
                                            <path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                                            <path d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                                        </svg>
                                        <span className="text-(--admin-muted)">交易地点：</span>
                                        <span className="font-semibold text-(--admin-ink)">{product.location}</span>
                                    </div>
                                )}
                            </div>
                        </div>
                    ) : (
                        <div className="admin-modal-state">
                            <ImageOff size={32} aria-hidden="true" style={{ opacity: 0.4 }} />
                            <span>商品不存在或已被删除</span>
                        </div>
                    )}
                </div>

                {/* Footer actions */}
                {product && (
                    <div className="admin-drawer-footer">
                        {/* 审核维度 */}
                        <div>
                            <div className="mb-[0.45rem] text-[0.78rem] font-semibold text-(--admin-muted)">
                                审核维度
                            </div>
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
                                                'admin-chip-toggle disabled:cursor-not-allowed disabled:opacity-60',
                                                active && 'admin-chip-toggle--active'
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
                                className="mb-[0.45rem] block text-[0.78rem] font-semibold text-(--admin-muted)"
                            >
                                审核意见（选填）
                            </label>
                            <Textarea
                                id="audit-remark"
                                placeholder="记录本次审核的判断依据..."
                                value={auditRemark}
                                onChange={e => setState(prev => ({ ...prev, auditRemark: e.target.value }))}
                                className="admin-textarea focus-visible:ring-0 focus-visible:ring-offset-0"
                            />
                        </div>

                        {/* 操作按钮行：窄屏由 .admin-footer-actions 换行 */}
                        <div className="admin-footer-actions">
                            <Button
                                onClick={() => setState(prev => ({ ...prev, showApproveModal: true }))}
                                disabled={updateStatus.isPending}
                                className="admin-action-btn admin-action-btn--approve rounded-xl"
                            >
                                通过审核
                            </Button>
                            <Button
                                onClick={() => setState(prev => ({ ...prev, showRejectModal: true }))}
                                disabled={updateStatus.isPending}
                                className="admin-action-btn admin-action-btn--reject rounded-xl"
                            >
                                驳回商品
                            </Button>
                            <Button
                                variant="outline"
                                onClick={onClose}
                                disabled={updateStatus.isPending}
                                className="admin-action-btn admin-action-btn--ghost rounded-xl"
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
                                    className="h-auto min-h-0 rounded-md border-[1.5px] border-(--admin-control-line) bg-(--admin-surface-solid) px-[0.55rem] py-[0.28rem] text-[0.76rem] font-medium text-(--admin-muted)"
                                >
                                    {tag}
                                </Button>
                            ))}
                        </div>
                        <div>
                            <label
                                htmlFor="reject-reason"
                                className="mb-1.5 block text-[0.78rem] font-semibold text-(--admin-muted)"
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
                                className="admin-textarea min-h-[80px] !py-[0.65rem] text-[0.85rem] focus-visible:border-(--status-error-dot) focus-visible:ring-0 focus-visible:ring-offset-0"
                            />
                        </div>
                    </div>
                }
            />
        </Sheet>
    );
}
