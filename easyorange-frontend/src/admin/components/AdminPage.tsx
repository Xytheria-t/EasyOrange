import { AlertTriangle, RefreshCw } from 'lucide-react';
import type { CSSProperties, ReactNode } from 'react';
import { Button } from '@/components/ui/button';

/**
 * 管理端页面外壳 —— 唯一入口。
 *
 * 收敛前 5 个页面各自复制了一份「根容器 + 背景层 + 内容层 + 错误条」，
 * 且背景层 `borderRadius` 在 16/20/24 之间摇摆、错误条位置在标题前后不一致。
 */
export function AdminPage({ children }: { children: ReactNode }) {
    return (
        <div className="admin-page-root">
            <div className="admin-page-backdrop" aria-hidden="true" />
            <div className="admin-page admin-page-body">{children}</div>
        </div>
    );
}

interface AdminPageHeaderProps {
    icon?: ReactNode;
    title: string;
    description?: string;
    actions?: ReactNode;
}

/** 页头：图标 + 标题 + 说明 + 右侧操作，窄屏由 `.admin-page-header` 自动换行。 */
export function AdminPageHeader({ icon, title, description, actions }: AdminPageHeaderProps) {
    return (
        <header className="admin-page-header">
            <div className="admin-page-header-main">
                {icon ? (
                    <span className="admin-page-header-icon" aria-hidden="true">
                        {icon}
                    </span>
                ) : null}
                <div className="admin-page-header-text">
                    <h1 className="admin-title">{title}</h1>
                    {description ? <p className="admin-subtitle">{description}</p> : null}
                </div>
            </div>
            {actions ? <div className="admin-page-header-actions">{actions}</div> : null}
        </header>
    );
}

interface AdminErrorBannerProps {
    message?: string | null;
    onRetry?: () => void;
    retrying?: boolean;
}

/** 页面级错误出口。`role="alert"` 让读屏立刻播报；重试走 refetch 而不是整页 reload。 */
export function AdminErrorBanner({ message, onRetry, retrying = false }: AdminErrorBannerProps) {
    if (!message) {
        return null;
    }
    return (
        <div className="admin-error-banner" role="alert">
            <AlertTriangle size={17} aria-hidden="true" className="admin-error-banner-icon" />
            <div className="admin-error-banner-text">
                <p className="admin-error-banner-title">加载失败</p>
                <p className="admin-error-banner-message">{message}</p>
            </div>
            {onRetry ? (
                <Button variant="outline" size="sm" onClick={onRetry} disabled={retrying} className="shrink-0">
                    <RefreshCw size={14} aria-hidden="true" />
                    {retrying ? '重试中' : '重试'}
                </Button>
            ) : null}
        </div>
    );
}

/** 玻璃内容卡。`grow` 用于撑满剩余高度的主表卡；`className` 追加语义变体（如 .admin-kpi）。 */
export function AdminCard({
    grow = false,
    className,
    style,
    children,
}: {
    grow?: boolean;
    className?: string;
    /** 只给按数据算出来的值（如 KPI 卡的指标色变量），静态视觉一律走 className */
    style?: CSSProperties;
    children: ReactNode;
}) {
    return (
        <section
            className={`${grow ? 'admin-card admin-card--grow' : 'admin-card'}${className ? ` ${className}` : ''}`}
            style={style}
        >
            {children}
        </section>
    );
}

interface AdminCardHeadProps {
    title: string;
    icon?: ReactNode;
    /** 口径说明：这一块的数字怎么来的、含什么不含什么 */
    note?: ReactNode;
    actions?: ReactNode;
}

/** 卡片头：标题 + 口径说明 + 右侧动作。表格卡与图卡共用，此前每个页面各拼一版，
 *  标题字号、说明位置、分隔线样式三页三个样。 */
export function AdminCardHead({ title, icon, note, actions }: AdminCardHeadProps) {
    return (
        <div className="admin-card-head">
            <div>
                <h2 className="admin-card-head-title">
                    {icon}
                    {title}
                </h2>
                {note ? <p className="admin-card-head-note">{note}</p> : null}
            </div>
            {actions ? <div className="admin-page-header-actions">{actions}</div> : null}
        </div>
    );
}

interface AdminListCardProps {
    title: string;
    icon?: ReactNode;
    /** 计数口径行，见 AdminListCount */
    count?: ReactNode;
    /** 筛选工具栏槽：与表格同卡，窄屏换行到标题下方 */
    toolbar?: ReactNode;
    children: ReactNode;
}

/**
 * 列表页主卡 —— 一页一卡，表格（或分类树）就是页面主体。
 *
 * 此前四个列表页都是「工具栏卡 + 表格卡」上下堆叠：上面那张卡里只有一行筛选控件，
 * 既没有标题也没有数据，凭空多出一层边框和一块空白。现在工具栏并进卡头右侧，
 * 筛选与它筛的那张表读成同一块。
 */
export function AdminListCard({ title, icon, count, toolbar, children }: AdminListCardProps) {
    return (
        <AdminCard grow>
            <div className="admin-card-head admin-card-head--list">
                <div>
                    <h2 className="admin-card-head-title">
                        {icon}
                        {title}
                    </h2>
                    {count}
                </div>
                {toolbar ? <div className="admin-toolbar">{toolbar}</div> : null}
            </div>
            {children}
        </AdminCard>
    );
}

interface AdminListCountProps {
    /** 「共」/「筛选出」等前缀 */
    prefix: ReactNode;
    count: ReactNode;
    /** 量词后缀，可带口径（如「/ 20 个分类」） */
    suffix?: ReactNode;
}

/**
 * 列表计数：分页前的全量条数，是这一屏数据的口径，五个页面原本各拼一份。
 * 请求失败时调用方整段不渲染——报「共 0 条」会被读成真的没有数据。
 */
export function AdminListCount({ prefix, count, suffix }: AdminListCountProps) {
    return (
        <p className="admin-card-head-note">
            {prefix} <strong>{count}</strong>
            {suffix ? <> {suffix}</> : null}
        </p>
    );
}
