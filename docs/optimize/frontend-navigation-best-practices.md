# EasyOrange 前端页面切换最佳实践

## 1. 目标

本文档用于规范 EasyOrange 前端的页面切换、权限跳转、页面生命周期和 URL 状态管理。

当前项目是 Vanilla TypeScript + Vite 的多页面应用，不是 React/Vue SPA。因此最佳实践不是引入前端框架路由，而是在现有多 HTML 入口架构下建立统一的导航层、页面启动层和状态约定。

核心目标：

- 所有页面跳转必须经过统一导航模块。
- 页面权限判断必须集中管理，禁止每个页面各写一套登录判断。
- 页面内部状态必须写入 URL query，刷新、复制链接、浏览器返回都应保持一致。
- 页面生命周期必须统一初始化和销毁，避免事件重复绑定、状态残留和内存泄漏。
- Header、页面、业务组件只表达"我要去哪里"，不直接操作 `window.location.href`。

## 2. 当前项目现状

### 2.1 已有的好东西

项目已有良好的基础设施：

- **`BasePage`**：统一页面基类，提供生命周期管理和事件清理
- **Modal 组件**：统一的弹窗管理
- **storage 工具**：封装 localStorage 存取，带泛型支持
- **Vite 多入口配置**：已配置 `index.html`、`products.html`、`publish.html`、`profile.html`、`favorites.html`、`messages.html`

### 2.2 当前问题

| 问题 | 严重程度 | 涉及文件 |
|------|---------|---------|
| `window.location.href` 滥用 | 🔴 高 | 11个文件21处 |
| 无统一路由配置 | 🔴 高 | 全部页面 |
| 无统一 bootstrap | 🔴 高 | 全部页面 |
| 权限判断分散 | 🟡 中 | publish, favorites, messages |
| URL 状态不同步 | 🟡 中 | products, profile 等 |

### 2.3 登录机制说明（重要）

**本项目没有独立的 login.html 页面！**

登录是首页的 Modal 弹窗组件：

```ts
// src/pages/home/auth.ts
showAuthInterface(type: 'login' | 'register' = 'login'): void {
    Modal.create('#authContainer');
}
```

因此：
- 文档中 `/?redirect=/publish.html` 的 redirect 目标是**首页 URL 带 query 参数**
- 登录成功后读取 `redirect` 参数进行跳转
- 不存在 `/login.html` 这个页面

### 2.4 第三方跳转例外

以下场景允许直接使用 `window.location.href`：

```ts
// 第三方支付 - 外部支付平台
window.location.href = payUrl;

// 文件下载 - 浏览器安全限制
window.location.href = '/api/file/download?id=123';
```

例外必须在代码旁注释说明原因。

## 3. 推荐架构

在当前项目中，推荐采用"多页面应用 + 统一导航层"的架构。

不建议短期改成 SPA，原因：

- 现有 Vite 配置已经是多入口 HTML。
- 页面 HTML 已经大量存在，改 SPA 会引入更高重构成本。
- 当前项目没有框架依赖，强行引入 Router 会破坏技术约束。
- 校园二手交易平台的页面边界清晰，多页架构足够支撑。

推荐结构：

```txt
easyorange-frontend/src/
├── app/
│   ├── bootstrap.ts          # 统一页面启动
│   ├── routeConfig.ts        # 页面路由声明
│   └── navigation.ts         # 统一跳转服务
├── pages/
│   ├── BasePage.ts
│   ├── home/
│   ├── products/
│   ├── publish/
│   ├── profile/
│   ├── favorites/
│   └── messages/
└── components/
    └── Header.ts
```

## 4. 页面类型划分

页面切换必须先区分"跨页面跳转"和"页面内部状态切换"。

### 4.1 跨页面跳转

跨 HTML 文件属于跨页面跳转，例如：

- 首页：`/`
- 商品列表：`/products.html`
- 发布商品：`/publish.html`
- 个人中心：`/profile.html`
- 收藏：`/favorites.html`
- 消息：`/messages.html`

处理方式：

- 使用 `navigation.go(routeName, params)`。
- 不使用 `history.pushState`。
- 不手写 `window.location.href`。

### 4.2 页面内部状态切换

同一个 HTML 内的状态变化属于页面内部切换，例如：

