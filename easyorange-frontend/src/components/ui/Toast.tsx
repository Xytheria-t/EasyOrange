import { useUIStore } from '@/store';
import { Toast, ToastClose, ToastDescription, ToastProvider, ToastViewport } from './toast-primitive';

const variantMap: Record<string, 'default' | 'success' | 'error' | 'info' | 'warning'> = {
    success: 'success',
    error: 'error',
    info: 'info',
    warning: 'warning',
};

export function ToastContainer() {
    const { toasts, removeToast } = useUIStore();

    return (
        <ToastProvider>
            {toasts.map(toast => (
                <Toast
                    key={toast.id}
                    variant={variantMap[toast.type] ?? 'default'}
                    className={`toast-item toast-${toast.type}`}
                    onOpenChange={(open: boolean) => !open && removeToast(toast.id)}
                >
                    <div className="grid gap-1">
                        <ToastDescription className="toast-message text-sm font-semibold">
                            {toast.message}
                        </ToastDescription>
                    </div>
                    <ToastClose aria-label="关闭通知" />
                </Toast>
            ))}
            <ToastViewport />
        </ToastProvider>
    );
}
