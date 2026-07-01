import { cva, type VariantProps } from 'class-variance-authority';
import type * as React from 'react';
import { cn } from '@/lib/utils';

const badgeVariants = cva(
    'inline-flex items-center justify-center rounded-full border px-2.5 py-0.5 text-xs font-medium tracking-tight transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2',
    {
        variants: {
            variant: {
                default: 'border-transparent bg-primary-100 text-primary-700 hover:bg-primary-200',
                secondary: 'border-transparent bg-muted text-muted-foreground hover:bg-muted/80',
                destructive: 'border-transparent bg-error-light text-error hover:bg-error-light/80',
                outline: 'border-border text-foreground',
                success: 'border-transparent bg-success-light text-success hover:bg-success-light/80',
                warning: 'border-transparent bg-warning-light text-warning hover:bg-warning-light/80',
            },
        },
        defaultVariants: {
            variant: 'default',
        },
    }
);

export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
    return <div className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };
