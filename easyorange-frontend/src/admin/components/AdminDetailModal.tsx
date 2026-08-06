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
    footer?: React.ReactNode;
    /** 渲染在 footer 之后、DialogContent 内（如全屏图片预览灯箱），避免被滚动容器裁剪 */
    overlay?: React.ReactNode;
    children: React.ReactNode;
}

/**
 * 管理端详情弹窗壳 — 统一 Dialog 骨架、标题栏、close、loading / 空态。
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
            <DialogContent
                className="[&>button]:hidden flex max-h-[calc(100vh-2rem)] w-[calc(100%-2rem)] flex-col gap-0 overflow-hidden rounded-3xl border border-white/70 bg-white/92 p-0 shadow-[0_24px_64px_rgba(42,37,32,0.18),0_8px_24px_rgba(249,115,22,0.06)]"
                style={{ maxWidth, backdropFilter: 'blur(24px)', WebkitBackdropFilter: 'blur(24px)' }}
            >
                <DialogHeader className="relative flex-row items-center justify-between border-b border-[rgba(229,224,219,0.5)] px-6 py-5 text-left">
                    <div
                        className="absolute bottom-0 left-6 right-6 h-px"
                        style={{
                            background:
                                'linear-gradient(90deg, rgba(249,115,22,0.12), rgba(195,155,211,0.08), transparent)',
                        }}
                    />
                    <DialogTitle
                        className="flex items-center gap-2 text-[1.1rem] font-bold text-[#2A2520]"
                        style={{ fontFamily: "'Playfair Display', 'Noto Serif SC', serif" }}
                    >
                        <span className="inline-flex h-[26px] w-[26px] shrink-0 items-center justify-center rounded-lg bg-[linear-gradient(135deg,#F97316,#FB923C)] text-white">
                            {icon}
                        </span>
                        {title}
                    </DialogTitle>
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={onClose}
                        disabled={closeBlocked}
                        className="inline-flex h-8 w-8 items-center justify-center rounded-[10px] border-[1.5px] border-[#E5E0DB] bg-white text-[#8B857E] transition-all duration-150 hover:border-[rgba(244,63,94,0.2)] hover:bg-[rgba(244,63,94,0.06)] hover:text-[#E11D48] disabled:cursor-not-allowed disabled:opacity-50"
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

                <div className="min-h-0 flex-1 overflow-y-auto p-6">
                    {loading ? <ModalSpinner /> : notFound ? <ModalEmptyState text={notFoundText} /> : children}
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
        <div className="flex flex-col items-center justify-center gap-[0.7rem] py-16 px-4">
            <div className="h-7 w-7 animate-spin rounded-full border-[2.5px] border-[#E5E0DB] border-t-[#F97316]" />
            <span className="text-[0.87rem] text-[#9B9590]">加载中...</span>
        </div>
    );
}

/** 详情弹窗空态。 */
export function ModalEmptyState({ text }: { text: string }) {
    return (
        <div className="flex flex-col items-center justify-center gap-2 py-16 text-[#B5AEA8]">
            <span className="text-[2rem] opacity-40">📭</span>
            <span className="text-[0.9rem]">{text}</span>
        </div>
    );
}

/** 详情弹窗信息格。 */
export function InfoCell({ label, value }: { label: string; value: React.ReactNode }) {
    return (
        <div className="rounded-xl border border-[rgba(229,224,219,0.4)] bg-white/60 px-[0.85rem] py-[0.65rem]">
            <p className="mb-0.5 text-[0.72rem] font-medium text-[#9B9590]">{label}</p>
            <p className="text-[0.87rem] font-semibold text-[#2A2520]">{value}</p>
        </div>
    );
}
