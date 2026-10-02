/**
 * @fileoverview 后端 API 返回的原始数据类型定义
 * @description 这些类型反映了后端实际返回的数据结构，需要通过 normalize 函数转换为前端类型
 */

import type { ProductStatus } from './product';

/**
 * 后端返回的原始商品数据
 * @description status 是后端下发的状态码（如 'ONLINE'），condition 是数字
 */
export interface RawProduct {
    id: string;
    title?: string;
    description?: string;
    price: number;
    originalPrice?: number | null;
    categoryId: string;
    categoryName?: string;
    condition?: number;
    status?: ProductStatus;
    images?: string[];
    location?: string;
    views?: number;
    sellerId: string;
    sellerName?: string;
    username?: string; // 后端可能返回 username 而非 sellerName
    sellerAvatar?: string | null;
    userAvatar?: string | null; // 后端可能返回 userAvatar 而非 sellerAvatar
    sellerRating?: number;
    createTime?: string;
    updateTime?: string;
    stock?: number;
    contactMethod?: string;
}

/**
 * 后端返回的原始聊天消息数据
 * @description type 和 status 可能缺失，需要设置默认值
 */
export interface RawChatMessage {
    id: string;
    conversationId?: string;
    senderId: string;
    senderAvatar?: string | null;
    receiverId: string;
    content: string;
    title?: string | null;
    /** 后端 MessageType.getCode()，数字型 */
    type?: number | null;
    /** 后端 MessageStatus.getCode()：SENT / RECALLED */
    status?: string | null;
    /** ReadStatus.getCode()：0 未读 / 1 已读。与 status 正交 —— status 只管发送与撤回 */
    isRead?: number;
    createTime: string;
    readTime?: string | null;
    recalledAt?: string | null;
}
