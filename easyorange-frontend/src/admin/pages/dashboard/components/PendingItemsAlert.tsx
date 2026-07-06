import { Link } from 'react-router-dom';

interface PendingItems {
    pendingProducts: number;
    pendingReports: number;
    pendingOrders: number;
}

interface PendingItemsAlertProps {
    pendingItems?: PendingItems;
    isLoading: boolean;
}

export function PendingItemsAlert({ pendingItems, isLoading }: PendingItemsAlertProps) {
    const hasPendingItems =
        pendingItems &&
        (pendingItems.pendingProducts > 0 || pendingItems.pendingReports > 0 || pendingItems.pendingOrders > 0);

    const totalPending =
        (pendingItems?.pendingProducts ?? 0) + (pendingItems?.pendingReports ?? 0) + (pendingItems?.pendingOrders ?? 0);

    if (!hasPendingItems) {
        return null;
    }

    const sectionCard: React.CSSProperties = {
        background: 'rgba(255,255,255,0.65)',
        backdropFilter: 'blur(24px)',
        WebkitBackdropFilter: 'blur(24px)',
        border: '1px solid rgba(255,255,255,0.55)',
        borderRadius: 24,
        overflow: 'hidden',
        transition: 'all 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
        position: 'relative',
    };

    const sectionHeader: React.CSSProperties = {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '1.25rem 1.65rem',
        borderBottom: '1px solid rgba(229,224,219,0.3)',
        position: 'relative',
    };

    const sectionTitle: React.CSSProperties = {
        fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
        fontSize: '1.05rem',
        fontWeight: 700,
        color: '#2A2520',
        letterSpacing: '-0.02em',
        display: 'flex',
        alignItems: 'center',
        gap: '0.55rem',
    };

    return (
        <section
            style={{
                ...sectionCard,
                border: '1px solid rgba(249,115,22,0.12)',
                marginBottom: '2rem',
                animation: 'dashAlertReveal 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.35s both',
                overflow: 'hidden',
            }}
        >
            <div
                style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    right: 0,
                    height: 2,
                    background:
                        'linear-gradient(90deg, transparent, rgba(249,115,22,0.4), rgba(251,113,133,0.3), rgba(249,115,22,0.4), transparent)',
                    backgroundSize: '200% 100%',
                    animation: 'dashBorderShimmer 3s ease-in-out infinite',
                }}
            />

            <div style={sectionHeader}>
                <h2 style={sectionTitle}>
                    <span
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: 28,
                            height: 28,
                            borderRadius: 8,
                            background: 'linear-gradient(135deg, rgba(249,115,22,0.12), rgba(249,115,22,0.05))',
                            marginRight: '0.2rem',
                        }}
                    >
                        <svg
                            aria-hidden="true"
                            width="15"
                            height="15"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="#F97316"
                            strokeWidth="2.2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        >
                            <circle cx="12" cy="12" r="10" />
                            <line x1="12" y1="8" x2="12" y2="12" />
                            <line x1="12" y1="16" x2="12.01" y2="16" />
                        </svg>
                    </span>
                    待处理事项
                    <span
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            minWidth: 24,
                            height: 24,
                            padding: '0 7px',
                            borderRadius: 9999,
                            background: 'linear-gradient(135deg, #F97316, #EA580C)',
                            color: '#fff',
                            fontSize: '0.72rem',
                            fontWeight: 700,
                            letterSpacing: 0,
                            boxShadow: '0 2px 8px rgba(249,115,22,0.3)',
                        }}
                    >
                        {totalPending}
                    </span>
                </h2>
                <span
                    style={{
                        fontSize: '0.78rem',
                        color: '#B5AEA8',
                        fontWeight: 400,
                        fontFamily: "'LXGW WenKai', sans-serif",
                    }}
                >
                    需要您的关注
                </span>
            </div>
            <div style={{ padding: '1.1rem 1.5rem' }}>
                {isLoading ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
                        {Array.from({ length: 3 }).map((_, i) => (
                            <div
                                // biome-ignore lint/suspicious/noArrayIndexKey: stable list
                                key={i}
                                style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}
                            >
                                <div
                                    style={{
                                        width: 200,
                                        height: 16,
                                        background: 'rgba(229,224,219,0.35)',
                                        borderRadius: 8,
                                    }}
                                />
                                <div
                                    style={{
                                        width: 84,
                                        height: 32,
                                        background: 'rgba(229,224,219,0.25)',
                                        borderRadius: 10,
                                    }}
                                />
                            </div>
                        ))}
                    </div>
                ) : (
                    <ul
                        style={{
                            listStyle: 'none',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '0.3rem',
                            margin: 0,
                            padding: 0,
                        }}
                    >
                        {pendingItems?.pendingProducts > 0 && (
                            <li
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between',
                                    padding: '0.85rem 1.1rem',
                                    borderRadius: 14,
                                    background: 'rgba(249,115,22,0.02)',
                                    border: '1px solid rgba(249,115,22,0.06)',
                                    transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
                                }}
                                onMouseEnter={e => {
                                    e.currentTarget.style.background = 'rgba(249,115,22,0.05)';
                                    e.currentTarget.style.paddingLeft = '1.3rem';
                                    e.currentTarget.style.borderColor = 'rgba(249,115,22,0.12)';
                                }}
                                onMouseLeave={e => {
                                    e.currentTarget.style.background = 'rgba(249,115,22,0.02)';
                                    e.currentTarget.style.paddingLeft = '0.85rem';
                                    e.currentTarget.style.borderColor = 'rgba(249,115,22,0.06)';
                                }}
                            >
                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem' }}>
                                    <span
                                        style={{
                                            width: 9,
                                            height: 9,
                                            borderRadius: '50%',
                                            background: '#F97316',
                                            flexShrink: 0,
                                            boxShadow: '0 0 10px rgba(249,115,22,0.45)',
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
                                        <strong style={{ fontWeight: 700, color: '#EA580C' }}>
                                            {pendingItems.pendingProducts}
                                        </strong>{' '}
                                        个商品待审核
                                    </span>
                                </div>
                                <Link
                                    to="/admin/products?status=0"
                                    style={{
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        gap: '0.35rem',
                                        padding: '0.42rem 0.95rem',
                                        borderRadius: 9999,
                                        fontSize: '0.8rem',
                                        fontWeight: 600,
                                        color: '#fff',
                                        background: 'linear-gradient(135deg, #F97316, #EA580C)',
                                        textDecoration: 'none',
                                        transition: 'all 0.25s ease',
                                        letterSpacing: '0.01em',
                                        border: 'none',
                                        cursor: 'pointer',
                                        fontFamily: "'LXGW WenKai', sans-serif",
                                        boxShadow: '0 2px 8px rgba(249,115,22,0.2)',
                                    }}
                                    onMouseEnter={e => {
                                        e.currentTarget.style.transform = 'translateX(3px)';
                                        e.currentTarget.style.boxShadow = '0 4px 14px rgba(249,115,22,0.35)';
                                    }}
                                    onMouseLeave={e => {
                                        e.currentTarget.style.transform = 'translateX(0)';
                                        e.currentTarget.style.boxShadow = '0 2px 8px rgba(249,115,22,0.2)';
                                    }}
                                >
                                    立即处理
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
                        )}
                        {pendingItems?.pendingReports > 0 && (
                            <li
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between',
                                    padding: '0.85rem 1.1rem',
                                    borderRadius: 14,
                                    background: 'rgba(244,63,94,0.02)',
                                    border: '1px solid rgba(244,63,94,0.06)',
                                    transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
                                }}
                                onMouseEnter={e => {
                                    e.currentTarget.style.background = 'rgba(244,63,94,0.05)';
                                    e.currentTarget.style.paddingLeft = '1.3rem';
                                    e.currentTarget.style.borderColor = 'rgba(244,63,94,0.12)';
                                }}
                                onMouseLeave={e => {
                                    e.currentTarget.style.background = 'rgba(244,63,94,0.02)';
                                    e.currentTarget.style.paddingLeft = '0.85rem';
                                    e.currentTarget.style.borderColor = 'rgba(244,63,94,0.06)';
                                }}
                            >
                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem' }}>
                                    <span
                                        style={{
                                            width: 9,
                                            height: 9,
                                            borderRadius: '50%',
                                            background: '#F43F5E',
                                            flexShrink: 0,
                                            boxShadow: '0 0 10px rgba(244,63,94,0.45)',
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
                                        <strong style={{ fontWeight: 700, color: '#E11D48' }}>
                                            {pendingItems.pendingReports}
                                        </strong>{' '}
                                        条举报待处理
                                    </span>
                                </div>
                                <Link
                                    to="/admin/reports"
                                    style={{
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        gap: '0.35rem',
                                        padding: '0.42rem 0.95rem',
                                        borderRadius: 9999,
                                        fontSize: '0.8rem',
                                        fontWeight: 600,
                                        color: '#fff',
                                        background: 'linear-gradient(135deg, #F43F5E, #E11D48)',
                                        textDecoration: 'none',
                                        transition: 'all 0.25s ease',
                                        letterSpacing: '0.01em',
                                        border: 'none',
                                        cursor: 'pointer',
                                        fontFamily: "'LXGW WenKai', sans-serif",
                                        boxShadow: '0 2px 8px rgba(244,63,94,0.2)',
                                    }}
                                    onMouseEnter={e => {
                                        e.currentTarget.style.transform = 'translateX(3px)';
                                        e.currentTarget.style.boxShadow = '0 4px 14px rgba(244,63,94,0.35)';
                                    }}
                                    onMouseLeave={e => {
                                        e.currentTarget.style.transform = 'translateX(0)';
                                        e.currentTarget.style.boxShadow = '0 2px 8px rgba(244,63,94,0.2)';
                                    }}
                                >
                                    立即处理
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
                        )}
                        {pendingItems?.pendingOrders > 0 && (
                            <li
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between',
                                    padding: '0.85rem 1.1rem',
                                    borderRadius: 14,
                                    background: 'rgba(251,191,36,0.02)',
                                    border: '1px solid rgba(251,191,36,0.06)',
                                    transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
                                }}
                                onMouseEnter={e => {
                                    e.currentTarget.style.background = 'rgba(251,191,36,0.05)';
                                    e.currentTarget.style.paddingLeft = '1.3rem';
                                    e.currentTarget.style.borderColor = 'rgba(251,191,36,0.12)';
                                }}
                                onMouseLeave={e => {
                                    e.currentTarget.style.background = 'rgba(251,191,36,0.02)';
                                    e.currentTarget.style.paddingLeft = '0.85rem';
                                    e.currentTarget.style.borderColor = 'rgba(251,191,36,0.06)';
                                }}
                            >
                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem' }}>
                                    <span
                                        style={{
                                            width: 9,
                                            height: 9,
                                            borderRadius: '50%',
                                            background: '#FBBF24',
                                            flexShrink: 0,
                                            boxShadow: '0 0 10px rgba(251,191,36,0.45)',
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
                                        <strong style={{ fontWeight: 700, color: '#D97706' }}>
                                            {pendingItems.pendingOrders}
                                        </strong>{' '}
                                        笔待处理订单
                                    </span>
                                </div>
                                <Link
                                    to="/admin/orders"
                                    style={{
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        gap: '0.35rem',
                                        padding: '0.42rem 0.95rem',
                                        borderRadius: 9999,
                                        fontSize: '0.8rem',
                                        fontWeight: 600,
                                        color: '#fff',
                                        background: 'linear-gradient(135deg, #FBBF24, #D97706)',
                                        textDecoration: 'none',
                                        transition: 'all 0.25s ease',
                                        letterSpacing: '0.01em',
                                        border: 'none',
                                        cursor: 'pointer',
                                        fontFamily: "'LXGW WenKai', sans-serif",
                                        boxShadow: '0 2px 8px rgba(251,191,36,0.2)',
                                    }}
                                    onMouseEnter={e => {
                                        e.currentTarget.style.transform = 'translateX(3px)';
                                        e.currentTarget.style.boxShadow = '0 4px 14px rgba(251,191,36,0.35)';
                                    }}
                                    onMouseLeave={e => {
                                        e.currentTarget.style.transform = 'translateX(0)';
                                        e.currentTarget.style.boxShadow = '0 2px 8px rgba(251,191,36,0.2)';
                                    }}
                                >
                                    立即处理
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
                        )}
                    </ul>
                )}
            </div>
        </section>
    );
}
