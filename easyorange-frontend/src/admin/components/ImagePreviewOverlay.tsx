import { Button } from '@/components/ui/button';

interface ImagePreviewOverlayProps {
    src: string;
    onClose: () => void;
}

/**
 * 图片全屏预览灯箱 — 商品详情弹窗/抽屉共用。
 * 含遮罩点击关闭 + Esc 关闭 + 右上角关闭按钮，键盘/aria 语义完整。
 */
export function ImagePreviewOverlay({ src, onClose }: ImagePreviewOverlayProps) {
    return (
        <>
            <Button
                type="button"
                variant="ghost"
                aria-label="关闭预览（点击空白处或按 Esc）"
                className="fixed inset-0 z-50 cursor-default border-0 bg-[rgba(42,37,32,0.88)] p-0 hover:bg-[rgba(42,37,32,0.88)]"
                style={{ backdropFilter: 'blur(8px)', WebkitBackdropFilter: 'blur(8px)' }}
                onClick={onClose}
                onKeyDown={e => {
                    if (e.key === 'Escape') {
                        onClose();
                    }
                }}
            />
            <img
                src={src}
                alt="预览"
                className="pointer-events-none fixed left-1/2 top-1/2 z-[51] max-h-[90vh] max-w-[90vw] -translate-x-1/2 -translate-y-1/2 rounded-xl object-contain"
            />
            <Button
                variant="ghost"
                size="icon"
                className="fixed right-5 top-5 z-[52] inline-flex h-10 w-10 items-center justify-center rounded-xl border-[1.5px] border-white/20 bg-white/10 text-white transition-all duration-150 hover:bg-white/20"
                onClick={onClose}
                aria-label="关闭预览"
            >
                <svg
                    aria-hidden="true"
                    width="18"
                    height="18"
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
        </>
    );
}
