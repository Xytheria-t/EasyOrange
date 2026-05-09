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
    senderId: number;
    receiverId: number;
    content: string;
    type: 'TEXT' | 'IMAGE' | 'PRODUCT';
    createTime: string;
    readTime: string | null;
}
