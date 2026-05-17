import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ForgotPasswordPage from './ForgotPasswordPage';

const mockUserApiSendSmsCode = vi.hoisted(() => vi.fn());
const mockUserApiForgotPassword = vi.hoisted(() => vi.fn());
const mockErrorHandlerHandle = vi.hoisted(() => vi.fn());
const mockNavigate = vi.hoisted(() => vi.fn());
const mockAddToast = vi.hoisted(() => vi.fn());
const mockUseUIStore = vi.hoisted(() =>
  vi.fn((selector?: (s: { addToast: typeof mockAddToast }) => unknown) => {
    const state = { addToast: mockAddToast };
    return selector ? selector(state) : state;
  }),
);

vi.mock('@/store/uiStore', () => ({
  useUIStore: mockUseUIStore,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...(actual as object), useNavigate: () => mockNavigate };
});

vi.mock('@/api/userApi', () => ({
  userApi: {
    sendSmsCode: mockUserApiSendSmsCode,
    forgotPassword: mockUserApiForgotPassword,
  },
}));

vi.mock('@/utils/errorHandler', () => ({
  errorHandler: { handle: mockErrorHandlerHandle },
}));

function renderPage() {
  return renderWithProviders(<ForgotPasswordPage />, { initialRoute: '/forgot-password' });
}

beforeEach(() => {
  vi.clearAllMocks();
  mockUserApiSendSmsCode.mockResolvedValue(undefined);
  mockUserApiForgotPassword.mockResolvedValue(undefined);
  mockErrorHandlerHandle.mockReturnValue('模拟错误');
  mockAddToast.mockClear();
});

