import { ChevronRight } from 'lucide-react';
import * as React from 'react';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';
import './breadcrumb.css';

const Breadcrumb = React.forwardRef<HTMLElement, React.ComponentProps<'nav'>>(({ className, ...props }, ref) => (
    <nav ref={ref} aria-label="面包屑" className={cn('bc-root', className)} {...props} />
));
Breadcrumb.displayName = 'Breadcrumb';

const BreadcrumbList = React.forwardRef<HTMLOListElement, React.ComponentProps<'ol'>>(
    ({ className, ...props }, ref) => <ol ref={ref} className={cn('bc-list', className)} {...props} />
);
BreadcrumbList.displayName = 'BreadcrumbList';

const BreadcrumbItem = React.forwardRef<HTMLLIElement, React.ComponentProps<'li'>>(({ className, ...props }, ref) => (
    <li ref={ref} className={cn('bc-item', className)} {...props} />
));
BreadcrumbItem.displayName = 'BreadcrumbItem';

interface BreadcrumbLinkProps extends Omit<React.ComponentProps<typeof Link>, 'to' | 'ref'> {
    /** 站外地址或需要整页跳转时用 <a>；站内路由一律走 <Link> */
    asExternal?: boolean;
    /** 站内路由地址 */
    to?: string;
    /** 站外绝对地址，配 asExternal 使用 */
    href?: string;
}

const BreadcrumbLink = React.forwardRef<HTMLAnchorElement, BreadcrumbLinkProps>(
    ({ className, asExternal = false, href, to, ...props }, ref) => {
        const classes = cn('bc-link', className);
        // href 绝不漏给 <Link>：整页跳转的地址混进 SPA 路由会渲染出错误跳转
        if (asExternal) {
            return <a ref={ref} className={classes} href={href} {...props} />;
        }
        return <Link ref={ref} className={classes} to={to ?? '#'} {...props} />;
    }
);
BreadcrumbLink.displayName = 'BreadcrumbLink';

const BreadcrumbPage = React.forwardRef<HTMLSpanElement, React.ComponentProps<'span'>>(
    ({ className, ...props }, ref) => (
        // 不挂 role="link"：当前页不可跳转，aria-current="page" 已把语义带给屏幕阅读器，
        // 伪交互角色反而会让辅助技术把它读成可点链接
        <span ref={ref} aria-current="page" className={cn('bc-current', className)} {...props} />
    )
);
BreadcrumbPage.displayName = 'BreadcrumbPage';

const BreadcrumbSeparator = ({ children, className }: { children?: React.ReactNode; className?: string }) => (
    <li role="presentation" aria-hidden="true" className={cn('bc-separator', className)}>
        {children ?? <ChevronRight className="bc-separator-icon" />}
    </li>
);
BreadcrumbSeparator.displayName = 'BreadcrumbSeparator';

export { Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator };
