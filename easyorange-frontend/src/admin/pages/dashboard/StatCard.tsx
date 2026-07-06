import { useState } from 'react';

interface StatCardProps {
    title: string;
    value: number | string;
    growth?: number;
    sub?: string;
    icon?: React.ReactNode;
    accent?: 'orange' | 'rose' | 'purple' | 'gold' | 'emerald';
}

function formatValue(value: number | string): string {
    if (typeof value === 'string') {
        return value;
    }
    if (value >= 10000) {
        return `${(value / 10000).toFixed(1)}万`;
    }
    return value.toLocaleString('zh-CN');
}

const ACCENT_CONFIG = {
    orange: {
        iconBg: 'linear-gradient(135deg, rgba(249,115,22,0.14), rgba(249,115,22,0.04))',
        iconColor: '#F97316',
        hoverShadow: '0 12px 32px -8px rgba(42,37,32,0.1), 0 4px 16px -4px rgba(249,115,22,0.18)',
        glowBg: 'radial-gradient(circle at 85% 15%, rgba(249,115,22,0.08) 0%, transparent 60%)',
        borderAccent: 'rgba(249,115,22,0.12)',
    },
    rose: {
        iconBg: 'linear-gradient(135deg, rgba(251,113,133,0.14), rgba(251,113,133,0.04))',
        iconColor: '#FB7185',
        hoverShadow: '0 12px 32px -8px rgba(42,37,32,0.1), 0 4px 16px -4px rgba(251,113,133,0.18)',
        glowBg: 'radial-gradient(circle at 85% 15%, rgba(251,113,133,0.08) 0%, transparent 60%)',
        borderAccent: 'rgba(251,113,133,0.12)',
    },
    purple: {
        iconBg: 'linear-gradient(135deg, rgba(195,155,211,0.16), rgba(195,155,211,0.05))',
        iconColor: '#C39BD3',
        hoverShadow: '0 12px 32px -8px rgba(42,37,32,0.1), 0 4px 16px -4px rgba(195,155,211,0.18)',
        glowBg: 'radial-gradient(circle at 85% 15%, rgba(195,155,211,0.08) 0%, transparent 60%)',
        borderAccent: 'rgba(195,155,211,0.12)',
    },
    gold: {
        iconBg: 'linear-gradient(135deg, rgba(251,191,36,0.14), rgba(251,191,36,0.04))',
        iconColor: '#FBBF24',
        hoverShadow: '0 12px 32px -8px rgba(42,37,32,0.1), 0 4px 16px -4px rgba(251,191,36,0.18)',
        glowBg: 'radial-gradient(circle at 85% 15%, rgba(251,191,36,0.08) 0%, transparent 60%)',
        borderAccent: 'rgba(251,191,36,0.12)',
    },
    emerald: {
        iconBg: 'linear-gradient(135deg, rgba(16,185,129,0.12), rgba(16,185,129,0.03))',
        iconColor: '#10B981',
        hoverShadow: '0 12px 32px -8px rgba(42,37,32,0.1), 0 4px 16px -4px rgba(16,185,129,0.18)',
        glowBg: 'radial-gradient(circle at 85% 15%, rgba(16,185,129,0.08) 0%, transparent 60%)',
        borderAccent: 'rgba(16,185,129,0.12)',
    },
} as const;

