import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { ChatMessage } from '@/types/message';
import ChatWindowPage from './ChatWindowPage';

const mockNavigate = vi.hoisted(() => vi.fn());
const mockSendMessage = vi.hoisted(() => vi.fn());
const mockUseChatMessages = vi.hoisted(() =>
    vi.fn(() => ({ messages: [] as ChatMessage[], isLoading: true, loadOlder: vi.fn(), hasMore: false }))
);
const mockRecallMessage = vi.hoisted(() => vi.fn());

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...(actual as object), useNavigate: () => mockNavigate, useParams: () => ({ targetUserId: 'user2' }) };
});

vi.mock('@/stores/chatStore', () => ({
    useChatStore: vi.fn((sel: (s: Record<string, unknown>) => unknown) => {
        const s = { connectionStatus: 'connected', typingUsers: new Set() };
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
    useMessageRecall: vi.fn(() => ({ canRecall: () => false, recallMessage: mockRecallMessage })),
}));

vi.mock('@/api/messageApi', () => ({
    messageApi: { markAsRead: vi.fn().mockResolvedValue({}) },
}));

vi.mock('@/components/chat', () => ({
    ChatHeader: () => <div data-testid="chat-header" />,
    MessageList: ({ messages }: { messages: ChatMessage[] }) => (
        <div data-testid="message-list">
            {messages.map((m: ChatMessage) => (
                <div key={m.id} data-testid="message-item">
                    {m.content}
                </div>
            ))}
        </div>
    ),
    ChatInputBar: ({ onSend }: { onSend: (content: string) => void }) => (
        <div data-testid="chat-input-bar">
            <button onClick={() => onSend?.('hello')}>send-btn</button>
        </div>
    ),
}));

function renderPage() {
    return renderWithProviders(<ChatWindowPage />, { initialRoute: '/messages/user2' });
}

beforeEach(() => {
    vi.clearAllMocks();
});

describe('ChatWindowPage', () => {
    it('renders loading state', () => {
        renderPage();
        expect(screen.getByText('加载消息中...')).toBeInTheDocument();
        expect(screen.getByTestId('chat-header')).toBeInTheDocument();
        expect(screen.getByTestId('chat-input-bar')).toBeInTheDocument();
    });

    it('renders messages when loaded', () => {
        mockUseChatMessages.mockReturnValue({
            messages: [
                {
                    id: 'msg1',
                    senderId: 'user2',
                    receiverId: 'user1',
                    content: '你好',
                    type: 'TEXT' as const,
                    status: 'SENT' as const,
                    createTime: '2026-05-15T10:00:00Z',
                    readTime: null,
                    recalledAt: null,
                },
            ],
            isLoading: false,
            loadOlder: vi.fn(),
            hasMore: false,
        });
        renderPage();
        expect(screen.getByTestId('message-list')).toBeInTheDocument();
        expect(screen.getByText('你好')).toBeInTheDocument();
    });

    it('sends a message via ChatInputBar', async () => {
        mockUseChatMessages.mockReturnValue({
            messages: [] as ChatMessage[],
            isLoading: false,
            loadOlder: vi.fn(),
            hasMore: false,
        });
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('send-btn'));
        expect(mockSendMessage).toHaveBeenCalledWith(
            expect.objectContaining({ content: 'hello', receiverId: 'user2', conversationId: 'conv_user1_user2' })
        );
    });
});
