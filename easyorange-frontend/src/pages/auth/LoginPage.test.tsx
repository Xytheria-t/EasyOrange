import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LoginPage from './LoginPage';

const mockUseLogin = vi.hoisted(() => vi.fn());
const mockUseRegister = vi.hoisted(() => vi.fn());
const mockUserApiSendSmsCode = vi.hoisted(() => vi.fn());
const mockErrorHandlerHandle = vi.hoisted(() => vi.fn());
const mockNavigate = vi.hoisted(() => vi.fn());
const mockAddToast = vi.hoisted(() => vi.fn());
const mockUseUIStore = vi.hoisted(() =>
  vi.fn((selector?: (s: { addToast: typeof mockAddToast }) => unknown) => {
    const state = { addToast: mockAddToast };
    return selector ? selector(state) : state;
  }),
);

vi.mock('@/hooks', () => ({
  useLogin: mockUseLogin,
  useRegister: mockUseRegister,
}));

vi.mock('@/store/uiStore', () => ({
  useUIStore: mockUseUIStore,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...(actual as object), useNavigate: () => mockNavigate };
});

vi.mock('@/api/userApi', () => ({
  userApi: { sendSmsCode: mockUserApiSendSmsCode },
}));

vi.mock('@/utils/errorHandler', () => ({
  errorHandler: { handle: mockErrorHandlerHandle },
}));

vi.mock('@/store/overlayStore', () => ({
  openOverlayLayer: vi.fn(),
  closeOverlayLayer: vi.fn(),
}));

const validLoginResponse = {
  token: 'mock-token',
  refreshToken: 'mock-refresh',
  user: { userId: 'u1', username: 'testuser', email: '', phone: null, avatar: null, nickname: 'Test', studentId: null, realName: null, status: 1, userType: '00' as const, createTime: '', updateTime: '' },
};

function renderPage() {
  return renderWithProviders(<LoginPage />, { initialRoute: '/login' });
}

beforeEach(() => {
  vi.clearAllMocks();
  mockUseLogin.mockReturnValue({ mutateAsync: vi.fn().mockResolvedValue(validLoginResponse) });
  mockUseRegister.mockReturnValue({ mutateAsync: vi.fn().mockResolvedValue(1) });
  mockUserApiSendSmsCode.mockResolvedValue(undefined);
  mockErrorHandlerHandle.mockReturnValue('模拟错误');
  mockAddToast.mockClear();
});