- 商品列表搜索关键词。
- 商品分类筛选。
- 排序方式。
- 商品详情弹窗打开。
- 个人中心 tab 切换。
- 消息会话切换。

处理方式：

- 使用 `URLSearchParams` 同步到 query。
- 使用 `history.pushState` 或 `history.replaceState` 更新当前页面 URL。
- 监听 `popstate` 恢复页面状态。
- 不跳转到新 HTML。

## 5. 路由配置规范

新增 `src/app/routeConfig.ts`。

```ts
export type RouteName =
    | 'home'
    | 'products'
    | 'publish'
    | 'profile'
    | 'favorites'
    | 'messages';

export interface RouteConfig {
    path: string;
    title: string;
    navKey?: string;
    requiresAuth: boolean;
}

export const routes: Record<RouteName, RouteConfig> = {
    home: {
        path: '/',
        title: '首页',
        navKey: 'home',
        requiresAuth: false
    },
    products: {
        path: '/products.html',
        title: '商品',
        navKey: 'products',
        requiresAuth: false
    },
    publish: {
        path: '/publish.html',
        title: '发布商品',
        requiresAuth: true
    },
    profile: {
        path: '/profile.html',
        title: '个人中心',
        requiresAuth: true
    },
    favorites: {
        path: '/favorites.html',
        title: '我的收藏',
        requiresAuth: true
    },
    messages: {
        path: '/messages.html',
        title: '消息',
        requiresAuth: true
    }
};
```

规范要求：

- 新增页面必须先注册路由配置。
- 权限通过 `requiresAuth` 声明。
- Header 高亮通过 `navKey` 声明。
- 页面真实路径只允许出现在 `routeConfig.ts`。

## 6. 统一导航服务

新增 `src/app/navigation.ts`。

```ts
import { routes, type RouteName } from './routeConfig.js';
import { storage } from '../utils/index.js';

export type QueryValue = string | number | boolean | null | undefined;

export interface NavigationOptions {
    replace?: boolean;
    query?: Record<string, QueryValue>;
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
    const params = new URLSearchParams();

    Object.entries(query ?? {}).forEach(([key, value]) => {
        if (value === null || value === undefined || value === '') {
            return;
        }
        params.set(key, String(value));
    });

    const queryString = params.toString();
    return queryString ? `${path}?${queryString}` : path;
}

function isLoggedIn(): boolean {
    return Boolean(storage.get<string>('token'));
}

function currentFullPath(): string {
    return `${window.location.pathname}${window.location.search}`;
}

export const navigation = {
    go(routeName: RouteName, options: NavigationOptions = {}): void {
        const route = routes[routeName];
        const targetUrl = buildUrl(route.path, options.query);

        if (route.requiresAuth && !isLoggedIn()) {
            const loginUrl = buildUrl('/', {
                redirect: targetUrl
            });
            window.location.assign(loginUrl);
            return;
        }

        if (options.replace) {
            window.location.replace(targetUrl);
            return;
        }

        window.location.assign(targetUrl);
    },

    replace(routeName: RouteName, query?: Record<string, QueryValue>): void {
        this.go(routeName, { replace: true, query });
    },

    loginRedirect(): void {
        const params = new URLSearchParams(window.location.search);
        const redirect = params.get('redirect');
        window.location.replace(redirect || routes.products.path);
    },

    requireAuth(): boolean {
        if (isLoggedIn()) {
            return true;
        }

        const loginUrl = buildUrl('/', {
            redirect: currentFullPath()
        });
        window.location.replace(loginUrl);
        return false;
    },

    updateQuery(query: Record<string, QueryValue>, mode: 'push' | 'replace' = 'replace'): void {
        const params = new URLSearchParams(window.location.search);

        Object.entries(query).forEach(([key, value]) => {
            if (value === null || value === undefined || value === '') {
                params.delete(key);
                return;
            }
            params.set(key, String(value));
        });

        const queryString = params.toString();
        const url = queryString
            ? `${window.location.pathname}?${queryString}`
            : window.location.pathname;

        if (mode === 'push') {
            window.history.pushState({}, '', url);
            return;
        }

        window.history.replaceState({}, '', url);
    }
};
```

使用规范：

