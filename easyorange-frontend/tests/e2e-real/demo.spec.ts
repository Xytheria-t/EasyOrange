import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

/**
 * 验收答辩演示流 · 真后端验证（T1–T4 UI 层）。
 * 不 mock 任何接口；依赖本地栈（后端 8080 带 .env、前端 5173、ES 已 reindex）。
 * 固定短信验证码 307519（application-dev.yaml easyorange.sms.demo-code）。
 */

const BASE = 'http://localhost:8080';
const SMS = '307519';
const stamp = () => String(Date.now()).slice(-9);

function toast(page: Page, text: string | RegExp) {
    return page.locator('.toast-item').filter({ hasText: text });
}

async function apiLogin(req: APIRequestContext, identifier: string, password: string): Promise<string> {
    const res = await req.post(`${BASE}/api/auth/login`, { data: { identifier, password } });
    const body = await res.json();
    expect(body.code, JSON.stringify(body)).toBe('A0000');
    return body.data.accessToken;
}

async function apiCreatePendingProduct(req: APIRequestContext, sellerToken: string, name: string): Promise<string> {
    const image = `${BASE}/api/file/2026/09/24/d19ed8faa8037ebd7d3ea217f1105b55.jpg`;
    const create = await req.post(`${BASE}/api/products`, {
        headers: { Authorization: `Bearer ${sellerToken}` },
        data: {
            categoryId: '15',
            name,
            description: 'e2e 临时商品，验证后删除',
            price: 1888,
            stock: 1,
            conditionLevel: '3',
            imageUrls: [image],
            location: '同城',
        },
    });
    const created = await create.json();
    expect(created.code, JSON.stringify(created)).toBe('A0000');
    const id = String(created.data?.id ?? created.data);
    const submit = await req.put(`${BASE}/api/products/${id}/submit`, {
        headers: { Authorization: `Bearer ${sellerToken}` },
    });
    expect((await submit.json()).code).toBe('A0000');
    return id;
}

async function apiDeleteProduct(req: APIRequestContext, sellerToken: string, id: string) {
    const res = await req.delete(`${BASE}/api/products/${id}`, {
        headers: { Authorization: `Bearer ${sellerToken}` },
    });
    expect((await res.json()).code).toBe('A0000');
}

async function uiLogin(page: Page, account: string, password: string) {
    await page.goto('/login');
    await page.getByTestId('tab-login').click();
    await page.getByTestId('input-account').fill(account);
    await page.getByTestId('input-password').fill(password);
    await page.getByTestId('btn-login-submit').click();
    await expect(page.getByTestId('btn-user-menu')).toBeVisible({ timeout: 20_000 });
}

async function uiRegister(page: Page, username: string, phone: string) {
    await page.goto('/login');
    await page.getByTestId('tab-register').click();
    await expect(page.getByTestId('tab-register')).toHaveClass(/auth-page-tab--active/);
    await page.getByTestId('input-register-username').fill(username);
    await page.getByTestId('input-register-phone').fill(phone);
    await page.getByTestId('btn-register-send-code').click();
    await expect(toast(page, '验证码已发送')).toBeVisible({ timeout: 15_000 });
    await page.getByTestId('input-register-verify-code').fill(SMS);
    await page.getByTestId('input-register-password').fill('Demo123456');
    await page.getByTestId('input-register-confirm-password').fill('Demo123456');
    await page.getByRole('checkbox').check();
    await page.getByTestId('btn-register-submit').click();
    await expect(page.getByTestId('btn-user-menu')).toBeVisible({ timeout: 20_000 });
    // 注册成功必弹「完善个人信息」引导 —— 等它出现并关掉，否则遮罩挡住后续一切点击
    const later = page.getByRole('button', { name: '稍后再说' });
    await expect(later).toBeVisible({ timeout: 10_000 });
    await later.click();
    await expect(later).toBeHidden({ timeout: 10_000 });
    // 浮动导航入场动画期间按钮「not stable」，等它停稳再交给后续用例点
    await page.waitForTimeout(800);
}

test.describe.configure({ mode: 'serial' });

