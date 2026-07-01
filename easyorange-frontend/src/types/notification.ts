export interface NotificationItem {
    id: string;
    senderId: string | null;
    senderName: string;
    receiverId: string;
    type: number;
    typeDesc: string;
    title: string;
    content: string;
    isRead: number;
    businessId: string | null;
    createTime: string;
    updateTime: string;
}

export interface UnreadCount {
    total: number;
    systemCount: number;
    chatCount: number;
    orderCount: number;
    paymentCount: number;
    activityCount: number;
}
