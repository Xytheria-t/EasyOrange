import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ProfileSetupModal } from './ProfileSetupModal';

// ─── Mocks ──────────────────────────────────────────────────────────────
const {
  mockNavigate,
  mockUpdateProfile,
  mockInvalidateQueries,
  mockAddToast,
  mockOpenOverlayLayer,
  mockCloseOverlayLayer,
  mockHandleError,
} = vi.hoisted(() => ({
  mockNavigate: vi.fn(),
  mockUpdateProfile: vi.fn(),
  mockInvalidateQueries: vi.fn(),
  mockAddToast: vi.fn(),
  mockOpenOverlayLayer: vi.fn(),
  mockCloseOverlayLayer: vi.fn(),
  mockHandleError: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock('@/api/userApi', () => ({
  userApi: { updateProfile: mockUpdateProfile },
}));

vi.mock('@tanstack/react-query', () => ({
  useQueryClient: () => ({ invalidateQueries: mockInvalidateQueries }),
}));

vi.mock('@/store/uiStore', () => ({
  useUIStore: (selector: (s: Record<string, unknown>) => unknown) => {
    const store = { addToast: mockAddToast };
    return selector ? selector(store) : store;
  },
}));

vi.mock('@/store/overlayStore', () => ({
  openOverlayLayer: mockOpenOverlayLayer,
  closeOverlayLayer: mockCloseOverlayLayer,
}));

vi.mock('@/utils/errorHandler', () => ({
  errorHandler: { handle: mockHandleError },
}));

// ─── Default Props ──────────────────────────────────────────────────────
const defaultProps = {
  isOpen: true,
  onClose: vi.fn(),
  username: 'TestUser',
};

// ─── Helpers ────────────────────────────────────────────────────────────
function fillAllFields() {
  fireEvent.change(screen.getByPlaceholderText('请输入您的真实姓名'), {
    target: { value: '张三' },
  });
  fireEvent.change(screen.getByPlaceholderText('请输入您的学号'), {
    target: { value: '2024001' },
  });
  fireEvent.change(screen.getByPlaceholderText('请输入您的邮箱地址'), {
    target: { value: 'test@example.com' },
  });
  fireEvent.change(screen.getByPlaceholderText('请输入您的手机号'), {
    target: { value: '13800138000' },
  });
}

// ─── Tests ──────────────────────────────────────────────────────────────
describe('ProfileSetupModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ── Rendering ────────────────────────────────────────────────────────

  it('returns null when isOpen is false', () => {
    const { container } = render(
      <ProfileSetupModal {...defaultProps} isOpen={false} />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders modal when isOpen is true', () => {
    render(<ProfileSetupModal {...defaultProps} />);
    expect(screen.getByText('完善个人信息')).toBeInTheDocument();
  });

  it('shows username in modal', () => {
    render(<ProfileSetupModal {...defaultProps} username="TestUser" />);
    expect(screen.getByText('TestUser')).toBeInTheDocument();
  });

  it('renders avatar initial from username', () => {
    render(<ProfileSetupModal {...defaultProps} username="Alice" />);
    expect(screen.getByText('A')).toBeInTheDocument();
  });

  it('renders avatar fallback "U" when username is empty', () => {
    render(<ProfileSetupModal {...defaultProps} username="" />);
    expect(screen.getByText('U')).toBeInTheDocument();
  });

  // ── Form Fields ──────────────────────────────────────────────────────

  it('renders all 4 form fields with labels', () => {
    render(<ProfileSetupModal {...defaultProps} />);
    expect(screen.getByText('真实姓名')).toBeInTheDocument();
    expect(screen.getByText('学号')).toBeInTheDocument();
    expect(screen.getByText('邮箱')).toBeInTheDocument();
    expect(screen.getByText('手机号')).toBeInTheDocument();
  });

  it('renders all 4 inputs with placeholders', () => {
    render(<ProfileSetupModal {...defaultProps} />);
    expect(
      screen.getByPlaceholderText('请输入您的真实姓名'),
    ).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText('请输入您的学号'),
    ).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText('请输入您的邮箱地址'),
    ).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText('请输入您的手机号'),
    ).toBeInTheDocument();
  });

  // ── Progress ─────────────────────────────────────────────────────────

  it('shows progress 0% initially', () => {
    render(<ProfileSetupModal {...defaultProps} />);
    expect(screen.getByText('0%')).toBeInTheDocument();
  });

  it('updates progress as fields are filled', () => {
    render(<ProfileSetupModal {...defaultProps} />);

    fireEvent.change(screen.getByPlaceholderText('请输入您的真实姓名'), {
      target: { value: '张三' },
    });
    expect(screen.getByText('25%')).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText('请输入您的学号'), {
      target: { value: '12345' },
    });
    expect(screen.getByText('50%')).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText('请输入您的邮箱地址'), {
      target: { value: 'test@test.com' },
    });
    expect(screen.getByText('75%')).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText('请输入您的手机号'), {
      target: { value: '13800138000' },
    });
    expect(screen.getByText('100%')).toBeInTheDocument();
  });

  // ── Validation ───────────────────────────────────────────────────────

  it('shows required errors when submitting with empty fields', () => {
    render(<ProfileSetupModal {...defaultProps} />);
    fireEvent.click(screen.getByText('完成设置'));

    expect(screen.getByText('真实姓名为必填项')).toBeInTheDocument();
    expect(screen.getByText('学号为必填项')).toBeInTheDocument();
    expect(screen.getByText('邮箱为必填项')).toBeInTheDocument();
    expect(screen.getByText('手机号为必填项')).toBeInTheDocument();
  });

  it('shows validation error for invalid email format', () => {
    render(<ProfileSetupModal {...defaultProps} />);

    const emailInput = screen.getByPlaceholderText('请输入您的邮箱地址');
    fireEvent.change(emailInput, { target: { value: 'invalid-email' } });
    fireEvent.blur(emailInput);

    expect(screen.getByText('请输入有效的邮箱地址')).toBeInTheDocument();
  });

  it('shows validation error for invalid phone format', () => {
    render(<ProfileSetupModal {...defaultProps} />);

    const phoneInput = screen.getByPlaceholderText('请输入您的手机号');
    fireEvent.change(phoneInput, { target: { value: '123' } });
    fireEvent.blur(phoneInput);

    expect(screen.getByText('请输入有效的11位手机号')).toBeInTheDocument();
  });

  it('shows validation error for too-short realName', () => {
    render(<ProfileSetupModal {...defaultProps} />);

    const nameInput = screen.getByPlaceholderText('请输入您的真实姓名');
    fireEvent.change(nameInput, { target: { value: 'a' } });
    fireEvent.blur(nameInput);

    expect(screen.getByText('真实姓名至少需要2个字符')).toBeInTheDocument();
  });

  it('clears field error when user corrects the value', () => {
    render(<ProfileSetupModal {...defaultProps} />);

    const nameInput = screen.getByPlaceholderText('请输入您的真实姓名');
    fireEvent.change(nameInput, { target: { value: 'a' } });
    fireEvent.blur(nameInput);
    expect(screen.getByText('真实姓名至少需要2个字符')).toBeInTheDocument();

    fireEvent.change(nameInput, { target: { value: '张三' } });
    expect(
      screen.queryByText('真实姓名至少需要2个字符'),
    ).not.toBeInTheDocument();
  });

  // ── Submit ───────────────────────────────────────────────────────────

  it('calls updateProfile on submit with correct data', async () => {
    mockUpdateProfile.mockResolvedValue({});
    render(<ProfileSetupModal {...defaultProps} />);
    fillAllFields();

    fireEvent.click(screen.getByText('完成设置'));

    await vi.waitFor(() => {
      expect(mockUpdateProfile).toHaveBeenCalledWith({
        realName: '张三',
        studentId: '2024001',
        email: 'test@example.com',
        phone: '13800138000',
      });
    });
  });

  it('shows warning toast when validation fails on submit', () => {
    render(<ProfileSetupModal {...defaultProps} />);
    fireEvent.click(screen.getByText('完成设置'));

    expect(mockAddToast).toHaveBeenCalledWith({
      type: 'warning',
      message: '请完善所有必填信息',
    });
  });

  it('shows success toast and invalidates queries on successful submit', async () => {
    mockUpdateProfile.mockResolvedValue({});
    const onClose = vi.fn();
    render(<ProfileSetupModal {...defaultProps} onClose={onClose} />);
    fillAllFields();

    fireEvent.click(screen.getByText('完成设置'));

    await vi.waitFor(() => {
      expect(mockAddToast).toHaveBeenCalledWith({
        type: 'success',
        message: '个人信息完善成功！',
      });
    });
    expect(mockInvalidateQueries).toHaveBeenCalledWith({
      queryKey: ['auth', 'user'],
    });
    expect(onClose).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('disables submit button while submitting', async () => {
    // Keep promise pending so isSubmitting stays true
    mockUpdateProfile.mockReturnValue(new Promise(() => {}));
    render(<ProfileSetupModal {...defaultProps} />);
    fillAllFields();

    fireEvent.click(screen.getByText('完成设置'));

    // Button text should change to "保存中..."
    expect(screen.getByText('保存中...')).toBeInTheDocument();

    // Button should be disabled while submitting
    const submitButton = screen.getByText('保存中...').closest('button');
    expect(submitButton).toBeDisabled();
  });

  // ── Close ────────────────────────────────────────────────────────────

  it('calls onClose and navigates to home on "稍后再说" click', () => {
    const onClose = vi.fn();
    render(<ProfileSetupModal {...defaultProps} onClose={onClose} />);
    fireEvent.click(screen.getByText('稍后再说'));

    expect(onClose).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/');
    expect(mockAddToast).toHaveBeenCalledWith({
      type: 'info',
      message: '您可以在个人中心随时完善信息',
    });
  });

  it('calls onClose and navigates on close button click (X)', () => {
    const onClose = vi.fn();
    render(<ProfileSetupModal {...defaultProps} onClose={onClose} />);
    fireEvent.click(screen.getByLabelText('关闭'));

    expect(onClose).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('calls onClose and navigates on overlay backdrop click', () => {
    const onClose = vi.fn();
    render(<ProfileSetupModal {...defaultProps} onClose={onClose} />);
    fireEvent.click(screen.getByLabelText('关闭对话框'));

    expect(onClose).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  // ── Keyboard Events ──────────────────────────────────────────────────

  it('handles Escape key to close modal', () => {
    const onClose = vi.fn();
    render(<ProfileSetupModal {...defaultProps} onClose={onClose} />);
    fireEvent.keyDown(document, { key: 'Escape' });

    expect(onClose).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('handles Enter key to submit form', async () => {
    mockUpdateProfile.mockResolvedValue({});
    render(<ProfileSetupModal {...defaultProps} />);
    fillAllFields();

    fireEvent.keyDown(document, { key: 'Enter' });

    await vi.waitFor(() => {
      expect(mockUpdateProfile).toHaveBeenCalled();
    });
  });

  it('does not submit via Enter when already submitting', async () => {
    // Keep promise pending
    mockUpdateProfile.mockReturnValue(new Promise(() => {}));
    render(<ProfileSetupModal {...defaultProps} />);
    fillAllFields();

    // Trigger submit via button
    fireEvent.click(screen.getByText('完成设置'));
    expect(screen.getByText('保存中...')).toBeInTheDocument();

    // Press Enter while submitting — should not trigger another submit
    mockUpdateProfile.mockClear();
    fireEvent.keyDown(document, { key: 'Enter' });

    // Small delay to let any potential call go through
    await vi.waitFor(() => {
      expect(mockUpdateProfile).not.toHaveBeenCalled();
    });
  });

  // ── Overlay Layer ────────────────────────────────────────────────────

  it('calls openOverlayLayer on mount and closeOverlayLayer on unmount', () => {
    const { unmount } = render(<ProfileSetupModal {...defaultProps} />);
    expect(mockOpenOverlayLayer).toHaveBeenCalledWith('profile-setup-modal');

    unmount();
    expect(mockCloseOverlayLayer).toHaveBeenCalledWith('profile-setup-modal');
  });

  // ── API Error Handling ───────────────────────────────────────────────

  it('handles API error during submit and shows error toast', async () => {
    const apiError = new Error('网络错误');
    mockUpdateProfile.mockRejectedValue(apiError);
    mockHandleError.mockReturnValue('网络错误');
    render(<ProfileSetupModal {...defaultProps} />);
    fillAllFields();

    fireEvent.click(screen.getByText('完成设置'));

    await vi.waitFor(() => {
      expect(mockHandleError).toHaveBeenCalledWith(apiError);
      expect(mockAddToast).toHaveBeenCalledWith({
        type: 'error',
        message: '网络错误',
      });
    });
  });
});