describe('LoginPage', () => {
  it('renders login tab by default', () => {
    renderPage();
    expect(screen.getByTestId('tab-login')).toHaveClass('auth-page-tab--active');
    expect(screen.getByTestId('input-account')).toBeInTheDocument();
    expect(screen.getByTestId('input-password')).toBeInTheDocument();
    expect(screen.getByTestId('btn-login-submit')).toBeInTheDocument();
  });

  it('switches to register tab when clicked', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByTestId('tab-register'));
    expect(screen.getByTestId('tab-register')).toHaveClass('auth-page-tab--active');
    expect(screen.getByTestId('input-register-username')).toBeInTheDocument();
    expect(screen.getByTestId('input-register-password')).toBeInTheDocument();
    expect(screen.getByTestId('input-register-confirm-password')).toBeInTheDocument();
    expect(screen.getByTestId('btn-register-submit')).toBeInTheDocument();
  });

  it('switches to SMS login method', async () => {
    renderPage();
    const user = userEvent.setup();
    const smsBtn = screen.getByText('短信登录');
    await user.click(smsBtn);
    expect(screen.getByPlaceholderText('请输入手机号')).toBeInTheDocument();
    expect(screen.getByText('获取验证码')).toBeInTheDocument();
    expect(screen.queryByTestId('input-password')).not.toBeInTheDocument();
  });

  it('calls login mutateAsync on password form submit', async () => {
    const mutateAsync = vi.fn().mockResolvedValue(validLoginResponse);
    mockUseLogin.mockReturnValue({ mutateAsync });
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-account'), 'testuser');
    await user.type(screen.getByTestId('input-password'), 'Password1');
    await user.click(screen.getByTestId('btn-login-submit'));
    expect(mutateAsync).toHaveBeenCalledWith({
      account: 'testuser',
      password: 'Password1',
    });
    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
  });

  it('displays login error message on failed login', async () => {
    const mutateAsync = vi.fn().mockRejectedValue(new Error('登录失败'));
    mockUseLogin.mockReturnValue({ mutateAsync });
    mockErrorHandlerHandle.mockReturnValue('用户名或密码错误');
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-account'), 'testuser');
    await user.type(screen.getByTestId('input-password'), 'Password1');
    await user.click(screen.getByTestId('btn-login-submit'));
    expect(await screen.findByTestId('login-error')).toHaveTextContent('用户名或密码错误');
  });

  it('calls SMS code send and starts countdown', async () => {
    renderPage();
    const user = userEvent.setup();
    const smsBtn = screen.getByText('短信登录');
    await user.click(smsBtn);
    await user.type(screen.getByPlaceholderText('请输入手机号'), '13800138000');
    await user.click(screen.getByText('获取验证码'));
    expect(mockUserApiSendSmsCode).toHaveBeenCalledWith('13800138000');
  });

  it('disables send code button while countdown is active', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByText('短信登录'));
    await user.type(screen.getByPlaceholderText('请输入手机号'), '13800138000');
    const sendBtn = screen.getByText('获取验证码');
    await user.click(sendBtn);
    expect(mockUserApiSendSmsCode).toHaveBeenCalled();
    const btn = await screen.findByRole('button', { name: /s$/i });
    expect(btn).toBeDisabled();
  });

  it('calls register mutateAsync on register form submit', async () => {
    const registerMutateAsync = vi.fn().mockResolvedValue(1);
    const loginMutateAsync = vi.fn().mockResolvedValue(validLoginResponse);
    mockUseRegister.mockReturnValue({ mutateAsync: registerMutateAsync });
    mockUseLogin.mockReturnValue({ mutateAsync: loginMutateAsync });
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByTestId('tab-register'));
    await user.type(screen.getByTestId('input-register-username'), 'newuser');
    await user.type(screen.getByTestId('input-register-password'), 'Password1');
    await user.type(screen.getByTestId('input-register-confirm-password'), 'Password1');
    await user.click(screen.getByRole('checkbox'));
    await user.click(screen.getByTestId('btn-register-submit'));
    expect(registerMutateAsync).toHaveBeenCalledWith({
      username: 'newuser',
      password: 'Password1',
    });
  });

  it('shows error on register form when password mismatch', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByTestId('tab-register'));
    await user.type(screen.getByTestId('input-register-username'), 'newuser');
    await user.type(screen.getByTestId('input-register-password'), 'Password1');
    await user.type(screen.getByTestId('input-register-confirm-password'), 'Password2');
    await user.click(screen.getByText('服务条款'));
    await user.click(screen.getByTestId('btn-register-submit'));
    expect(mockAddToast).toHaveBeenCalledWith({ type: 'error', message: '两次输入的密码不一致' });
  });

  it('shows terms checkbox unchecked on register tab', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByTestId('tab-register'));
    const checkbox = screen.getByRole('checkbox');
    expect(checkbox).not.toBeChecked();
    await user.click(checkbox);
    expect(checkbox).toBeChecked();
  });

  it('navigates to forgot password on link click', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByTestId('link-forgot-password'));
    expect(mockNavigate).toHaveBeenCalledWith('/forgot-password');
  });

  it('navigates to home on close button click', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByLabelText('关闭登录页'));
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('switches to register from login footer link', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByText('立即注册'));
    expect(screen.getByTestId('tab-register')).toHaveClass('auth-page-tab--active');
  });

  it('renders social login buttons', () => {
    renderPage();
    expect(screen.getByLabelText('微信登录')).toBeInTheDocument();
    expect(screen.getByLabelText('QQ 登录')).toBeInTheDocument();
    expect(screen.getByLabelText('微博登录')).toBeInTheDocument();
  });

  it('calls SMS login when method is sms and form submitted', async () => {
    const mutateAsync = vi.fn().mockResolvedValue(validLoginResponse);
    mockUseLogin.mockReturnValue({ mutateAsync });
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByText('短信登录'));
    await user.type(screen.getByPlaceholderText('请输入手机号'), '13800138000');
    const codeInput = screen.getByPlaceholderText('请输入验证码');
    await user.type(codeInput, '123456');
    await user.click(screen.getByTestId('btn-login-submit'));
    expect(mutateAsync).toHaveBeenCalledWith({
      account: '13800138000',
      password: '123456',
      loginMethod: 'sms',
    });
    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
  });

  it('shows loading state on login button while submitting', async () => {
    const mutateAsync = vi.fn().mockReturnValue(new Promise(() => {}));
    mockUseLogin.mockReturnValue({ mutateAsync });
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByTestId('input-account'), 'testuser');
    await user.type(screen.getByTestId('input-password'), 'Password1');
    await user.click(screen.getByTestId('btn-login-submit'));
    expect(screen.getByText('登录中...')).toBeInTheDocument();
  });
});
