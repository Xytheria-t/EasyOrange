# easyorange-frontend/src/admin/pages/dashboard/

## Responsibility

Admin dashboard overview page showing platform stats, trends, and quick actions.

## Files

| File | Responsibility |
|------|---------------|
| `DashboardPage.tsx` | Main dashboard page — stats cards, quick actions, pending items, recent users/products, chart section |
| `StatCard.tsx` | Reusable stat card component with accent colors and hover effects |
| `charts/TrendChart.tsx` | Recharts line chart for monthly trend (users/products/orders), supports compact & full modes |
| `charts/ActivityHeatmap.tsx` | SVG-based heatmap (day × hour) showing user activity distribution |
| `charts/TopProductsChart.tsx` | Recharts horizontal bar chart showing top viewed products |
