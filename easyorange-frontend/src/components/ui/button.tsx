import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';
import * as React from 'react';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
    'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl text-[0.9375rem] font-semibold tracking-tight transition-all duration-200 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0 active:scale-[0.97]',
    {
        variants: {
            variant: {
                default:
                    'bg-primary text-primary-foreground shadow-[0_4px_12px_rgba(249,115,22,0.35)] hover:shadow-[0_8px_20px_rgba(249,115,22,0.45)] hover:-translate-y-0.5',
                destructive: 'bg-destructive text-destructive-foreground hover:bg-destructive/90',
                outline:
                    'border-2 border-border bg-background text-foreground hover:border-primary-400 hover:text-primary-500 hover:bg-primary-50 hover:-translate-y-0.5',
                secondary: 'bg-secondary text-secondary-foreground hover:bg-secondary/80',
                ghost: 'text-foreground hover:bg-muted hover:text-foreground',
                link: 'text-primary underline-offset-4 hover:underline',
            },
            size: {
                default: 'h-11 px-6 py-2.5',
                sm: 'h-9 rounded-lg px-4 text-sm',
                lg: 'h-12 rounded-2xl px-8 text-base',
                icon: 'h-10 w-10 rounded-xl',
            },
        },
        defaultVariants: {
            variant: 'default',
            size: 'default',
        },
    }
);

export interface ButtonProps
    extends React.ButtonHTMLAttributes<HTMLButtonElement>,
        VariantProps<typeof buttonVariants> {
    asChild?: boolean;
    isLoading?: boolean;
    loadingText?: string;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
    (
        {
            className,
            variant,
            size,
            asChild = false,
            isLoading,
            loadingText = '处理中...',
            children,
            disabled,
            ...props
        },
        ref
    ) => {
        if (asChild) {
            return (
                <Slot className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props}>
                    {children}
                </Slot>
            );
        }

        return (
            <button
                className={cn(buttonVariants({ variant, size, className }))}
                ref={ref}
                disabled={disabled || isLoading}
                {...props}
            >
                {isLoading && (
                    <svg className="animate-spin" viewBox="0 0 24 24" aria-hidden="true">
                        <circle
                            className="opacity-25"
                            cx="12"
                            cy="12"
                            r="10"
                            stroke="currentColor"
                            strokeWidth="4"
                            fill="none"
                        />
                        <path
                            className="opacity-75"
                            fill="currentColor"
                            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                        />
                    </svg>
                )}
                {isLoading ? loadingText : children}
            </button>
        );
    }
);
Button.displayName = 'Button';

export { Button, buttonVariants };