```ts
navigation.go('products');
navigation.go('products', { query: { product: productId } });
navigation.go('profile', { query: { userId } });
navigation.replace('home');
navigation.updateQuery({ keyword, categoryId, sort }, 'replace');
```

禁止：

```ts
window.location.href = '/products.html';
location.reload();
window.location.assign('/profile.html');
```

例外（必须在代码旁写明原因）：

```ts
// 第三方支付跳转 - 外部平台必须使用直接 URL
window.location.href = payUrl;

// 文件下载 - 浏览器安全限制无法拦截
window.location.href = '/api/file/download?token=' + token;
```

## 7. 统一页面启动流程

新增 `src/app/bootstrap.ts`。

```ts
import header from '../components/Header.js';
import { routes, type RouteName } from './routeConfig.js';
import { navigation } from './navigation.js';

export interface PageModule {
    init(): Promise<void> | void;
    destroy?(): void;
}

export interface BootstrapOptions {
    routeName: RouteName;
    page: PageModule;
}

export async function bootstrapPage(options: BootstrapOptions): Promise<void> {
    const route = routes[options.routeName];

    document.title = `${route.title} - EasyOrange`;

    if (route.requiresAuth && !navigation.requireAuth()) {
        return;
    }

    header.init();
    if (route.navKey) {
        header.setActiveNav(route.navKey);
    }

    await options.page.init();

    window.addEventListener('beforeunload', () => {
        options.page.destroy?.();
        header.destroy();
    });
}
```

每个页面入口只做一件事：声明自己是谁，然后交给 `bootstrapPage`。

示例：`src/pages/products.entry.ts`

```ts
import '../styles/main.css';
import '../styles/products.css';
import { bootstrapPage } from '../app/bootstrap.js';
import { ProductsPage } from './products/index.js';

const page = new ProductsPage();

void bootstrapPage({
    routeName: 'products',
    page
});
```

## 8. 页面内部状态规范

页面内部状态必须符合"单一事实来源"。

推荐状态流：

```txt
URL query -> PageState -> render
用户操作 -> update PageState -> update URL query -> render
浏览器返回 -> parse URL query -> PageState -> render
```

### 8.1 商品列表页状态

商品列表页建议使用以下 query：

```txt
/products.html?keyword=书&categoryId=12&sort=latest&page=1&product=1001
```

字段建议：

| 字段 | 含义 | 示例 |
|------|------|------|
| `keyword` | 搜索关键词 | `keyword=键盘` |
| `categoryId` | 分类 ID | `categoryId=12` |
| `sort` | 排序 | `sort=latest` |
| `minPrice` | 最低价 | `minPrice=100` |
| `maxPrice` | 最高价 | `maxPrice=500` |
| `page` | 页码 | `page=2` |
| `product` | 当前打开的详情商品 ID | `product=1001` |

要求：

- 搜索输入使用 `replaceState`，避免每输入一个字都产生历史记录。
- 点击分页使用 `pushState`，允许浏览器返回上一页。
- 打开商品详情使用 `pushState`，关闭详情删除 `product`。
- `popstate` 时必须重新解析 URL 并恢复筛选和详情状态。

### 8.2 个人中心 tab

个人中心 tab 不应只存在 DOM class 中。

推荐：

```txt
/profile.html?tab=orders
/profile.html?tab=products
/profile.html?tab=settings
```

切换 tab：

```ts
navigation.updateQuery({ tab: nextTab }, 'push');
```

刷新后：

```ts
const tab = new URLSearchParams(window.location.search).get('tab') || 'overview';
```

### 8.3 消息会话

推荐：

```txt
/messages.html?conversationId=123
```

要求：

- 左侧会话列表选中项由 URL 决定。
- 复制链接后可以直接打开同一会话。
- 如果会话不存在，清理 query 并展示空状态。

## 9. 权限跳转规范

### 9.1 未登录访问受保护页面

访问：

```txt
/publish.html
```

未登录时跳转：

```txt
/?redirect=/publish.html
```

登录成功后（在首页 AuthManager 中调用）：

```ts
navigation.loginRedirect();
```

### 9.2 登录用户访问首页

如果用户已登录，首页不应再打开登录弹窗。

在 `AuthManager.checkAuthStatus()` 中：

```ts
if (token && user) {
    // 已登录，自动跳转到 redirect 目标或商品页
    navigation.loginRedirect();
}
```

