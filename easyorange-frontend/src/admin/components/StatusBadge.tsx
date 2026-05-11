
export interface StatusBadgeProps {
  status: number;
  type: 'user' | 'product' | 'order' | 'report';
  className?: string;
}

const userStatusConfig: Record<number, { label: string; variant: 'success' | 'warning' | 'error' }> = {
  0: { label: '正常', variant: 'success' },
  1: { label: '禁用', variant: 'error' },
  2: { label: '锁定', variant: 'warning' },
};

const productStatusConfig: Record<number, { label: string; variant: 'success' | 'warning' | 'error' | 'info' | 'default' }> = {
  0: { label: '草稿', variant: 'default' },
  1: { label: '上架', variant: 'success' },
  2: { label: '已售', variant: 'info' },
  3: { label: '下架', variant: 'error' },
};

const orderStatusConfig: Record<number, { label: string; variant: 'success' | 'warning' | 'error' | 'info' | 'default' }> = {
  0: { label: '待付款', variant: 'warning' },
  1: { label: '待发货', variant: 'info' },
  2: { label: '已发货', variant: 'info' },
  3: { label: '已完成', variant: 'success' },
  4: { label: '已取消', variant: 'default' },
  5: { label: '退款中', variant: 'error' },
};

const reportStatusConfig: Record<number, { label: string; variant: 'success' | 'warning' | 'error' | 'info' | 'default' }> = {
  0: { label: '待处理', variant: 'error' },
  1: { label: '处理中', variant: 'warning' },
  2: { label: '已处理', variant: 'success' },
  3: { label: '已驳回', variant: 'default' },
};

const variantStyles: Record<string, { bg: string; color: string; dot: string }> = {
  success: { bg: 'linear-gradient(135deg, rgba(16,185,129,0.12), rgba(16,185,129,0.05))', color: '#059669', dot: '#10B981' },
  warning: { bg: 'linear-gradient(135deg, rgba(245,158,11,0.12), rgba(245,158,11,0.05))', color: '#D97706', dot: '#F59E0B' },
  error:   { bg: 'linear-gradient(135deg, rgba(244,63,94,0.10), rgba(244,63,94,0.05))', color: '#E11D48', dot: '#F43F5E' },
  info:    { bg: 'linear-gradient(135deg, rgba(59,130,246,0.10), rgba(59,130,246,0.05))', color: '#2563EB', dot: '#3B82F6' },
  default: { bg: 'linear-gradient(135deg, rgba(168,160,152,0.10), rgba(168,160,152,0.05))', color: '#9B9590', dot: '#C4BCB4' },
};

export function StatusBadge({ status, type }: StatusBadgeProps) {
  const configMap = {
    user: userStatusConfig,
    product: productStatusConfig,
    order: orderStatusConfig,
    report: reportStatusConfig,
  };
  const config = configMap[type][status];

  if (!config) {
    return (
      <span style={{
        display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
        padding: '0.27rem 0.72rem', borderRadius: 9999,
        fontSize: '0.73rem', fontWeight: 600, letterSpacing: '0.02em',
        whiteSpace: 'nowrap', background: variantStyles.default.bg, color: variantStyles.default.color,
      }}>
        <span style={{ width: 6, height: 6, borderRadius: '50%', flexShrink: 0, background: variantStyles.default.dot }} />
        未知
      </span>
    );
  }

  const vs = variantStyles[config.variant];

  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
      padding: '0.27rem 0.72rem', borderRadius: 9999,
      fontSize: '0.73rem', fontWeight: 600, letterSpacing: '0.02em',
      whiteSpace: 'nowrap', background: vs.bg, color: vs.color,
    }}>
      <span style={{ width: 6, height: 6, borderRadius: '50%', flexShrink: 0, background: vs.dot }} />
      {config.label}
    </span>
  );
}

export interface CustomBadgeProps {
  children: React.ReactNode;
  variant?: 'success' | 'warning' | 'error' | 'info' | 'default';
  className?: string;
}

export function CustomBadge({ children, variant = 'default' }: CustomBadgeProps) {
  const vs = variantStyles[variant];
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
      padding: '0.27rem 0.72rem', borderRadius: 9999,
      fontSize: '0.73rem', fontWeight: 600, letterSpacing: '0.02em',
      whiteSpace: 'nowrap', background: vs.bg, color: vs.color,
    }}>
      {children}
    </span>
  );
}