test('T1a 手机号注册 → 连续输错 5 次锁定', async ({ page }) => {
    const s = stamp();
    const username = `ui${s}`;
    const phone = `138${s.slice(0, 8)}`;

    await uiRegister(page, username, phone);

    // ── 连续输错 5 次（快速连点也逐次计数）→ 第 5 次「账户已被锁定」──
    await page.goto('/login');
    await page.getByTestId('tab-login').click();
    await page.getByTestId('input-account').fill(username);
    for (let i = 0; i < 5; i++) {
        await page.getByTestId('input-password').fill('WrongPass1');
        await page.getByTestId('btn-login-submit').click();
        await expect(
            toast(page, /密码错误|已被锁定/).or(page.getByTestId('login-error'))
        ).toBeVisible({ timeout: 10_000 });
    }
    await expect(
        toast(page, '账户已被锁定').or(page.getByTestId('login-error').filter({ hasText: '已被锁定' }))
    ).toBeVisible({ timeout: 10_000 });
    // 锁定后正确密码同样进不去
    await page.getByTestId('input-password').fill('Demo123456');
    await page.getByTestId('btn-login-submit').click();
    await expect(
        toast(page, '账户已被锁定').or(page.getByTestId('login-error').filter({ hasText: '已被锁定' }))
    ).toBeVisible({ timeout: 10_000 });
});

test('T1b 短信验证码登录（固定验证码 307519）', async ({ page, request }) => {
    // 账号经 API 预注册（手机号已绑定）→ UI 从干净会话走短信登录
    const s = stamp();
    const phone = `136${s.slice(0, 8)}`;
    // 先取码再注册（注册即消费验证码，顺序反了会 B1008）
    const send = await request.post(`${BASE}/api/auth/sms-code?phone=${phone}`);
    expect((await send.json()).code).toBe('A0000');
    const reg = await request.post(`${BASE}/api/auth/register`, {
        data: { username: `sms${s}`, password: 'Demo123456', phone, verifyCode: SMS },
    });
    expect((await reg.json()).code, JSON.stringify(await reg.json())).toBe('A0000');

    await page.goto('/login');
    await page.getByRole('button', { name: '短信登录' }).click();
    await page.getByLabel('手机号').fill(phone);
    // 同号 3s 内重发会被防重拦（刚 API 取过一次码）—— 等冷却过再点，真人操作节奏天然在这之上
    await page.waitForTimeout(3200);
    await page.getByRole('button', { name: '获取验证码' }).click();
    await expect(toast(page, '验证码已发送')).toBeVisible({ timeout: 15_000 });
    await page.getByLabel('短信验证码').fill(SMS);
    await page.getByTestId('btn-login-submit').click();
    await expect(page.getByTestId('btn-user-menu')).toBeVisible({ timeout: 20_000 });
});

test('T2 描述式搜索 ≤3s 出结果 + 空结果中文引导', async ({ page }) => {
    await page.goto('/search');

    // AI 智能搜索默认开（评委直输描述句也能出结果的关键）
    await expect(page.getByTitle('关闭AI智能搜索')).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('.search-ai-btn-label')).toHaveText('AI 开');

    const input = page.getByLabel('搜索商品');
    // 冷启动后的第一查即正式计量：后端启动后自动预热搜索增强缓存（AiSearchEnhancementWarmup，
    // 每 4min 强制刷新 < 5min TTL），录屏不再依赖「先手动搜一次」的人工预热动作。
    // 门槛 3s 是预热后的新基线；真撞上冷查（预热尚未完成）也由加载中的「正在搜索中...」反馈兜底。
    await input.fill('适合拍夜景的相机');
    const t0 = Date.now();
    await page.locator('.search-submit-btn').click();
    await expect
        .poll(async () => page.locator('.product-card-premium').count(), { timeout: 15_000 })
        .toBeGreaterThanOrEqual(4);
    const elapsed = Date.now() - t0;
    console.log(`[T2] 描述式搜索（冷启动首查）出结果耗时 ${elapsed}ms`);
    expect(elapsed, `描述式搜索耗时 ${elapsed}ms`).toBeLessThan(3000);

    // AI 意图面板有内容（不是空壳）
    await expect(page.locator('.ai-search-panel').first()).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('.ai-section-content').first()).not.toHaveText('');

    // 空结果 → 中文引导，不白屏不英文
    await input.fill('绝不可能存在的宝贝zzz');
    await page.locator('.search-submit-btn').click();
    await expect(page.locator('.search-no-results-desc')).toBeVisible({ timeout: 15_000 });
});

