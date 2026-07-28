import { Button } from '@/components/ui/button';

interface DashboardErrorBannerProps {
    errors: Array<Error | null>;
}

export function DashboardErrorBanner({ errors }: DashboardErrorBannerProps) {
    const firstErrorMessage = errors.find(Boolean)?.message;

    return (
        <section
            style={{
                background: 'linear-gradient(135deg, rgba(244,63,94,0.06), rgba(244,63,94,0.02))',
                border: '1px solid rgba(244,63,94,0.12)',
                borderRadius: 20,
                padding: '1.1rem 1.4rem',
                marginBottom: '1.75rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.85rem',
                animation: 'dashSlideDown 0.5s cubic-bezier(0.16, 1, 0.3, 1) both',
                backdropFilter: 'blur(12px)',
                WebkitBackdropFilter: 'blur(12px)',
            }}
        >
            <div
                style={{
                    width: 38,
                    height: 38,
                    borderRadius: 12,
                    background: 'linear-gradient(135deg, rgba(244,63,94,0.12), rgba(244,63,94,0.05))',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                }}
            >
                <svg
                    aria-hidden="true"
                    width="18"
                    height="18"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="#E11D48"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                >
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
            </div>
            <div style={{ flex: 1 }}>
                <div
                    style={{
                        fontSize: '0.88rem',
                        fontWeight: 600,
                        color: '#E11D48',
                        marginBottom: '0.15rem',
                    }}
                >
                    数据加载失败
                </div>
                <div style={{ fontSize: '0.81rem', color: '#9B9590' }}>
                    无法连接到服务器，请检查后端服务是否启动
                    {firstErrorMessage && ` · ${firstErrorMessage}`}
                </div>
            </div>
            <Button
                variant="default"
                onClick={() => window.location.reload()}
                style={{
                    padding: '0.5rem 1.15rem',
                    borderRadius: 12,
                    background: 'linear-gradient(135deg, #F43F5E, #E11D48)',
                    color: '#fff',
                    fontSize: '0.82rem',
                    fontWeight: 600,
                    border: 'none',
                    cursor: 'pointer',
                    whiteSpace: 'nowrap',
                    boxShadow: '0 3px 12px rgba(244,63,94,0.28)',
                    transition: 'all 0.25s ease',
                    fontFamily: "'LXGW WenKai', sans-serif",
                }}
                onMouseEnter={e => {
                    e.currentTarget.style.transform = 'translateY(-1px)';
                    e.currentTarget.style.boxShadow = '0 6px 20px rgba(244,63,94,0.35)';
                }}
                onMouseLeave={e => {
                    e.currentTarget.style.transform = 'translateY(0)';
                    e.currentTarget.style.boxShadow = '0 3px 12px rgba(244,63,94,0.28)';
                }}
            >
                刷新页面
            </Button>
        </section>
    );
}
