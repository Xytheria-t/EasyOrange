import { test } from '@playwright/test';
import { seedAdminSession, spaNavigate } from '../e2e/helpers/auth';

/**
 * 管理后台视觉快照 —— 纯本地 mock，不依赖后端栈。
 *
 * 放在 e2e-real 下是因为它要和 `demo.spec.ts` 共用「本地栈已起」的约定，
 * 但本文件不碰真实数据：跑的是写死的样例数据，只出图不做断言，
 * 用来在改样式后肉眼比对（截图落在 tests/e2e-real/shots/）。
 *
 * 两个用例分别覆盖统计页与五个列表页，列表页额外核窄屏与侧边栏折叠两档。
 *
 * 数值照抄真实后台截图（16 用户 / 70 商品 / 20 订单 / 营收 33987），
 * 新旧两版能直接对着看。
 */
const OK = { code: 'A0000', message: 'success' };

/** 后端把 long 序列化成字符串，前端 coerceCounts 再 Number() 回来——这里照抄真实线形 */
const n = (v: number) => String(v);

const cat = (
    id: string,
    name: string,
    level: number,
    count: number,
    parentId: string | null = null,
    parentName: string | null = null
) => ({
    id,
    name,
    parentId,
    parentName,
    level,
    sortOrder: 0,
    status: 1,
    productCount: count,
    createTime: null,
    updateTime: null,
    children: [],
});

test('admin stats screenshot', async ({ page }) => {
    await seedAdminSession(page, {
        userId: '1',
        username: 'admin',
        nickname: '平台管理员',
        userType: '00',
    });

    await page.route('**/api/admin/dashboard/stats**', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                ...OK,
                data: {
                    totalUsers: 16,
                    todayNewUsers: 0,
                    totalProducts: 70,
                    pendingProducts: 3,
                    totalOrders: 20,
                    todayOrders: 0,
                    totalRevenue: 33987,
                },
            }),
        })
    );

    await page.route('**/api/admin/dashboard/trend**', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                ...OK,
                data: [
                    { month: '2026-03', users: 0, products: 0, orders: 0 },
                    { month: '2026-04', users: 0, products: 0, orders: 0 },
                    { month: '2026-05', users: 0, products: 1, orders: 0 },
                    { month: '2026-06', users: 0, products: 2, orders: 1 },
                    { month: '2026-07', users: 1, products: 3, orders: 1 },
                    { month: '2026-08', users: 7, products: 26, orders: 6 },
                    { month: '2026-09', users: 13, products: 41, orders: 4 },
                ].map(r => ({ ...r, users: n(r.users), products: n(r.products), orders: n(r.orders) })),
            }),
        })
    );

    await page.route('**/api/admin/dashboard/activity**', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                ...OK,
                data: [
                    { time: '10分钟前', text: '用户「橘子汽水」发布了新商品《考研数学全程班》', type: 'product' },
                    { time: '35分钟前', text: '订单 EO20260929001 已完成，交易金额 ¥1,299', type: 'order' },
                    { time: '1小时前', text: '新用户「橙子味的风」完成注册', type: 'user' },
                    { time: '3小时前', text: '订单 EO20260928007 已发货', type: 'order' },
                    { time: '5小时前', text: '商品《机械键盘 87 键》通过审核并上架', type: 'product' },
                ],
            }),
        })
    );

    await page.route('**/api/admin/orders/stats**', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                ...OK,
                data: {
                    totalOrders: 20,
                    todayOrders: 0,
                    pendingPayment: 0,
                    toShip: 2,
                    toReceive: 2,
                    completed: 10,
                    cancelled: 4,
                    refunded: 2,
                    totalRevenue: 33987,
                    todayRevenue: 0,
                },
            }),
        })
    );

    // 8 个一级分类：前 6 + 「其他 2 个分类」，用来验证占比能不能加到 100%
    await page.route('**/api/admin/categories**', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                ...OK,
                data: [
                    cat('c1', '电子数码', 1, 24),
                    cat('c2', '书籍教材', 1, 11),
                    cat('c3', '服饰鞋包', 1, 7),
                    cat('c4', '生活用品', 1, 6),
                    cat('c5', '运动健身', 1, 4),
                    cat('c6', '虚拟物品', 1, 4),
                    cat('c7', '图书杂志', 1, 3),
                    cat('c8', '美妆护肤', 1, 2),
                    cat('c9', '手机', 2, 9, 'c1', '电子数码'),
                ],
            }),
        })
    );

    const shot = 'tests/e2e-real/shots';

    await page.goto('/');
    await page.waitForSelector('[data-testid="btn-user-menu"]', { timeout: 20000 });
    await spaNavigate(page, '/admin');

    await page.waitForSelector('.admin-kpi-grid', { timeout: 20000 });
    await page.waitForSelector('.admin-small-multiples', { timeout: 20000 });
    await page.waitForTimeout(1200);

    await page.screenshot({ path: `${shot}/admin-stats.png`, fullPage: true });

    // 折叠态：侧边栏文字收起后，选中色条与图标不能错位
    await page.getByTitle('收起侧边栏').click();
    await page.waitForTimeout(600);
    await page.screenshot({ path: `${shot}/admin-stats-collapsed.png` });
    await page.getByTitle('展开侧边栏').click();
    await page.waitForTimeout(400);

    // 窄屏：双栏收单列，KPI 与指标条换行不溢出
    await page.setViewportSize({ width: 820, height: 900 });
    await page.waitForTimeout(700);
    await page.screenshot({ path: `${shot}/admin-stats-narrow.png`, fullPage: true });
});

