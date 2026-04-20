export type MessageType = 'SYSTEM' | 'ORDER' | 'CHAT' | 'ACTIVITY';

export type MessageStatus = 'UNREAD' | 'READ';

export interface Message {
    id: number;
    type: MessageType;
    title: string;
    content: string;
    status: MessageStatus;
    senderId: number | null;
    senderName: string | null;
    senderAvatar: string | null;
    receiverId: number;
    createTime: string;
    readTime: string | null;
    extra: Record<string, unknown> | null;
}

export interface ChatSession {
    id: number;
    targetUserId: number;
    targetUserName: string;
    targetUserAvatar: string | null;
    lastMessage: string;
    lastMessageTime: string;
    unreadCount: number;
}

export interface ChatMessage {
    id: number;
    sessionId: number;
    senderId: number;
    receiverId: number;
    content: string;
    type: 'TEXT' | 'IMAGE' | 'PRODUCT';
    createTime: string;
    status: 'SENDING' | 'SENT' | 'READ' | 'FAILED';
}

export interface SendMessageRequest {
    receiverId: number;
    content: string;
    type?: 'TEXT' | 'IMAGE' | 'PRODUCT';
    productId?: number;
}

export interface MessageQueryParams {
    type?: MessageType;
    status?: MessageStatus;
    current?: number;
    size?: number;
}
