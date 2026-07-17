import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { PasswordModal } from './PasswordModal';

describe('PasswordModal', () => {
    const defaultProps = {
        show: true,
        isLoading: false,
        onClose: vi.fn(),
        onSubmit: vi.fn(),
    } as const;

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('does not render when show is false', () => {
        renderWithProviders(<PasswordModal {...defaultProps} show={false} />);
        expect(screen.queryByText('修改密码')).not.toBeInTheDocument();
    });

    it('renders old password, new password and confirm password fields', () => {
        renderWithProviders(<PasswordModal {...defaultProps} />);
        expect(screen.getByPlaceholderText('请输入旧密码')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('至少8位字符')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('再次输入新密码')).toBeInTheDocument();
    });

    it('calls onClose when cancel button clicked', async () => {
        renderWithProviders(<PasswordModal {...defaultProps} />);
        await userEvent.click(screen.getByRole('button', { name: '取消' }));
        expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
    });

    it('calls onSubmit with form data when confirm button clicked', async () => {
        const onSubmit = vi.fn();
        renderWithProviders(<PasswordModal {...defaultProps} onSubmit={onSubmit} />);
        await userEvent.type(screen.getByPlaceholderText('请输入旧密码'), 'oldPass123');
        await userEvent.type(screen.getByPlaceholderText('至少8位字符'), 'newPass123');
        await userEvent.type(screen.getByPlaceholderText('再次输入新密码'), 'newPass123');
        await userEvent.click(screen.getByRole('button', { name: '确认修改' }));
        expect(onSubmit).toHaveBeenCalledTimes(1);
        expect(onSubmit).toHaveBeenCalledWith(
            expect.objectContaining({
                oldPassword: 'oldPass123',
                newPassword: 'newPass123',
                confirmPassword: 'newPass123',
            })
        );
    });

    it('disables submit button when loading', () => {
        renderWithProviders(<PasswordModal {...defaultProps} isLoading />);
        expect(screen.getByRole('button', { name: '修改中...' })).toBeDisabled();
    });

    it('resets form on close', async () => {
        const onClose = vi.fn();
        const { rerender } = renderWithProviders(<PasswordModal {...defaultProps} onClose={onClose} />);
        await userEvent.type(screen.getByPlaceholderText('请输入旧密码'), 'oldPass123');
        expect(screen.getByPlaceholderText('请输入旧密码')).toHaveValue('oldPass123');
        await userEvent.click(screen.getByRole('button', { name: '取消' }));
        rerender(<PasswordModal {...defaultProps} show={false} onClose={onClose} />);
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
