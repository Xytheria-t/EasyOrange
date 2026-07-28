import { PendingItemRow } from './PendingItemRow';

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
                            <PendingItemRow
                                count={pendingItems.pendingProducts}
                                label="个商品待审核"
                                theme="orange"
                                actionLink="/admin/products?status=0"
                                actionText="立即处理"
                            />
                        )}
                        {pendingItems?.pendingReports > 0 && (
                            <PendingItemRow
                                count={pendingItems.pendingReports}
                                label="条举报待处理"
                                theme="rose"
                                actionLink="/admin/reports"
                                actionText="立即处理"
                            />
                        )}
                        {pendingItems?.pendingOrders > 0 && (
                            <PendingItemRow
                                count={pendingItems.pendingOrders}
                                label="笔待处理订单"
                                theme="amber"
                                actionLink="/admin/orders"
                                actionText="立即处理"
                            />
                        )}
                    </ul>
                )}
            </div>
        </section>
    );
}
