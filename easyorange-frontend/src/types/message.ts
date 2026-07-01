export interface ChatSession {
    id: string;
    targetUserId: string;
    targetUserName: string;
    targetUserAvatar: string | null;
    lastMessage: string;
    lastMessageTime: string;
    unreadCount: number;
}

export type ChatMessageType = 'TEXT' | 'IMAGE' | 'PRODUCT' | 'RECALLED';

export type ChatMessageStatus = 'SENDING' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED' | 'RECALLED';

export interface ChatMessage {
    id: string;
    senderId: string;
    receiverId: string;
    content: string;
    type: ChatMessageType;
    status: ChatMessageStatus;
    createTime: string;
    readTime: string | null;
    recalledAt: string | null;
}

export interface TypingPayload {
    conversationId: string;
    userId: string;
    userName: string;
    timestamp: number;
}

export interface RecallPayload {
    messageId: string;
    conversationId: string;
    operatorId: string;
    recalledAt: string;
}
