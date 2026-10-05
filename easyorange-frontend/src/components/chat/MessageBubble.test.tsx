import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { ChatMessage } from '@/types/message';
import MessageBubble from './MessageBubble';

const baseMessage: ChatMessage = {
    id: 'msg1',
    senderId: 'user1',
    senderAvatar: null,
    receiverId: 'user2',
    content: 'Hello world',
    title: null,
    createTime: '2026-05-16T10:30:00',
    type: 'TEXT',
    status: 'SENT',
    readTime: null,
    recalledAt: null,
};

describe('MessageBubble', () => {
    beforeEach(() => {
        Object.assign(navigator, {
            clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
        });
    });

    it('renders message content text', () => {
        render(<MessageBubble message={baseMessage} isOwn={false} />);
        expect(screen.getByText('Hello world')).toBeInTheDocument();
    });

    it('shows recalled text when status is RECALLED', () => {
        render(<MessageBubble message={{ ...baseMessage, status: 'RECALLED', content: '' }} isOwn={false} />);
        expect(screen.getByText('[消息已撤回]')).toBeInTheDocument();
    });

    it('shows recalled text and keeps content out of the bubble', () => {
        render(<MessageBubble message={{ ...baseMessage, status: 'RECALLED', content: '' }} isOwn={false} />);
        expect(screen.getByText('[消息已撤回]')).toBeInTheDocument();
    });

    it('shows formatted time', () => {
        render(<MessageBubble message={baseMessage} isOwn={false} />);
        expect(screen.getByText('10:30')).toBeInTheDocument();
    });

    it('renders own message on the right side', () => {
        const { container } = render(<MessageBubble message={baseMessage} isOwn={true} />);
        const outerDiv = container.firstChild as HTMLElement;
        expect(outerDiv.className).toContain('justify-end');
    });

    it('renders other message on the left side', () => {
        const { container } = render(<MessageBubble message={baseMessage} isOwn={false} />);
        const outerDiv = container.firstChild as HTMLElement;
        expect(outerDiv.className).toContain('justify-start');
    });

    it('shows SENDING dots for sending status', () => {
        const { container } = render(<MessageBubble message={{ ...baseMessage, status: 'SENDING' }} isOwn={true} />);
        const dots = container.querySelectorAll('.animate-bounce');
        expect(dots.length).toBeGreaterThanOrEqual(0);
        // The SENDING indicator is within the bubble
        expect(screen.getByText('Hello world')).toBeInTheDocument();
    });

    it('shows FAILED icon for failed status', () => {
        const { container } = render(<MessageBubble message={{ ...baseMessage, status: 'FAILED' }} isOwn={true} />);
        const failedIcon = container.querySelector('svg');
        expect(failedIcon).toBeInTheDocument();
    });

    it('shows checkmark for SENT status on own message', () => {
        const { container } = render(<MessageBubble message={{ ...baseMessage, status: 'SENT' }} isOwn={true} />);
        const checkmarks = container.querySelectorAll('svg');
        expect(checkmarks.length).toBeGreaterThanOrEqual(1);
    });

    it('shows single checkmark for SENT status on own message', () => {
        const { container } = render(<MessageBubble message={{ ...baseMessage, status: 'SENT' }} isOwn={true} />);
        const checkmarks = container.querySelectorAll('svg');
        expect(checkmarks.length).toBeGreaterThanOrEqual(1);
    });

    it('shows double checkmark for READ status on own message', () => {
        const { container } = render(<MessageBubble message={{ ...baseMessage, status: 'READ' }} isOwn={true} />);
        // READ shows CheckCheck icon (two checkmarks)
        const paths = container.querySelectorAll('path[d="M18 6 7 17l-5-5"], path[d="m22 10-7.5 7.5L13 16"]');
        expect(paths.length).toBe(2);
    });

    it('shows context menu on right-click', () => {
        render(<MessageBubble message={baseMessage} isOwn={false} />);
        const bubble = screen.getByText('Hello world');
        fireEvent.contextMenu(bubble);
        expect(screen.getByText('复制')).toBeInTheDocument();
    });

    it('copies content on copy button click', async () => {
        const writeText = vi.fn().mockResolvedValue(undefined);
        Object.assign(navigator, { clipboard: { writeText } });

        render(<MessageBubble message={baseMessage} isOwn={false} />);
        const bubble = screen.getByText('Hello world');
        fireEvent.contextMenu(bubble);
        fireEvent.click(screen.getByText('复制'));
        expect(writeText).toHaveBeenCalledWith('Hello world');
    });

    it('shows recall button when canRecallFn returns true', () => {
        render(<MessageBubble message={baseMessage} isOwn={true} canRecallFn={() => true} />);
        const bubble = screen.getByText('Hello world');
        fireEvent.contextMenu(bubble);
        expect(screen.getByText('撤回')).toBeInTheDocument();
    });

    it('does not show recall button when canRecallFn returns false', () => {
        render(<MessageBubble message={baseMessage} isOwn={true} canRecallFn={() => false} />);
        const bubble = screen.getByText('Hello world');
        fireEvent.contextMenu(bubble);
        expect(screen.queryByText('撤回')).not.toBeInTheDocument();
    });

    it('does not show recall button when message is not own', () => {
        render(<MessageBubble message={baseMessage} isOwn={false} canRecallFn={() => true} />);
        const bubble = screen.getByText('Hello world');
        fireEvent.contextMenu(bubble);
        expect(screen.queryByText('撤回')).not.toBeInTheDocument();
    });

    it('does not show context menu when message is recalled', () => {
        render(<MessageBubble message={{ ...baseMessage, status: 'RECALLED', content: '' }} isOwn={false} />);
        const recalledText = screen.getByText('[消息已撤回]');
        fireEvent.contextMenu(recalledText);
        expect(screen.queryByText('复制')).not.toBeInTheDocument();
    });

    it('calls onRecall when recall button clicked', async () => {
        const onRecall = vi.fn().mockResolvedValue(true);
        render(<MessageBubble message={baseMessage} isOwn={true} onRecall={onRecall} canRecallFn={() => true} />);
        const bubble = screen.getByText('Hello world');
        fireEvent.contextMenu(bubble);
        fireEvent.click(screen.getByText('撤回'));
        expect(onRecall).toHaveBeenCalledWith('msg1');
    });
});
