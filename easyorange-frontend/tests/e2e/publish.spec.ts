import { test, expect } from '@playwright/test';
import { seedSession, spaNavigate } from './helpers/auth';

/**
 * 发布资产流程 — 与 checkout.spec.ts 同款模式：preview 构建 + page.route mock API。
 * 覆盖「登录态 → 发布表单 → 选分类/成色 → 提交 → 创建 + 上架 → 跳转详情」的关键链路。
 */
const MOCK_CATEGORY = { id: 'cat-1', name: '数码数码', icon: null, sortOrder: 1, status: 1 };

test.describe('发布资产流程', () => {
  // 分类 mock 单独成方法：Playwright 路由后注册者优先（LIFO），须在通用 **/api/products** 之后注册
  async function mockCategories(page: import('@playwright/test').Page) {
    await page.route('**/api/products/categories**', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ code: 'A0000', message: 'ok', data: [MOCK_CATEGORY] }),
        })
    );
  }

  test.beforeEach(async ({ page }) => {
    await seedSession(page, { userId: '1', username: 'seller', nickname: '资产方' });
    await mockCategories(page);
    await page.goto('/');
    await expect(page.locator('[data-testid="btn-user-menu"]')).toBeVisible({ timeout: 20000 });
  });

  test('发布表单渲染必填控件且分类下拉加载成功', async ({ page }) => {
    await spaNavigate(page, '/publish');

    await expect(page.locator('input[placeholder="给资产起个吸引人的名字"]')).toBeVisible({ timeout: 15000 });
    await expect(page.getByText('立即发布').first()).toBeVisible();
    await expect(page.locator('textarea').first()).toBeVisible();

    // 分类下拉：mock 分类加载后可选
    const categoryTrigger = page.getByRole('combobox').first();
    await categoryTrigger.click();
    await expect(page.getByRole('option', { name: MOCK_CATEGORY.name })).toBeVisible({ timeout: 5000 });
    await page.getByRole('option', { name: MOCK_CATEGORY.name }).click();
    await expect(categoryTrigger).toContainText(MOCK_CATEGORY.name);
  });

  test('填写必填项提交后调用创建与上架接口并跳转详情', async ({ page }) => {
    const calls: { method: string; url: string; body?: unknown }[] = [];
    await page.route('**/api/products**', async route => {
      const method = route.request().method();
      if (method === 'POST') {
        calls.push({ method, url: route.request().url(), body: route.request().postDataJSON() });
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ code: 'A0000', message: 'ok', data: 'prod-new-1' }),
        });
        return;
      }
      if (method === 'PUT' && route.request().url().includes('/prod-new-1/online')) {
        calls.push({ method, url: route.request().url() });
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ code: 'A0000', message: 'ok', data: null }),
        });
        return;
      }
      await route.continue();
    });
    // 详情页渲染兜底（提交成功后跳转 /products/prod-new-1）
    await page.route('**/api/products/prod-new-1', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                code: 'A0000',
                message: 'ok',
                data: {
                    id: 'prod-new-1',
                    title: 'E2E 测试资产',
                    price: 199,
                    categoryId: 'cat-1',
                    categoryName: MOCK_CATEGORY.name,
                    status: 'ONLINE',
                    sellerId: '1',
                    stock: 1,
                    images: [],
                },
            }),
        })
    );

    // 图片上传 mock（发布表单必填 ≥1 张图）
    await page.route('**/api/file/upload**', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                code: 'A0000',
                message: 'ok',
                data: { url: 'https://e2e.local/a.png', filename: 'e2e.png', size: 1024, type: 'image/png' },
            }),
        })
    );
    // 通用 handler 抢占分类路由 → 重新注册分类 mock（后注册优先）
    await mockCategories(page);

    await spaNavigate(page, '/publish');

    // 1x1 PNG 注入图片上传（命中 /file/upload mock）
    const png = Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
        'base64'
    );
    await page.locator('input[type="file"]').first().setInputFiles({
        name: 'e2e.png',
        mimeType: 'image/png',
        buffer: png,
    });

    await page.locator('input[placeholder="给资产起个吸引人的名字"]').fill('E2E 测试资产');
    await page.getByRole('combobox').nth(0).click();
    await page.getByRole('option', { name: MOCK_CATEGORY.name }).click();
    await page.getByRole('combobox').nth(1).click();
    await page.getByRole('option', { name: '全新', exact: true }).click();
    await page.locator('textarea').first().fill('E2E 自动化发布的测试描述');
    await page.locator('input[placeholder="0.00"]').first().fill('199');

    await page.getByText('立即发布').first().click();

    // 创建 + 上架都被调用，且跳转到新资产详情页
    await expect(page).toHaveURL(/\/products\/prod-new-1/, { timeout: 15000 });
    const createCall = calls.find(c => c.method === 'POST');
    expect(createCall).toBeTruthy();
    expect((createCall?.body as { name?: string }).name).toBe('E2E 测试资产');
    expect(calls.some(c => c.method === 'PUT' && c.url.includes('/online'))).toBeTruthy();
  });
});