/* ─────────── 列表页：筛选工具栏与表格同卡 ─────────── */

const user = (id: string, username: string, nickname: string | null, userType: string, status: string) => ({
    userId: id,
    username,
    nickname,
    avatar: null,
    email: `${username}@easyorange.dev`,
    phone: null,
    realName: null,
    userType,
    userTypeDesc: null,
    status,
    statusDesc: null,
    loginIp: null,
    loginDate: null,
    createTime: '2026-05-16 10:00:00',
    updateTime: null,
});

const product = (id: string, name: string, price: number, status: string, seller: string, hasImage: boolean) => ({
    productId: id,
    name,
    description: null,
    price,
    originalPrice: null,
    stock: 1,
    status,
    statusDesc: null,
    conditionLevel: 9,
    location: null,
    contactMethod: null,
    images: [],
    mainImage: hasImage ? `https://picsum.photos/seed/${id}/96` : null,
    categoryId: 'c1',
    categoryName: '电子数码',
    sellerId: null,
    sellerName: seller,
    sellerAvatar: null,
    viewCount: 12,
    createTime: '2026-09-28 09:12:00',
    updateTime: null,
});

const orderItem = (productName: string, unitPrice: number) => ({
    itemId: `it-${productName}`,
    productId: `p-${productName}`,
    productName,
    productImage: '',
    unitPrice,
    quantity: 1,
    subtotal: unitPrice,
});

const order = (no: string, productNames: string[], amount: number, status: string) => ({
    orderId: `o-${no}`,
    orderNo: no,
    buyerId: 'u2',
    buyerName: '橘子汽水',
    sellerId: 'u3',
    sellerName: '橙子味的风',
    items: productNames.map(name => orderItem(name, amount / productNames.length)),
    totalAmount: amount,
    singleItem: productNames.length === 1,
    status,
    statusDesc: null,
    paymentStatus: 'PAID',
    paymentStatusDesc: null,
    createTime: '2026-09-28 14:03:00',
});

const page_ = <T,>(records: T[], total: number) => ({
    records,
    total: n(total),
    current: n(1),
    size: n(10),
    pages: n(Math.ceil(total / 10)),
});

const doc = (id: string, title: string, status: string, chunkCount: number) => ({
    id,
    title,
    source: '平台规则',
    status,
    chunkCount,
    createTime: '2026-08-14 10:00:00',
});