export function StatCard({ title, value, growth, sub, icon, accent = 'orange' }: StatCardProps) {
    const [shining, setShining] = useState(false);
    const showGrowth = growth !== undefined && growth !== 0;
    const isPositive = growth !== undefined && growth > 0;
    const cfg = ACCENT_CONFIG[accent];

    return (
        <button
            type="button"
            style={{
                position: 'relative',
                background: 'rgba(255,255,255,0.72)',
                backdropFilter: 'blur(20px)',
                WebkitBackdropFilter: 'blur(20px)',
                border: '1px solid rgba(255,255,255,0.7)',
                borderRadius: 24,
                padding: '1.85rem 1.65rem',
                overflow: 'hidden',
                transition: 'all 0.35s cubic-bezier(0.16, 1, 0.3, 1)',
                cursor: 'default',
                fontFamily: 'inherit',
                fontSize: 'inherit',
                color: 'inherit',
                textAlign: 'left',
                width: '100%',
                borderTop: '1px solid rgba(255,255,255,0.7)',
                borderRight: '1px solid rgba(255,255,255,0.7)',
                borderBottom: '1px solid rgba(255,255,255,0.7)',
                borderLeft: '1px solid rgba(255,255,255,0.7)',
                backgroundImage: 'none',
            }}
            onMouseEnter={e => {
                e.currentTarget.style.transform = 'translateY(-4px)';
                e.currentTarget.style.boxShadow = cfg.hoverShadow;
                e.currentTarget.style.borderColor = cfg.borderAccent;
                setShining(true);
                setTimeout(() => setShining(false), 600);
            }}
            onMouseLeave={e => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = 'none';
                e.currentTarget.style.borderColor = 'rgba(255,255,255,0.7)';
            }}
        >
            <style>{`@keyframes adminShineSweep { from { transform: translateX(-100%); } to { transform: translateX(200%); } }`}</style>
            {/* Shine sweep */}
            {shining && (
                <div
                    style={{
                        position: 'absolute',
                        inset: 0,
                        background:
                            'linear-gradient(105deg, transparent 35%, rgba(255,255,255,0.45) 42%, rgba(255,255,255,0) 48%, transparent 52%)',
                        pointerEvents: 'none',
                        animation: 'adminShineSweep 0.6s ease-out forwards',
                    }}
                />
            )}
            {/* Accent glow */}
            <div
                style={{
                    position: 'absolute',
                    inset: 0,
                    background: cfg.glowBg,
                    pointerEvents: 'none',
                    transition: 'opacity 0.35s ease',
                }}
            />

            {/* Decorative corner ring */}
            <div
                style={{
                    position: 'absolute',
                    top: '-16px',
                    right: '-16px',
                    width: 82,
                    height: 82,
                    borderRadius: '50%',
                    border: `2px solid ${cfg.borderAccent}`,
                    opacity: 0.4,
                    pointerEvents: 'none',
                }}
            />

            <div
                style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    justifyContent: 'space-between',
                    gap: '1rem',
                    position: 'relative',
                    zIndex: 1,
                }}
            >
                <div style={{ flex: 1, minWidth: 0 }}>
                    <p
                        style={{
                            fontSize: '0.82rem',
                            fontWeight: 600,
                            color: '#9B9590',
                            letterSpacing: '0.04em',
                            textTransform: 'uppercase',
                            marginBottom: '0.75rem',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {title}
                    </p>
                    <p
                        style={{
                            fontFamily: "'DM Sans', 'Playfair Display', serif",
                            fontSize: '2.35rem',
                            fontWeight: 700,
                            color: '#2A2520',
                            letterSpacing: '-0.03em',
                            lineHeight: 1.15,
                        }}
                    >
                        {formatValue(value)}
                    </p>
                    {showGrowth && (
                        <span
                            style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '0.2rem',
                                marginTop: '0.6rem',
                                padding: '0.2rem 0.5rem',
                                borderRadius: 9999,
                                fontSize: '0.73rem',
                                fontWeight: 600,
                                letterSpacing: '0.01em',
                                background: isPositive ? 'rgba(16,185,129,0.08)' : 'rgba(244,63,94,0.08)',
                                color: isPositive ? '#059669' : '#E11D48',
                            }}
                        >
                            {isPositive ? (
                                <svg
                                    aria-hidden="true"
                                    width="11"
                                    height="11"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2.5"
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                >
                                    <path d="M18 15l-6-6-6 6" />
                                </svg>
                            ) : (
                                <svg
                                    aria-hidden="true"
                                    width="11"
                                    height="11"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2.5"
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                >
                                    <path d="M6 9l6 6 6-6" />
                                </svg>
                            )}
                            {Math.abs(growth)}%
                        </span>
                    )}
                    {sub && !showGrowth && (
                        <span
                            style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                marginTop: '0.6rem',
                                fontSize: '0.73rem',
                                fontWeight: 500,
                                color: '#9B9590',
                                letterSpacing: '0.01em',
                            }}
                        >
                            {sub}
                        </span>
                    )}
                </div>
                {icon && (
                    <div
                        style={{
                            width: 58,
                            height: 58,
                            borderRadius: 16,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            flexShrink: 0,
                            background: cfg.iconBg,
                            color: cfg.iconColor,
                            position: 'relative',
                            overflow: 'hidden',
                        }}
                    >
                        <div style={{ position: 'relative', zIndex: 1, display: 'flex' }}>{icon}</div>
                    </div>
                )}
            </div>
        </button>
    );
}
