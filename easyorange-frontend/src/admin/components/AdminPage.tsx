import { AlertTriangle, RefreshCw } from 'lucide-react';
import type { ReactNode } from 'react';
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
            <div className="admin-page admin-page-body" style={{ animation: 'pageIn 0.5s var(--ease-out) both' }}>
                {children}
            </div>
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
            <div style={{ display: 'flex', gap: '0.9rem', alignItems: 'flex-start', minWidth: 0 }}>
                {icon ? (
                    <span
                        aria-hidden="true"
                        style={{
                            width: 34,
                            height: 34,
                            borderRadius: 11,
                            background: 'var(--admin-primary-bg)',
                            color: '#fff',
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            flexShrink: 0,
                            boxShadow: 'var(--admin-primary-shadow)',
                        }}
                    >
                        {icon}
                    </span>
                ) : null}
                <div style={{ minWidth: 0 }}>
                    <h1 className="admin-title">{title}</h1>
                    {description ? (
                        <p className="admin-subtitle" style={{ marginTop: '0.35rem' }}>
                            {description}
                        </p>
                    ) : null}
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
            <AlertTriangle
                size={17}
                aria-hidden="true"
                style={{ color: 'var(--admin-danger)', flexShrink: 0, marginTop: 1 }}
            />
            <div style={{ flex: 1, minWidth: 0 }}>
                <p style={{ margin: 0, fontSize: '0.88rem', fontWeight: 600, color: 'var(--admin-danger)' }}>
                    加载失败
                </p>
                <p style={{ margin: '0.2rem 0 0', fontSize: '0.84rem', color: 'var(--admin-ink-soft)' }}>{message}</p>
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

/** 玻璃内容卡。`grow` 用于撑满剩余高度的主表卡。 */
export function AdminCard({ grow = false, children }: { grow?: boolean; children: ReactNode }) {
    return (
        <section
            className={grow ? 'admin-card admin-card--grow' : 'admin-card'}
            style={{ animation: 'cardIn 0.45s var(--ease-out) both' }}
        >
            {children}
        </section>
    );
}

/** 工具栏内的分隔线，窄屏隐藏。 */
export function ToolbarDivider() {
    return <span className="admin-toolbar-divider" aria-hidden="true" />;
}
