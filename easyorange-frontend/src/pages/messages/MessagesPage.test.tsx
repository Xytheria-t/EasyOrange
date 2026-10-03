import { screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import MessagesPage from './MessagesPage';

const mockGetConversations = vi.hoisted(() => vi.fn());
const mockTargetUserId = vi.hoisted(() => ({ value: undefined as string | undefined }));
const mockLocationState = vi.hoisted(() => ({ value: null as { counterpartName?: string } | null }));

vi.mock('@/api/messageApi', () => ({
    messageApi: { getConversations: mockGetConversations },
}));

// ChatThread 会连 WebSocket、拉消息历史，这里只关心「选中会话时右栏挂上了没有」
vi.mock('@/components/chat', () => ({
    ChatThread: ({ counterpart }: { counterpart: { id: string; name: string } }) => (
        <div data-testid="chat-thread" data-name={counterpart.name}>
            thread
        </div>
    ),
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...(actual as object),
        useParams: () => ({ targetUserId: mockTargetUserId.value }),
        useLocation: () => ({ state: mockLocationState.value, pathname: '/messages' }),
    };
});

function createMockConversations(count = 2) {
    return Array.from({ length: count }, (_, i) => ({
        targetUserId: `user${i}`,
        targetUserName: `用户${i}`,
        targetUserAvatar: i === 0 ? 'https://example.com/avatar.jpg' : null,
        lastMessage: `最后消息${i}`,
        lastMessageTime: new Date().toISOString(),
        unreadCount: i === 0 ? 3 : 0,
    }));
}

function renderPage(route = '/messages') {
    return renderWithProviders(<MessagesPage />, { initialRoute: route });
}

beforeEach(() => {
    vi.clearAllMocks();
    mockTargetUserId.value = undefined;
    mockLocationState.value = null;
});

describe('MessagesPage', () => {
    it('renders loading state', () => {
        mockGetConversations.mockReturnValue(new Promise(() => {}));
        renderPage();
        expect(screen.getByText('正在加载会话')).toBeInTheDocument();
    });

    it('renders error state with retry, not an empty list', async () => {
        mockGetConversations.mockRejectedValue(new Error('fail'));
        renderPage();
        expect(await screen.findByText('会话加载失败')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '重新加载' })).toBeInTheDocument();
    });

    it('renders empty state with a next action instead of a blank panel', async () => {
        mockGetConversations.mockResolvedValue({ data: [] });
        renderPage();
        expect(await screen.findByText('暂无会话')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: '去逛逛商品' })).toBeInTheDocument();
    });

    it('renders conversation list', async () => {
        mockGetConversations.mockResolvedValue({ data: createMockConversations() });
        renderPage();
        expect(await screen.findByText('用户0')).toBeInTheDocument();
        expect(screen.getByText('用户1')).toBeInTheDocument();
        expect(screen.getByText('最后消息0')).toBeInTheDocument();
        expect(screen.getByText('最后消息1')).toBeInTheDocument();
    });

    it('shows unread badge and announces the unread count on the row', async () => {
        mockGetConversations.mockResolvedValue({ data: createMockConversations() });
        renderPage();
        const row = await screen.findByLabelText('用户0，3 条未读');
        expect(row).toHaveTextContent('3');
    });

    it('summarises total unread in the page subtitle', async () => {
        mockGetConversations.mockResolvedValue({ data: createMockConversations() });
        renderPage();
        expect(await screen.findByText('2 个会话 · 3 条未读')).toBeInTheDocument();
    });

    it('shows fallback avatar initial', async () => {
        mockGetConversations.mockResolvedValue({ data: createMockConversations() });
        renderPage();
        expect(await screen.findByText('用')).toBeInTheDocument();
    });

    it('links each row to its conversation deep link', async () => {
        mockGetConversations.mockResolvedValue({ data: createMockConversations() });
        renderPage();
        expect(await screen.findByText('用户0')).toBeInTheDocument();
        const links = screen.getAllByRole('link');
        expect(links[0]).toHaveAttribute('href', '/messages/user0');
    });

    it('keeps the system conversation and marks it read-only', async () => {
        // 后端把 senderId 为 null 的消息（系统/订单/支付各类通知）都归并到这一会话，
        // 通知页只列 type=1 —— 从列表里滤掉它，订单类通知就在界面上无处可见了
        mockGetConversations.mockResolvedValue({
            data: [
                ...createMockConversations(1),
                {
                    targetUserId: 'system',
                    targetUserName: '系统通知',
                    targetUserAvatar: null,
                    lastMessage: '您的商品已通过审核',
                    lastMessageTime: new Date().toISOString(),
                    unreadCount: 1,
                },
            ],
        });
        renderPage();
        const row = await screen.findByLabelText('系统通知，1 条未读');
        expect(row).toHaveAttribute('href', '/messages/system');
        expect(row).toHaveTextContent('只读');
    });

    it('shows the placeholder thread panel when no conversation is selected', async () => {
        mockGetConversations.mockResolvedValue({ data: createMockConversations() });
        renderPage();
        expect(await screen.findByText('选择会话')).toBeInTheDocument();
        expect(screen.queryByTestId('chat-thread')).not.toBeInTheDocument();
    });

    it('renders the thread for the selected conversation and marks the row current', async () => {
        mockGetConversations.mockResolvedValue({ data: createMockConversations() });
        mockTargetUserId.value = 'user0';
        renderPage('/messages/user0');

        // 会话名随列表加载后才补上，右栏先以兜底名渲染
        await waitFor(() => expect(screen.getByTestId('chat-thread')).toHaveAttribute('data-name', '用户0'));
        expect(screen.getByLabelText('用户0，3 条未读')).toHaveAttribute('aria-current', 'page');
    });

    it('uses the name carried by navigation state for a brand-new conversation', async () => {
        mockGetConversations.mockResolvedValue({ data: createMockConversations() });
        mockTargetUserId.value = 'user99';
        // 从商品页首次联系卖家：会话列表里还没有这一行，名字靠导航状态带过来
        mockLocationState.value = { counterpartName: '卖家小橙' };
        renderPage('/messages/user99');

        expect(await screen.findByTestId('chat-thread')).toHaveAttribute('data-name', '卖家小橙');
    });

    it('does not show a raw user id as the thread title', async () => {
        mockGetConversations.mockResolvedValue({ data: createMockConversations() });
        mockTargetUserId.value = 'user99';
        renderPage('/messages/user99');

        expect(await screen.findByTestId('chat-thread')).toHaveAttribute('data-name', '私聊');
    });
});
