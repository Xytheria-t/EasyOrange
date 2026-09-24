import { Link } from 'react-router-dom';

function ForbiddenPage() {
    return (
        <div
            style={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '1rem',
                position: 'relative',
                overflow: 'hidden',
                background: 'linear-gradient(180deg, #FAF8F5 0%, #FDF9F6 40%, #FAF8F5 100%)',
            }}
        >
            {/* Atmosphere */}
            <div
                style={{
                    position: 'absolute',
                    inset: 0,
                    pointerEvents: 'none',
                    background: `
          radial-gradient(ellipse 55% 35% at 15% 20%, rgba(244,63,94,0.04) 0%, transparent 50%),
          radial-gradient(ellipse 40% 45% at 85% 80%, rgba(195,155,211,0.03) 0%, transparent 48%)
        `,
                }}
            />

            <div style={{ position: 'relative', zIndex: 1, textAlign: 'center', maxWidth: 400, width: '100%' }}>
                {/* 403 visual */}
                <div style={{ position: 'relative', marginBottom: '2rem' }}>
                    <div
                        style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: 'clamp(6rem, 15vw, 9rem)',
                            fontWeight: 700,
                            background: 'linear-gradient(135deg, rgba(244,63,94,0.08), rgba(249,115,22,0.06))',
                            WebkitBackgroundClip: 'text',
                            WebkitTextFillColor: 'transparent',
                            backgroundClip: 'text',
                            lineHeight: 1,
                            userSelect: 'none',
                        }}
                    >
                        403
                    </div>
                    <div
                        style={{
                            position: 'absolute',
                            inset: 0,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                        }}
                    >
                        <div
                            style={{
                                width: 80,
                                height: 80,
                                borderRadius: 24,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                background: 'linear-gradient(135deg, rgba(244,63,94,0.10), rgba(251,113,133,0.06))',
                                border: '1px solid rgba(244,63,94,0.08)',
                                color: '#E11D48',
                                boxShadow: '0 8px 32px rgba(244,63,94,0.08)',
                            }}
                        >
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

                <h1
                    style={{
                        fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                        fontSize: '1.5rem',
                        fontWeight: 700,
                        color: '#2A2520',
                        marginBottom: '0.6rem',
                    }}
                >
                    访问受限
                </h1>
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
                            color: '#fff',
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
                            background: '#fff',
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
