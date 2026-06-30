import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PublishPage from './PublishPage';

const mockUseCreateProduct = vi.hoisted(() => vi.fn());
const mockUseCategories = vi.hoisted(() => vi.fn());
const mockNavigate = vi.hoisted(() => vi.fn());
const mockAddToast = vi.hoisted(() => vi.fn());
const mockUploadFile = vi.hoisted(() => vi.fn());
const mockCompressImage = vi.hoisted(() => vi.fn());
const mockGoOnline = vi.hoisted(() => vi.fn());

vi.mock('@/hooks', () => ({
  useCreateProduct: mockUseCreateProduct,
  useCategories: mockUseCategories,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...(actual as object), useNavigate: () => mockNavigate };
});

vi.mock('@/store/uiStore', () => ({
  useUIStore: vi.fn((sel) => {
    const s = { addToast: mockAddToast };
    return sel ? sel(s) : s;
  }),
}));

vi.mock('@/api/uploadApi', () => ({
  uploadFile: mockUploadFile,
}));

vi.mock('@/utils/imageCompress', () => ({
  compressImage: mockCompressImage,
}));

vi.mock('@/api/productApi', () => ({
  productApi: {
    goOnline: mockGoOnline,
  },
}));

HTMLCanvasElement.prototype.toDataURL = vi
  .fn()
  .mockImplementation(() => 'data:image/png;base64,test');

function renderPage() {
  return renderWithProviders(<PublishPage />, {
    initialRoute: '/publish',
  });
}

async function uploadImage() {
  const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
  const file = new File([''], 'test.jpg', { type: 'image/jpeg' });
  Object.defineProperty(fileInput, 'files', { value: [file] });
  fireEvent.change(fileInput);
  await waitFor(() => {
    expect(mockUploadFile).toHaveBeenCalled();
  });
}

beforeEach(() => {
  vi.clearAllMocks();
  HTMLCanvasElement.prototype.toDataURL = vi.fn(() => 'data:image/png;base64,test');
  mockUseCreateProduct.mockReturnValue({
    mutateAsync: vi.fn().mockResolvedValue('product-123'),
    isPending: false,
    isError: false,
    error: null,
  });
  mockUseCategories.mockReturnValue({
    data: [
      { id: '1', name: '电子产品' },
      { id: '2', name: '服装鞋帽' },
    ],
    isLoading: false,
  });
  mockUploadFile.mockResolvedValue({
    data: { url: 'https://example.com/uploaded.jpg' },
  });
  mockCompressImage.mockResolvedValue(new File([''], 'compressed.jpg', { type: 'image/jpeg' }));
  mockGoOnline.mockResolvedValue({});
});

