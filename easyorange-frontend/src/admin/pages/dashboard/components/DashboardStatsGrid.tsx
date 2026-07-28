import { StatCard } from '../StatCard';

interface DashboardStats {
    totalUsers?: number;
    todayNewUsers?: number;
    totalProducts?: number;
    pendingProducts?: number;
    totalOrders?: number;
}

interface DashboardStatsGridProps {
    stats?: DashboardStats;
    isLoading: boolean;
}

export function DashboardStatsGrid({ stats, isLoading }: DashboardStatsGridProps) {
    return (
        <div
            style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(5, 1fr)',
                gap: '1.4rem',
                marginBottom: '2.25rem',
                animation: 'dashGridReveal 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both',
            }}
        >
            {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                    <div
                        // biome-ignore lint/suspicious/noArrayIndexKey: stable list
                        key={i}
                        style={{
                            background: 'rgba(255,255,255,0.65)',
                            backdropFilter: 'blur(24px)',
                            WebkitBackdropFilter: 'blur(24px)',
                            border: '1px solid rgba(255,255,255,0.55)',
                            borderRadius: 24,
                            padding: '1.85rem 1.65rem',
                            animation: `dashStatCardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) ${0.08 + i * 0.06}s both`,
                        }}
                    >
                        <div
                            style={{
                                width: '42%',
                                height: 13,
                                background: 'rgba(229,224,219,0.4)',
                                borderRadius: 6,
                                marginBottom: '0.85rem',
                            }}
                        />
                        <div
                            style={{
                                width: '58%',
                                height: 34,
                                background: 'rgba(229,224,219,0.3)',
                                borderRadius: 8,
                            }}
                        />
                    </div>
                ))
            ) : (
                <>
                    <div style={{ animation: `dashStatCardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.05s both` }}>
                        <StatCard
                            title="用户总数"
                            value={stats?.totalUsers ?? 0}
                            accent="orange"
                            icon={
                                <svg
                                    aria-hidden="true"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                    strokeWidth="1.8"
                                    width="26"
                                    height="26"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"
                                    />
                                </svg>
                            }
                        />
                    </div>
                    <div style={{ animation: `dashStatCardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.11s both` }}>
                        <StatCard
                            title="今日新增"
                            value={stats?.todayNewUsers ?? 0}
                            accent="rose"
                            icon={
                                <svg
                                    aria-hidden="true"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                    strokeWidth="1.8"
                                    width="26"
                                    height="26"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"
                                    />
                                </svg>
                            }
                        />
                    </div>
                    <div style={{ animation: `dashStatCardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.17s both` }}>
                        <StatCard
                            title="商品总数"
                            value={stats?.totalProducts ?? 0}
                            accent="purple"
                            icon={
                                <svg
                                    aria-hidden="true"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                    strokeWidth="1.8"
                                    width="26"
                                    height="26"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
                                    />
                                </svg>
                            }
                        />
                    </div>
                    <div style={{ animation: `dashStatCardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.23s both` }}>
                        <StatCard
                            title="待审商品"
                            value={stats?.pendingProducts ?? 0}
                            accent="gold"
                            icon={
                                <svg
                                    aria-hidden="true"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                    strokeWidth="1.8"
                                    width="26"
                                    height="26"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                                    />
                                </svg>
                            }
                        />
                    </div>
                    <div style={{ animation: `dashStatCardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.29s both` }}>
                        <StatCard
                            title="订单总量"
                            value={stats?.totalOrders ?? 0}
                            accent="emerald"
                            icon={
                                <svg
                                    aria-hidden="true"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                    strokeWidth="1.8"
                                    width="26"
                                    height="26"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                                    />
                                </svg>
                            }
                        />
                    </div>
                </>
            )}
        </div>
    );
}