test('T3 发布助手：拍照 AI 填单 → 立即发布 → 我的发布上下架', async ({ page, request }) => {
    const seller = await apiLogin(request, 'testuser', 'Password123');
    await uiLogin(page, 'testuser', 'Password123');

    // ── 上传图片 → AI 智能识别自动填单 ──
    await page.goto('/publish');
    await page.locator('input[type="file"]').setInputFiles('/tmp/demo_camera.jpg');
    const aiBtn = page.locator('.ai-photo-btn');
    await expect(aiBtn).toBeVisible({ timeout: 15_000 });
    const t0 = Date.now();
    await aiBtn.click();
    const nameInput = page.locator('#name');
    await expect(nameInput).not.toHaveValue('', { timeout: 25_000 });
    const title = await nameInput.inputValue();
    await expect(page.locator('#price')).not.toHaveValue('');
    console.log(`[T3] AI 拍照填单耗时 ${Date.now() - t0}ms → ${title}`);
    expect(title.length).toBeGreaterThan(2);

    // ── 立即发布（创建 → 提审）──
    const newName = `UI发布验证相机 ${stamp()}`;
    await nameInput.fill(newName);
    await page.locator('.btn-publish-v2').click();
    await page.waitForURL(/\/products\/[0-9a-zA-Z-]+/, { timeout: 30_000 });
    const pid = page.url().split('/products/')[1];
    console.log(`[T3] 发布成功 pid=${pid}`);

    // ── 我的发布：新商品出现在「审核中」──
    await page.goto('/my-products');
    await page.getByRole('button', { name: /审核中/ }).click();
    await expect(page.getByText(newName)).toBeVisible({ timeout: 15_000 });

    // ── 上下架：种子「AKG K72」下架位 → 点上架 → 再点下架复位 ──
    await page.getByRole('button', { name: /已下架/ }).click();
    const offlineCard = page.locator('.order-card-premium', { hasText: 'AKG K72' });
    await expect(offlineCard).toBeVisible({ timeout: 15_000 });
    await offlineCard.getByRole('button', { name: '上架' }).click();
    await expect(toast(page, '已重新上架')).toBeVisible({ timeout: 15_000 });

    await page.getByRole('button', { name: /在售/ }).click();
    const onlineCard = page.locator('.order-card-premium', { hasText: 'AKG K72' });
    await expect(onlineCard).toBeVisible({ timeout: 15_000 });
    await onlineCard.getByRole('button', { name: '下架' }).click();
    await expect(toast(page, /^已下架$/)).toBeVisible({ timeout: 15_000 });

    // ── 收尾：删除本测试产生的提审商品（保住种子里 410 的待审核演示位）──
    await apiDeleteProduct(request, seller, pid);
});

