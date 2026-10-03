import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { ChatMessage } from '@/types/message';
import ChatThread from './ChatThread';

const mockSendMessage = vi.hoisted(() => vi.fn());
const mockUseChatMessages = vi.hoisted(() =>
    vi.fn(() => ({ messages: [] as ChatMessage[], isLoading: true, loadOlder: vi.fn(), hasMore: false }))
);

vi.mock('@/store/chatStore', () => ({
    useChatStore: vi.fn((sel: (s: Record<string, unknown>) => unknown) => {
        const s = { connectionStatus: 'connected', typingUsers: new Set<string>() };
        return sel ? sel(s) : s;
    }),
}));

vi.mock('@/store/authStore', () => ({
    useAuthStore: vi.fn(() => ({ user: { userId: 'user1' }, token: 'token', isAuthenticated: true })),
}));

vi.mock('@/hooks/chat', () => ({
    useStompChat: vi.fn(() => ({
        sendMessage: mockSendMessage,
        sendTyping: vi.fn(),
        subscribe: vi.fn(),
        unsubscribe: vi.fn(),
    })),
    useChatMessages: mockUseChatMessages,
    useMessageRecall: vi.fn(() => ({ canRecall: () => false, recallMessage: vi.fn() })),
}));

vi.mock('@/api/messageApi', () => ({
    messageApi: { markAsRead: vi.fn().mockResolvedValue({}) },
}));

vi.mock('./ChatHeader', () => ({
    default: ({ targetUser, isTyping }: { targetUser?: { name: string } | null; isTyping?: boolean }) => (
        <div data-testid="chat-header" data-typing={String(isTyping ?? false)}>
            {targetUser?.name}
        </div>
    ),
}));

vi.mock('./MessageList', () => ({
    default: ({ messages }: { messages: ChatMessage[] }) => (
        <div data-testid="message-list">
            {messages.map((m: ChatMessage) => (
                <div key={m.id} data-testid="message-item">
                    {m.content}
                </div>
            ))}
        </div>
    ),
}));

vi.mock('./ChatInputBar', () => ({
    default: ({
        onSend,
        isDisabled,
        disabledPlaceholder,
    }: {
        onSend: (content: string) => void;
        isDisabled?: boolean;
        disabledPlaceholder?: string;
    }) => (
        <div data-testid="chat-input-bar" data-disabled={String(isDisabled ?? false)}>
            {disabledPlaceholder && <span data-testid="disabled-placeholder">{disabledPlaceholder}</span>}
            <button type="button" onClick={() => onSend?.('hello')}>
                send-btn
            </button>
        </div>
    ),
}));

function renderThread(id = 'user2', name = '卖家小橙') {
    return renderWithProviders(<ChatThread counterpart={{ id, name, avatar: null }} />);
}

beforeEach(() => {
    vi.clearAllMocks();
    mockUseChatMessages.mockReturnValue({
        messages: [] as ChatMessage[],
        isLoading: true,
        loadOlder: vi.fn(),
        hasMore: false,
    });
});

describe('ChatThread', () => {
    it('renders loading state with header and input', () => {
        renderThread();
        expect(screen.getByText('加载消息中...')).toBeInTheDocument();
        expect(screen.getByTestId('chat-header')).toBeInTheDocument();
        expect(screen.getByTestId('chat-input-bar')).toBeInTheDocument();
    });

    it('shows the counterpart name carried by the conversation list', () => {
        renderThread();
        expect(screen.getByTestId('chat-header')).toHaveTextContent('卖家小橙');
    });

    it('renders messages when loaded', () => {
        mockUseChatMessages.mockReturnValue({
            messages: [
                {
                    id: 'msg1',
                    senderId: 'user2',
                    receiverId: 'user1',
                    senderAvatar: null,
                    content: '你好',
                    type: 'TEXT' as const,
                    status: 'SENT' as const,
                    createTime: '2026-05-15T10:00:00',
                    readTime: null,
                    recalledAt: null,
                },
            ],
            isLoading: false,
            loadOlder: vi.fn(),
            hasMore: false,
        });
        renderThread();
        expect(screen.getByText('你好')).toBeInTheDocument();
    });

    it('sends a message via ChatInputBar', async () => {
        mockUseChatMessages.mockReturnValue({
            messages: [] as ChatMessage[],
            isLoading: false,
            loadOlder: vi.fn(),
            hasMore: false,
        });
        renderThread();
        await userEvent.click(screen.getByText('send-btn'));
        expect(mockSendMessage).toHaveBeenCalledWith(
            expect.objectContaining({ content: 'hello', receiverId: 'user2', conversationId: 'conv_user1_user2' })
        );
    });

    it('system conversation is read-only: input disabled with placeholder, send blocked', async () => {
        mockUseChatMessages.mockReturnValue({
            messages: [] as ChatMessage[],
            isLoading: false,
            loadOlder: vi.fn(),
            hasMore: false,
        });
        renderThread('system', '系统通知');

        expect(screen.getByTestId('chat-input-bar')).toHaveAttribute('data-disabled', 'true');
        expect(screen.getByTestId('disabled-placeholder')).toHaveTextContent('系统通知不支持回复');

        // 即使绕过组件禁用触发 onSend，组件守卫也不应发出 WS 帧（发了也不落库，静默失败）
        await userEvent.click(screen.getByText('send-btn'));
        expect(mockSendMessage).not.toHaveBeenCalled();
    });
});
