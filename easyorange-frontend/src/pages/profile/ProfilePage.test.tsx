import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { User } from '@/types';
import ProfilePage from './ProfilePage';

const mockUseCurrentUser = vi.hoisted(() => vi.fn());
const mockUseLogout = vi.hoisted(() => vi.fn());
const mockUserApiUpdateProfile = vi.hoisted(() => vi.fn());
const mockUserApiSendSmsCode = vi.hoisted(() => vi.fn());
const mockUserApiChangePassword = vi.hoisted(() => vi.fn());
const mockUserApiUploadAvatar = vi.hoisted(() => vi.fn());
const mockFavoriteApiGetCount = vi.hoisted(() => vi.fn());
const mockErrorHandlerHandle = vi.hoisted(() => vi.fn());
const mockNavigate = vi.hoisted(() => vi.fn());
const mockAddToast = vi.hoisted(() => vi.fn());
const mockUseUIStore = vi.hoisted(() =>
    vi.fn((selector?: (s: { addToast: typeof mockAddToast }) => unknown) => {
        const state = { addToast: mockAddToast };
        return selector ? selector(state) : state;
    })
);

vi.mock('@/hooks', () => ({
    useCurrentUser: mockUseCurrentUser,
    useLogout: mockUseLogout,
}));

vi.mock('@/store/uiStore', () => ({
    useUIStore: mockUseUIStore,
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...(actual as object), useNavigate: () => mockNavigate };
});

vi.mock('@/api/userApi', () => ({
    userApi: {
        updateProfile: mockUserApiUpdateProfile,
        sendSmsCode: mockUserApiSendSmsCode,
        changePassword: mockUserApiChangePassword,
        uploadAvatar: mockUserApiUploadAvatar,
    },
}));

vi.mock('@/api/favoriteApi', () => ({
    favoriteApi: { getCount: mockFavoriteApiGetCount },
}));

vi.mock('@/utils/errorHandler', () => ({
    errorHandler: { handle: mockErrorHandlerHandle },
}));

const mockUser: User = {
    userId: 'u1',
    username: 'testuser',
    nickname: 'TestNick',
    email: 'test@example.com',
    phone: '13800138000',
    studentId: '2024001',
    realName: '张三',
    avatar: null,
    status: 1,
    userType: '00',
    createTime: '2026-01-01T00:00:00Z',
    updateTime: '2026-01-01T00:00:00Z',
};

function renderPage() {
    return renderWithProviders(<ProfilePage />, { initialRoute: '/profile' });
}

beforeEach(() => {
    vi.clearAllMocks();
    mockUseCurrentUser.mockReturnValue({ data: mockUser, isLoading: false });
    mockUseLogout.mockReturnValue({ mutateAsync: vi.fn().mockResolvedValue(undefined) });
    mockUserApiUpdateProfile.mockResolvedValue({ data: mockUser });
    mockUserApiSendSmsCode.mockResolvedValue(undefined);
    mockUserApiChangePassword.mockResolvedValue(undefined);
    mockUserApiUploadAvatar.mockResolvedValue({ data: mockUser });
    mockFavoriteApiGetCount.mockResolvedValue({ data: 42 });
    mockErrorHandlerHandle.mockReturnValue('模拟错误');
    mockAddToast.mockClear();
});

