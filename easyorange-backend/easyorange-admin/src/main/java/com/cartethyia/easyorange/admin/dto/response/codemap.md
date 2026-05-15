# easyorange-backend/easyorange-admin/src/main/java/com/cartethyia/easyorange/admin/dto/response/

## Responsibility

Response VOs (Value Objects) for admin API endpoints.

## VOs

| VO | Used By | Description |
|----|---------|-------------|
| `DashboardStatsVO` | GET `/dashboard/stats` | Platform aggregate counts |
| `PendingItemsVO` | GET `/dashboard/pending` | Pending review/report/order counts |
| `RecentUserVO` | GET `/dashboard/recent-users` | Recently registered users |
| `RecentProductVO` | GET `/dashboard/recent-products` | Recently listed products |
| `TrendVO` | GET `/dashboard/trend` | Monthly users/products/orders trend |
| `ActivityVO` | GET `/dashboard/activity` | Recent platform activity feed |
| `UserActivityHeatmapVO` | GET `/dashboard/user-activity-heatmap` | User activity by dayOfWeek × hour |
| `TopProductVO` | GET `/dashboard/top-products` | Top viewed products |
| `AdminUserVO` | User management | User detail for admin |
| `AdminProductVO` | Product management | Product detail for admin |
| `AdminOrderVO` | Order management | Order list item |
| `AdminOrderDetailVO` | Order management | Full order detail |
| `AdminReportVO` | Report management | Report list item |
| `ReportStatsVO` | Report management | Report statistics |
| `ReportHandleHistoryVO` | Report management | Report handle history |
| `AuditLogVO` | Product audit | Audit log entry |
| `BatchAuditResultVO` | Product audit | Batch audit result |
| `AdminReviewVO` | Review management | Review list item |
| `OrderStatsVO` | Order management | Order statistics |
| `CategoryVO` | Category management | Category item |
| `CategoryTreeVO` | Category management | Category tree node |
| `ResetPasswordVO` | User management | Reset password result |
