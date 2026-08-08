import { Badge } from '@/components/ui';
import { cn } from '@/lib/utils';

export interface StatusBadgeProps {
    status: number | string;
    type: 'user' | 'product' | 'order' | 'report';
    className?: string;
}

type StatusConfig = { label: string; variant: StatusVariant };

const userStatusConfig: Record<string, StatusConfig> = {
    NORMAL: { label: '正常', variant: 'success' },
    DISABLED: { label: '禁用', variant: 'error' },
    LOCKED: { label: '锁定', variant: 'warning' },
};

const productStatusConfig: Record<string, StatusConfig> = {
    DRAFT: { label: '草稿', variant: 'default' },
    ONLINE: { label: '上架', variant: 'success' },
    SOLD: { label: '已售', variant: 'info' },
    OFFLINE: { label: '下架', variant: 'error' },
    PENDING_REVIEW: { label: '待审核', variant: 'warning' },
    REJECTED: { label: '已驳回', variant: 'error' },
};

const orderStatusConfig: Record<string, StatusConfig> = {
    PENDING_PAYMENT: { label: '待付款', variant: 'warning' },
    PAID: { label: '待发货', variant: 'info' },
    SHIPPED: { label: '已发货', variant: 'info' },
    COMPLETED: { label: '已完成', variant: 'success' },
    CANCELLED: { label: '已取消', variant: 'default' },
    REFUNDED: { label: '退款中', variant: 'error' },
};

const reportStatusConfig: Record<string, StatusConfig> = {
    0: { label: '待处理', variant: 'error' },
    1: { label: '处理中', variant: 'warning' },
    2: { label: '已处理', variant: 'success' },
    3: { label: '已驳回', variant: 'default' },
};

type StatusVariant = 'success' | 'warning' | 'error' | 'info' | 'default';

const variantConfig: Record<StatusVariant, { bg: string; color: string; dot: string }> = {
    success: {
        bg: 'var(--status-success-bg)',
        color: 'var(--status-success)',
        dot: 'var(--status-success-dot)',
    },
    warning: {
        bg: 'var(--status-warning-bg)',
        color: 'var(--status-warning)',
        dot: 'var(--status-warning-dot)',
    },
    error: {
        bg: 'var(--status-error-bg)',
        color: 'var(--status-error)',
        dot: 'var(--status-error-dot)',
    },
    info: {
        bg: 'var(--status-info-bg)',
        color: 'var(--status-info)',
        dot: 'var(--status-info-dot)',
    },
    default: {
        bg: 'var(--status-default-bg)',
        color: 'var(--status-default)',
        dot: 'var(--status-default-dot)',
    },
};

const configMap: Record<StatusBadgeProps['type'], Record<string, StatusConfig>> = {
    user: userStatusConfig,
    product: productStatusConfig,
    order: orderStatusConfig,
    report: reportStatusConfig,
};

export function StatusBadge({ status, type, className }: StatusBadgeProps) {
    const config = configMap[type][String(status)];
    const fallbackLabel = typeof status === 'string' && status.trim() ? status : '未知';
    const { label, variant } = config ?? { label: fallbackLabel, variant: 'default' as StatusVariant };
    const vs = variantConfig[variant];

    return (
        <Badge
            variant="outline"
            className={cn(
                'inline-flex items-center gap-1.5 border-0 px-2.5 py-[0.27rem] text-[0.73rem] font-semibold tracking-wide rounded-full pointer-events-none',
                className
            )}
            style={{
                background: vs.bg,
                color: vs.color,
            }}
        >
            <span className="h-1.5 w-1.5 shrink-0 rounded-full" style={{ background: vs.dot }} aria-hidden="true" />
            {label}
        </Badge>
    );
}
