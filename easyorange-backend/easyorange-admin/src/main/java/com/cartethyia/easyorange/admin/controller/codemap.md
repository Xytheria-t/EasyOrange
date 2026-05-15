# easyorange-backend/easyorange-admin/src/main/java/com/cartethyia/easyorange/admin/controller/

## Responsibility

REST controllers for the admin management interface. All routes under `/api/admin/`.

## Controllers

| Controller | Endpoints |
|------------|-----------|
| `AdminDashboardController` | GET `/dashboard/stats`, `/pending`, `/recent-users`, `/recent-products`, `/trend`, `/activity`, `/user-activity-heatmap`, `/top-products` |
| `AdminUserController` | User CRUD, status, unlock, reset password, force logout, role |
| `AdminProductController` | Product list, detail, status |
| `AdminProductAuditController` | Single/batch audit, audit logs |
| `AdminOrderController` | Order CRUD, cancel, force complete, refund, stats |
| `AdminCategoryController` | Category CRUD, tree, status |
| `AdminReportController` | Report list, detail, handle, batch handle, history, stats |
| `AdminReviewController` | Review list, detail, delete |
