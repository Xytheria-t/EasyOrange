import { ErrorState } from '@/components/feedback/StateDisplay';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui';
import { Button } from '@/components/ui/button';

interface AdminDetailModalProps {
    open: boolean;
    onClose: () => void;
    title: string;
    icon: React.ReactNode;
    maxWidth?: number;
    loading?: boolean;
    /** close 按钮禁用条件（如提交中），默认复用 loading */
    closeDisabled?: boolean;
    notFound?: boolean;
    notFoundText?: string;
    /** 详情请求失败。传了会渲染重试出口，而不是伪装成"记录不存在" */
    error?: Error | null;
    onRetry?: () => void;
    footer?: React.ReactNode;
    /** 渲染在 footer 之后、DialogContent 内（如全屏图片预览灯箱），避免被滚动容器裁剪 */
    overlay?: React.ReactNode;
    children: React.ReactNode;
}

/**
 * 管理端详情弹窗壳 — 统一 Dialog 骨架、标题栏、close、loading / 错误 / 空态。
 * 三个详情弹窗（订单/用户/商品）共用，业务内容通过 children 传入。
 */
export function AdminDetailModal({
    open,
    onClose,
    title,
    icon,
    maxWidth = 480,
    loading = false,
    closeDisabled,
    notFound = false,
    notFoundText = '记录不存在或已被删除',
    error = null,
    onRetry,
    footer,
    overlay,
    children,
}: AdminDetailModalProps) {
    const closeBlocked = closeDisabled ?? loading;

    return (
        <Dialog
            open={open}
            onOpenChange={isOpen => {
                if (!isOpen) {
                    onClose();
                }
            }}
        >
            <DialogContent className="admin-modal-panel [&>button]:hidden" style={{ maxWidth }}>
                <DialogHeader className="admin-modal-header">
                    <DialogTitle className="admin-modal-title">
                        <span className="admin-modal-title-icon">{icon}</span>
                        {title}
                    </DialogTitle>
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={onClose}
                        disabled={closeBlocked}
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
                </DialogHeader>

                <div className="admin-modal-body">
                    {loading ? (
                        <ModalSpinner />
                    ) : error ? (
                        <ErrorState title="数据加载失败" description={error.message} onRetry={onRetry} />
                    ) : notFound ? (
                        <ModalEmptyState text={notFoundText} />
                    ) : (
                        children
                    )}
                </div>
                {footer}
                {overlay}
            </DialogContent>
        </Dialog>
    );
}

/** 详情弹窗加载态。 */
export function ModalSpinner() {
    return (
        <div className="admin-modal-state admin-modal-state--loading">
            <div className="admin-spinner animate-spin" />
            <span>加载中...</span>
        </div>
    );
}

/** 详情弹窗空态。 */
export function ModalEmptyState({ text }: { text: string }) {
    return (
        <div className="admin-modal-state">
            <span className="admin-modal-state-emoji">📭</span>
            <span>{text}</span>
        </div>
    );
}

/** 详情弹窗信息格。 */
export function InfoCell({ label, value }: { label: string; value: React.ReactNode }) {
    return (
        <div className="admin-info-cell">
            <p className="admin-info-label">{label}</p>
            <p className="admin-info-value">{value}</p>
        </div>
    );
}
