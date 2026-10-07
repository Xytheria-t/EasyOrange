import { create } from 'zustand';
import type { ChatMessage } from '@/types/message';

export type ConnectionStatus = 'connected' | 'connecting' | 'disconnected' | 'reconnecting';

interface TargetUser {
    id: string;
    name: string;
    avatar: string | null;
}

interface ChatState {
    activeConversationId: string | null;
    activeTargetUser: TargetUser | null;
    messages: Record<string, ChatMessage[]>;
    typingUsers: Set<string>;
    connectionStatus: ConnectionStatus;

    setActiveConversation: (convId: string | null, user?: TargetUser | null) => void;
    addMessage: (convId: string, message: ChatMessage) => void;
    updateMessage: (convId: string, msgId: string, patch: Partial<ChatMessage>) => void;
    setMessages: (convId: string, messages: ChatMessage[]) => void;
    prependMessages: (convId: string, messages: ChatMessage[]) => void;
    setTyping: (userId: string, isTyping: boolean) => void;
    clearTyping: () => void;
    setConnectionStatus: (status: ConnectionStatus) => void;
    reset: () => void;
}

const initialState = {
    activeConversationId: null as string | null,
    activeTargetUser: null as TargetUser | null,
    messages: {} as Record<string, ChatMessage[]>,
    typingUsers: new Set<string>(),
    connectionStatus: 'disconnected' as ConnectionStatus,
};

export const useChatStore = create<ChatState>()(set => ({
    ...initialState,

    setActiveConversation: (convId, user) => set({ activeConversationId: convId, activeTargetUser: user ?? null }),

    addMessage: (convId, message) =>
        set(state => ({
            messages: {
                ...state.messages,
                [convId]: [...(state.messages[convId] ?? []), message],
            },
        })),

    updateMessage: (convId, msgId, patch) =>
        set(state => ({
            messages: {
                ...state.messages,
                [convId]: (state.messages[convId] ?? []).map(m => (m.id === msgId ? { ...m, ...patch } : m)),
            },
        })),

    setMessages: (convId, messages) =>
        set(state => ({
            messages: { ...state.messages, [convId]: messages },
        })),

    prependMessages: (convId, messages) =>
        set(state => ({
            messages: {
                ...state.messages,
                [convId]: [...messages, ...(state.messages[convId] ?? [])],
            },
        })),

    setTyping: (userId, isTyping) =>
        set(state => {
            const next = new Set(state.typingUsers);
            if (isTyping) {
                next.add(userId);
            } else {
                next.delete(userId);
            }
            return { typingUsers: next };
        }),

    clearTyping: () => set({ typingUsers: new Set() }),

    // 值未变不写：set 会换掉 state 引用、让所有订阅者重渲染。useStompChat 的 effect 里有一次
    // 「未登录 → disconnected」的写入，无守卫时每次 effect 跑都白刷一遍订阅者（值其实没变）。
    // 返回原 state 对象即被 Zustand 判为无变化而跳过通知。
    setConnectionStatus: status =>
        set(state => (state.connectionStatus === status ? state : { connectionStatus: status })),

    reset: () => set(initialState),
}));
