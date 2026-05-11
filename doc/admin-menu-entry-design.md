# Admin Menu Entry 设计文档

> 日期：2026-05-11
> 状态：已实施

## 背景

当前管理员用户访问管理后台（`/admin`）需要手动在浏览器地址栏输入 URL，用户侧导航栏没有任何入口。本功能在用户下拉菜单中添加「管理后台」入口，仅对管理员角色可见。

## 需求

- 在用户下拉菜单中添加「管理后台」入口
- 仅当 `user.userType === ADMIN_USER_TYPE`（管理员）时显示
- 点击后跳转到 `/admin`
- 管理员登录后始终显示该入口

## 方案：抽取 AdminMenuEntry 组件

### 新增文件

**`src/components/admin/AdminMenuEntry.tsx`**

可复用组件，职责：
- 从 `useAuthStore` 读取当前用户
- 判断 `userType === ADMIN_USER_TYPE`（与 `useAdminGuard.ts` 保持一致）
- 管理员时渲染菜单项按钮 → `/admin`
- 非管理员时返回 `null`

接口：

```tsx
interface AdminMenuEntryProps {
  className?: string;
  onClick?: () => void;
}
```

### 修改文件

**`src/components/layout/Header.tsx`**

在用户下拉菜单中，"我的订单"与分隔线之间插入 `<AdminMenuEntry />`：

```
个人中心
我的收藏
我的订单
──────────────
🛠 管理后台   ← <AdminMenuEntry />
退出登录
```

传入 `onClick` 回调用于关闭下拉菜单（与现有菜单项模式一致）。

**`src/constants/app.ts`**

新增 `ADMIN_USER_TYPE = '00' as const` 常量，统一管理员角色判断值。

**`src/admin/hooks/useAdminGuard.ts`**

将硬编码 `'00'` 替换为 `ADMIN_USER_TYPE` 常量引用。

### 视觉设计

- 复用 `.floating-nav__menu-item` 样式类，保持一致外观
- 图标使用 lucide-react 的 `LayoutDashboard`
- 文案：「管理后台」
- 带 `aria-label="进入管理后台"` 无障碍标注

### 不做范围

- 不改管理员权限判断逻辑（判断条件不变，仅常量化）
- 不改 AdminRouteGuard 或路由配置
- 不加移动端特殊处理（复用现有响应式机制）
- 不加二级确认弹窗

## 文件变更清单

| 操作 | 文件路径 |
|------|---------|
| 新增 | `easyorange-frontend/src/components/admin/AdminMenuEntry.tsx` |
| 修改 | `easyorange-frontend/src/components/layout/Header.tsx` |
| 修改 | `easyorange-frontend/src/constants/app.ts` |
| 修改 | `easyorange-frontend/src/admin/hooks/useAdminGuard.ts` |
