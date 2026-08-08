import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { AdminUser } from '../../types/admin';
import { UserDetailModal } from './UserDetailModal';

const mockUser: AdminUser = {
    userId: '42',
    username: 'alice_wonder',
    nickname: 'Wonder',
    avatar: null,
    email: 'alice@example.com',
    phone: '13800138000',
    studentId: null,
    realName: null,
    userType: '01',
    userTypeDesc: '学生',
    status: 'NORMAL',
    statusDesc: '正常',
    loginIp: null,
    loginDate: null,
    createTime: '2024-01-15T08:30:00',
    updateTime: '2024-01-15T08:30:00',
};

const defaultProps = {
    open: true,
    user: mockUser,
    onClose: vi.fn(),
    onSave: vi.fn(),
    loading: false,
};

describe('UserDetailModal', () => {
    it('renders user details when open', () => {
        render(<UserDetailModal {...defaultProps} />);
        expect(screen.getByText('用户详情')).toBeInTheDocument();
        expect(screen.getByText('@alice_wonder')).toBeInTheDocument();
        expect(screen.getByText('a***e@example.com')).toBeInTheDocument();
        expect(screen.getByText('138****8000')).toBeInTheDocument();
    });

    it('does not render when closed', () => {
        render(<UserDetailModal {...defaultProps} open={false} />);
        expect(screen.queryByText('用户详情')).not.toBeInTheDocument();
    });

    it('does not render when user is null', () => {
        render(<UserDetailModal {...defaultProps} user={null} />);
        expect(screen.queryByText('用户详情')).not.toBeInTheDocument();
    });

    it('calls onClose when close button clicked', () => {
        const onClose = vi.fn();
        render(<UserDetailModal {...defaultProps} onClose={onClose} />);
        fireEvent.click(screen.getByLabelText('关闭'));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('calls onSave with selected status when save clicked', async () => {
        const onSave = vi.fn().mockResolvedValue(undefined);
        render(<UserDetailModal {...defaultProps} onSave={onSave} />);
        fireEvent.click(screen.getByText('禁用'));
        fireEvent.click(screen.getByText('保存修改'));
        await waitFor(() => expect(onSave).toHaveBeenCalledWith('DISABLED'));
    });

    it('disables save button when status unchanged', () => {
        render(<UserDetailModal {...defaultProps} />);
        const saveButton = screen.getByText('保存修改').closest('button');
        expect(saveButton).toBeDisabled();
    });

    it('disables buttons and shows loading state', () => {
        render(<UserDetailModal {...defaultProps} loading={true} />);
        expect(screen.getByText('保存中...')).toBeInTheDocument();
        expect(screen.getByText('取消').closest('button')).toBeDisabled();
    });

    it('renders fallback initial when nickname is missing', () => {
        render(<UserDetailModal {...defaultProps} user={{ ...mockUser, nickname: null }} />);
        expect(screen.getByText('A')).toBeInTheDocument();
    });
});
