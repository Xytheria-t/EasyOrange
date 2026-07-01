import { AlertTriangle, Info } from 'lucide-react';
import type { ReactNode } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui';
import { Button } from '@/components/ui/button';

export interface ConfirmModalProps {
    isOpen: boolean;
    title: string;
    content: ReactNode;
    confirmText?: string;
    cancelText?: string;
    onConfirm: () => Promise<void> | void;
    onCancel: () => void;
    isLoading?: boolean;
    confirmDisabled?: boolean;
    variant?: 'danger' | 'warning' | 'info';
}

const VARIANT_CONFIG = {
    danger: {
        Icon: AlertTriangle,
        iconBg: 'bg-error/10',
        iconColor: 'text-error',
        buttonVariant: 'destructive' as const,
    },
    warning: {
        Icon: AlertTriangle,
        iconBg: 'bg-primary-100',
        iconColor: 'text-primary-600',
        buttonVariant: 'default' as const,
    },
    info: {
        Icon: Info,
        iconBg: 'bg-blue-50',
        iconColor: 'text-blue-500',
        buttonVariant: 'default' as const,
    },
};

export function ConfirmModal({
    isOpen,
    title,
    content,
    confirmText = '确认',
    cancelText = '取消',
    onConfirm,
    onCancel,
    isLoading = false,
    confirmDisabled = false,
    variant = 'danger',
}: ConfirmModalProps) {
    const { Icon, iconBg, iconColor, buttonVariant } = VARIANT_CONFIG[variant];

    return (
        <Dialog open={isOpen} onOpenChange={open => !open && onCancel()}>
            <DialogContent className="gap-0 overflow-hidden border-border/60 bg-white/94 p-0 backdrop-blur-2xl sm:max-w-[440px]">
                <div className="p-7">
                    <div className="flex gap-4">
                        <div
                            className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${iconBg} ${iconColor}`}
                        >
                            <Icon className="h-5 w-5" aria-hidden="true" />
                        </div>
                        <div className="flex-1 min-w-0">
                            <DialogHeader className="space-y-2 text-left">
                                <DialogTitle className="font-serif text-[1.05rem] leading-tight text-[#2A2520]">
                                    {title}
                                </DialogTitle>
                                <DialogDescription className="text-[0.87rem] leading-relaxed text-[#6B6460]">
                                    {content}
                                </DialogDescription>
                            </DialogHeader>
                        </div>
                    </div>
                </div>

                <DialogFooter className="flex-row justify-end gap-2.5 border-t border-[rgba(229,224,219,0.4)] bg-gradient-to-b from-[rgba(250,248,245,0.5)] to-[rgba(250,248,245,0.9)] px-6 py-4">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={onCancel}
                        disabled={isLoading}
                        className="h-9 rounded-xl px-5 text-[0.87rem]"
                    >
                        {cancelText}
                    </Button>
                    <Button
                        variant={buttonVariant}
                        size="sm"
                        onClick={onConfirm}
                        disabled={isLoading || confirmDisabled}
                        isLoading={isLoading}
                        className="h-9 rounded-xl px-6 text-[0.87rem]"
                    >
                        {confirmText}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