describe('ProfilePage', () => {
    it('renders loading state', () => {
        mockUseCurrentUser.mockReturnValue({ data: undefined, isLoading: true });
        renderPage();
        expect(screen.getByText('加载中...')).toBeInTheDocument();
    });

    it('renders profile overview tab by default', () => {
        renderPage();
        expect(screen.getByText('数据概览')).toBeInTheDocument();
        expect(screen.getByText('个人信息')).toBeInTheDocument();
        expect(screen.getByText('testuser')).toBeInTheDocument();
    });

    it('renders sidebar with user info', () => {
        renderPage();
        const sidebar = screen.getByTestId('profile-sidebar');
        expect(sidebar).toBeInTheDocument();
        expect(sidebar.textContent).toContain('TestNick');
        expect(sidebar.textContent).toContain('@testuser');
    });

    it('renders favorite count', async () => {
        renderPage();
        expect(await screen.findByText('42')).toBeInTheDocument();
    });

    it('switches to activity tab', async () => {
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('动态'));
        expect(screen.getByText('最近动态')).toBeInTheDocument();
    });

    it('switches to security tab', async () => {
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('安全'));
        expect(screen.getByText('安全中心')).toBeInTheDocument();
    });

    it('switches to preferences tab', async () => {
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('偏好'));
        expect(screen.getByText('偏好设置')).toBeInTheDocument();
    });

    it('handles inline editing of nickname', async () => {
        renderPage();
        const user = userEvent.setup();
        const pencilBtns = document.querySelectorAll('.profile-edit-btn');
        expect(pencilBtns.length).toBeGreaterThan(0);
        await user.click(pencilBtns[0] as HTMLElement);
        const input = screen.getByRole('textbox');
        await user.clear(input);
        await user.type(input, 'NewNick');
        const saveBtn = document.querySelector('.profile-action-save') as HTMLElement;
        expect(saveBtn).not.toBeNull();
        await user.click(saveBtn);
        expect(mockUserApiUpdateProfile).toHaveBeenCalledWith({ nickname: 'NewNick' });
    });

    it('cancels inline editing', async () => {
        renderPage();
        const user = userEvent.setup();
        const pencilBtns = document.querySelectorAll('.profile-edit-btn');
        await user.click(pencilBtns[0] as HTMLElement);
        const input = screen.getByRole('textbox');
        expect(input).toBeInTheDocument();
        const cancelBtn = document.querySelector('.profile-action-cancel') as HTMLElement;
        expect(cancelBtn).not.toBeNull();
        await user.click(cancelBtn);
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    });

    it('shows password modal and handles password change', async () => {
        const mockLogout = vi.fn().mockResolvedValue(undefined);
        mockUseLogout.mockReturnValue(mockLogout);
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('安全'));

        const changePwdBtns = screen.getAllByText('修改密码');
        expect(changePwdBtns.length).toBeGreaterThanOrEqual(1);
        fireEvent.click(changePwdBtns[0]);

        await screen.findByPlaceholderText('请输入6位验证码');
        const verifyCodeInput = document.getElementById('change-verify-code') as HTMLElement;
        const newPwdInput = document.getElementById('new-password') as HTMLElement;
        const confirmPwdInput = document.getElementById('confirm-password') as HTMLElement;

        fireEvent.change(verifyCodeInput, { target: { value: '123456' } });
        fireEvent.change(newPwdInput, { target: { value: 'NewPass1' } });
        fireEvent.change(confirmPwdInput, { target: { value: 'NewPass1' } });

        fireEvent.click(screen.getByText('确认修改'));

        await waitFor(() => {
            expect(mockUserApiChangePassword).toHaveBeenCalledWith({ verifyCode: '123456', newPassword: 'NewPass1' });
        });
        expect(mockAddToast).toHaveBeenCalledWith({ type: 'success', message: '密码修改成功，请重新登录' });
        expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    it('shows error when password change API fails', async () => {
        mockUserApiChangePassword.mockRejectedValue(new Error('修改失败'));
        mockErrorHandlerHandle.mockReturnValue('验证码错误');
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('安全'));
        const changePwdBtns = screen.getAllByText('修改密码');
        expect(changePwdBtns.length).toBeGreaterThanOrEqual(1);
        fireEvent.click(changePwdBtns[0]);

        await screen.findByPlaceholderText('请输入6位验证码');
        const verifyCodeInput = document.getElementById('change-verify-code') as HTMLElement;
        const newPwdInput = document.getElementById('new-password') as HTMLElement;
        const confirmPwdInput = document.getElementById('confirm-password') as HTMLElement;

        fireEvent.change(verifyCodeInput, { target: { value: '000000' } });
        fireEvent.change(newPwdInput, { target: { value: 'NewPass1' } });
        fireEvent.change(confirmPwdInput, { target: { value: 'NewPass1' } });

        fireEvent.click(screen.getByText('确认修改'));

        await waitFor(() => {
            expect(mockAddToast).toHaveBeenCalledWith({ type: 'error', message: '验证码错误' });
        });
    });

    it('shows warning when confirm password does not match', async () => {
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('安全'));
        const changePwdBtns = screen.getAllByText('修改密码');
        expect(changePwdBtns.length).toBeGreaterThanOrEqual(1);
        fireEvent.click(changePwdBtns[0]);

        await screen.findByPlaceholderText('请输入6位验证码');
        const verifyCodeInput = document.getElementById('change-verify-code') as HTMLElement;
        const newPwdInput = document.getElementById('new-password') as HTMLElement;
        const confirmPwdInput = document.getElementById('confirm-password') as HTMLElement;

        fireEvent.change(verifyCodeInput, { target: { value: '123456' } });
        fireEvent.change(newPwdInput, { target: { value: 'NewPass1' } });
        fireEvent.change(confirmPwdInput, { target: { value: 'NewPass2' } });

        fireEvent.click(screen.getByText('确认修改'));

        await waitFor(() => {
            expect(mockAddToast).toHaveBeenCalledWith({ type: 'warning', message: '两次输入的新密码不一致' });
        });
        expect(mockUserApiChangePassword).not.toHaveBeenCalled();
    });

    it('handles password modal cancel', async () => {
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('安全'));
        const changePwdBtns = screen.getAllByText('修改密码');
        expect(changePwdBtns.length).toBeGreaterThanOrEqual(1);
        fireEvent.click(changePwdBtns[0]);

        await waitFor(() => {
            expect(screen.getByText('取消')).toBeInTheDocument();
        });
        fireEvent.click(screen.getByText('取消'));
        await waitFor(() => {
            expect(screen.queryByText('确认修改')).not.toBeInTheDocument();
        });
    });

    it('handles logout', async () => {
        const mockLogout = vi.fn().mockResolvedValue(undefined);
        mockUseLogout.mockReturnValue(mockLogout);
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByTestId('btn-profile-logout'));
        expect(mockLogout).toHaveBeenCalled();
        expect(mockNavigate).toHaveBeenCalledWith('/');
    });
});
