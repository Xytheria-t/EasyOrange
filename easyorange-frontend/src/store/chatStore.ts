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

    setConnectionStatus: status => set({ connectionStatus: status }),

    reset: () => set(initialState),
}));
