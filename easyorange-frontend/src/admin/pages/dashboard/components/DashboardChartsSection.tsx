import { Link } from 'react-router-dom';
import type { TopProductItem, TrendItem, UserActivityItem } from '@/admin/types/admin';
import ActivityHeatmap from '../charts/ActivityHeatmap';
import { LazyTopProductsChart, LazyTrendChart } from '../charts/lazyCharts';
import { linkStyle, sectionCard, sectionHeader, sectionIconWrap, sectionTitle } from '../lib/dashboardStyles';

interface DashboardChartsSectionProps {
    trend: TrendItem[];
    heatmap: UserActivityItem[];
    topProducts: TopProductItem[];
}

export function DashboardChartsSection({ trend, heatmap, topProducts }: DashboardChartsSectionProps) {
    return (
        <div
            className="dash-charts-grid-responsive"
            style={{
                display: 'grid',
                gap: '1.5rem',
                marginTop: '1.5rem',
                animation: 'dashContentReveal 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.5s both',
            }}
        >
            {/* Trend chart */}
            <section style={sectionCard}>
                <div style={sectionHeader}>
                    <h2 style={sectionTitle}>
                        <span
                            style={sectionIconWrap(
                                'linear-gradient(135deg, rgba(249,115,22,0.1), rgba(249,115,22,0.03))',
                                '#F97316'
                            )}
                        >
                            <svg
                                aria-hidden="true"
                                width="15"
                                height="15"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            >
                                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                            </svg>
                        </span>
                        趋势概览
                    </h2>
                    <Link
                        to="/admin/stats"
                        style={{ ...linkStyle, color: '#F97316' }}
                        onMouseEnter={e => {
                            e.currentTarget.style.gap = '0.5rem';
                        }}
                        onMouseLeave={e => {
                            e.currentTarget.style.gap = '0.25rem';
                        }}
                    >
                        详情
                        <svg
                            aria-hidden="true"
                            width="13"
                            height="13"
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
                </div>
                <div style={{ padding: '0.75rem 0.5rem' }}>
                    <LazyTrendChart data={trend} isCompact height={180} />
                </div>
            </section>

            {/* Activity heatmap */}
            <section style={sectionCard}>
                <div style={sectionHeader}>
                    <h2 style={sectionTitle}>
                        <span
                            style={sectionIconWrap(
                                'linear-gradient(135deg, rgba(251,113,133,0.12), rgba(251,113,133,0.03))',
                                '#FB7185'
                            )}
                        >
                            <svg
                                aria-hidden="true"
                                width="15"
                                height="15"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            >
                                <rect x="3" y="3" width="7" height="7" />
                                <rect x="14" y="3" width="7" height="7" />
                                <rect x="14" y="14" width="7" height="7" />
                                <rect x="3" y="14" width="7" height="7" />
                            </svg>
                        </span>
                        用户活跃时段
                    </h2>
                </div>
                <div style={{ padding: '0.5rem' }}>
                    <ActivityHeatmap data={heatmap} />
                </div>
            </section>

            {/* Top products */}
            <section style={sectionCard}>
                <div style={sectionHeader}>
                    <h2 style={sectionTitle}>
                        <span
                            style={sectionIconWrap(
                                'linear-gradient(135deg, rgba(195,155,211,0.12), rgba(195,155,211,0.03))',
                                '#C39BD3'
                            )}
                        >
                            <svg
                                aria-hidden="true"
                                width="15"
                                height="15"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            >
                                <path d="M6 9l6 6 6-6" />
                            </svg>
                        </span>
                        Top 浏览量商品
                    </h2>
                </div>
                <div style={{ padding: '0.5rem 0.75rem' }}>
                    <LazyTopProductsChart data={topProducts} />
                </div>
            </section>
        </div>
    );
}