### 9.3 退出登录

退出登录后使用 replace，避免浏览器返回到受保护页面。

```ts
// Header.ts logout()
storage.remove('token');
storage.remove('user');
navigation.replace('home');
```

## 10. Header 规范

Header 只负责展示和触发导航，不负责拼 URL。

推荐：

```ts
notificationBtn.addEventListener('click', () => {
    navigation.go('messages');
});

logoutBtn.addEventListener('click', () => {
    storage.remove('token');
    storage.remove('user');
    navigation.replace('home');
});
```

Header 模板中的链接也建议使用路由配置生成，或至少保持与 `routeConfig.ts` 一致。

不推荐：

```ts
window.location.href = '/messages.html';
window.location.href = '/';
```

## 11. 页面类规范

每个复杂页面都应继承 `BasePage`。

标准模板：

```ts
export class ProductsPage extends BasePage<ProductsPageElements> {
    private state: ProductsPageState = getDefaultState();

    protected cacheElements(): void {
        this.elements = {
            productsGrid: document.getElementById('productsGrid')
        };
    }

    protected bindEvents(): void {
        this.onEvent(window, 'popstate', () => {
            this.state = this.parseStateFromUrl();
            void this.loadAndRender();
        });
    }

    protected async onInit(): Promise<void> {
        this.state = this.parseStateFromUrl();
        await this.loadAndRender();
    }

    private parseStateFromUrl(): ProductsPageState {
        const params = new URLSearchParams(window.location.search);
        return {
            keyword: params.get('keyword') || '',
            categoryId: params.get('categoryId'),
            sort: params.get('sort') || 'latest',
            page: Number(params.get('page') || 1),
            productId: params.get('product')
        };
    }
}
```

要求：

- `cacheElements` 只查 DOM，不发请求。
- `bindEvents` 只绑定事件，不发请求。
- `onInit` 负责读取 URL、加载数据、首屏渲染。
- `destroy` 必须清理事件、定时器、请求和弹窗。
- 页面状态必须集中到 `state`，禁止散落多个无关联字段。

## 12. 弹窗和抽屉规范

弹窗分两类。

### 12.1 普通 UI 弹窗

例如确认删除、提示、上传裁剪。

要求：

- 不写入 URL。
- ESC 和遮罩关闭。
- 页面销毁时关闭。

### 12.2 可分享业务弹窗

例如商品详情。

要求：

- 必须写入 URL。
- 打开：`/products.html?product=1001`
- 关闭：删除 `product` query。
- 刷新页面后应重新打开对应商品详情。
- 浏览器返回应关闭弹窗，而不是直接离开页面。

## 13. 企业级落地路线

建议分阶段改造，避免一次性重构风险。

### 第 1 阶段：收口跳转

目标：

- 新增 `src/app/routeConfig.ts`。
- 新增 `src/app/navigation.ts`。
- 替换所有 `window.location.href`（第三方支付和文件下载除外）。
- Header 改为调用 `navigation`。

验收标准：

- `src/` 下除 `navigation.ts` 和第三方跳转外不再出现 `window.location.href`。
- 所有跨页面跳转都能通过路由名定位。
- 未登录访问受保护页面能保留 redirect。

### 第 2 阶段：统一页面启动

目标：

- 新增 `src/app/bootstrap.ts`。
- 每个 HTML 对应一个 `*.entry.ts` 入口文件。
- 页面入口不再手动初始化 Header、鉴权、标题。

验收标准：

- 所有页面入口结构一致。
- Header 初始化只在 `bootstrapPage` 中发生。
- 受保护页面不再各自写登录判断。

### 第 3 阶段：URL 状态治理

优先顺序：

1. `products.html`
2. `profile.html`
3. `messages.html`
4. `favorites.html`
5. `publish.html`

验收标准：

- 商品筛选、搜索、排序、详情弹窗可通过 URL 恢复。
- 浏览器返回行为符合用户预期。
- 刷新页面不丢失核心状态。

### 第 4 阶段：页面生命周期治理

目标：

- 复杂页面统一继承 `BasePage`。
- 使用 `onEvent` 管理事件。
- 使用 `AbortController` 取消请求。
- 页面销毁时释放资源。

验收标准：

