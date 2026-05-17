import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ChatHeader from './ChatHeader';

describe('ChatHeader', () => {
  it('renders back button with aria-label', () => {
    render(<ChatHeader onBack={() => {}} />);
    expect(screen.getByLabelText('返回')).toBeInTheDocument();
  });

  it('calls onBack when back button clicked', async () => {
    const onBack = vi.fn();
    render(<ChatHeader onBack={onBack} />);
    await userEvent.click(screen.getByLabelText('返回'));
    expect(onBack).toHaveBeenCalledTimes(1);
  });

  it('does not crash when targetUser is null', () => {
    render(<ChatHeader targetUser={null} onBack={() => {}} />);
    expect(screen.getByLabelText('返回')).toBeInTheDocument();
  });

  it('shows user name when targetUser is provided', () => {
    render(<ChatHeader targetUser={{ id: '1', name: 'Alice', avatar: null }} onBack={() => {}} />);
    expect(screen.getByText('Alice')).toBeInTheDocument();
  });

  it('shows avatar initial when no avatar URL', () => {
    render(<ChatHeader targetUser={{ id: '1', name: 'Bob', avatar: null }} onBack={() => {}} />);
    expect(screen.getByText('B')).toBeInTheDocument();
  });

  it('renders img when avatar URL is provided', () => {
    render(
      <ChatHeader
        targetUser={{ id: '1', name: 'Charlie', avatar: '/avatar.png' }}
        onBack={() => {}}
      />,
    );
    const img = screen.getByAltText('Charlie');
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/avatar.png');
  });
});
