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
    /** 业务对象类型（后端 MessageBizType code）：决定点击跳商品还是订单，0 表示无跳转 */
    bizType: number;
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