describe('PublishPage', () => {
  it('renders page title and subtitle', () => {
    renderPage();

    expect(screen.getByText('提交资产')).toBeInTheDocument();
    expect(
      screen.getByText('填写信息，让 AI 帮你智能托管发布'),
    ).toBeInTheDocument();
  });

  it('renders all form sections (checking unique text)', () => {
    renderPage();

    const basicInfo = screen.getAllByText('基本信息');
    expect(basicInfo.length).toBeGreaterThanOrEqual(1);
    const detailInfo = screen.getAllByText('详细信息');
    expect(detailInfo.length).toBeGreaterThanOrEqual(1);
    const priceInfo = screen.getAllByText('价格库存');
    expect(priceInfo.length).toBeGreaterThanOrEqual(1);
  });

  it('renders upload trigger when no images', () => {
    renderPage();

    expect(screen.getByText('点击或拖拽上传图片')).toBeInTheDocument();
  });

  it('renders form inputs', () => {
    renderPage();

    expect(screen.getByPlaceholderText('给资产起个吸引人的名字')).toBeInTheDocument();
    expect(screen.getAllByPlaceholderText('0.00').length).toBe(2);
    expect(screen.getByPlaceholderText('如：清水河校区南门')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('微信号 / QQ号')).toBeInTheDocument();
  });

  it('renders category and condition selects', () => {
    renderPage();

    expect(screen.getByText('选择类别')).toBeInTheDocument();
    expect(screen.getByText('选择成色')).toBeInTheDocument();
  });

  it('shows validation errors on submit with empty form', async () => {
    renderPage();

    const user = userEvent.setup();
    const publishBtn = screen.getByText('立即发布');
    await user.click(publishBtn);

    expect(screen.getByText('请输入资产名称')).toBeInTheDocument();
    expect(screen.getByText('请输入价格')).toBeInTheDocument();
    expect(screen.getByText('请选择资产类别')).toBeInTheDocument();
    expect(screen.getByText('请选择新旧程度')).toBeInTheDocument();
    expect(screen.getByText('请至少上传一张图片')).toBeInTheDocument();
  });

  it('clears field error when user starts typing', async () => {
    renderPage();

    const user = userEvent.setup();
    const publishBtn = screen.getByText('立即发布');
    await user.click(publishBtn);

    expect(screen.getByText('请输入资产名称')).toBeInTheDocument();

    const nameInput = screen.getByPlaceholderText('给资产起个吸引人的名字');
    await user.type(nameInput, '测试资产');

    await waitFor(() => {
      expect(screen.queryByText('请输入资产名称')).not.toBeInTheDocument();
    });
  });

  it('shows character count for name field', async () => {
    renderPage();

    const nameInput = screen.getByPlaceholderText('给资产起个吸引人的名字');
    const user = userEvent.setup();
    await user.type(nameInput, '测试资产');

    expect(screen.getByText('4/200')).toBeInTheDocument();
  });

  it('shows discount info when both prices are set', async () => {
    renderPage();

    const user = userEvent.setup();
    const priceInputs = screen.getAllByPlaceholderText('0.00');
    const priceInput = priceInputs[0];
    const originalPriceInput = priceInputs[1];

    await user.type(priceInput, '80');
    await user.type(originalPriceInput, '100');

    expect(screen.getByText(/省 20%/)).toBeInTheDocument();
  });

  it('renders both submit buttons', () => {
    renderPage();

    expect(screen.getByText('保存草稿')).toBeInTheDocument();
    expect(screen.getByText('立即发布')).toBeInTheDocument();
  });

  it('uploads an image and shows it in the grid', async () => {
    renderPage();

    await uploadImage();

    expect(document.querySelectorAll('.image-item-v2').length).toBe(1);
  });

  it('submits form as draft when "保存草稿" is clicked', async () => {
    const mockMutateAsync = vi.fn().mockResolvedValue('product-123');
    mockUseCreateProduct.mockReturnValue({
      mutateAsync: mockMutateAsync,
      isPending: false,
      isError: false,
    });

    renderPage();

    await uploadImage();

    const user = userEvent.setup();
    await user.type(screen.getByPlaceholderText('给资产起个吸引人的名字'), '测试资产');
    await user.type(screen.getAllByPlaceholderText('0.00')[0], '100');
    await user.click(screen.getByRole('combobox', { name: /资产类别/ }));
    await user.click(screen.getByRole('option', { name: '电子产品' }));
    await user.click(screen.getByRole('combobox', { name: /新旧程度/ }));
    await user.click(screen.getByRole('option', { name: '几乎全新' }));

    const draftBtn = screen.getByText('保存草稿');
    await user.click(draftBtn);

    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalled();
    });
  });

  it('submits form and navigates to product detail on publish', async () => {
    const mockMutateAsync = vi.fn().mockResolvedValue('product-123');
    mockUseCreateProduct.mockReturnValue({
      mutateAsync: mockMutateAsync,
      isPending: false,
      isError: false,
    });

    renderPage();

    await uploadImage();

    const user = userEvent.setup();
    await user.type(screen.getByPlaceholderText('给资产起个吸引人的名字'), '测试资产');
    await user.type(screen.getAllByPlaceholderText('0.00')[0], '100');
    await user.click(screen.getByRole('combobox', { name: /资产类别/ }));
    await user.click(screen.getByRole('option', { name: '电子产品' }));
    await user.click(screen.getByRole('combobox', { name: /新旧程度/ }));
    await user.click(screen.getByRole('option', { name: '几乎全新' }));

    const publishBtn = screen.getByText('立即发布');
    await user.click(publishBtn);

    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalled();
      expect(mockGoOnline).toHaveBeenCalledWith('product-123');
      expect(mockNavigate).toHaveBeenCalledWith('/products/product-123');
    });
  });

  it('disables buttons while submitting', () => {
    mockUseCreateProduct.mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: true,
      isError: false,
    });

    renderPage();

    expect(screen.getByText('保存草稿')).toBeDisabled();
    expect(screen.getByText('发布中...')).toBeInTheDocument();
  });

  it('shows error message when submission fails', () => {
    mockUseCreateProduct.mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: false,
      isError: true,
      error: new Error('发布失败'),
    });

    renderPage();

    expect(screen.getByText('发布失败，请稍后重试')).toBeInTheDocument();
  });

  it('renders progress bar and steps', () => {
    renderPage();

    expect(screen.getByText('完成度')).toBeInTheDocument();
    expect(screen.getByText('0%')).toBeInTheDocument();
  });
});
