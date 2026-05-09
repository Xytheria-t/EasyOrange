export interface ChatSession {
    id: string;
    targetUserId: string;
    targetUserName: string;
    targetUserAvatar: string | null;
    lastMessage: string;
    lastMessageTime: string;
    unreadCount: number;
}

export interface ChatMessage {
    id: string;
    senderId: string;
    receiverId: string;
    content: string;
    type: 'TEXT' | 'IMAGE' | 'PRODUCT';
    createTime: string;
    readTime: string | null;
}
