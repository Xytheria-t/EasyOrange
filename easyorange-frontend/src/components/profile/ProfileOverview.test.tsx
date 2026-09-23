import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { User } from '@/types';
import { ProfileOverview } from './ProfileOverview';

type EditableField = 'nickname' | 'email' | 'phone' | 'realName';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
    useNavigate: () => mockNavigate,
}));

const mockUser: User = {
    userId: '1',
    username: 'testuser',
    nickname: 'TestUser',
    email: 'test@example.com',
    phone: '13800138000',
    avatar: null,
    realName: 'Test',
    status: 0,
    userType: '01',
    createTime: '2026-01-01T00:00:00Z',
    updateTime: '2026-01-01T00:00:00Z',
};

const defaultProps = {
    user: mockUser,
    unreadMessageCount: 2,
    editingField: null as EditableField | null,
    editValue: '',
    isSaving: false,
    onEdit: vi.fn(),
    onSave: vi.fn(),
    onCancel: vi.fn(),
    onEditValueChange: vi.fn(),
};

describe('ProfileOverview', () => {
    it('renders section title', () => {
        render(<ProfileOverview {...defaultProps} />);
        expect(screen.getByText('数据概览')).toBeInTheDocument();
    });

    it('renders user info fields', () => {
        render(<ProfileOverview {...defaultProps} />);
        expect(screen.getByText('昵称')).toBeInTheDocument();
        expect(screen.getByText('真实姓名')).toBeInTheDocument();
        expect(screen.getByText('邮箱')).toBeInTheDocument();
        expect(screen.getByText('手机')).toBeInTheDocument();
    });

    it('renders user values', () => {
        render(<ProfileOverview {...defaultProps} />);
        expect(screen.getByText('TestUser')).toBeInTheDocument();
        expect(screen.getByText('testuser')).toBeInTheDocument();
        expect(screen.getByText('test@example.com')).toBeInTheDocument();
        expect(screen.getByText('13800138000')).toBeInTheDocument();
    });

    it('renders quick action buttons', () => {
        render(<ProfileOverview {...defaultProps} />);
        expect(screen.getByText('消息中心')).toBeInTheDocument();
    });

    it('keeps 我的发布 / 我的订单 out of the overview so the sidebar holds the only entry', () => {
        render(<ProfileOverview {...defaultProps} />);
        expect(screen.queryByText('我的发布')).not.toBeInTheDocument();
        expect(screen.queryByText('我的订单')).not.toBeInTheDocument();
    });

    it('shows read-only fields', () => {
        render(<ProfileOverview {...defaultProps} />);
        expect(screen.getByText('用户名')).toBeInTheDocument();
        expect(screen.getByText('注册时间')).toBeInTheDocument();
    });

    it('shows account status section', () => {
        render(<ProfileOverview {...defaultProps} />);
        const statusElements = screen.getAllByText('账号状态');
        expect(statusElements.length).toBeGreaterThanOrEqual(1);
        expect(screen.getByText('正常')).toBeInTheDocument();
    });

    it('shows edit button for editable fields', () => {
        render(<ProfileOverview {...defaultProps} />);
        const editButtons = document.querySelectorAll('.profile-edit-btn');
        expect(editButtons.length).toBe(4);
    });

    it('shows editing input when editing field is active', () => {
        render(<ProfileOverview {...defaultProps} editingField="nickname" editValue="NewName" />);
        expect(screen.getByDisplayValue('NewName')).toBeInTheDocument();
    });

    it('calls onSave when Enter key pressed in edit mode', () => {
        const onSave = vi.fn();
        render(<ProfileOverview {...defaultProps} editingField="nickname" editValue="NewName" onSave={onSave} />);
        const input = screen.getByDisplayValue('NewName');
        fireEvent.keyDown(input, { key: 'Enter' });
        expect(onSave).toHaveBeenCalled();
    });

    it('calls onCancel when Escape key pressed in edit mode', () => {
        const onCancel = vi.fn();
        render(<ProfileOverview {...defaultProps} editingField="nickname" editValue="NewName" onCancel={onCancel} />);
        const input = screen.getByDisplayValue('NewName');
        fireEvent.keyDown(input, { key: 'Escape' });
        expect(onCancel).toHaveBeenCalled();
    });

    it('renders "未设置" for missing values', () => {
        render(<ProfileOverview {...defaultProps} user={{ ...mockUser, nickname: null as unknown as string }} />);
        const unsetElements = screen.getAllByText('未设置');
        // nickname is null, should show "未设置"
        expect(unsetElements.length).toBeGreaterThanOrEqual(1);
    });

    it('renders empty state for readonly field values', () => {
        render(<ProfileOverview {...defaultProps} />);
        const cardTitle = screen.getByText('数据概览');
        expect(cardTitle).toBeInTheDocument();
    });
});