- 不再重复绑定全局事件。
- 切换页面、刷新页面无明显残留状态。
- TypeScript 类型检查通过。

## 14. 代码检查规则

禁止项：

- 页面直接写 `window.location.href`（第三方支付和文件下载除外）。
- 页面直接拼接跨页面 URL。
- 受保护页面自行写一套登录跳转。
- 页面状态只存在 DOM class。
- 搜索、筛选、tab 状态无法通过刷新恢复。
- 组件内部知道具体 HTML 文件名。

必须项：

- 新页面必须注册 `routeConfig.ts`。
- 新页面入口必须使用 `bootstrapPage`。
- 跨页面跳转必须使用 `navigation.go`。
- 页面内部状态必须从 URL 初始化。
- 业务详情弹窗必须支持 URL 直达。
- 复杂页面必须提供 `destroy` 清理。

## 15. 推荐命名规范

页面入口（当前实际命名）：

```txt
src/main.ts          -> index.html (首页)
src/pages/products/ProductsPage.ts  -> products.html
src/pages/publish.ts -> publish.html
src/pages/profile.ts -> profile.html
src/pages/favorites.ts -> favorites.html
src/pages/messages.ts -> messages.html
```

未来推荐的 `*.entry.ts` 命名：

```txt
src/pages/home.entry.ts
src/pages/products.entry.ts
src/pages/publish.entry.ts
src/pages/profile.entry.ts
src/pages/favorites.entry.ts
src/pages/messages.entry.ts
```

页面类：

```txt
HomePage
ProductsPage
PublishPage
ProfilePage
FavoritesPage
MessagesPage
```

页面状态：

```txt
ProductsPageState
ProfilePageState
MessagesPageState
```

导航方法：

```txt
navigation.go()
navigation.replace()
navigation.updateQuery()
navigation.requireAuth()
navigation.loginRedirect()
```

## 16. 最终目标架构

最终页面切换关系应变成：

```txt
用户点击
  -> 组件事件
  -> navigation.go / navigation.updateQuery
  -> routeConfig 决定路径和权限
  -> 页面入口 bootstrap
  -> BasePage 生命周期
  -> URL state 驱动渲染
```

而不是：

```txt
用户点击
  -> 任意组件 window.location.href
  -> 页面自己判断登录
  -> 页面自己初始化 Header
  -> 页面状态散落在 DOM 和字段中
```

## 17. 当前项目最优选择

对 EasyOrange 当前阶段，最贴合项目的企业级方案是：

- 保留多 HTML 页面架构。
- 不引入 React/Vue/Router。
- 新增统一导航层（`routeConfig.ts`、`navigation.ts`）。
- 新增统一 bootstrap（`bootstrap.ts`）。
- 用 URL query 管理页面内部状态。
- 用 `BasePage` 统一生命周期。
- 分阶段迁移，不做一次性大重构。

这条路线能最小化改动成本，同时解决页面切换混乱、登录跳转不一致、状态无法恢复和事件重复绑定的问题。

## 18. 关键代码对照表

| 场景 | 当前代码（有问题） | 目标代码（正确） |
|------|-------------------|-----------------|
| Header 跳转消息 | `window.location.href = '/messages.html'` | `navigation.go('messages')` |
| 退出登录跳转 | `window.location.href = '/'` | `navigation.replace('home')` |
| 未登录访问发布页 | `window.location.href = '/?redirect=/publish.html'` | `navigation.go('publish')` + `requireAuth()` |
| 登录成功跳转 | 手动处理 | `navigation.loginRedirect()` |
| 商品详情跳转 | `window.location.href = '/products.html?product=1'` | `navigation.updateQuery({ product: 1 }, 'push')` |
| 发布成功跳转 | `window.location.href = '/products.html'` | `navigation.replace('products')` |

## 19. storage.get 泛型用法

项目中的 `storage.get()` 是泛型方法，必须指定类型：

```ts
// 正确 - 指定泛型类型
const token = storage.get<string>('token');
const user = storage.get<User>('user');

// 错误 - 可能导致类型推断问题
const token = storage.get('token');
const user = storage.get('user');
```

`isLoggedIn()` 正确实现：

```ts
function isLoggedIn(): boolean {
    return Boolean(storage.get<string>('token'));
}
```
