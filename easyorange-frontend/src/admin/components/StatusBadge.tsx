import { Badge } from '@/components/ui';
import { cn } from '@/lib/utils';

export interface StatusBadgeProps {
    status: number | string;
    type: 'user' | 'product' | 'order';
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
};

/**
 * 状态视觉的唯一出口。
 *
 * 此前这三份配置是模块私有的，页面为了做筛选下拉又各抄一份标签，订单详情更是连
 * 背景渐变和圆点色一起重写了一遍——同一状态在列表、徽章、详情三处可以显示成三个样子。
 * 页面要标签、要筛选选项、要直接上色，都从这里取。
 */
export function statusVisual(
    type: StatusBadgeProps['type'],
    status: number | string | null | undefined
): { label: string; bg: string; color: string; dot: string } {
    const key = String(status ?? '');
    const config = configMap[type][key];
    const vs = variantConfig[config?.variant ?? 'default'];
    const label = config?.label ?? (key.trim() ? key : '未知');
    return { label, ...vs };
}

/** 由状态配置派生筛选下拉选项，避免页面再抄一份标签。 */
export function statusFilterOptions(type: StatusBadgeProps['type'], allLabel = '全部状态') {
    return [
        { value: '', label: allLabel },
        ...Object.entries(configMap[type]).map(([value, config]) => ({ value, label: config.label })),
    ];
}

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
