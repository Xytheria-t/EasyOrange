import { Link } from 'react-router-dom';

function ForbiddenPage() {
    return (
        <div className="admin-forbidden-page">
            <div className="admin-forbidden-atmosphere" aria-hidden="true" />

            <div style={{ position: 'relative', zIndex: 1, textAlign: 'center', maxWidth: 400, width: '100%' }}>
                {/* 403 visual */}
                <div className="admin-forbidden-number-wrap">
                    <div className="admin-forbidden-number">403</div>
                    <div className="admin-forbidden-shield" aria-hidden="true">
                        <div className="admin-forbidden-shield-badge">
                            <svg
                                aria-hidden="true"
                                width="36"
                                height="36"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            >
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                                <path d="M12 8v4m0 4h.01" />
                            </svg>
                        </div>
                    </div>
                </div>

                <h1 className="admin-title mb-[0.6rem] !text-[1.5rem]">访问受限</h1>
                <p style={{ fontSize: '0.92rem', color: 'var(--admin-muted)', marginBottom: '2rem', lineHeight: 1.6 }}>
                    抱歉，您没有权限访问此页面。
                    <br />
                    请改用管理员账号登录，或联系开通权限后再访问。
                </p>

                <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', flexWrap: 'wrap' }}>
                    <Link
                        to="/"
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.5rem',
                            padding: '0.7rem 1.6rem',
                            borderRadius: 'var(--admin-radius-control)',
                            background: 'var(--admin-primary-bg)',
                            color: 'var(--admin-surface-solid)',
                            fontSize: '0.9rem',
                            fontWeight: 600,
                            textDecoration: 'none',
                            boxShadow: 'var(--admin-primary-shadow)',
                        }}
                    >
                        <svg
                            aria-hidden="true"
                            width="16"
                            height="16"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2.5"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        >
                            <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
                            <polyline points="9 22 9 12 15 12 15 22" />
                        </svg>
                        返回主站
                    </Link>
                    {/* 权限问题的可执行下一步：换账号重登，而不是一句「联系管理员」 */}
                    <Link
                        to="/login"
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.5rem',
                            padding: '0.7rem 1.4rem',
                            borderRadius: 'var(--admin-radius-control)',
                            border: 'var(--admin-control-border)',
                            background: 'var(--admin-surface-solid)',
                            color: 'var(--admin-ink-soft)',
                            fontSize: '0.9rem',
                            fontWeight: 600,
                            textDecoration: 'none',
                        }}
                    >
                        换个账号登录
                    </Link>
                </div>
            </div>
        </div>
    );
}

export default ForbiddenPage;
