import { Link } from 'react-router-dom';

type PendingItemTheme = 'orange' | 'rose' | 'amber';

const THEME_STYLES: Record<
    PendingItemTheme,
    {
        background: string;
        border: string;
        hoverBackground: string;
        hoverBorder: string;
        dot: string;
        dotShadow: string;
        countColor: string;
        buttonGradient: string;
        buttonShadow: string;
        buttonHoverShadow: string;
    }
> = {
    orange: {
        background: 'rgba(249,115,22,0.02)',
        border: 'rgba(249,115,22,0.06)',
        hoverBackground: 'rgba(249,115,22,0.05)',
        hoverBorder: 'rgba(249,115,22,0.12)',
        dot: '#F97316',
        dotShadow: 'rgba(249,115,22,0.45)',
        countColor: '#EA580C',
        buttonGradient: 'linear-gradient(135deg, #F97316, #EA580C)',
        buttonShadow: 'rgba(249,115,22,0.2)',
        buttonHoverShadow: 'rgba(249,115,22,0.35)',
    },
    rose: {
        background: 'rgba(244,63,94,0.02)',
        border: 'rgba(244,63,94,0.06)',
        hoverBackground: 'rgba(244,63,94,0.05)',
        hoverBorder: 'rgba(244,63,94,0.12)',
        dot: '#F43F5E',
        dotShadow: 'rgba(244,63,94,0.45)',
        countColor: '#E11D48',
        buttonGradient: 'linear-gradient(135deg, #F43F5E, #E11D48)',
        buttonShadow: 'rgba(244,63,94,0.2)',
        buttonHoverShadow: 'rgba(244,63,94,0.35)',
    },
    amber: {
        background: 'rgba(251,191,36,0.02)',
        border: 'rgba(251,191,36,0.06)',
        hoverBackground: 'rgba(251,191,36,0.05)',
        hoverBorder: 'rgba(251,191,36,0.12)',
        dot: '#FBBF24',
        dotShadow: 'rgba(251,191,36,0.45)',
        countColor: '#D97706',
        buttonGradient: 'linear-gradient(135deg, #FBBF24, #D97706)',
        buttonShadow: 'rgba(251,191,36,0.2)',
        buttonHoverShadow: 'rgba(251,191,36,0.35)',
    },
};

interface PendingItemRowProps {
    count: number;
    label: string;
    theme: PendingItemTheme;
    actionLink: string;
    actionText: string;
}

export function PendingItemRow({ count, label, theme, actionLink, actionText }: PendingItemRowProps) {
    const styles = THEME_STYLES[theme];

    return (
        <li
            style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0.85rem 1.1rem',
                borderRadius: 14,
                background: styles.background,
                border: `1px solid ${styles.border}`,
                transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
            }}
            onMouseEnter={e => {
                e.currentTarget.style.background = styles.hoverBackground;
                e.currentTarget.style.paddingLeft = '1.3rem';
                e.currentTarget.style.borderColor = styles.hoverBorder;
            }}
            onMouseLeave={e => {
                e.currentTarget.style.background = styles.background;
                e.currentTarget.style.paddingLeft = '0.85rem';
                e.currentTarget.style.borderColor = styles.border;
            }}
        >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem' }}>
                <span
                    style={{
                        width: 9,
                        height: 9,
                        borderRadius: '50%',
                        background: styles.dot,
                        flexShrink: 0,
                        boxShadow: `0 0 10px ${styles.dotShadow}`,
                        animation: 'dashPulse 2s ease-in-out infinite',
                    }}
                />
                <span
                    style={{
                        fontSize: '0.9rem',
                        color: '#4A4540',
                        fontWeight: 500,
                        fontFamily: "'LXGW WenKai', sans-serif",
                    }}
                >
                    <strong style={{ fontWeight: 700, color: styles.countColor }}>{count}</strong> {label}
                </span>
            </div>
            <Link
                to={actionLink}
                style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.35rem',
                    padding: '0.42rem 0.95rem',
                    borderRadius: 9999,
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    color: '#fff',
                    background: styles.buttonGradient,
                    textDecoration: 'none',
                    transition: 'all 0.25s ease',
                    letterSpacing: '0.01em',
                    border: 'none',
                    cursor: 'pointer',
                    fontFamily: "'LXGW WenKai', sans-serif",
                    boxShadow: `0 2px 8px ${styles.buttonShadow}`,
                }}
                onMouseEnter={e => {
                    e.currentTarget.style.transform = 'translateX(3px)';
                    e.currentTarget.style.boxShadow = `0 4px 14px ${styles.buttonHoverShadow}`;
                }}
                onMouseLeave={e => {
                    e.currentTarget.style.transform = 'translateX(0)';
                    e.currentTarget.style.boxShadow = `0 2px 8px ${styles.buttonShadow}`;
                }}
            >
                {actionText}
                <svg
                    aria-hidden="true"
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                >
                    <path d="M5 12h14M12 5l7 7-7 7" />
                </svg>
            </Link>
        </li>
    );
}
