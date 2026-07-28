import {
    useDashboardStats,
    usePendingItems,
    useRecentProducts,
    useRecentUsers,
    useTopProducts,
    useTrend,
    useUserActivityHeatmap,
} from '../../hooks/useAdminDashboard';
import './dashboard.css';
import { DashboardBackground } from './components/DashboardBackground';
import { DashboardChartsSection } from './components/DashboardChartsSection';
import { DashboardErrorBanner } from './components/DashboardErrorBanner';
import { DashboardHeader } from './components/DashboardHeader';
import { DashboardRecentProducts } from './components/DashboardRecentProducts';
import { DashboardRecentUsers } from './components/DashboardRecentUsers';
import { DashboardStatsGrid } from './components/DashboardStatsGrid';
import { PendingItemsAlert } from './components/PendingItemsAlert';
import { QuickActionsPanel } from './components/QuickActionsPanel';

export default function DashboardPage() {
    const { data: stats, isLoading: statsLoading, isError: statsError, error: statsErr } = useDashboardStats();
    const {
        data: pendingItems,
        isLoading: pendingLoading,
        isError: pendingError,
        error: pendingErr,
    } = usePendingItems();
    const { data: recentUsers, isLoading: usersLoading, isError: usersError, error: usersErr } = useRecentUsers(5);
    const {
        data: recentProducts,
        isLoading: productsLoading,
        isError: productsError,
        error: productsErr,
    } = useRecentProducts(5);
    const { data: trend = [] } = useTrend();
    const { data: heatmap = [] } = useUserActivityHeatmap();
    const { data: topProducts = [] } = useTopProducts(10);

    const hasAnyError = statsError || pendingError || usersError || productsError;

    return (
        <div style={{ position: 'relative', minHeight: '100%' }}>
            <DashboardBackground />

            <div style={{ position: 'relative', zIndex: 1 }}>
                <DashboardHeader />

                {hasAnyError && <DashboardErrorBanner errors={[statsErr, pendingErr, usersErr, productsErr]} />}

                <DashboardStatsGrid stats={stats} isLoading={statsLoading} />

                <QuickActionsPanel pendingItems={pendingItems} />

                <PendingItemsAlert pendingItems={pendingItems} isLoading={pendingLoading} />

                <div
                    style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(2, 1fr)',
                        gap: '1.5rem',
                        animation: 'dashContentReveal 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.4s both',
                    }}
                >
                    <DashboardRecentUsers users={recentUsers} isLoading={usersLoading} />
                    <DashboardRecentProducts products={recentProducts} isLoading={productsLoading} />
                </div>

                <DashboardChartsSection trend={trend} heatmap={heatmap} topProducts={topProducts} />
            </div>
        </div>
    );
}
