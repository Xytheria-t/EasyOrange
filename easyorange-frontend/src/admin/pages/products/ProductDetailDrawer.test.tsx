import { fireEvent, screen } from '@testing-library/react';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { AdminProduct, AuditLogResponse } from '../../types/admin';
import { ProductDetailDrawer } from './ProductDetailDrawer';

// ─── Mock scrollIntoView for AdminSelect portal (used in drawer's parent) ───
beforeAll(() => {
    Element.prototype.scrollIntoView = vi.fn();
});

// ─── Hook mocks ───
const mockUseAdminProductDetail = vi.fn();
const mockUseAuditProduct = vi.fn();
const mockUseAuditLogs = vi.fn();

vi.mock('../../hooks/useAdminProducts', () => ({
    useAdminProductDetail: (...args: unknown[]) => mockUseAdminProductDetail(...args),
}));

vi.mock('../../hooks/useAdminProductAudit', () => ({
    useAuditProduct: (...args: unknown[]) => mockUseAuditProduct(...args),
    useAuditLogs: (...args: unknown[]) => mockUseAuditLogs(...args),
    ADMIN_AUDIT_KEYS: { all: ['admin', 'audit'] },
}));

// ─── Sample data ───
const sampleProduct: AdminProduct = {
    productId: '1',
    name: '审核商品',
    description: '这是一个待审核的商品描述',
    price: 100,
    originalPrice: 150,
    stock: 10,
    status: 'PENDING_REVIEW',
    statusDesc: '待审核',
    conditionLevel: 9,
    location: '北京',
    contactMethod: '微信',
    images: ['https://example.com/img1.jpg'],
    mainImage: null,
    categoryId: '1',
    categoryName: '电子产品',
    sellerId: '1',
    sellerName: '资产方A',
    sellerAvatar: null,
    viewCount: 100,
    createTime: '2026-05-16T10:00:00',
    updateTime: '2026-05-16T10:00:00',
};

const auditLogs: AuditLogResponse[] = [
    {
        id: '1',
        productId: '1',
        operatorId: '1',
        operatorName: '管理员',
        action: 2,
        actionDesc: '驳回',
        reason: '信息不完整',
        dimensions: ['basic'],
        beforeStatus: 4,
        beforeStatusDesc: '待审核',
        afterStatus: 5,
        afterStatusDesc: '已驳回',
        remark: null,
        createTime: '2026-05-16T08:00:00',
    },
];

function setupDefaultMocks() {
    mockUseAdminProductDetail.mockReturnValue({
        data: sampleProduct,
        isLoading: false,
        refetch: vi.fn(),
    });

    mockUseAuditProduct.mockReturnValue({
        mutateAsync: vi.fn().mockResolvedValue({}),
        isPending: false,
    });

    mockUseAuditLogs.mockReturnValue({
        data: auditLogs,
        isLoading: false,
    });
}

