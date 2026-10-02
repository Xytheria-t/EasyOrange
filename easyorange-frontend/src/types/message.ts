export interface ChatSession {
    /** 会话唯一标识是对方 userId（后端 ConversationListVO 无独立会话 id） */
    targetUserId: string;
    targetUserName: string;
    targetUserAvatar: string | null;
    lastMessage: string;
    lastMessageTime: string;
    unreadCount: number;
}

/**
 * 消息类型 —— 后端 MessageType 五档（系统 / 聊天 / 订单 / 支付 / 活动）在本链路都按纯文本渲染，
 * 故只有 TEXT；真要分化渲染时随对应分支一起加。
 */
export type ChatMessageType = 'TEXT';

/** WS 发送协议的消息类型（与后端 MessageType.CHAT 的 code 对齐） */
export const WS_MESSAGE_TYPE_CHAT = 2;

/**
 * 消息状态 ——
 * `SENDING` / `FAILED` 是前端乐观更新的本地态；
 * `SENT` / `READ` / `RECALLED` 来自后端：后端 MessageStatus 只有 SENT 与 RECALLED，
 * READ 由 `is_read` 列单独表达，两者不交叉。
 */
export type ChatMessageStatus = 'SENDING' | 'SENT' | 'READ' | 'FAILED' | 'RECALLED';

export interface ChatMessage {
    id: string;
    senderId: string;
    /** 对方档案：聊天页头部头像取自第一条对方消息，带上就不必再查一次用户档案 */
    senderAvatar: string | null;
    receiverId: string;
    content: string;
    type: ChatMessageType;
    status: ChatMessageStatus;
    createTime: string;
    readTime: string | null;
    recalledAt: string | null;
}

export interface TypingPayload {
    userId: string;
    timestamp: number;
}

export interface RecallPayload {
    messageId: string;
    conversationId: string;
    operatorId: string;
    recalledAt: string;
}
