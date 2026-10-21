import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';

interface ImagePreviewOverlayProps {
    src: string;
    onClose: () => void;
}

/**
 * 图片全屏预览灯箱 — 商品详情弹窗/抽屉共用。
 * 基于 Radix Dialog：Esc 关闭、遮罩点击关闭、焦点陷阱与关闭后焦点还原
 * 都由组件库保证，不依赖焦点恰好落在某个元素上。
 */
export function ImagePreviewOverlay({ src, onClose }: ImagePreviewOverlayProps) {
    return (
        <Dialog open onOpenChange={open => !open && onClose()}>
            <DialogContent
                className="max-w-[90vw] border-0 bg-transparent p-0 shadow-none"
                aria-describedby={undefined}
            >
                <DialogHeader className="sr-only">
                    <DialogTitle>图片预览</DialogTitle>
                    <DialogDescription>按 Esc 或点击遮罩关闭</DialogDescription>
                </DialogHeader>
                <img src={src} alt="预览" className="max-h-[90vh] max-w-[90vw] rounded-xl object-contain" />
            </DialogContent>
        </Dialog>
    );
}
