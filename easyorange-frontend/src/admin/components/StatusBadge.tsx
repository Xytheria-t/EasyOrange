import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';
import { Badge } from '@/components/ui';

export interface StatusBadgeProps {
  status: number | string;
  type: 'user' | 'product' | 'order' | 'report';
  className?: string;
}

const userStatusConfig: Record<number, { label: string; variant: StatusVariant }> = {
  0: { label: '正常', variant: 'success' },
  1: { label: '禁用', variant: 'error' },
  2: { label: '锁定', variant: 'warning' },
};

const productStatusConfig: Record<number, { label: string; variant: StatusVariant }> = {
  0: { label: '草稿', variant: 'default' },
  1: { label: '上架', variant: 'success' },
  2: { label: '已售', variant: 'info' },
  3: { label: '下架', variant: 'error' },
};

const orderStatusConfig: Record<number, { label: string; variant: StatusVariant }> = {
  0: { label: '待付款', variant: 'warning' },
  1: { label: '待发货', variant: 'info' },
  2: { label: '已发货', variant: 'info' },
  3: { label: '已完成', variant: 'success' },
  4: { label: '已取消', variant: 'default' },
  5: { label: '退款中', variant: 'error' },
};

const reportStatusConfig: Record<number, { label: string; variant: StatusVariant }> = {
  0: { label: '待处理', variant: 'error' },
  1: { label: '处理中', variant: 'warning' },
  2: { label: '已处理', variant: 'success' },
  3: { label: '已驳回', variant: 'default' },
};

type StatusVariant = 'success' | 'warning' | 'error' | 'info' | 'default';

const variantConfig: Record<StatusVariant, { bg: string; color: string; dot: string }> = {
  success: {
    bg: 'linear-gradient(135deg, rgba(16,185,129,0.12), rgba(16,185,129,0.05))',
    color: '#059669',
    dot: '#10B981',
  },
  warning: {
    bg: 'linear-gradient(135deg, rgba(245,158,11,0.12), rgba(245,158,11,0.05))',
    color: '#D97706',
    dot: '#F59E0B',
  },
  error: {
    bg: 'linear-gradient(135deg, rgba(244,63,94,0.10), rgba(244,63,94,0.05))',
    color: '#E11D48',
    dot: '#F43F5E',
  },
  info: {
    bg: 'linear-gradient(135deg, rgba(59,130,246,0.10), rgba(59,130,246,0.05))',
    color: '#2563EB',
    dot: '#3B82F6',
  },
  default: {
    bg: 'linear-gradient(135deg, rgba(168,160,152,0.10), rgba(168,160,152,0.05))',
    color: '#9B9590',
    dot: '#C4BCB4',
  },
};

const configMap = {
  user: userStatusConfig,
  product: productStatusConfig,
  order: orderStatusConfig,
  report: reportStatusConfig,
};

export function StatusBadge({ status, type, className }: StatusBadgeProps) {
  const numericStatus = typeof status === 'number' ? status : Number(status);
  const config = !Number.isNaN(numericStatus) ? configMap[type][numericStatus] : undefined;

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
      <span
        className="h-1.5 w-1.5 shrink-0 rounded-full"
        style={{ background: vs.dot }}
        aria-hidden="true"
      />
      {label}
    </Badge>
  );
}

export interface CustomBadgeProps {
  children: ReactNode;
  variant?: StatusVariant;
  className?: string;
}

export function CustomBadge({ children, variant = 'default', className }: CustomBadgeProps) {
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
      {children}
    </Badge>
  );
}