describe('ForgotPasswordPage', () => {
  it('renders step 1 with phone input by default', () => {
    renderPage();
    expect(screen.getByTestId('input-forgot-phone')).toBeInTheDocument();
    expect(screen.getByTestId('btn-send-code')).toBeInTheDocument();
    expect(screen.getByText('发送验证码')).toBeInTheDocument();
  });

  it('shows step indicators', () => {
    renderPage();
    expect(screen.getByText('验证手机')).toBeInTheDocument();
    expect(screen.getByText('输入验证码')).toBeInTheDocument();
    expect(screen.getByText('重置密码')).toBeInTheDocument();
  });

  it('calls sendSmsCode and advances to step 2 on send code click', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-forgot-phone'), '13800138000');
    await user.click(screen.getByTestId('btn-send-code'));
    expect(mockUserApiSendSmsCode).toHaveBeenCalledWith('13800138000');
    expect(mockAddToast).toHaveBeenCalledWith({ type: 'success', message: '验证码已发送' });
    expect(screen.getByTestId('input-verify-code')).toBeInTheDocument();
  });

  it('shows warning toast when phone is empty on send code', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByTestId('btn-send-code'));
    expect(mockUserApiSendSmsCode).not.toHaveBeenCalled();
    expect(mockAddToast).toHaveBeenCalledWith({ type: 'warning', message: '请输入手机号' });
  });

  it('shows error toast when sendSmsCode API fails', async () => {
    mockUserApiSendSmsCode.mockRejectedValue(new Error('发送失败'));
    mockErrorHandlerHandle.mockReturnValue('发送失败，请稍后重试');
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-forgot-phone'), '13800138000');
    await user.click(screen.getByTestId('btn-send-code'));
    expect(mockAddToast).toHaveBeenCalledWith({ type: 'error', message: '发送失败，请稍后重试' });
  });

  it('renders step 2 with verify code input and resend button', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-forgot-phone'), '13800138000');
    await user.click(screen.getByTestId('btn-send-code'));
    expect(screen.getByTestId('input-verify-code')).toBeInTheDocument();
    expect(screen.getByTestId('btn-verify-next')).toBeInTheDocument();
    expect(screen.getByText('下一步')).toBeInTheDocument();
  });

  it('shows warning when verify code is empty and clicking next', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-forgot-phone'), '13800138000');
    await user.click(screen.getByTestId('btn-send-code'));
    await user.click(screen.getByTestId('btn-verify-next'));
    expect(mockAddToast).toHaveBeenCalledWith({ type: 'warning', message: '请输入验证码' });
  });

  it('advances to step 3 after entering verify code', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-forgot-phone'), '13800138000');
    await user.click(screen.getByTestId('btn-send-code'));
    await user.type(screen.getByTestId('input-verify-code'), '123456');
    await user.click(screen.getByTestId('btn-verify-next'));
    expect(screen.getByTestId('input-new-password')).toBeInTheDocument();
    expect(screen.getByTestId('input-confirm-new-password')).toBeInTheDocument();
    expect(screen.getByTestId('btn-reset-password')).toBeInTheDocument();
  });

  it('renders step 3 with password inputs and reset button', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-forgot-phone'), '13800138000');
    await user.click(screen.getByTestId('btn-send-code'));
    await user.type(screen.getByTestId('input-verify-code'), '123456');
    await user.click(screen.getByTestId('btn-verify-next'));
    expect(screen.getByTestId('input-new-password')).toBeInTheDocument();
    expect(screen.getByTestId('input-confirm-new-password')).toBeInTheDocument();
    expect(screen.getByTestId('btn-reset-password')).toBeInTheDocument();
  });

  it('calls forgotPassword API on reset and navigates to login', async () => {
    mockUserApiForgotPassword.mockResolvedValue(undefined);
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-forgot-phone'), '13800138000');
    await user.click(screen.getByTestId('btn-send-code'));
    await user.type(screen.getByTestId('input-verify-code'), '123456');
    await user.click(screen.getByTestId('btn-verify-next'));
    await user.type(screen.getByTestId('input-new-password'), 'NewPass1');
    await user.type(screen.getByTestId('input-confirm-new-password'), 'NewPass1');
    await user.click(screen.getByTestId('btn-reset-password'));
    expect(mockUserApiForgotPassword).toHaveBeenCalledWith({
      phone: '13800138000',
      verifyCode: '123456',
      newPassword: 'NewPass1',
    });
    expect(mockAddToast).toHaveBeenCalledWith({ type: 'success', message: '密码重置成功，请使用新密码登录' });
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('shows warning when new passwords do not match on reset', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-forgot-phone'), '13800138000');
    await user.click(screen.getByTestId('btn-send-code'));
    await user.type(screen.getByTestId('input-verify-code'), '123456');
    await user.click(screen.getByTestId('btn-verify-next'));
    await user.type(screen.getByTestId('input-new-password'), 'NewPass1');
    await user.type(screen.getByTestId('input-confirm-new-password'), 'NewPass2');
    await user.click(screen.getByTestId('btn-reset-password'));
    expect(mockAddToast).toHaveBeenCalledWith({ type: 'warning', message: '两次输入的密码不一致' });
    expect(mockUserApiForgotPassword).not.toHaveBeenCalled();
  });

  it('shows error toast when forgotPassword API fails', async () => {
    mockUserApiForgotPassword.mockRejectedValue(new Error('重置失败'));
    mockErrorHandlerHandle.mockReturnValue('重置失败，请稍后重试');
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-forgot-phone'), '13800138000');
    await user.click(screen.getByTestId('btn-send-code'));
    await user.type(screen.getByTestId('input-verify-code'), '123456');
    await user.click(screen.getByTestId('btn-verify-next'));
    await user.type(screen.getByTestId('input-new-password'), 'NewPass1');
    await user.type(screen.getByTestId('input-confirm-new-password'), 'NewPass1');
    await user.click(screen.getByTestId('btn-reset-password'));
    expect(mockAddToast).toHaveBeenCalledWith({ type: 'error', message: '重置失败，请稍后重试' });
  });

  it('shows warning when new password is empty on reset', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-forgot-phone'), '13800138000');
    await user.click(screen.getByTestId('btn-send-code'));
    await user.type(screen.getByTestId('input-verify-code'), '123456');
    await user.click(screen.getByTestId('btn-verify-next'));
    await user.click(screen.getByTestId('btn-reset-password'));
    expect(mockAddToast).toHaveBeenCalledWith({ type: 'warning', message: '请输入新密码' });
  });

  it('navigates to login on back button click', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByText('返回登录'));
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('navigates to login on close button click', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByLabelText('返回登录'));
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });
});
