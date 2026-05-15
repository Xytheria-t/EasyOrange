# easyorange-backend/easyorange-admin/src/main/java/com/cartethyia/easyorange/admin/service/

## Responsibility

Business logic layer for admin management APIs.

## Services

| Service | Methods |
|---------|---------|
| `AdminDashboardService` | getDashboardStats, getPendingItems, getRecentUsers, getRecentProducts, getTrend, getRecentActivity, getUserActivityHeatmap (eo_oper_log), getTopProducts (eo_product.view_count) |
| `AdminUserService` | User CRUD, status toggle, unlock, reset password, force logout |
| `AdminUserServiceExtension` | Extended user operations |
| `AdminProductService` | Product listing/detail, status management |
| `AdminProductAuditService` | Single & batch audit, audit log, domain events |
| `AdminOrderService` | Order CRUD, cancel, force complete, refund |
| `AdminCategoryService` | Category CRUD, tree, status |
| `AdminReportService` | Report CRUD, handle, batch handle, event publishing |
| `AdminReviewService` | Review list, detail, soft delete |