test('T4 管理端：仪表盘有数 + 审核通过/驳回填理由 + 各列表非空', async ({ page, request }) => {
    const admin = await apiLogin(request, 'admin', 'Password123');
    const seller = await apiLogin(request, 'testuser', 'Password123');
    // 名称含本次 stamp：行定位用全名（前缀过滤会命中历史失败遗留的同类临时件 → strict mode 爆）
    const approveName = `UI审核通过临时件 ${stamp()}`;
    const rejectName = `UI驳回临时件 ${stamp()}`;
    const tempApprove = await apiCreatePendingProduct(request, seller, approveName);
    const tempReject = await apiCreatePendingProduct(request, seller, rejectName);
    // 无论中途成败都删临时件（失败遗留会污染待审核队列与下一次行定位）
    try {

    await uiLogin(page, 'admin', 'Password123');
    // 深链直达后台（浮动导航菜单入口的可见性由 tests/e2e/admin.spec 钉住；
    // 这里验的是真实管理员会话能过 AdminRouteGuard 进入后台）
    await page.goto('/admin');
    await expect(page.locator('.admin-layout')).toBeVisible({ timeout: 20_000 });

    // ── 数据统计：卡片有真实数字、趋势图非空 ──
    await page.goto('/admin');
    await expect(page.getByText('总用户数')).toBeVisible({ timeout: 20_000 });
    await expect(page.getByText('总商品数')).toBeVisible();
    // 趋势图：区块标题 + recharts 真实渲染出 SVG（testid 只存在于单测 mock，真图看 recharts-wrapper）
    await expect(page.getByText('月度趋势')).toBeVisible({ timeout: 25_000 });
    await expect(page.locator('.recharts-wrapper').first()).toBeVisible({ timeout: 25_000 });

    // ── 商品审核：通过一件 ──
    await page.goto('/admin/products');
    const approveRow = page.locator('tr', { hasText: approveName });
    await expect(approveRow).toBeVisible({ timeout: 20_000 });
    await approveRow.getByRole('button', { name: /审核/ }).click();
    await expect(page.getByRole('button', { name: /通过审核/ })).toBeVisible({ timeout: 15_000 });
    await page.getByRole('button', { name: /通过审核/ }).click();
    // 新版管理端：点击「通过审核」先弹二次确认（ConfirmModal），确认文案「通过并上架」
    await page.getByRole('button', { name: '通过并上架' }).click();
    await expect(toast(page, /审核已通过/)).toBeVisible({ timeout: 15_000 });
    // 抽屉/弹窗可能已自动收起，关闭动作做尽力而为（收尾竞态不计入判定）
    const closeBtn = page.getByRole('button', { name: '关闭', exact: true });
    if (await closeBtn.isVisible().catch(() => false)) {
        await closeBtn.click({ timeout: 3000 }).catch(() => {});
    }
    await page.keyboard.press('Escape').catch(() => {});

    // ── 驳回一件（必填理由）──
    const rejectRow = page.locator('tr', { hasText: rejectName });
    await expect(rejectRow).toBeVisible({ timeout: 20_000 });
    await rejectRow.getByRole('button', { name: /审核/ }).click();
    await expect(page.getByRole('button', { name: /驳回商品/ })).toBeVisible({ timeout: 15_000 });
    await page.getByRole('button', { name: /驳回商品/ }).click();
    await page.locator('#reject-reason').fill('图片不清晰，请补充实拍图');
    await page.getByRole('button', { name: '确认驳回' }).click();
    await expect(toast(page, /已驳回/)).toBeVisible({ timeout: 15_000 });

    // 后端复核两件临时商品的终态
    const st1 = await request.get(`${BASE}/api/products/${tempApprove}`, {
        headers: { Authorization: `Bearer ${seller}` },
    });
    expect((await st1.json()).data.status).toBe('ONLINE');
    const st2 = await request.get(`${BASE}/api/products/${tempReject}`, {
        headers: { Authorization: `Bearer ${seller}` },
    });
    expect((await st2.json()).data.status).toBe('REJECTED');

    // 驳回理由进了审核日志
    const logs = await request.get(`${BASE}/api/admin/products/${tempReject}/audit-logs`, {
        headers: { Authorization: `Bearer ${admin}` },
    });
    const logRows = (await logs.json()).data as Array<{ reason?: string }>;
    expect(logRows.some(r => r.reason === '图片不清晰，请补充实拍图')).toBeTruthy();

    // ── 用户 / 订单 / 分类 / 知识库 列表均非空 ──
    await page.goto('/admin/users');
    await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 20_000 });
    await page.goto('/admin/orders');
    await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 20_000 });
    await page.goto('/admin/categories');
    await expect(page.getByText('电子数码').first()).toBeVisible({ timeout: 20_000 });
    await page.goto('/admin/knowledge');
    await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 20_000 });

    } finally {
        // ── 收尾：删除两件临时商品 ──
        await request.delete(`${BASE}/api/products/${tempApprove}`, {
            headers: { Authorization: `Bearer ${seller}` },
        });
        await request.delete(`${BASE}/api/products/${tempReject}`, {
            headers: { Authorization: `Bearer ${seller}` },
        });
    }
});