describe('ProductDetailDrawer', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        setupDefaultMocks();
    });

    // ── Test 1: Does not render when open=false ──
    it('does not render when open is false', () => {
        const { container } = renderWithProviders(
            <ProductDetailDrawer open={false} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />
        );
        // Returns null when open=false
        expect(container.innerHTML).toBe('');
    });

    // ── Test 2: Opens with product data when open=true ──
    it('renders product detail drawer when open is true', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByText('商品详情')).toBeInTheDocument();
        expect(screen.getByText('审核商品')).toBeInTheDocument();
    });

    // ── Test 3: Shows loading state ──
    it('shows loading state while fetching product', () => {
        mockUseAdminProductDetail.mockReturnValue({
            data: undefined,
            isLoading: true,
            refetch: vi.fn(),
        });

        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByText('加载中...')).toBeInTheDocument();
    });

    // ── Test 4: Shows "商品不存在" when product is null ──
    it('shows "商品不存在或已被删除" when product data is undefined after load', () => {
        mockUseAdminProductDetail.mockReturnValue({
            data: undefined,
            isLoading: false,
            refetch: vi.fn(),
        });

        renderWithProviders(
            <ProductDetailDrawer open={true} productId={'999'} onClose={vi.fn()} onSuccess={vi.fn()} />
        );
        expect(screen.getByText('商品不存在或已被删除')).toBeInTheDocument();
    });

    // ── Test 5: Renders product info ──
    it('renders product info fields', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByText('¥100.00')).toBeInTheDocument();
        expect(screen.getByText('¥150.00')).toBeInTheDocument();
        expect(screen.getByText('电子产品')).toBeInTheDocument();
        expect(screen.getByText('资产方A')).toBeInTheDocument();
    });

    // ── Test 6: Renders 4 audit dimension buttons ──
    it('renders 4 audit dimension toggle buttons', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByText('基本信息合规')).toBeInTheDocument();
        expect(screen.getByText('内容无违规')).toBeInTheDocument();
        expect(screen.getByText('图片质量合格')).toBeInTheDocument();
        expect(screen.getByText('价格合理')).toBeInTheDocument();
    });

    // ── Test 7: Clicking dimension button toggles selection ──
    it('toggles dimension selection when clicked', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        const dimensionBtn = screen.getByText('基本信息合规');
        // Initially no checkmark
        expect(dimensionBtn.textContent).toBe('基本信息合规');
        // Click to select
        fireEvent.click(dimensionBtn);
        expect(dimensionBtn.textContent).toBe('✓ 基本信息合规');
        // Click again to deselect
        fireEvent.click(dimensionBtn);
        expect(dimensionBtn.textContent).toBe('基本信息合规');
    });

    // ── Test 8: "审核维度" header exists ──
    it('renders audit dimension section header', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByText('审核维度')).toBeInTheDocument();
    });

    // ── Test 9: "通过审核" button exists ──
    it('renders "通过审核" approve button', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByText('✅ 通过审核')).toBeInTheDocument();
    });

    // ── Test 10: "驳回商品" button exists ──
    it('renders "驳回商品" reject button', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByText('🚫 驳回商品')).toBeInTheDocument();
    });

    // ── Test 11: Clicking "通过审核" calls approve mutation ──
    it('calls audit mutateAsync with action=1 when "通过审核" is clicked', () => {
        const mutateAsync = vi.fn().mockResolvedValue({});
        mockUseAuditProduct.mockReturnValue({
            mutateAsync,
            isPending: false,
        });

        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        fireEvent.click(screen.getByText('✅ 通过审核'));
        expect(mutateAsync).toHaveBeenCalledWith({
            id: '1',
            data: { action: 1, dimensions: [], remark: undefined },
        });
    });

    // ── Test 12: Clicking "驳回商品" opens reject modal ──
    it('opens reject confirmation modal when "驳回商品" is clicked', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        fireEvent.click(screen.getByText('🚫 驳回商品'));
        // The h3 text includes emoji "⚠️ 确认驳回商品" — use a partial text matcher
        expect(screen.getByText(content => content.includes('确认驳回商品'))).toBeInTheDocument();
        expect(screen.getByText('确认驳回')).toBeInTheDocument();
    });

    // ── Test 13: Cancel button in reject modal closes it ──
    it('closes reject modal when cancel is clicked', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        // Open reject modal
        fireEvent.click(screen.getByText('🚫 驳回商品'));
        expect(screen.getByText(content => content.includes('确认驳回商品'))).toBeInTheDocument();

        // Cancel
        fireEvent.click(screen.getByText('取消'));
        expect(screen.queryByText(content => content.includes('确认驳回商品'))).not.toBeInTheDocument();
    });

    // ── Test 14: "确认驳回" is disabled when reason is empty ──
    it('disables confirm reject button when reason is empty', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        fireEvent.click(screen.getByText('🚫 驳回商品'));
        const confirmBtn = screen.getByText('确认驳回');
        expect(confirmBtn).toBeDisabled();
    });

    // ── Test 15: Adding reason enables confirm reject ──
    it('enables confirm reject button when reason is typed', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        fireEvent.click(screen.getByText('🚫 驳回商品'));

        // Type a reason
        const textarea = screen.getByPlaceholderText('请填写驳回原因（必填）...');
        fireEvent.change(textarea, { target: { value: '信息不完整' } });

        const confirmBtn = screen.getByText('确认驳回');
        expect(confirmBtn).not.toBeDisabled();
    });

    // ── Test 16: Confirm reject with reason calls mutation ──
    it('calls audit mutateAsync with action=2 when reject is confirmed with reason', () => {
        const mutateAsync = vi.fn().mockResolvedValue({});
        mockUseAuditProduct.mockReturnValue({
            mutateAsync,
            isPending: false,
        });

        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        fireEvent.click(screen.getByText('🚫 驳回商品'));

        const textarea = screen.getByPlaceholderText('请填写驳回原因（必填）...');
        fireEvent.change(textarea, { target: { value: '信息不完整' } });

        fireEvent.click(screen.getByText('确认驳回'));

        expect(mutateAsync).toHaveBeenCalledWith({
            id: '1',
            data: { action: 2, reason: '信息不完整', dimensions: [], remark: undefined },
        });
    });

    // ── Test 17: Quick reject reason tags ──
    it('renders quick reject reason tags', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        fireEvent.click(screen.getByText('🚫 驳回商品'));

        // "信息不完整" appears both in audit log and reject tags, use getAllByText
        const infoElements = screen.getAllByText('信息不完整');
        expect(infoElements.length).toBeGreaterThanOrEqual(2);
        expect(screen.getByText('图片模糊')).toBeInTheDocument();
        expect(screen.getByText('疑似虚假信息')).toBeInTheDocument();
        expect(screen.getByText('价格异常')).toBeInTheDocument();
        expect(screen.getByText('违规内容')).toBeInTheDocument();
        expect(screen.getByText('其他')).toBeInTheDocument();
    });

    // ── Test 18: Clicking quick reason tag fills reason ──
    it('appends quick reason tag text to reject reason', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        fireEvent.click(screen.getByText('🚫 驳回商品'));

        // Use getAllByText since "信息不完整" appears in both audit log and reject tags
        const infoTags = screen.getAllByText('信息不完整');
        // Click the one from the reject modal tags (the button element)
        const rejectTag = infoTags.find(el => el.tagName === 'BUTTON');
        expect(rejectTag).toBeTruthy();
        fireEvent.click(rejectTag as HTMLElement);
        const textarea = screen.getByPlaceholderText('请填写驳回原因（必填）...') as HTMLTextAreaElement;
        expect(textarea.value).toContain('信息不完整');
    });

    // ── Test 19: "关闭" button exists in drawer ──
    it('renders close button in drawer header', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        // "关闭" button is in the footer (ignore hidden sheet sr-only text)
        const closeButtons = screen.getAllByText('关闭').filter(el => el.tagName === 'BUTTON');
        expect(closeButtons.length).toBeGreaterThanOrEqual(1);
    });

    // ── Test 20: "关闭" button calls onClose ──
    it('calls onClose when close button is clicked', () => {
        const onClose = vi.fn();
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={onClose} onSuccess={vi.fn()} />);
        // Filter to visible button elements and click the footer close button
        const closeButtons = screen.getAllByText('关闭').filter(el => el.tagName === 'BUTTON');
        fireEvent.click(closeButtons[closeButtons.length - 1]);
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    // ── Test 21: Audit logs display ──
    it('renders audit log timeline entries', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByText('📜 审核记录')).toBeInTheDocument();
        expect(screen.getByText('管理员')).toBeInTheDocument();
        expect(screen.getByText('信息不完整')).toBeInTheDocument();
        expect(screen.getByText('驳回')).toBeInTheDocument();
    });

    // ── Test 22: Audit log dimension tags render ──
    it('renders audit log dimension tags', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        // Should show "基本信息" dimension tag from audit log
        expect(screen.getByText('基本信息')).toBeInTheDocument();
    });

    // ── Test 23: Audit logs are hidden when empty ──
    it('does not show audit log section when no logs exist', () => {
        mockUseAuditLogs.mockReturnValue({
            data: [],
            isLoading: false,
        });

        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.queryByText('📜 审核记录')).not.toBeInTheDocument();
    });

    // ── Test 24: View count display ──
    it('renders view count', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByText('100')).toBeInTheDocument();
        expect(screen.getByText(/次浏览/)).toBeInTheDocument();
    });

    // ── Test 25: "审核意见" textarea exists ──
    it('renders audit remark textarea', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByPlaceholderText('审核意见（选填）...')).toBeInTheDocument();
    });

    // ── Test 26: Options for the "点击预览" label ──
    it('renders click-to-preview label on image', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByText('点击预览')).toBeInTheDocument();
    });

    // ── Test 27: Product description renders ──
    it('renders product description section', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByText('商品描述')).toBeInTheDocument();
        expect(screen.getByText('这是一个待审核的商品描述')).toBeInTheDocument();
    });

    // ── Test 28: Location renders ──
    it('renders product location', () => {
        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        expect(screen.getByText('北京')).toBeInTheDocument();
        expect(screen.getByText('交易地点：')).toBeInTheDocument();
    });

    // ── Test 29: Approve with dimensions selected ──
    it('sends selected dimensions with approve mutation', () => {
        const mutateAsync = vi.fn().mockResolvedValue({});
        mockUseAuditProduct.mockReturnValue({
            mutateAsync,
            isPending: false,
        });

        renderWithProviders(<ProductDetailDrawer open={true} productId={'1'} onClose={vi.fn()} onSuccess={vi.fn()} />);
        // Select two dimensions
        fireEvent.click(screen.getByText('基本信息合规'));
        fireEvent.click(screen.getByText('内容无违规'));

        fireEvent.click(screen.getByText('✅ 通过审核'));

        expect(mutateAsync).toHaveBeenCalledWith({
            id: '1',
            data: {
                action: 1,
                dimensions: ['basic', 'compliance'],
                remark: undefined,
            },
        });
    });
});
