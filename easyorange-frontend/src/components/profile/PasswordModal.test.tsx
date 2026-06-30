import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { PasswordModal } from './PasswordModal';

describe('PasswordModal', () => {
  const defaultProps = {
    show: true,
    form: { verifyCode: '', newPassword: '', confirmPassword: '' },
    isLoading: false,
    countdown: 0,
    phone: '13800138000',
    onFormChange: vi.fn(),
    onClose: vi.fn(),
    onSubmit: vi.fn(),
    onSendCode: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('does not render when show is false', () => {
    renderWithProviders(<PasswordModal {...defaultProps} show={false} />);
    expect(screen.queryByText('修改密码')).not.toBeInTheDocument();
  });

  it('renders phone and form fields', () => {
    renderWithProviders(<PasswordModal {...defaultProps} />);
    expect(screen.getByDisplayValue('13800138000')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('请输入6位验证码')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('需包含大小写字母和数字，6-20位')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('再次输入新密码')).toBeInTheDocument();
  });

  it('calls onFormChange when inputs change', async () => {
    renderWithProviders(<PasswordModal {...defaultProps} />);
    await userEvent.type(screen.getByPlaceholderText('请输入6位验证码'), '123456');
    expect(defaultProps.onFormChange).toHaveBeenCalled();
  });

  it('calls onSendCode when send code button clicked', async () => {
    renderWithProviders(<PasswordModal {...defaultProps} />);
    await userEvent.click(screen.getByRole('button', { name: '发送验证码' }));
    expect(defaultProps.onSendCode).toHaveBeenCalledTimes(1);
  });

  it('shows countdown and disables send code button', () => {
    renderWithProviders(<PasswordModal {...defaultProps} countdown={45} />);
    expect(screen.getByRole('button', { name: '45s' })).toBeDisabled();
  });

  it('calls onClose when cancel button clicked', async () => {
    renderWithProviders(<PasswordModal {...defaultProps} />);
    await userEvent.click(screen.getByRole('button', { name: '取消' }));
    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
  });

  it('calls onSubmit when confirm button clicked', async () => {
    renderWithProviders(<PasswordModal {...defaultProps} />);
    await userEvent.click(screen.getByRole('button', { name: '确认修改' }));
    expect(defaultProps.onSubmit).toHaveBeenCalledTimes(1);
  });

  it('disables submit button when loading', () => {
    renderWithProviders(<PasswordModal {...defaultProps} isLoading />);
    expect(screen.getByRole('button', { name: '修改中...' })).toBeDisabled();
  });
});