test('admin list pages screenshot', async ({ page }) => {
    await seedAdminSession(page, { userId: '1', username: 'admin', nickname: '平台管理员', userType: '00' });

    // 用 URL 谓词而不是 glob：'/api/admin/orders' 的 glob 会连带命中 orders/stats
    const fulfillList = (predicate: (url: URL) => boolean, data: unknown) =>
        page.route(predicate, route =>
            route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ ...OK, data }),
            })
        );

    await fulfillList(
        url => url.pathname.endsWith('/admin/users'),
        page_(
            [
                user('u1', 'admin', '平台管理员', '00', 'NORMAL'),
                user('u2', 'orange_juice', '橘子汽水', '01', 'NORMAL'),
                user('u3', 'wind_orange', null, '01', 'LOCKED'),
                user('u4', 'old_student', '备考的老王', '01', 'DISABLED'),
                user('u5', 'ops_huang', '黄工', '02', 'NORMAL'),
            ],
            16
        )
    );

    await fulfillList(
        url => url.pathname.endsWith('/admin/products'),
        page_(
            [
                product('p1', '考研数学全程班（强化冲刺）', 1299, 'PENDING_REVIEW', '橘子汽水', true),
                product('p2', '机械键盘 87 键 静音红轴', 289, 'PENDING_REVIEW', 'wind_orange', false),
                product('p3', '二手 iPad 第九代 64G', 2380, 'PENDING_REVIEW', '橘子汽水', false),
                product('p4', '四六级真题套装', 68, 'PENDING_REVIEW', '老王', true),
                product('p5', '未命名草稿', 0, 'PENDING_REVIEW', '黄工', false),
            ],
            70
        )
    );

    await fulfillList(
        url => url.pathname.endsWith('/admin/orders') && url.searchParams.has('pageNum'),
        page_(
            [
                order('EO20260928001', ['机械键盘 87 键 静音红轴'], 289, 'COMPLETED'),
                order('EO20260928002', ['考研数学全程班（强化冲刺）', '四六级真题套装'], 1367, 'SHIPPED'),
                order('EO20260927003', ['二手 iPad 第九代 64G'], 2380, 'PENDING_PAYMENT'),
                order('EO20260927004', ['未命名草稿'], 0, 'CANCELLED'),
            ],
            20
        )
    );

    await fulfillList(
        url => url.pathname.endsWith('/admin/categories/tree'),
        [
            {
                ...cat('c1', '电子数码', 1, 24),
                children: [{ ...cat('c1-1', '手机', 2, 9, 'c1', '电子数码'), children: [] }],
            },
            { ...cat('c2', '书籍教材', 1, 11), children: [] },
            { ...cat('c3', '服饰鞋包', 1, 7), children: [] },
        ]
    );

    // 商品审核页的分类下拉取扁平列表，不是树
    await fulfillList(
        url => url.pathname.endsWith('/admin/categories'),
        [
            cat('c1', '电子数码', 1, 24),
            cat('c2', '书籍教材', 1, 11),
            cat('c3', '服饰鞋包', 1, 7),
        ]
    );

    await fulfillList(
        url => url.pathname.endsWith('/admin/knowledge') && url.searchParams.has('pageNum'),
        page_(
            [
                doc('kb-1', '平台交易流程：从发布到收货的完整链路说明', 'INDEXED', 12),
                doc('kb-2', '退款规则', 'PENDING', 0),
                doc('kb-3', '违禁词与描述规范', 'FAILED', 0),
            ],
            3
        )
    );

    const shot = 'tests/e2e-real/shots';

    const pages: { path: string; name: string; ready: string }[] = [
        { path: '/admin/users', name: 'users', ready: '.admin-toolbar' },
        { path: '/admin/products', name: 'products', ready: '.admin-media-tile' },
        { path: '/admin/orders', name: 'orders', ready: '.admin-table' },
        { path: '/admin/categories', name: 'categories', ready: '.admin-level-tag' },
        { path: '/admin/knowledge', name: 'knowledge', ready: '.admin-table' },
    ];

    await page.goto('/');
    await page.waitForSelector('[data-testid="btn-user-menu"]', { timeout: 20000 });

    for (const { path, name, ready } of pages) {
        await spaNavigate(page, path);
        await page.waitForSelector(ready, { timeout: 20000 });
        await page.waitForTimeout(600);
        await page.screenshot({ path: `${shot}/admin-${name}.png`, fullPage: true });
    }

    // 窄屏：工具栏换行到标题下方，表格横向滚动，页面不横向溢出
    await spaNavigate(page, '/admin/products');
    await page.waitForSelector('.admin-media-tile', { timeout: 20000 });
    await page.setViewportSize({ width: 820, height: 900 });
    await page.waitForTimeout(700);
    await page.screenshot({ path: `${shot}/admin-products-narrow.png`, fullPage: true });

    // 折叠态：侧边栏收起后，列表卡的标题与工具栏不能互相压住
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.getByTitle('收起侧边栏').click();
    await page.waitForTimeout(600);
    await page.screenshot({ path: `${shot}/admin-products-collapsed.png`, fullPage: true });
});
