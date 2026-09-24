import { AlertCircle, Inbox, type LucideIcon, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

const containerClass = 'flex flex-col items-center justify-center px-6 py-16 text-center';

interface EmptyStateProps {
    icon?: LucideIcon;
    title: string;
    description?: string;
    action?: React.ReactNode;
    className?: string;
}

/** 无数据。语义是"确实没有"，不要用它表示请求失败。 */
export function EmptyState({ icon: Icon = Inbox, title, description, action, className }: EmptyStateProps) {
    return (
        <div className={cn(containerClass, className)}>
            <Icon aria-hidden="true" className="mb-4 size-12 text-muted-foreground/50" />
            <p className="text-base font-medium text-foreground">{title}</p>
            {description ? <p className="mt-1.5 max-w-md text-sm text-muted-foreground">{description}</p> : null}
            {action ? <div className="mt-5">{action}</div> : null}
        </div>
    );
}

interface ErrorStateProps {
    title?: string;
    description?: string;
    /** 传 refetch 或任何"再来一次"的处理函数，组件会渲染重试按钮 */
    onRetry?: () => void;
    retryLabel?: string;
    className?: string;
}

/**
 * 请求失败。必须与 EmptyState 分开：失败时页面不能显示"暂无数据"，
 * 否则用户会误以为数据本身为空。默认带重试出口。
 */
export function ErrorState({
    title = '加载失败',
    description = '网络或服务暂时不可用，请稍后重试。',
    onRetry,
    retryLabel = '重新加载',
    className,
}: ErrorStateProps) {
    return (
        <div className={cn(containerClass, className)} role="alert">
            <AlertCircle aria-hidden="true" className="mb-4 size-12 text-destructive/70" />
            <p className="text-base font-medium text-foreground">{title}</p>
            <p className="mt-1.5 max-w-md text-sm text-muted-foreground">{description}</p>
            {onRetry ? (
                <Button variant="outline" onClick={onRetry} className="mt-5">
                    <RefreshCw aria-hidden="true" className="size-4" />
                    {retryLabel}
                </Button>
            ) : null}
        </div>
    );
}

interface LoadingStateProps {
    label?: string;
    className?: string;
}

/** 加载中。保留既有骨架屏，额外播报忙状态给辅助技术。 */
export function LoadingState({ label = '加载中…', className }: LoadingStateProps) {
    return (
        <div className={cn(containerClass, className)} role="status" aria-live="polite" aria-busy="true">
            <span className="sr-only">{label}</span>
            <RefreshCw aria-hidden="true" className="size-8 animate-spin text-muted-foreground/60" />
        </div>
    );
}
