import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { Product } from '@/types';
import EditProductPage from './EditProductPage';

const mockUseProduct = vi.hoisted(() => vi.fn());
const mockUseUpdateProduct = vi.hoisted(() => vi.fn());
const mockUseDeleteProduct = vi.hoisted(() => vi.fn());
const mockUseCategories = vi.hoisted(() => vi.fn());
const mockNavigate = vi.hoisted(() => vi.fn());
const mockUploadFile = vi.hoisted(() => vi.fn());
const mockCompressImage = vi.hoisted(() => vi.fn());

vi.mock('@/hooks', () => ({
    useProduct: mockUseProduct,
    useUpdateProduct: mockUseUpdateProduct,
    useDeleteProduct: mockUseDeleteProduct,
    useCategories: mockUseCategories,
    useCurrentUser: vi.fn(() => ({ data: { userId: 'user1' }, isLoading: false })),
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...(actual as object),
        useNavigate: () => mockNavigate,
        useParams: () => ({ id: '1' }),
    };
});

vi.mock('@/api/uploadApi', () => ({
    uploadFile: mockUploadFile,
}));

vi.mock('@/utils/imageCompress', () => ({
    compressImage: mockCompressImage,
}));

HTMLCanvasElement.prototype.toDataURL = vi.fn().mockImplementation(() => 'data:image/png;base64,test');

function createMockProduct(overrides: Partial<Product> = {}): Product {
    return {
        id: '1',
        title: '可编辑商品',
        description: '这是一个可以编辑的测试商品',
        price: 99.99,
        originalPrice: 150,
        categoryId: '1',
        categoryName: '电子产品',
        condition: 2,
        conditionLevel: 2,
        status: 'ONLINE',
        images: ['https://example.com/img1.jpg', 'https://example.com/img2.jpg'],
        location: '北京海淀',
        views: 100,
        favorites: 20,
        sellerId: 'user1',
        sellerName: '资产方小明',
        sellerAvatar: null,
        sellerRating: 4.5,
        createTime: '2026-05-16 10:00:00',
        updateTime: '2026-05-16 10:00:00',
        ...overrides,
    };
}

function renderPage() {
    return renderWithProviders(<EditProductPage />, {
        initialRoute: '/products/1/edit',
    });
}

beforeEach(() => {
    vi.clearAllMocks();
    HTMLCanvasElement.prototype.toDataURL = vi.fn(() => 'data:image/png;base64,test');
    mockUseCategories.mockReturnValue({
        data: [
            { id: '1', name: '电子产品' },
            { id: '2', name: '服装鞋帽' },
        ],
        isLoading: false,
    });
    mockUseProduct.mockReturnValue({
        data: createMockProduct(),
        isLoading: false,
        isError: false,
        error: null,
    });
    mockUseUpdateProduct.mockReturnValue({
        mutateAsync: vi.fn().mockResolvedValue(undefined),
        isPending: false,
        isError: false,
        error: null,
    });
    mockUseDeleteProduct.mockReturnValue({
        mutateAsync: vi.fn().mockResolvedValue(undefined),
        isPending: false,
    });
    mockUploadFile.mockResolvedValue({
        data: { url: 'https://example.com/new-image.jpg' },
    });
    mockCompressImage.mockResolvedValue(new File([''], 'compressed.jpg', { type: 'image/jpeg' }));
});

