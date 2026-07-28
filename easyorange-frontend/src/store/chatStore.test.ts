import { beforeEach, describe, expect, it } from 'vitest';
import { useChatStore } from './chatStore';

function createMessage(overrides: Partial<import('@/types/message').ChatMessage> = {}) {
    return {
        id: 'msg-1',
        senderId: 'user-1',
        receiverId: 'user-2',
        content: 'hello',
        type: 'TEXT' as const,
        status: 'SENT' as const,
        createTime: '2026-07-28T10:00:00Z',
        readTime: null,
        recalledAt: null,
        ...overrides,
    };
}

beforeEach(() => {
    useChatStore.getState().reset();
});

describe('chatStore', () => {
    describe('initial state', () => {
        it('starts with no active conversation and disconnected status', () => {
            const state = useChatStore.getState();
            expect(state.activeConversationId).toBeNull();
            expect(state.activeTargetUser).toBeNull();
            expect(state.messages).toEqual({});
            expect(state.typingUsers).toEqual(new Set());
            expect(state.connectionStatus).toBe('disconnected');
        });
    });

    describe('setActiveConversation', () => {
        it('sets active conversation and target user', () => {
            useChatStore.getState().setActiveConversation('conv-1', { id: 'user-2', name: 'Bob', avatar: null });
            const state = useChatStore.getState();
            expect(state.activeConversationId).toBe('conv-1');
            expect(state.activeTargetUser).toEqual({ id: 'user-2', name: 'Bob', avatar: null });
        });

        it('defaults target user to null when omitted', () => {
            useChatStore.getState().setActiveConversation('conv-1');
            expect(useChatStore.getState().activeTargetUser).toBeNull();
        });
    });

    describe('addMessage', () => {
        it('appends a message to the conversation', () => {
            const message = createMessage();
            useChatStore.getState().addMessage('conv-1', message);
            expect(useChatStore.getState().messages['conv-1']).toEqual([message]);
        });

        it('keeps messages for other conversations intact', () => {
            const msgA = createMessage({ id: 'msg-a' });
            const msgB = createMessage({ id: 'msg-b' });
            useChatStore.getState().addMessage('conv-a', msgA);
            useChatStore.getState().addMessage('conv-b', msgB);
            expect(useChatStore.getState().messages['conv-a']).toEqual([msgA]);
            expect(useChatStore.getState().messages['conv-b']).toEqual([msgB]);
        });
    });

    describe('updateMessage', () => {
        it('patches a message by id', () => {
            const message = createMessage({ id: 'msg-1', content: 'original' });
            useChatStore.getState().addMessage('conv-1', message);
            useChatStore.getState().updateMessage('conv-1', 'msg-1', { content: 'updated', status: 'READ' });
            const updated = useChatStore.getState().messages['conv-1'][0];
            expect(updated.content).toBe('updated');
            expect(updated.status).toBe('READ');
            expect(updated.senderId).toBe('user-1');
        });

        it('does nothing when message id is not found', () => {
            const message = createMessage({ id: 'msg-1' });
            useChatStore.getState().addMessage('conv-1', message);
            useChatStore.getState().updateMessage('conv-1', 'msg-missing', { content: 'updated' });
            expect(useChatStore.getState().messages['conv-1'][0].content).toBe('hello');
        });

        it('creates an empty conversation when it does not exist', () => {
            useChatStore.getState().updateMessage('conv-1', 'msg-1', { content: 'updated' });
            expect(useChatStore.getState().messages['conv-1']).toEqual([]);
        });
    });

    describe('setMessages', () => {
        it('replaces conversation messages', () => {
            const msg1 = createMessage({ id: 'msg-1' });
            const msg2 = createMessage({ id: 'msg-2' });
            useChatStore.getState().addMessage('conv-1', msg1);
            useChatStore.getState().setMessages('conv-1', [msg2]);
            expect(useChatStore.getState().messages['conv-1']).toEqual([msg2]);
        });
    });

    describe('prependMessages', () => {
        it('prepends messages while keeping existing ones', () => {
            const oldMsg = createMessage({ id: 'msg-2' });
            const newMsg = createMessage({ id: 'msg-1' });
            useChatStore.getState().addMessage('conv-1', oldMsg);
            useChatStore.getState().prependMessages('conv-1', [newMsg]);
            expect(useChatStore.getState().messages['conv-1']).toEqual([newMsg, oldMsg]);
        });
    });

    describe('setTyping', () => {
        it('adds a typing user', () => {
            useChatStore.getState().setTyping('user-2', true);
            expect(useChatStore.getState().typingUsers.has('user-2')).toBe(true);
        });

        it('removes a typing user', () => {
            useChatStore.getState().setTyping('user-2', true);
            useChatStore.getState().setTyping('user-2', false);
            expect(useChatStore.getState().typingUsers.has('user-2')).toBe(false);
        });
    });

    describe('clearTyping', () => {
        it('clears all typing users', () => {
            useChatStore.getState().setTyping('user-2', true);
            useChatStore.getState().setTyping('user-3', true);
            useChatStore.getState().clearTyping();
            expect(useChatStore.getState().typingUsers.size).toBe(0);
        });
    });

    describe('setConnectionStatus', () => {
        it.each([
            'connected',
            'connecting',
            'disconnected',
            'reconnecting',
        ] as const)('sets connection status to %s', status => {
            useChatStore.getState().setConnectionStatus(status);
            expect(useChatStore.getState().connectionStatus).toBe(status);
        });
    });

    describe('reset', () => {
        it('restores initial state', () => {
            useChatStore.getState().setActiveConversation('conv-1', { id: 'user-2', name: 'Bob', avatar: null });
            useChatStore.getState().addMessage('conv-1', createMessage());
            useChatStore.getState().setTyping('user-2', true);
            useChatStore.getState().setConnectionStatus('connected');

            useChatStore.getState().reset();

            const state = useChatStore.getState();
            expect(state.activeConversationId).toBeNull();
            expect(state.activeTargetUser).toBeNull();
            expect(state.messages).toEqual({});
            expect(state.typingUsers).toEqual(new Set());
            expect(state.connectionStatus).toBe('disconnected');
        });
    });
});
