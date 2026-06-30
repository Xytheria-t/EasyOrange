import type { ReactNode } from 'react';
import { TrendingUp, TrendingDown } from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui';

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
    <Card className={cn('rounded-2xl border border-border/60 bg-white shadow-md', className)}>
      {(title || extra) && (
        <CardHeader className="flex flex-row items-center justify-between gap-4 p-5 pb-0">
          {title && <CardTitle className="font-serif text-base font-bold tracking-tight">{title}</CardTitle>}
          {extra && <div className="flex items-center gap-2">{extra}</div>}
        </CardHeader>
      )}
      <CardContent className={cn(!noPadding && 'p-5', bodyClassName)}>{children}</CardContent>
    </Card>
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
    <Card className={cn('rounded-2xl border border-border/60 bg-white p-5 shadow-md transition-all hover:-translate-y-1 hover:shadow-lg', className)}>
      <div className="flex items-start justify-between">
        <div>
          <p className="mb-1 text-sm text-muted-foreground">{title}</p>
          <p className="text-2xl font-bold text-foreground">{value}</p>
          {trend && (
            <p
              className={cn(
                'mt-2 flex items-center gap-1 text-sm',
                trend.isPositive ? 'text-green-600' : 'text-red-500'
              )}
            >
              {trend.isPositive ? (
                <TrendingUp className="h-4 w-4" />
              ) : (
                <TrendingDown className="h-4 w-4" />
              )}
              <span>{Math.abs(trend.value)}%</span>
              <span className="text-muted-foreground/70">较昨日</span>
            </p>
          )}
        </div>
        {icon && (
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 text-white shadow-md">
            {icon}
          </div>
        )}
      </div>
    </Card>
  );
}