describe('EditProductPage', () => {
    it('renders loading state while product is loading', () => {
        mockUseProduct.mockReturnValue({
            data: undefined,
            isLoading: true,
        });

        renderPage();

        expect(screen.getByText('加载商品信息...')).toBeInTheDocument();
    });

    it('renders empty state when product is not found', () => {
        mockUseProduct.mockReturnValue({
            data: undefined,
            isLoading: false,
        });

        renderPage();

        expect(screen.getByText('商品不存在')).toBeInTheDocument();
        const backBtn = screen.getByText('返回商品列表');
        expect(backBtn).toBeInTheDocument();
    });

    it('navigates to product list from empty state', async () => {
        mockUseProduct.mockReturnValue({
            data: undefined,
            isLoading: false,
        });

        renderPage();

        const user = userEvent.setup();
        const backBtn = screen.getByText('返回商品列表');
        await user.click(backBtn);
        expect(mockNavigate).toHaveBeenCalledWith('/products');
    });

    it('renders page title and navigation', () => {
        renderPage();

        expect(screen.getByText('编辑商品')).toBeInTheDocument();
        expect(screen.getByText('修改商品信息')).toBeInTheDocument();
    });

    it('renders form pre-filled with product data', () => {
        renderPage();

        const nameInput = screen.getByDisplayValue('可编辑商品') as HTMLInputElement;
        expect(nameInput).toBeInTheDocument();

        const priceInput = screen.getByDisplayValue('99.99') as HTMLInputElement;
        expect(priceInput).toBeInTheDocument();

        const descTextarea = screen.getByDisplayValue('这是一个可以编辑的测试商品') as HTMLTextAreaElement;
        expect(descTextarea).toBeInTheDocument();
    });

    it('renders existing images', () => {
        renderPage();

        const images = document.querySelectorAll('.edit-image-item');
        expect(images.length).toBe(2);
        expect(document.querySelector('.edit-cover-badge')).toBeInTheDocument();
    });

    it('removes an image when remove button is clicked', async () => {
        renderPage();

        const user = userEvent.setup();
        const removeBtns = document.querySelectorAll('.edit-image-remove');
        expect(removeBtns.length).toBe(2);

        await user.click(removeBtns[0]);

        const remainingImages = document.querySelectorAll('.edit-image-item');
        expect(remainingImages.length).toBe(1);
    });

    it('shows validation errors when saving with empty required fields', async () => {
        renderPage();

        const user = userEvent.setup();
        const nameInput = screen.getByDisplayValue('可编辑商品');
        await user.clear(nameInput);

        const saveBtn = screen.getByText('保存修改');
        await user.click(saveBtn);

        expect(screen.getByText('请输入资产名称')).toBeInTheDocument();
    });

    it('calls updateProduct and navigates on save', async () => {
        const mockUpdateMutateAsync = vi.fn().mockResolvedValue(undefined);
        mockUseUpdateProduct.mockReturnValue({
            mutateAsync: mockUpdateMutateAsync,
            isPending: false,
            isError: false,
        });

        renderPage();

        const user = userEvent.setup();
        const saveBtn = screen.getByText('保存修改');
        await user.click(saveBtn);

        await waitFor(() => {
            expect(mockUpdateMutateAsync).toHaveBeenCalled();
            expect(mockNavigate).toHaveBeenCalledWith('/products/1');
        });
    });

    it('shows delete confirmation modal when delete button is clicked', async () => {
        renderPage();

        const user = userEvent.setup();
        const deleteBtn = screen.getByText('删除商品');
        await user.click(deleteBtn);

        const confirmTexts = screen.getAllByText('确认删除');
        expect(confirmTexts.length).toBe(2);
        expect(screen.getByText('确定要删除这个商品吗？此操作不可撤销。')).toBeInTheDocument();
    });

    it('closes delete confirmation modal on cancel', async () => {
        renderPage();

        const user = userEvent.setup();
        const deleteBtn = screen.getByText('删除商品');
        await user.click(deleteBtn);

        expect(screen.getByText('确定要删除这个商品吗？此操作不可撤销。')).toBeInTheDocument();

        const cancelBtns = screen.getAllByText('取消');
        const modalCancelBtn = cancelBtns[cancelBtns.length - 1];
        await user.click(modalCancelBtn);

        await waitFor(() => {
            expect(screen.queryByText('确定要删除这个商品吗？此操作不可撤销。')).not.toBeInTheDocument();
        });
    });

    it('calls deleteProduct and navigates on confirm delete', async () => {
        const mockDeleteMutateAsync = vi.fn().mockResolvedValue(undefined);
        mockUseDeleteProduct.mockReturnValue({
            mutateAsync: mockDeleteMutateAsync,
            isPending: false,
        });

        renderPage();

        const user = userEvent.setup();
        const deleteBtn = screen.getByText('删除商品');
        await user.click(deleteBtn);

        const confirmBtns = screen.getAllByText('确认删除');
        await user.click(confirmBtns[1]);

        await waitFor(() => {
            expect(mockDeleteMutateAsync).toHaveBeenCalledWith('1');
            expect(mockNavigate).toHaveBeenCalledWith('/products');
        });
    });

    it('disables save button while submitting', () => {
        mockUseUpdateProduct.mockReturnValue({
            mutateAsync: vi.fn(),
            isPending: true,
            isError: false,
        });

        renderPage();

        const saveBtn = document.querySelector('.edit-btn-primary') as HTMLButtonElement;
        expect(saveBtn).toBeInTheDocument();
        expect(saveBtn).toBeDisabled();
    });

    it('shows update error message', () => {
        mockUseUpdateProduct.mockReturnValue({
            mutateAsync: vi.fn(),
            isPending: false,
            isError: true,
            error: new Error('更新失败'),
        });

        renderPage();

        expect(screen.getByText('更新失败，请稍后重试')).toBeInTheDocument();
    });

    it('navigates back when back button is clicked', async () => {
        renderPage();

        const user = userEvent.setup();
        const backBtn = document.querySelector('.edit-back-btn') as HTMLElement;
        expect(backBtn).toBeInTheDocument();
        await user.click(backBtn);
        expect(mockNavigate).toHaveBeenCalledWith(-1);
    });

    it('navigates back when cancel button is clicked', async () => {
        renderPage();

        const user = userEvent.setup();
        const cancelBtn = screen.getByText('取消');
        await user.click(cancelBtn);
        expect(mockNavigate).toHaveBeenCalledWith(-1);
    });

    it('renders danger zone with delete button', () => {
        renderPage();

        expect(screen.getByText('危险操作')).toBeInTheDocument();
        expect(screen.getByText('删除商品后数据将无法恢复，请谨慎操作')).toBeInTheDocument();
    });

    it('renders AI tip section', () => {
        renderPage();

        expect(screen.getByText('AI智能助手：完善商品信息可获得更多曝光')).toBeInTheDocument();
    });

    it('renders category select with options', async () => {
        renderPage();

        const user = userEvent.setup();
        const categoryTrigger = screen.getByRole('combobox', { name: /商品类别/ });
        await user.click(categoryTrigger);

        expect(screen.getByRole('option', { name: '电子产品' })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: '服装鞋帽' })).toBeInTheDocument();
    });
});
