import { ReactNode } from 'react';
import { cn } from '@/utils/cn';

export interface AdminCardProps {
  title?: string;
  extra?: ReactNode;
  children: ReactNode;
  className?: string;
  bodyClassName?: string;
  noPadding?: boolean;
}

export function AdminCard({
  title,
  extra,
  children,
  className,
  bodyClassName,
  noPadding = false,
}: AdminCardProps) {
  return (
    <div className={cn('admin-card', className)}>
      {(title || extra) && (
        <div className="admin-card-header">
          {title && <h3 className="admin-card-title">{title}</h3>}
          {extra && <div className="flex items-center gap-2">{extra}</div>}
        </div>
      )}
      <div className={cn(!noPadding && 'admin-card-body', bodyClassName)}>{children}</div>
    </div>
  );
}

export interface AdminCardStatsProps {
  title: string;
  value: string | number;
  icon?: ReactNode;
  trend?: {
    value: number;
    isPositive: boolean;
  };
  className?: string;
}

export function AdminCardStats({ title, value, icon, trend, className }: AdminCardStatsProps) {
  return (
    <div className={cn('admin-card p-5', className)}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-gray-500 mb-1">{title}</p>
          <p className="text-2xl font-bold text-gray-900">{value}</p>
          {trend && (
            <p
              className={cn(
                'text-sm mt-2 flex items-center gap-1',
                trend.isPositive ? 'text-green-600' : 'text-red-500'
              )}
            >
              {trend.isPositive ? (
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 15l7-7 7 7" />
                </svg>
              ) : (
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              )}
              <span>{Math.abs(trend.value)}%</span>
              <span className="text-gray-400">较昨日</span>
            </p>
          )}
        </div>
        {icon && (
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-orange-400 to-orange-600 flex items-center justify-center text-white">
            {icon}
          </div>
        )}
      </div>
    </div>
  );
}
